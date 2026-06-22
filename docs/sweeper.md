# Sweeper

Sweeper brute-forces any art in `work/` over a grid of inputs, renders them all headless and in
parallel, and tiles each batch into a labelled contact sheet — so you can scan a wall of variants
and fish out the good ones ("find the beauty in the noise"). Everything — inputs *and* run settings
— lives in a **config file**; the only thing you pass on the command line is which config to run.

- Tool: `work/src/sweeper/Sweeper.kt` (`work.sweeper.SweeperKt`)
- Configs: `work/sweeps/<name>.sweep`
- Output: `<out>/<name>.png` + `<name>.txt` per render, `<out>/_sheet[_branch].png` per batch

## Usage

```bash
just sweep <name>           # runs work/sweeps/<name>.sweep
```

`just sweep` builds the module classpath, then invokes the Sweeper. Equivalent to:

```bash
./gradlew :work:classes :work:writeClasspath -q
java @work/build/classpath.txt work.sweeper.SweeperKt work/sweeps/example.sweep
```

There are **no command-line flags** — to change anything (parallelism, sampling, a dry preview…),
edit the config. To preview a run without rendering, set `dry = true` in the file.

## The config file

A `.sweep` file is line-based (`key = value`, `#` comments). Top-level keys configure the run;
`[branch …]` sections define what to sweep.

```ini
art   = work.nervure.NervureKt   # fully-qualified main class (required)
out   = output/example           # output folder
fixed = preset=reach             # params held constant across every branch

[branch fiery]
palette = inferno,magma
seed    = 1:8
agew    = 0.6:1.0:0.2

[branch cool]
palette = turbo,viridis
pull    = 0.2,0.6,0.9
```

`art` is the only required key, and `fixed` holds space-separated `k=v` pairs applied to every
branch. Any *other* top-level `key = spec` line (not a [setting](#settings)) is a **global axis**
swept across all branches.

### Specs

A spec is one of:

| Form               | Meaning                                          | Example                        |
|--------------------|--------------------------------------------------|--------------------------------|
| `key=from:to:step` | inclusive numeric range (step optional for ints) | `seed=1:8`, `pull=0.2:0.8:0.1` |
| `key=a,b,c`        | discrete list — the only way to sweep words      | `palette=plasma,turbo,inferno` |
| `key=value`        | a single fixed value                             | `preset=reach`                 |

Each becomes a `-Dkey=value` JVM property passed to the art.

## Branches

A branch is one **distinct input set**. The art is rendered over the **cartesian product** of a
branch's axes — so 2 palettes × 8 seeds × 3 agew = 48 renders for that branch. You define a handful
of branches (promising directions) instead of one exploding grid, because you can't sweep every
combination of every knob. Each branch gets its own contact sheet. A config with no `[branch]`
sections is a single implicit branch.

## Settings

Optional top-level keys that control the run (everything is in the file — there are no flags):

| Key       | Default            | Effect                                        |
|-----------|--------------------|-----------------------------------------------|
| `out`     | `output`           | output folder                                 |
| `par`     | ≈ half your cores  | parallel renders                              |
| `name`    | the art's short name | filename prefix                             |
| `sheet`   | `on`               | the contact sheet; `off` to skip it           |
| `thumb`   | auto               | contact-sheet thumbnail size, px              |
| `sample`  | —                  | render a random N of the grid                 |
| `limit`   | —                  | render the first N of the grid                |
| `timeout` | `180`              | per-render timeout, seconds                   |
| `yes`     | `false`            | proceed when the grid exceeds 500 renders     |
| `dry`     | `false`            | print the plan and render nothing             |

Grids over **500** renders need `yes = true` (a guard against an accidental 10k-render run); trim
with `sample` / `limit`.

## Output

Per render:

- `<name>.png` — the image. `<name>` is `<prefix>_<branch>_<index>__<swept-params>`, e.g.
  `nervure_fiery_007_palette-magma_seed-1_agew-0.6.png`.
- `<name>.txt` — the **full resolved parameter set** (every knob, defaults included) plus a
  one-line reproduce command.

Per batch:

- `_sheet_<branch>.png` — a labelled grid of every image in the branch (the "big table"), rendered
  with Skia. Set `sheet = off` to skip it, `thumb = PX` to resize the cells.

## The taste loop (`continue`)

The workflow is manual-first: render a batch, review the sheet, **keep** the good ones, then branch
a tight neighbourhood around them. No scoring heuristics — you build the taste, the tool explores
around it.

1. Run a sweep, then move the keepers (the `.png` **and** their `.txt`) into a `keep/` folder.
2. Write a continue config and run it:

```ini
# work/sweeps/refine.sweep
continue = output/example/keep
out      = output/refine
vary     = pull,curl,agew     # numeric knobs to nudge
spread   = 0.2                # ± fraction
steps    = 3                  # points per knob
```

```bash
just sweep refine
```

For each survivor it reads the full params from the `.txt`, sweeps `±spread` around each `vary`
knob (`steps` points each, cartesian), holds everything else, and writes one contact sheet per
survivor. Because the `.txt` holds the *full* resolved set, you can vary **any** numeric knob — even
one that wasn't in the original sweep. `seed` is excluded by default (a different seed is a
different organism, not a neighbour). The [settings](#settings) above (`par`, `sample`, `dry`, …)
work in a continue config too.

## How it works

1. The Sweeper expands the config into tasks (one per cartesian combo per branch) and gives each a
   unique name.
2. Each task runs as its own **headless JVM subprocess** —
   `java -cp … -Dgart.headless=true -Dparams.out=… -D<knob>=<val> … <MainClass> --render` — in a
   fresh temp working directory, with a timeout.
3. The art renders in-memory (no window) and saves its PNG. The Sweeper prefers `-Dout`, but also
   recovers a PNG the art saved into its working directory, so even arts that ignore `-Dout` still
   get the unique name.
4. `dev.oblac.gart.io`'s `pi/pf/ps` record every resolved knob; with `-Dparams.out` set they dump
   the full set on JVM exit, which becomes the render's `.txt`.
5. Surviving images are tiled into per-branch contact sheets via Skia (`Surface` + gart's bundled
   fonts).

## The art's side of the contract

Any art works with the Sweeper if it:

- reads its knobs from `-D` system properties — ideally via `dev.oblac.gart.io` `pi/pf/ps`, which
  also gets you the full-param `.txt` for free;
- goes **headless** with `--render` or `-Dgart.headless` (skip `window().showImage(...)`);
- ideally honours `-Dout=<path>` for the output filename (`gart.saveImage(g, "$out.png")`).

`work.nervure.NervureKt` follows this contract and is the reference.

## Tips

- Start coarse (wide ranges, few steps) to find regions, then `continue` with a small `spread` to
  refine.
- Use lists for structural knobs (`palette`, `preset`) and ranges for continuous ones.
- Set `dry = true` first when a grid might be large — it prints the combo count and sample names.
- Keep each sweep's `out` per-name (`out = output/<name>`) so batches don't collide.
- The `.txt` beside any image is a complete recipe; its `# reproduce:` line re-renders just that one.
