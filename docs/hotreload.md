# Hot Reload

Gart supports hot reload via `GartLauncher`, which watches for `.class` file changes
and re-invokes the art's `main()` with a fresh classloader. The existing Swing window
is _reused automatically_ (no flicker).

## Usage

```bash
just dev <module> <main-class>
```

`<module>` is the Gradle project path with `:` separators (`work`, `arts:sea`).
`<main-class>` is the file's facade class: the package, the file name and a `Kt` suffix.
Note it follows the `package` line, not the module path.

```bash
just dev arts:sea unda.UndaKt
just dev arts:flowforce flowforce.monolith.MonolithKt
just dev arts:pixelmania pixelmania.cosmic.CosmicTopoKt
```

A piece you keep coming back to gets a one-line shortcut recipe in the `justfile`:

```just
# Dev session for unda.
[group('dev')]
dev-unda: (dev "arts:sea" "unda.UndaKt")
```

so `just dev-unda` does the same thing. Existing shortcuts: `dev-unda`, `dev-monolith`,
`dev-cosmic` (`just --list` shows them under `dev`).

This starts a tmux session with two panes:

- **left**: Gradle continuous compilation (watches for source changes)
- **right**: `GartLauncher` (watches for `.class` changes, re-runs `main()`)

## What the art needs to do

Nothing special. Write `main()` the normal way: draw onto a gartvas and hand it to the
window with `gart.window().showImage(g)`, or `gart.window().show { c, d, f -> ... }` for
animation. On reload the launcher runs `main()` again and `Window.show()` swaps the new
draw into the existing window.

Subclassing `Drawing` (`private class MyDraw(g: Gartvas) : Drawing(g)`) was the previous
hot-reload mechanism. It still works but is not needed; don't use it in new pieces.

## How it works

1. You edit a `.kt` file and save.
2. Gradle detects the change and recompiles.
3. `FileWatcher` detects new `.class` files, debounces (150ms), then triggers reload.
4. `GartLauncher` creates a fresh `URLClassLoader` and invokes `main()` on a new thread.
5. `Window.show()` detects the existing window via `ActiveWindow` and reuses it, swapping the new draw frame in.

## Tips

- The reload time equals the `main()` execution time. Keep expensive computations
  (JFA, large noise fields) minimal during iteration, then restore for the final render.
- The window is positioned with `-Dgart.align=right` by default (set in `justfile`).
- Use `just dev-stop` to kill the tmux session.
- JVM flags `-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xverify:none` are used to
  minimize startup overhead.
