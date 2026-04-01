# Hot Reload

Gart supports hot reload via `GartLauncher`, which watches for `.class` file changes
and re-invokes the art's `main()` with a fresh classloader. The existing Swing window
is _reused automatically_ (no flicker).

## Usage

```bash
just dev <module> <main-class>
```

Example:

```bash
just dev work dev.oblac.gart.cosmic.CosmicTopoKt
```

This starts a tmux session with two panes:

- **left**: Gradle continuous compilation (watches for source changes)
- **right**: `GartLauncher` (watches for `.class` changes, re-runs `main()`)

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
