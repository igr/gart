# gart — framework reference & catalogue

A working catalogue of the `gart` generative-art framework (Kotlin / JVM 21 + Skia via skiko): the key classes, palettes, effects, algorithms, and the idioms for assembling them into a piece. Built to make future work fast — *skim the relevant section before reaching for a primitive instead of re-reading source.*

> **Provenance:** generated 2026-06-20 by cataloguing the real source under `gart/src/main/kotlin/dev/oblac/gart/` (~330 files, 48 packages). Signatures were read from source, but APIs drift — for an exact arg order on a hot path, glance at the cited file. Sections 1–8 are the inventory; §0 and §9 are the how-to.

## Table of contents

- [§0 Quick start — anatomy of a piece](#0-quick-start--anatomy-of-a-piece)
- [§1 App & Canvas Core](#1-app--canvas-core)
- [§2 Color & Palettes](#2-color--palettes)
- [§3 Noise & Flow Fields](#3-noise--flow-fields)
- [§4 Math, Angles, Vectors, Matrices](#4-math-angles-vectors-matrices)
- [§5 Geometry, Paths & Curves](#5-geometry-paths--curves)
- [§6 Paints, Effects & Shaders](#6-paints-effects--shaders)
- [§7 Simulations & Generative Engines](#7-simulations--generative-engines)
- [§8 Spatial Structures, Point Algorithms & 3D](#8-spatial-structures-point-algorithms--3d)
- [§9 Idioms & worked examples](#9-idioms--worked-examples)

---

## 0. Quick start — anatomy of a piece

Every piece is a **self-contained `main()`** in its own file, with all knobs as top-of-file constants and a doc-comment stating the concept. Finished pieces live under `arts/<module>/src/<name>/`; experiments under `work/src/<name>/`.

### Canonical skeleton (from `work/src/nervure/Nervure.kt`, `work/src/corona/Corona.kt`)

```kotlin
package work.foo

import dev.oblac.gart.Gart
// ... engine + render imports ...

private const val W = 1200
private const val H = 1200
// ... every tunable as a top-of-file constant ...

fun main() {
    val seed = System.getProperty("seed")?.toLong() ?: DEFAULT_SEED
    val out = System.getProperty("out") ?: "work/foo.png"
    val headless = System.getProperty("headless") != null

    val gart = Gart.of("foo", W, H)   // name drives default save filename
    val g = gart.gartvas()            // Gartvas: the drawable surface; g.canvas, g.d
    val c = g.canvas

    // ... build a deterministic engine (seeded), then draw to c ...

    gart.saveImage(g, out)            // writes File(out) relative to CWD (repo root)
    if (!headless) gart.window().showImage(g)
}
```

### Build & run

```bash
# headless one-shot (used for batch renders, sweeps, CI-style verification)
./gradlew :<module>:classes :<module>:writeClasspath -q
java -Dheadless [-Dseed=N] [-Dout=work/foo.png] @<module>/build/classpath.txt <pkg>.<File>Kt
# e.g. java -Dheadless @work/build/classpath.txt work.corona.CoronaKt

# interactive hot-reload dev session (tmux + Skia window, recompiles on save)
just dev <module> <pkg>.<File>Kt          # e.g. just dev work work.corona.CoronaKt
just dev arts:flowforce monolith.MonolithKt   # nested module uses ':' form
just dev-stop
```

- `writeClasspath` (root `build.gradle.kts`, `allprojects`) writes `-cp\n<module classes>:<runtime jars>` to `<module>/build/classpath.txt` — the `@file` arg form for `java`. `writeLauncherClasspath` writes deps-only (no module classes), used by the hot-reload launcher.
- `gart.saveImage(...)` ultimately does `File(name).writeBytes(...)` relative to the **current working directory** (the repo root when run as above). So save to `"work/foo.png"` to land in the tracked location next to the source.

### Conventions

- **One self-contained engine per file.** A `main()`, an engine class, render functions, and constants. No shared piece-to-piece state.
- **Determinism is locked deliberately** — the *art* is choosing the seed/params. Patterns in use: a fixed RNG (`Random(47)` in rugae), a locked seed (`9` in nervure), or a curated parameter table indexed by `-Dseed` with a locked default (`4` in corona). Sweep many, hand-pick the striking one.
- **One commit per piece**, commit message = the piece name (e.g. `corona`). Pieces are committed directly to `main`.
- **Diagnostics via `println`** are normal in pieces (e.g. `println(gart)`, then engine stats).

### Environment quirks (this machine)

- **GPG signing is broken** (`gpg` not on PATH): commit with `git -c commit.gpgsign=false commit -m "..."`.
- **Fish is the shell.** Two recurring traps: `for ... end` loops with pipes inside the body can fail to parse → wrap batch loops in `bash -c '...'`; and an unmatched glob *aborts* the command → use `find … -delete` / `rg -g '*.kt'` instead of bare `*.png` globs.
- **ImageMagick** needs a font even for empty labels — build contact sheets with `magick … -resize WxH +append` (a row) then `… -append` (stack rows) rather than `montage`.

### Source quirks worth knowing (found while cataloguing)

- There is **no `clamp(...)` helper** — use Kotlin stdlib `coerceIn`. (`math/clamp.kt` actually defines `cond(b,x,y)`.)
- The 2×2 matrix class is **`Matrix22`** (not `Matrix2`); `matrix33.kt` only adds a `multiply` extension to Skia's `Matrix33`.
- No Worley/curl noise exists; closest are value `noise(x)` / `cellnoise` and `NoiseColor` (a Skia fractal/turbulence shader).
- `Pixels` exposes `set(offset, Int)` and `set(x, y, Int)` but **no `get(offset)`** — read a pixel buffer back via `map.pixels[i]`.
## 1. App & Canvas Core

The framework is built on Kotlin + Skia (skiko). An art piece typically does: `Gart.of(...)` → make a `Gartvas` (Skia canvas) → draw → either `show()` in a `Window` or `saveImage(...)` / record a `Movie`. Pixel-level work goes through `Gartmap` / `Pixels`.

All file paths are relative to the repo root; sources live under `gart/src/main/kotlin/dev/oblac/gart/`.

### Entry point ⭐

**`Gart`** — `data class Gart(name: String, d: Dimension, fps: Int)` — the app handle / factory for everything else. `Gart.kt`

- `companion fun of(name: String, width: Number, height: Number, fps: Int = 60): Gart` — primary constructor; builds `Dimension(width.toInt(), height.toInt())`. ⭐
- `companion fun of(name: String, d: Dimension, fps: Int = 60): Gart` — variant taking a pre-made `Dimension`.
- `fun gartvas(dimension: Dimension = this.d): Gartvas` — new Skia canvas at the gart's (or given) dimension.
- `fun gartmap(gartvas: Gartvas): Gartmap` — pixel buffer bound to a canvas.
- `fun gg(): GartGG` — bundles `this` + a fresh `Gartvas` (convenience aggregate).
- `fun dimension(width: Int, height: Int): Dimension`
- `fun window(d = this.d, fps = this.fps, printFps = true): Window` — interactive window.
- `fun fullScreenWindow(d = this.d, fps = this.fps): Window` — exclusive full-screen window.
- `fun movie(d = this.d, name = "$name.mp4"): Movie` — MP4 recorder buffer.
- `fun movieGif(d = this.d, name = "$name.gif"): Movie` — GIF recorder buffer.
- `fun saveImage(gartvas: Gartvas, name = "$name.png")` — save canvas to file (see Saving). ⭐
- `fun saveImage(canvas: Canvas, d = this.d, name = "$name.png")` / `fun saveImage(image: Image, name = "$name.png")` — overloads.
- `fun saveMovie(movie: Movie, fps = this.fps, name = movie.name)` — encode buffered frames to disk.
- `fun snapshot(): GartSnapshot` — frame-capture helper for view loops.
- `fun rand(reply: Boolean): GartRand` — seeded/replayable RNG keyed on the gart name.

### Dimensions

**`Dimension`** — `data class Dimension(w: Int, h: Int)` — integer canvas size with many precomputed geometry helpers. `Dimension.kt`

- Float/double views: `wf`, `hf`, `wd`, `hd`, `width`, `height`, `aspectRatio`, `area`, `diag`.
- Corner/edge points: `leftTop`, `rightBottom`, `leftBottom`, `rightTop`, `leftMiddle`, `rightMiddle`, `center`; edge indices `r`/`rf` (right = w-1), `b`/`bf` (bottom = h-1); `rect: Rect`.
- Centers & thirds: `cx`, `cy`, `w3`, `w3x2`, `h3`, `h3x2`.
- `fun isInside(x: Float, y: Float): Boolean`; `fun forEach(step = 1, (x,y)->Unit)` (alias `loop`); `fun ofW(x)/ofH(y)`, `fun normW/normH`, `fun min()`.
- `operator fun times(factor: Number): Dimension` — scale.
- Companion: `DESKTOP_FULL_HD (1920×1080)`, `DESKTOP_FULL__LANDSCAPE_HD (1080×1920)`, `LAPTOP_FULL_HD (1366×768)`, `fun of(w: Float, h: Float)`.

`Screen` (`Screen.kt`) — `object Screen { fun resolution(): Dimension; fun dimension(): Dimension }` — host display size.

### Canvas surface ⭐

**`Gartvas`** — `class Gartvas(val d: Dimension)` — the in-memory **Skia raster surface + Canvas**; this is what you draw on. `Gartvas.kt`

- `val canvas: Canvas` — the Skia `Canvas` to issue draw calls against.
- `internal val surface` — `Surface.makeRasterN32Premul(w, h)`.
- `fun snapshot(): Image` / `fun snapshot(rect: Rect): Image?` — capture current pixels as a Skia `Image`.
- `fun draw(draw: Draw)` / `fun draw(draw: Drawing)` — invoke a draw lambda against `canvas` (the `Drawing` form supplies a `FrameCounter(60)`).
- `fun createBitmap(): Bitmap` — N32-premul bitmap matching the surface; `fun writeBitmap(bitmap: Bitmap)` — push bitmap pixels back into the surface.
- `fun snapshotTo(c: Canvas)` — draw this surface's snapshot onto another canvas at (0,0).
- `fun sprite(): Sprite` — snapshot as a transformable `Sprite`.
- Companion: `fun of(width: Number, height: Number): Gartvas`.

**`GartGG`** — `data class GartGG(gart: Gart, g: Gartvas)` with `saveImage()` and `showImage()` — terse "render once + show/save" aggregate. `GartGG.kt`

### Drawing callbacks

`Draw.kt` defines the lambda contracts used everywhere:

- `fun interface Draw { operator fun invoke(canvas: Canvas, dimension: Dimension) }` — static draw.
- `fun interface DrawFrame { operator fun invoke(canvas: Canvas, dimension: Dimension, frames: Frames) }` — per-frame draw (the animation callback). ⭐
- `abstract class Drawing(g: Gartvas? = null) : DrawFrame` — base class with `open fun draw(canvas, dimension, frames)`; after `draw` it blits `g`'s snapshot if `g != null`. Used mainly for hot-reload.

### Pixel buffers ⭐

**`Pixels`** — `interface Pixels { val pixels: IntArray; val d: Dimension }` — single source of truth is an ARGB-packed `IntArray` indexed `y * d.w + x`. `Pixels.kt`

- `operator fun get(x,y): Int`; `operator set(x,y, Int|Long)`; `operator set(offset, Int|Long)`.
- `fun setBlock(x,y,pixelSize,color: Int)` / `(... color: RGBA)` — fill a square block (edge-clipped).
- `fun calcAverageBlockColor(x,y,pixelSize): Int`; `fun addBlockColor(x,y,pixelSize, deltaR,deltaG,deltaB)`.
- `fun fill(color: Int)` — bulk fill; `fun copyPixelsFrom(other: Pixels)` — same-size copy.
- `fun row(y): IntArray` / `fun row(y, row)`; `fun column(x): IntArray` / `fun column(x, column)`.
- `fun sampleNearest(px,py, mode: SampleMode, background: Int): Int`; `fun sampleBilinear(fx,fy, mode, background): Int` — out-of-bounds handled per `SampleMode`.
- `inline fun Pixels.forEach((x,y,color)->Unit)` — fast row-major iteration (inlined; ~13× faster than the old default).
- `enum class SampleMode { CLAMP, TILE, BACKGROUND }`; `data class Pixel(x: Int, y: Int)`.
- `class MemPixels(d: Dimension) : Pixels` — pure in-memory buffer, no Skia binding.

**`Gartmap`** — `class Gartmap : Pixels, AutoCloseable` — bridges an `IntArray` of ARGB pixels to/from a Skia `Gartvas`. `Gartmap.kt` ⭐

Two construction modes (primary ctor is private):
- `constructor(d: Dimension)` — pure in-memory, **no** canvas binding (accumulation buffer / procedural generation).
- `constructor(gartvas: Gartvas)` — bound to a canvas; eagerly calls `updatePixelsFromCanvas()` at construction.

How pixels move between the `IntArray` and the canvas:
- `override val pixels: IntArray` — the live ARGB buffer you read/write via `Pixels` `get`/`set`.
- `fun updatePixelsFromCanvas()` — **pull**: reads the bound surface's pixels into `pixels` (errors if no `Gartvas` bound).
- `fun drawToCanvas(target: Gartvas? = gartvas)` — **push**: writes `pixels` back into the bound (or given) surface; errors if neither is available.
- `fun image(): Image` — returns a Skia `Image` snapshot of the current `pixels` (no binding required). The image is **owned by this Gartmap** and released on the next `image()`/`close()`; do not retain it across frames or request two live ones at once.
- `override fun close()` — releases the cached `image()` and backing bitmap (idempotent); `Gartmap` is `AutoCloseable`.
- Exposed: `w/wf/h/hf`, `val bitmap: Bitmap`. Internally keeps a reused little-endian byte buffer so per-frame Skia round-trips don't reallocate.

> Difference: **`Gartvas`** = the Skia drawing surface (vector/raster draw ops via `canvas`). **`Gartmap`** = a CPU-side `IntArray` of pixels for per-pixel manipulation; it can be detached (in-memory) or attached to a `Gartvas` and explicitly synced with `updatePixelsFromCanvas()` (pull) / `drawToCanvas()` (push).

### Pixel operations (`pixels/` package)

Free functions operating on `Pixels` / `Gartmap`:

| Function | Signature (key params) | Source |
|---|---|---|
| `createScaledPixels` | `(input: Pixels, newD: Dimension): Pixels` | `pixels/scalePixels.kt` |
| `floodFill` | `(m: Gartmap, start: Pixel, fillColor: Int, shouldFill: (Int)->Float)` | `pixels/floodFill.kt` |
| `matchExactColor` / `matchNotColor` / `matchSimilarColor` | predicate builders returning `(Int)->Float` (e.g. `matchSimilarColor(target, tolerance=30)`) | `pixels/floodFill.kt` |
| `applyGaussianBlur` | `(b: Gartmap)` | `pixels/gaussianBlur.kt` |
| `applyMotionBlur` | `(b: Gartmap, distance: Int, angle: Angle)` | `pixels/motionBlur.kt` |
| `makeGray` | `(bitmap: Pixels)` | `pixels/gray.kt` |
| `liquify` | `(target: Pixels, circle: Circle, growFactor=0.7f)` | `pixels/liquify.kt` |
| `conformalWarp` | `(src: Gartmap, outDimension=src.d, rInner=0.5, rOuter=2.0, unitPixels=…, sampleMode=TILE, background, bilinear=true): Gartmap` | `pixels/conformalWarp.kt` |
| `pixelSorter` / `pixelSorterRow` | `(bitmap: Pixels, threshold=100, sortValueOf: (Int)->Int)` | `pixels/pixelSorting.kt` |
| `drawBlackWhiteMoire` | `(b: Pixels, p1: Point, p2: Point, width=32)` | `pixels/moire.kt` |
| `Pixels.scrollUp` | `(delta: Int)` ext. | `pixels/pixelsScroller.kt` |
| `Int.roundToNearestQuantization` | `(stepSize: Int): Int` ext. | `pixels/pixelColors.kt` |

`pixels/ImageFilters.kt` provides `DoubleArray`-based filter kernels: `gaussianBlur`, `uniformFilter`, `laplacianFilter`, `maximumFilter`, `minimumFilter`, `adaptiveMedianFilter`, plus `enum class PadMode`.

### Sprites

**`Sprite`** — `class Sprite(surface: Surface)` — an immutable image snapshot you can crop and transform. `Sprite.kt`

- `val image: Image`, `val d: Dimension`.
- `fun cropRect(x,y,width,height): Sprite`; `fun cropTriangle(p: Point, size: Float, angle: Angle = Degrees.ZERO): Sprite`.
- `fun draw(): SpriteTransformations`; companion `fun of(gartvas: Gartvas): Sprite`.
- `fun Canvas.drawSprite(sprite, (SpriteTransformations)->SpriteTransformations)` — extension to place a sprite.
- `data class SpriteTransformations` — fluent transforms: `rotate(deg[, x, y])` / `rotateRB` / `rotateLB`, `translate`/`right`/`left`/`down`, `flipHorizontal`/`flipVertical`, `scaleX`, `at(x,y)`, terminal `draw(canvas)`.

### Window & interaction

**`Window`** — `open class Window(d: Dimension, fps: Int, printFps, fullScreen=false)` — a Swing `JFrame` hosting a Skia render loop. `Window.kt`

- `fun showImage(gartvas: Gartvas | image: Image | image: () -> Image): WindowView` — display a static image.
- `fun show(g: Gartvas): WindowView` — repaint a canvas each frame.
- `open fun show(drawFrame: DrawFrame): WindowView` — **the main animation entry**: runs the `DrawFrame` on every repaint. Supports hot-reuse of an existing window with matching dimensions via `ActiveWindow`. ⭐
- `fun close()`; `protected open fun onClose()` (runs `onCloseHandlers` in reverse).

**`WindowView`** — `class WindowView` — fluent handle returned by `show*`. `Window.kt`

- `fun onKey((Key)->Unit): WindowView`; `fun onMouse((MouseEvent)->Unit): WindowView`; `fun onMouseMotion((MouseEvent)->Unit): WindowView`.
- `fun skipTo(frame: Long)`; `fun resetFrames()`; `fun onClose(()->Unit)`.

Supporting types: `GartView : SkikoRenderDelegate` (`GartView.kt`) drives `onRender` → `fpsGuard.withFps(nanoTime)` then `drawFrame(canvas, d, frames)`; `ActiveWindow` (`ActiveWindow.kt`) holds the reusable frame/view for hot-reload; `enum class Key(platformKeyCode: Int)` (`Key.kt`); `object KeyHandlers { val showKey }`.

### Animation / frame loop ⭐

**`Frames`** — `interface Frames` — the per-frame clock passed into every `DrawFrame`. `Frames.kt`

- Constants: `val fps: Int`, `val frameDurationNanos: Long`, `val frameDurationSeconds: Float`.
- State: `val frame: Long` (elapsed frame count), `val new: Boolean` (true exactly once per real frame, ~`fps` times/sec), `val time: Long` (elapsed ns), derived `frameTime`, `frameTimeSeconds`, `timeSeconds`.
- Frame hooks (all gated on `new`): `fun tick(cb)`, `fun onFrame(target, cb)`, `fun onBeforeFrame`, `fun onAfterFrame`, `fun onEveryFrame(target, cb)` (modulo). `fun print()` debug line.
- Companion `Frames.ZERO` (all-zero no-op instance).
- `internal class FrameCounter(fps) : Frames` — manual counter (used by `Gartvas.draw(Drawing)` and `Movie`); `internal class FpsGuard(fps, printFps)` — paces the render loop to target fps and tracks real vs max FPS.

Time helpers (`time.kt`): `Duration.toSeconds(): Float`, `Duration.toFrames(fps): Long`, `Long.toTime(frameTimeNanos): Duration`.

### Movie / recording

**`Movie`** — `class Movie(d, name, startRecording=true, format=MovieFormat.MP4)` — a buffer of snapshot `Image`s. `Movie.kt`

- `fun addFrame(gartvas: Gartvas)` / `fun addFrame(canvas: Canvas)` — capture a frame (no-op when not recording).
- `fun startRecording()` / `fun stopRecording()`; `fun totalFrames(): Int`; `operator fun get(index): Image`; `fun forEachFrame((Int, Image)->Unit)`.
- `fun record(window: Window, recording = this.recording): Window` — wraps a `Window` so each `new` frame is auto-captured, and saves on window close. ⭐

`enum class MovieFormat { MP4, GIF }` (`media.kt`).

### Saving (output to disk)

All saving is in `media.kt`; files are written via `java.io.File(name)`, so `name` is resolved **relative to the current working directory (CWD)**.

- `fun saveImageToFile(image: Image, name: String)` — picks `EncodedImageFormat` from the file extension (defaults to PNG), encodes, writes bytes, prints `"Image saved: $name"`. This is what `gart.saveImage(...)` ultimately calls.
- `fun saveImageToFile(canvas: Canvas, d: Dimension, name: String)` — round-trips canvas → bitmap → snapshot → file.
- `internal fun saveImageToFile(gartvas: Gartvas, name)` — snapshot then save.
- `internal fun saveMovieToFile(movie: Movie, fps: Int, name: String)` — dispatches to MP4 or GIF encoder per `movie.format`, sets `movie.saved = true`.

> `gart.saveImage(gartvas)` flow: `Gart.saveImage` → `saveImageToFile(gartvas, "$name.png")` → `saveImageToFile(image, name)` → `image.encodeToData(format).bytes` → `File(name).writeBytes(...)`. So a piece named `"foo"` writes `foo.png` into the process CWD.

**GIF**: `class GifSequenceWriter(out: ImageOutputStream, imageType: Int, delay: Int, loop: Boolean)` with `writeToSequence(img: RenderedImage)` and `close()` — `gif/GifSequenceWriter.kt`. Driven by `saveGifToFile` (delay = `1000/fps`).

**MP4**: `class VideoRecorder(width, height, framesPerSecond=25)` — H.264 via JavaCPP/FFmpeg, encoding to an in-memory buffer. `video/VideoRecorder.kt`
- `fun writeFrame(pixels: ByteArray)` (RGBA bytes), `fun finish(): ByteArray`, `fun close()`. Driven by `saveMp4ToFile`.

### Snapshots & determinism

- **`GartSnapshot`** — `class GartSnapshot(gart: Gart)` (`GartSnapshot.kt`) — capture/replay the canvas state inside a view loop: `fun freeze(targetCanvas: Canvas)`, `fun draw(targetCanvas: Canvas)`, `fun get(): Gartvas?`, `fun isCaptured(): Boolean`, `fun saveImage()`.
- **`GartRand`** — `class GartRand(name, replay=false)` (`GartRand.kt`) — record/replay RNG for reproducible art: `fun f(min=0f, max=1f): Float`, `fun b(): Boolean`, `fun save()`. Replay reads `"$name.rand"`.

### Hot reload (brief)

`hotreload/` enables live editing without restarting the window. `GartLauncher` (`hotreload/GartLauncher.kt`, has its own `main(args)` — `<classes-dir> <main-class>`) runs an art project's `main()` in an isolated `URLClassLoader`, watches `.class` files for changes, and re-invokes `main()` with a fresh classloader; the existing Swing window is reused (the `DrawFrame` is swapped via `ActiveWindow`, so no flicker). `FileWatcher` (`hotreload/FileWatcher.kt`) does the actual filesystem watching (`start()` / `stop()`).
## 2. Color & Palettes

All in `gart/src/main/kotlin/dev/oblac/gart/color/`. Colors are plain ARGB `Int`s (alpha in the high byte). Skia interop uses `org.jetbrains.skia.Color4f`. `0xFF……` long literals are common because the int form would overflow.

### Palette API

`Palette` — `Palette.kt`. An ordered, fixed `IntArray` of ARGB colors.

Constructors:
- `Palette(internal val colors: IntArray)` — primary, **`internal`**; not callable from other modules.
- `Palette(vararg values: Long)` — public; the usual way to build from `0xFF……` literals (each `.toInt()`).
- Companion `Palette.of(...)` overloads:
  - `of(values: Collection<Int>)`
  - `of(vararg values: Int)`
  - `of(vararg values: Color4f)` (via `Color4f.toColor()`)
  - `of(vararg values: java.awt.Color)` (note: maps `rgb(red, blue, green)` — channels swapped in source)
  - `of(vararg values: String)` — parses hex strings via `parseColor()`

Properties: `size: Int`, `indices: IntRange`.

Methods (signature + one-liner):
- `get(position: Int): Int` / `at(position): Int` — direct index (operator `[]` = `get`); throws if out of range.
- ⭐ `safe(position: Number): Int` — modulo wrap: `colors[abs(position.toInt()) % size]`; never throws.
- `bound(position: Number): Int` — clamps index to `0..size-1`.
- `relative(offset: Float): Int` — maps `offset` (0..1-ish) onto the palette: `colors[(offset*size).toInt() % size]`.
- `last(): Int` — last color.
- `random(): Int` — random color.
- `randomExclude(vararg color: Int): Int` — random color excluding the given ones; throws if none remain.
- `plus(other: Palette): Palette` (operator `+`) — concatenate two palettes.
- `plus(color: Int): Palette` (operator `+`) — append one color.
- `rem(index: Int)` (operator `%`) — alias for `safe(index)`.
- `map(transform: (Int) -> R): List<R>` — map over colors.
- `reversed(): Palette` — reversed copy.
- ⭐ `expand(steps: Int): Palette` — grows palette to **exactly `steps` colors** by inserting RGB gradients between each adjacent pair (uses `Palettes.gradient`); throws `IllegalStateException` if it can't hit `steps`. Pairs well with `safe`/`relative` for smooth lookups.
- `sequence(): Sequence<Int>` — colors as a `Sequence`.
- `expandReversed(): Palette` — `this + this.reversed()` (palindrome, good for looping gradients).
- `shuffle(): Palette` — shuffled copy.
- `split(numberOfSplits: Int): Array<Palette>` — splits into N sub-palettes (last gets remainder).
- `splitIn(numberOfPalettes: Int): List<Palette>` — same as `split` but returns a `List`.
- `shifted(steps: Int): Palette` — rotates colors by `steps` (mod size).
- `removeLast(): Palette` — drops the last color.
- `toIntArray(): IntArray` — defensive clone of the backing array.

### Color helpers (`color.kt`)

Channel extraction (Int, 0..255):
- `alpha(color: Int): Int`, `red(color: Int): Int`, `green(color: Int): Int`, `blue(color: Int): Int`.
- Float variants (0..1): `alphaf`, `redf`, `greenf`, `bluef`.

Construction:
- `rgb(r: Int, g: Int, b: Int): Int` — opaque ARGB (alpha 0xFF).
- `argb(a: Int, r: Int, g: Int, b: Int): Int` — int channels.
- `argb(af: Float, rf: Float, gf: Float, bf: Float): Int` — float channels (coerced to 0..255).

Channel mutation (return a new color):
- `alpha(color: Int, a: Int): Int` (`@JvmName("setAlpha")`) + extension `Int.alpha(a: Int)` — replace alpha.
- `red(color, r)`, `green(color, g)`, `blue(color, b)` — replace a channel.

Conversions:
- `Int.covertARGBtoRGBA(): Int` / `Int.convertRGBAtoARGB(): Int` (note the typo "covert" in the ARGB→RGBA name).
- `Long.toIntColor(): Int` — `Long` literal → opaque int color (forces alpha 255); used by RetroColors/MidCenturyColors.
- `Int.toFillPaint(): Paint` / `Int.toStrokePaint(width: Float): Paint` — quick Skia paints.

Blending / mixing:
- ⭐ `lerpColor(from: Int, to: Int, t: Float): Int` — linear ARGB interpolation, `t` coerced to 0..1.
- `blendColors(front: Int, back: Int): Int` — alpha-aware Porter-Duff SRC_OVER, integer math.
- `blendDarken(existing: Int, new: Int): Int` — per-channel min (darken) with alpha compositing.

Parsing / distance:
- `String.parseColor(): Int` — parses `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB`, `0xAARRGGBB`; throws otherwise.
- `String.parseColor4f(): Color4f` — same, as `Color4f`.
- `colorDistance(c1: Int, c2: Int): Int` — Chebyshev (max abs per-channel RGB) distance.

### Gradients (`gradient.kt`)

- ⭐ `gradientOf(colors: IntArray, positions: FloatArray? = null): Gradient`
- `gradientOf(colors: Array<Color4f>, positions: FloatArray? = null): Gradient`

Both wrap Skia `org.jetbrains.skia.Gradient` with `Gradient.Colors(...)` and `FilterTileMode.CLAMP`. `positions` are the optional 0..1 color stops. The resulting `Gradient` is the input to Skia's `Shader.makeLinearGradient` / `makeRadialGradient` / `makeSweepGradient` (e.g. `Shader.makeLinearGradient(x0, y0, x1, y1, gradient)`).

Note: a separate, lower-level gradient generator is `Palettes.gradient(colorFrom, colorTo, steps): Palette` (in `Palettes.kt`) — produces a `steps`-length `Palette` interpolating two colors in ARGB; this is what `Palette.expand` uses.

#### Color ramp (`colorRamp.kt`)
- `ColorStop(color: Int, threshold: Float)` — one stop (data class).
- `ColorRamp(stops: List<ColorStop>)` — maps a scalar 0..1 → ARGB via `colorAt(value: Float): Int` (linear ARGB blend between bracketing stops, clamps at ends).
- `ColorRamp.Default` — black→teal→magenta→pink→cyan at 0/.2/.4/.6/.8.
- `ColorRamp.of(palette: Palette): ColorRamp` — evenly spread a palette's colors across 0..1.

#### Color matrices (`colorMatrix.kt`)
`object ColorMatrices` builds Skia `ColorMatrix` filters: `saturation`, `brightness`, `contrast`, `grayscale`, `invert`, `swap(colorA, colorB)`, `sepia`, `hueRotate(degrees)`. Plus `ColorMatrix.concat(other)` and `ColorMatrix.toColorFilter(): ColorFilter`.

#### Palette generation (`PaletteGenerator.kt`)
`object PaletteGenerator` — chroma.js-style palette synthesis:
- `sequential(colors: List<Int>, numColors, bezier=true, correctLightness=true): Palette`
- `diverging(colorsLeft, colorsRight, numColors, bezier=true, correctLightness=true): Palette`
- `sequentialHex(vararg hexColors: String, numColors, …)` / `divergingHex(colorsLeft, colorsRight, numColors, …)`
Bezier interpolation runs in LAB space (De Casteljau); `correctLightness` makes L change linearly.

### Color spaces (`color/space/`)

Each is a `data class` with `of(color4f)` (companion), `toColor4f()`, and a `mix(other, f=0.5f)`; hue-based spaces use `mixHue` (shortest-path angle interpolation, `space/mixHue.kt`).

| Type | File | Components | Note |
|------|------|-----------|------|
| `ColorHSV` | `ColorHSV.kt` | h, s, v, a=1 | Hue/Saturation/Value (cylindrical RGB). |
| `ColorHSL` | `ColorHSL.kt` | h, s, l, a | Hue/Saturation/Lightness. |
| `ColorHSI` | `ColorHSI.kt` | h, s, i, a=1 | Hue/Saturation/Intensity. |
| `ColorLAB` | `ColorLAB.kt` | l, a, b, alpha=1 | CIE L*a*b* (perceptual); has `xyz2lab`/`lab2xyz`. |
| `ColorLCH` | `ColorLCH.kt` | l, c, h, alpha=1 | LAB in polar (Lightness/Chroma/Hue). |
| `ColorOKLAB` | `ColorOKLAB.kt` | l, a, b, alpha=1 | OKLab perceptual space. |
| `ColorOKLCH` | `ColorOKLCH.kt` | l, c, h, alpha=1 | OKLab polar form. |
| `ColorCMYK` | `ColorCMYK.kt` | c, m, y, k | Subtractive/print model. |
| `RGBA` | `RGBA.kt` | r, g, b, a=255 (Int) | Arithmetic color: `+ - * /`, `mix`, `quantize`, `coerce`, `value`, `of(Int/Long/Color4f)`; constants WHITE/BLACK/YELLOW/CYAN/MAGENTA. |

Supporting (`space/`):
- `color4f.kt` — `Color4f.of(int)` / `of(r,g,b,a)`, `Color4f.luminance` (WCAG), `contrastRatio(other)`, `mix`, `mixLrgb` (linear-RGB mix), `Number.color4f()`.
- `xyz.kt` — internal CIE XYZ conversion + chromatic-adaptation constants (used by LAB/OKLAB).
- `temperature.kt` — `Color4f.temperature: Int` (Kelvin estimate) and `Color4f.Companion.ofTemperature(kelvin)`.

### Predefined palettes

Hand-curated named-color objects:

| Object | File | Access | Mood |
|--------|------|--------|------|
| ⭐ `CyanotypeColors` | `CyanotypeColors.kt` | `.palette1` (8), `.palette2` (16), `.accent`, `.accent2` | Deep blue cyanotype prints, warm orange accent. |
| `BgColors` | `BgColors.kt` | named fields (`bg01`–`bg08`, `pearlWhite`…, `dark01`…, `warmBlack1`–`5`) | Curated light/dark background neutrals. |
| `MidCenturyColors` | `MidCenturyColors.kt` | named fields (white1, green, black, red, yellow, blue, blue2) | Warm mid-century-modern accents. |
| `RetroColors` | `RetroColors.kt` | 14 named fields + `allColors: List<Int>` | Muted vintage/retro hues. |
| `NipponColors` | `NipponColors.kt` | 250 named fields `colNNN_NAME` (e.g. `col001_NADESHIKO`) via `rgb(...)` | Traditional Japanese colors, soft and earthy. |
| `CssColors` | `CssColors.kt` | 141 `const val` (camelCase) + `cssColors` map + `color(name)` / `color4f(name)` lookup | Standard W3C/SVG CSS named colors. |
| `PalettesOf4` | `PalettesOf4.kt` | `q01`–`q19`, each a 4-color `Palette` | Curated quad combos for posters/flat design. |

### `Palettes` collection (`Palettes.kt`)

`object Palettes` re-exposes the (mostly `internal`) palette families in `color/palettes/` as public flat fields, plus generators. Family source vals are `internal val <Family>_<Name> = Palette(...)`; access them through `Palettes`:

| Family field (in `Palettes`) | Count | Source file | Notes / mood |
|------|------:|-------------|------|
| `cool1`–`cool173` | 173 | `cool.kt` | Large grab-bag of curated artistic palettes (multi-hue). |
| `mix1`–`mix15` | 15 | `mix.kt` | Mixed hand-picked palettes. |
| `colormap001`–`colormap133` | 133 | (aliases below) | Scientific/data-viz colormaps, mapped 1:1 in order. |

The 133 `colormapNNN` entries alias these source families (in this order):
- **CARTO** (`carto.kt`, 32: `CARTO_Antique…Vivid`) — CARTOColors cartographic schemes.
- **CET** (`cet.kt`, 4: `CET_KovesiBGYW/KovesiKRYW/MRYBM/MYGBM`) — Kovesi perceptually-uniform maps.
- **ColorBrewer** (`colorbrewer.kt`, 35: `ColorBrewer_Accent…YlOrRd`) — classic sequential/diverging/qualitative cartography sets.
- **misc** (`misc.kt`, 5: `D3_D3`, `Google_AI_Turbo`, `misc_HSV`, `misc_Jet`, `MyCarta_CubeYF`) — assorted standards (Turbo, HSV, Jet, CubeYF, D3).
- **Kenneth Moreland** (`kenneth.kt`, 4: BentCoolWarm, BlackBody, BlackBodyExtended, SmoothCoolWarm) — diverging cool-warm / black-body.
- **Kindlmann** (`kindleman.kt`, 2: Kindlmann, KindlmannExtended) — iso-luminant rainbow.
- **Matplotlib** (`matplotlib.kt`, 6: Cividis, Inferno, Magma, Plasma, Twilight, Viridis) — the famous perceptual matplotlib maps.
- **Ocean / cmocean** (`ocean.kt`, 22: `Ocean_Algae…Turbid`) — oceanography colormaps (haline, thermal, deep, ice…).
- **ParaView** (`paraview.kt`, 2: Edge, IceFire).
- **Peter Karpov** (`peterkarpov.kt`, 4: Hesperia, Lacerta, Laguna, PlasmaModified) — artistic astrophotography maps.
- **Plotly** (`plotly.kt`, 11: AgGrnYl, AgSunset, BlackbodyAlt, Electric, G10, Hot, Picnic, Plotly, Plotly3, Portland, T10).
- **Polychrome** (`polychrome.kt`, 3: Alphabet, Dark24, Light24) — high-count categorical.
- **Tableau** (`tableau.kt`, 3: Tab10, Tab20b, Tab20c) — Tableau categorical sets.

Generators / accessors on `Palettes`:
- `gradient(colorFrom: Int/Long, colorTo, steps): Palette` — interpolate two colors into `steps` colors.
- `mixPalette(num)` → mix1..15; `coolPalette(num)` → cool1..173; `colormapPalette(num)` → colormap001..133 (1-based, throws on out-of-range).
- `navigator(): PalettesNavigator`.

`PalettesNavigator` (`PalettesNavigator.kt`) — UI-style cycler over sets `mix` (15), `cool` (76), `colormap` (133): `palette()`, `name()`, `nextPalette()/previousPalette()`, `nextSet()/previousSet()`. (Note: its `cool` set bound is 76, narrower than the 173 actually available.)

Source-of-truth files: `Palette.kt`, `color.kt`, `gradient.kt`, `colorRamp.kt`, `colorMatrix.kt`, `PaletteGenerator.kt`, `CyanotypeColors.kt`, `BgColors.kt`, `MidCenturyColors.kt`, `RetroColors.kt`, `NipponColors.kt`, `CssColors.kt`, `PalettesOf4.kt`, `Palettes.kt`, `PalettesNavigator.kt`, `color/palettes/*.kt`, `color/space/*.kt`.
## 3. Noise & Flow Fields

All paths are under `gart/src/main/kotlin/dev/oblac/gart/`.

### Noise functions

**⭐ `SimplexNoise`** — `object` (static, no seed; fixed `permutation` table).
- `fun noise(x: Double, y: Double): Double` — 2D simplex noise.
- `fun noise(x: Double, y: Double, z: Double): Double` — 3D simplex noise.
- Output range: roughly **[-1, 1]** (2D scaled by 70.0, 3D by 32.0). Sample at fractional coordinates; integer coords hit the lattice and produce a regular pattern.
- File: `noise/SimplexNoise.kt`

**`OpenSimplexNoise`** — `class OpenSimplexNoise(seed: Long = 0L)` (instance, seeded; also `constructor(seed: IntArray)` for a raw permutation). Based on Kurt Spencer's public-domain OpenSimplex.
- `fun random2D(x: Float, y: Float): Float` and `fun random2D(x: Double, y: Double): Double` — 2D.
- `fun random3D(x: Float, y: Float, z: Float): Float` and `fun random3D(x: Double, y: Double, z: Double): Double` — 3D.
- `fun random4D(x: Float, y: Float, z: Float, w: Float): Float` and `fun random4D(x: Double, y: Double, z: Double, w: Double): Double` — 4D.
- Output range: roughly **[-1, 1]** (standard OpenSimplex gradient noise).
- File: `noise/OpenSimplexNoise.kt` (~92 KB, hardcoded gradient/lookup tables)

**`PerlinNoise`** — `class PerlinNoise(perlinOctaves: Int = 4, perlinAmpFalloff: Float = 0.5f)` (instance; random permutation table built per instance via `Random.nextFloat()`, so each instance is differently seeded). Processing-style octave-summed Perlin.
- `fun noise(x: Number, y: Number = 0, z: Number = 0): Float` — 1D/2D/3D via defaulted args.
- Output range: **[0, 1]** (doc: "Generate noise between 0 and 1").
- File: `noise/PerlinNoise.kt`

**`Perlin`** — `object` (static; classic Ken Perlin improved noise using the shared `permutation` table).
- `fun noise(x: Double, y: Double, z: Double): Double` — 3D Perlin. Output range roughly **[-1, 1]**.
- `fun noise(): Double` — random sample mapped to **[0, 1]** (`noise(rndXYZ)/2 + 0.5`).
- File: `noise/Perlin.kt`

**`fbm(...)`** — top-level fn. Fractal Brownian Motion accumulating a user-supplied noise fn over octaves.
- `fun fbm(point: Point, octaves: Int = 6, lacunarity: Float = 4.0f, gain: Float = 0.6f, frequency: Float = 1.0f, amplitude: Float = 0.5f, offset: Float = 0.0f, noise: (Point) -> Float): Float`
- Range depends on supplied `noise` and params; starts at `offset`, adds `amplitude * noise(p * frequency)` per octave then `p *= lacunarity; amplitude *= gain`.
- File: `noise/fbm.kt`

**`noise(x: Float): Float`** — top-level 1-D value-noise helper (smoothstep-interpolated `cellnoise`). Output range roughly **[-1, 1]** (final `2.0f * (a + ...)`).
- File: `noise/noise.kt`

**`cellnoise(x: Float): Float`** — top-level integer-hash value noise. Output range **[0, 1]** (`n / 65535.0f`). Used internally by `noise(x)`.
- File: `noise/noise.kt`

**`NoiseColor`** — `class NoiseColor(baseX = 0.5f, baseY = 0.5f, octaves = 4, seed = 2.0f, noiseType = NoiseType.FRACTAL)`. Wraps Skia `Shader.makeFractalNoise` / `makeTurbulence` to produce a colored noise shader.
- `fun composeShader(color: Int, blend: BlendType = BlendType.PRINT): Shader` — blends a solid color with the noise shader.
- Enums: `NoiseType { FRACTAL, TURBULENCE }`; `BlendType(blendMode)` (DARKER_RGB, GRAY_TV_RGB, HARD_NOISE, DARK_NOIR, NOISE, LIGHT_NOISE, DARKEN, SATURATED, PRINT).
- File: `noise/NoiseColor.kt`

### Point distributions / blue noise

**⭐ `PoissonDiskSamplingNoise`** — `class PoissonDiskSamplingNoise(seed: Long = System.nanoTime())` (instance, seeded `SplittableRandom`). Blue-noise point sets via a fork of Martin Roberts's tweak to Bridson's algorithm. Exposes `var points: MutableList<Point>` (populated during generate). All generate overloads return `List<Point>`:
- `fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, minDist: Double, rejectionLimit: Int): List<Point>` — minimum-distance + explicit rejection limit (candidate tries per active point).
- `fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, minDist: Double): List<Point>` — same, `rejectionLimit` defaults to **11**.
- `fun generate(xmin: Double, ymin: Double, xmax: Double, ymax: Double, n: Int): List<Point>` — target **count** `n`: derives a radius, generates, shuffles with fixed `Random(1337)`, and returns up to `n` points.
- Note: `minDist`/radius is `Double`; the n-points overload distinguishes itself by taking `Int n` instead of `Double minDist`.
- File: `noise/PoissonDiskSamplingNoise.kt`

**`poissonDiskSampling(...)`** — top-level fn (ORX-derived). Bridson sampler with HashGrid acceleration, obstacle avoidance, and custom bounds.
- `fun poissonDiskSampling(bounds: Rect, radius: Float, tries: Int = 30, randomOnRing: Boolean = true, random: Random = Random.Default, initialPoints: List<Point> = listOf(bounds.center()), obstacleHashGrids: List<HashGrid> = emptyList(), boundsMapper: ((v: Point) -> Boolean)? = null): List<Point>`
- File: `noise/OrxPoissonDiskSamplingNoise.kt`

**`poissonDiskSamplingNoise(d: Dimension, r: Number = 30.0): List<Point>`** — convenience wrapper calling `poissonDiskSampling` over `Rect.ofXYWH(0,0,d.wd,d.hd)`.
- File: `noise/OrxPoissonDiskSamplingNoise.kt`

**`Point.Companion.uniformRing(innerRadius = 0.0f, outerRadius = 1.0f): Point`** — extension; uniform random point in an annulus. File: `noise/OrxPoissonDiskSamplingNoise.kt`

**`poissonDither(...)`** — top-level fn. Variable-density Poisson-disk dithering: dark pixels → dense dots, light → sparse (Bridson with per-point radius).
- `fun poissonDither(pixels: Pixels, minR: Float, maxR: Float, gamma: Float = 1.6f, brightnessMax: Float = 0.9f, k: Int = 30, seed: Int = 42): List<Point>`
- File: `noise/PoissonDither.kt`

**`HaltonSequenceGenerator`** — `class HaltonSequenceGenerator(dimension: Int, base: IntArray = primes40, weight: IntArray? = WEIGHTS)`. Low-discrepancy quasi-random sequence in [0,1] (scrambled, up to 40 dims).
- `fun get(): DoubleArray` — next point; `fun skipTo(index: Int): DoubleArray`; `fun nextIndex(): Int`.
- File: `noise/HaltonSequenceGenerator.kt`

### Flow fields

**⭐ `Flow`** — `fun interface Flow` (functional, `(Point) -> Vec2`).
- `operator fun invoke(p: Point): Vec2` — force/offset vector at a point.
- `fun offset(p: Point): Point` — default; applies the flow (`p.offset(invoke(p))`).
- File: `flow/Flow.kt`

**`Flow1`** — `data class Flow1(val direction: Angle, val magnitude: Float = 1f) : Flow`. Scalar flow (direction + speed magnitude, NOT a true vector). `direction` measured from negative x-axis: 0 up, PI/2 right, PI down, 3PI/2 left.
- `operator fun plus(other: Flow1): Flow1` — averages angle (`middleAngle`) and magnitude (not mathematically correct vector add).
- `override fun invoke(p: Point): Vec2` — `Vec2(sin(dir)*mag, -cos(dir)*mag)`.
- File: `flow/Flow1.kt`

**`Flow2`** — `data class Flow2(val direction: Angle, val magnitude: Float = 1f) : Flow`. True vector flow.
- `operator fun plus(other: Flow2): Flow2` — correct vector addition (component sum → magnitude/angle).
- `override fun invoke(p: Point): Vec2` — same form as Flow1.
- File: `flow/Flow2.kt`

**`FlowField`** — `class FlowField(val w: Int, val h: Int, field: Array<Array<Flow>>)`. Grid of per-cell `Flow`.
- Indexing: `get(x: Int, y: Int): Flow`, `get(point: Point): Flow`, `get(x: Number, y: Number): Flow`.
- `fun apply(points: List<Point>, pointConsumer: (Point, Point) -> Unit): List<Point>` — advances each in-bounds point one step, calls consumer(old, new).
- `fun drawField(c: Canvas, d: Dimension, gap: Int = 20)` and `fun drawField2(...)` — visualisation helpers.
- Companion: `fun of(d: Dimension, fn: (Float, Float) -> Flow): FlowField`; `fun from(d: Dimension, fn: (Int, Int) -> Vec2): FlowField`.
- File: `flow/FlowField.kt`

**`PointTracer`** — `class PointTracer(d: Dimension, flowField: FlowField)`. Follows a point through the field.
- `fun trace(point: Point, steps: Int): List<Point>` — path of up to `steps + 1` points (stops when out of bounds).
- `fun trace(p: Point): Point?` — single step; null if out of bounds.
- File: `flow/PointTracer.kt`

**`StreamlineTracer`** — `class StreamlineTracer(d: Dimension, flowField: FlowField, dSep: Float = 18f, maxSteps: Int = 300, seedInterval: Int = 4)`. Evenly-spaced streamlines (Jobard & Lefer) using a `HashGrid`; seeds perpendicular candidates.
- `fun trace(): List<Path>` — traces forward+backward per seed, merges into Skia `Path`s.
- Companion consts: `D_SEP = 18f`, `STEP_SIZE = 1f`, `MAX_STEPS = 300`, `SEED_INTERVAL = 4`.
- File: `flow/StreamlineTracer.kt`

### Prebuilt flow generators

All implement `(Float, Float) -> Flow` (i.e. `fun invoke(x, y): Flow1` or `Flow2`), suitable for `FlowField.of(d, generator)`.

Flow1 (scalar) — file `flow/flow1Generators.kt`:
- `class CircularFlow(cx: Float, cy: Float, direction: RotationDirection = CW, magnitude: Float = 1f)` — circular flow around center.
- `class SpiralFlow(cx: Float, cy: Float, spiralSpeed: Float = 0.3f, direction: RotationDirection = CW, magnitude: Float = 1f)` — spiral flow.
- `class WaveFlow(xFreq = 0.01f, yFreq = 0.03f, xAmp = 0.8f, yAmp = 0.5f, magnitude = 1f)` — sin/cos wave-driven direction.

Flow2 (vector) — file `flow/flow2Generators.kt`:
- `class CircularVecFlow(cx: Float, cy: Float, maxMagnitude: Float = 1024f, direction: RotationDirection = CW)` — circular pull; closer points faster.
- `class SpiralVecFlow(cx: Float, cy: Float, spiralSpeed: Float = 0.3f, maxMagnitude: Float = 1024f, minDistance: Float = 200f, direction: RotationDirection = CW)` — spiral pull; center never reached.
## 4. Math, Angles, Vectors, Matrices

Package roots: `dev.oblac.gart.math`, `dev.oblac.gart.angle`, `dev.oblac.gart.vector`, `dev.oblac.gart.matrix`.

### Constants

All in `math/constants.kt` (top-level `const val`):

- `WIDE_SCREEN_RATIO = 16.0f/9` (Float) — 16:9 aspect ratio.
- `ULTRA_WIDE_SCREEN_RATIO = 21.0f/9` (Float) — 21:9 aspect ratio.
- `CINEMA_RATIO = 2.4f` (Float) — cinema aspect ratio.
- `GOLDEN_RATIO = 1.61803398875` (Double) — golden ratio.
- `GOLDEN_RATIOf = 1.618034f` (Float) — golden ratio, Float.
- `PIf = Math.PI.toFloat()` (Float) — π as Float.
- `TAUf = 2 * PIf` (Float) — τ (2π).
- `DOUBLE_PIf = 2 * Math.PI.toFloat()` (Float) — 2π (used widely internally for normalization).
- `TWO_PIf = 2 * Math.PI.toFloat()` (Float) — 2π (alias of DOUBLE_PIf).
- `HALF_PIf = PIf / 2` (Float) — π/2.
- `QUARTER_PIf = PIf / 4` (Float) — π/4.

Note: there is NO `PIf`-named Double constant; `GOLDEN_RATIO` is the only Double-typed constant here.

### Scalar functions

- ⭐ `map(value: Number, inLower: Number, inUpper: Number, outLower: Number, outUpper: Number): Float` — `math/map.kt`. Re-maps a number from `[inLower, inUpper]` to `[outLower, outUpper]`. Arg order is **value first, then in-range, then out-range** (NOT clamped; accepts any `Number`, returns Float).
- ⭐ `lerp(a: Float, b: Float, t: Float): Float` — `math/math.kt`. Linear interpolation `a + t*(b-a)`.
- ⭐ `lerp(a: Double, b: Double, t: Double): Double` — `math/math.kt`. Double overload.
- ⭐ `lerp(a: Int, b: Int, t: Float): Float` — `math/math.kt`. Int endpoints, Float `t`, returns Float. (Three lerp overloads total; no `(Int,Int,Int)` form.)
- `mix(a: Float, b: Float, t: Float): Float` — `math/math.kt`. `a*(1-t) + b*t` (algebraically equals lerp but different formulation). Vec3 overloads in `vector3.kt`.
- ⭐ `smoothstep(edge0: Float, edge1: Float, x: Float): Float` — `math/math.kt`. Hermite smoothstep; `t` coerced to [0,1] then `t*t*(3-2t)`.
- `step(threshold: Float, x: Float): Float` — `math/math.kt`. Returns 1.0f if `x >= threshold` else 0.0f.
- ⭐ `wrap(v: Int, size: Int): Int` — `math/math.kt`. Wraps into `[0, size)`; negatives folded up.
- ⭐ `wrap(v: Float, size: Float): Float` — `math/math.kt`. Wraps into `[0, size)`; guards against float rounding to exactly `size` (folds back to 0f, safe as array index).
- `mod(a, b)` over `Double`, `Int`, `Float`, `Long` (each same-type pair) — `math/math.kt`. True modulo `((a % b) + b) % b` (always non-negative).
- `frac(value: Float): Float` / `frac(value: Double): Double` — `math/math.kt`. Fractional part `value - floor(value)`. Vec2 overload in `vector2.kt`.
- `clamp` — NOT present as a named function. Codebase uses Kotlin stdlib `coerceIn`. `math/clamp.kt` only defines `cond(b: Boolean, x: T, y: T): T` (ternary helper), despite the filename.
- ⭐ `dist(x1, y1, x2, y2: Float): Float` and `dist(p1: Point, p2: Point): Float` — `math/distance.kt`. Euclidean distance using `fastSqrt`.
- `distSquared(x1, y1, x2, y2: Float): Float` and `distSquared(p1: Point, p2: Point): Float` — `math/distance.kt`. Squared distance (no sqrt).
- ⭐ `hypotFast(a: Float, b: Float): Float` and `hypotFast(a: Double, b: Double): Double` — `math/math.kt`. `fastSqrt(a*a + b*b)`.
- `fastSqrt(d: Double): Double` and `fastSqrt(f: Float): Float` — `math/sqrt.kt`. Fast sqrt approximation (bit-hack seed + one Newton step). Accurate enough for most cases.
- `fastFastSqrt(number: Double): Double` — `math/sqrt.kt`. Less accurate, faster sqrt approximation.
- Number conversion shorthands — `math/math.kt`: `Double.f()`, `Int.f()` → Float; `Float.d()` → Double; `Float.i()`, `Long.i()` → Int.
- `Int.isEven(): Boolean`, `Int.isOdd(): Boolean` — `math/math.kt`.
- `Float.format(digits: Int): String` — `math/math.kt`. `"%.${digits}f"` formatting.
- Trig/degree helpers — `math/trig.kt`: `Float.toRadians(): Float`, `Float.toDegrees(): Float`, `Float.subDeg(delta: Number)`, `Float.addDeg(delta: Number)` (note: addDeg currently subtracts — likely bug), `sinDeg(degrees: Number): Float`, `cosDeg(degrees: Number): Float`, `normalizeRad(rad: Float): Float` (folds into [0, 2π]).
- `binaryEntropy(p: Double): Double` — `math/entropy.kt`. `-p·ln(p) - (1-p)·ln(1-p)`; 0 outside (0,1).
- `globalStdDev(data: DoubleArray): Double` and `windowedStdDev(data, w, h, radius, mode=PadMode.REFLECT): DoubleArray` — `math/stdev.kt`. Standard deviation (global / per-pixel windowed).
- `primes40: IntArray` — `math/primes.kt`. First 40 primes.

### Precalc tables, loops, curves

- `MathPrecalcTable` (sealed), `MathCos(resolution: Float = 0.1f)`, `MathSin(resolution: Float = 0.1f)` — `math/MathPrecalcTables.kt`. Precomputed cos/sin lookup tables indexed by degrees via `operator get(degrees: Number): Float`.
- `GaussianFunction(height: Number, center: Number, standardDeviation: Number)` with `operator invoke(x: Number): Float` — `math/GaussianFunction.kt`. Gaussian bell curve evaluator.
- `Lissajous(center: Point, A, B, a, b: Float, dx=0f, dy=0f, t=0f)` — `math/Lissajous.kt`. `step(delta: Float): Point`, `position(): Point`. Lissajous curve generator.
- `Polar(theta: Angle, radius: Float = 1.0f)` — `math/polar.kt`. `companion of(point: Point): Polar`, `val cartesian: Point`, `+`/`-` operators. Polar↔Cartesian.
- `doubleLoop(...)` (several overloads) and `doubleLoopSequence(iMax, jMax): Sequence<Pair<Int,Int>>` — `math/DoubleLoop.kt`. Grid iteration helpers (index-based, stepped, and gap/step or gap/count Float-grid variants).
- Transforms — `math/transform.kt`: `fun interface Transform` (`invoke(Point): Point`, composable via `+`), `Transform.rotate(point/x,y, angle: Angle)`, `RotationTransform`, enum `RotationDirection { CW, CCW }`.
- Circle tangents — `math/tangents.kt`: `circleTangents(circle0, circle1): List<Tangent>`, `pointToCircleTangents(point: Point, circle): List<Line>`, data class `Tangent`, enum `TangentType { EXTERNAL, INTERNAL }`.
- Fractal/iteration — `math/zfunc.kt`: `zfunc(x, y, maxIterations, escapeRadius, f: (Complex)->Complex): ZFuncResult`, data class `ZFuncResult`, enum `Convergence { CONVERGED, DIVERGED }`.

### Random helpers

All in `math/random.kt`. Defined BOTH as extensions on `kotlin.random.Random` AND as bare top-level functions delegating to `Random.xxx` (so `rndf()` works without a receiver).

`Random` extensions:
- ⭐ `Random.rndf(): Float` — uniform in [0, 1).
- ⭐ `Random.rndf(max: Float): Float` — uniform in [0, max).
- `Random.rndf(max: Number): Float` — Number overload.
- ⭐ `Random.rndf(min: Float, max: Float): Float` — uniform in [min, max).
- `Random.rndf(min: Number, max: Number): Float` — Number overload.
- `Random.rndi(): Int` / `rndi(max: Int): Int` / `rndi(min: Int, max: Int): Int` — random ints.
- `Random.rnd(min: Double, max: Double): Double` — uniform Double in [min, max).
- `Random.rndb(): Boolean` — 50/50 coin flip.
- `Random.rndb(success: Int, total: Int): Boolean` — true with probability success/total.
- `Random.rndsgn(): Int` — random sign, +1 or -1.
- `Random.rndGaussian(mean: Float = 0.0f, standardDeviation: Float = 1.0f): Float` — Box–Muller Gaussian.
- `Random.rndInDisc(r: Float): Point` — uniform point inside disc radius r (rejection sampling).
- `Random.rndInBall(r: Float): Vec3` — uniform point inside ball radius r (rejection sampling).

Bare top-level mirrors (no receiver, delegate to `Random`): `rndi()`, `rndi(max)`, `rndi(min, max)`, `rndf()`, `rndf(max: Float)`, `rndf(max: Number)`, `rndf(min: Float, max: Float)`, `rndf(min: Number, max: Number)`, `rnd(min: Double, max: Double)`, `rndb(success, total)`, `rndb()`, `rndsgn()`, `rndGaussian(mean=0f, sd=1f)`, `rndInDisc(r)`, `rndInBall(r)`. (Note: no bare `rnd()` no-arg, no bare `rndGaussian` extension-only diff.)

### Angles

All in `angle/angle.kt`. Package `dev.oblac.gart.angle`.

- ⭐ `sealed interface Angle` — `val radians: Float`, `val degrees: Float`; operators `+`, `-`, `* Number`, `/ Number`, unary `-`, `compareTo`; `fun normalize(): Angle`.
- `data class Radians(val value: Float) : Angle` — `radians = value`, lazy `degrees`. `normalize()` folds into [0, 2π]. Companion: `ZERO`, `PI_HALF`, `PI`, `TWO_PI`, `of(value: Number): Angle`.
- `data class Degrees(val value: Float) : Angle` — lazy `radians`, `degrees = value`. `normalize()` folds into [0, 360]. Companion: `ZERO`, `D90`, `D180`, `D270`, `D360`, `of(value: Number): Angle`.
- `cos(a: Angle): Double` / `cosf(a: Angle): Float` — `angle/angle.kt`. Cosine of an angle (Double / Float variants).
- `sin(a: Angle): Double` / `sinf(a: Angle): Float` — `angle/angle.kt`. Sine of an angle (Double / Float variants).
- `middleAngle(a: Angle, b: Angle): Angle` — `angle/angle.kt`. Shortest mid-angle, normalized into -π..π.

### Vectors

- ⭐ `data class Vec2(val x: Float, val y: Float)` — `vector/vector2.kt`. Secondary ctor `(x: Number, y: Number)`. Factory `vec2(x: Number, y: Number)`. Operators: `+`/`-`/`*`/`/` with both `Vec2` and `Number`. Methods: `dot(other): Float`, `cross(other): Float` (scalar 2D cross), `length(): Float`, lazy `magnitude`, `normalize(): Vec2` (returns self if magnitude 0), `rotate(angle: Float): Vec2`, lazy `val angle: Radians` (`atan2(y,x)`). Companion: `ZERO`, `of(angle: Angle): Vec2` (unit vector from angle). Free fns: `sin(v: Vec2)`, `frac(v: Vec2)`, `length(v: Vec2): Float`.
- `data class Vec3(val x: Float, val y: Float, val z: Float)` — `vector/vector3.kt`. Factory `vec3(x, y, z: Number)`. Operators: `+`/`-`/`*`/`/` (Vec3 and Number; note: no `minus(Number)`, no `div(Vec3)`). Methods: `pow(other: Vec3)`, `length(): Float`, `normalize()`, `dot(other): Float`, `cross(other): Vec3`. Companion: `of(a: Float)`, `of(v: Vec2, a: Float)`, `ZERO`, `ONE`, `TWO_PI`. Free fns: `sin(v: Vec3)`, `cos(v: Vec3)`, `mix(a, b: Vec3, t: Float)`, `mix(a, b: Vec3, t: Vec3)`, `abs(v: Vec3)`.
- `data class Vec4(val x, y, z, w: Float)` — `vector/vector4.kt`. Companion `of(vec3: Vec3, w: Float)`. Plain data holder, no operators.

### Matrices

- `Matrix<D>(rows: Int, cols: Int, init: (Int, Int) -> D)` — `math/Matrix.kt`. Generic dense row-major matrix. `get/set(row, col)` (bounds-checked), `forEach((row, col, value) -> Unit)`, `coordinates(): List<Pair<Int,Int>>`.
- `data class Matrix22(a, b, c, d: Float)` — `matrix/matrix2.kt`. 2×2 matrix; `operator times(v: Vec2): Vec2`. (File is `matrix2.kt`; class is named `Matrix22`.)
- `Matrix33.Companion.multiply(a: Matrix33, b: Matrix33): Matrix33` — `matrix/matrix33.kt`. Extension on Skia's `org.jetbrains.skia.Matrix33`: `IDENTITY.makeConcat(a).makeConcat(b)`. (No custom Matrix33 class — extends the Skia type. File is `matrix33.kt`.)

### Complex

- `class Complex(val real: Double, val imag: Double)` — `math/Complex.kt`. Secondary ctor `(real: Number, img: Number)`. Operators: unary `-`, `+`/`-`/`*` with `Complex` and `Number`, `/ Number`, `/ Complex`, `component1/2`. Methods: `conjugate()`, `normSquared()`, `norm()`, `mod()`, `abs(): Double`, `phase(): Double` (`atan2(imag, real)`), `magnitude()`, `pow(a: Double/Number/Complex)`, `isZero(tolerance: Double)`, infix `to(exponent: Int/Complex/Number)`. Companion: `ZERO`, `ONE`, `i`, `DEFAULT_TOLERANCE = 1.0E-15`, `fromNumber`, `fromPolar(radius, theta)`, `real(n)`, `imag(n)`.
- Top-level in `math/Complex.kt`: `val i = Complex(0.0, 1.0)`; complex functions `exp`, `sinh`, `cosh`, `tanh`, `coth`, `cos`, `sin`, `tan`, `cot`, `sec`, `ln`, `floor`, `sqrt`, `arcsinh`, `roots(n: Int)`; `Number`-receiver operators `plus/minus/times/div` with `Complex`.
- `ComplexField(xFrom, xTo, yFrom, yTo: Float, stepsX, stepsY: Int, supplier: (x,y)->Complex)` — `math/ComplexField.kt`. Precomputed grid of Complex; `operator get(x, y): Complex`; companion `of(d: Dimension, supplier)` (domain -1..1).
- `ComplexFunctions` (object) — `math/ComplexFunctions.kt`. Sample functions: `simple`, `threes`, `polesAndHoles(poles, holes)`, `julia(zx, zy, cx, cy, maxIter=100)`.
- `class ComplexPolynomial(vararg coefficients: Complex)` — `math/ComplexPolynomial.kt`. `c[0] + c[1]z + ...`. `invoke(z: Complex)`/`invoke(n: Number)`, `degree`, `get(i)`, operators (`+`/`-`/`*`/`/` with Number/Complex/ComplexPolynomial; `div(other)` returns `Pair<quotient, remainder>`), `derivative()`, infix `to(exponent: Int)`, `isMonomial()`. Companion: `ZERO`, `constant`, `monomial(degree, coefficient/number)`, `of(vararg Double)`. Top-level in same file: `val Z`, `Number/Complex` `times`/`plus` `ComplexPolynomial` operators.
- `divide(dividend, divisor): Pair<ComplexPolynomial, ComplexPolynomial>` and `gcd(f, g): ComplexPolynomial` — `math/complexDivide.kt`. Polynomial long division and GCD.
## 5. Geometry, Paths & Curves

All geometry uses Skia's `org.jetbrains.skia.Point` (mutable x/y floats), `Rect`, and `Path`. `Angle` is gart's own sealed type (`Degrees`/`Radians`, package `dev.oblac.gart.angle`). Most builders return plain `List<Point>` so they compose with the path constructors and smoothers below.

### Points

Source: `gart/src/main/kotlin/dev/oblac/gart/gfx/point.kt`, `point_misc.kt`, `Points.kt`

- `Point(x: Number, y: Number): Point` — factory accepting any `Number` (converts to float).
- `pointOf(x: Number, y: Number): Point` — same, named-function form.
- `Pair<Number, Number>.toPoint(): Point` — `(x to y).toPoint()`.
- `Point.copy(): Point` — duplicate.
- `randomPoint(d: Dimension): Point` — uniform random point in the canvas.
- `randomPoints(d: Dimension, count: Int): List<Point>` — N random points.
- `randomPoint(cx: Float, cy: Float, rmax: Float, rmin: Float = rmax): Point` — random point in a (ring) radius around a center.
- `randomPoint(min: Point, max: Point): Point` — random in a bounding box.
- `Point.Companion.random(d: Dimension)` / `random(w: Number, h: Number)` / `random(r: Rect)` — companion variants.
- `Point.Companion.relative(x: Float, y: Float, d: Dimension): Point` — point from fractional coords (`d.ofW`/`d.ofH`).
- `Point.isCloseTo(other: Point, tolerance: Float): Boolean` — distance < tolerance.
- `Point.moveTowards(destination: Point, amount: Float): Point` — step `amount` toward destination.
- `Point.rotate(angle: Radians, rx: Float, ry: Float): Point` — rotate around pivot (rx, ry).
- `Point.isInside(circle: Circle): Boolean`, `Point.isInside(dimension: Dimension)`, `Point.isInside(rect: Rect)`, `Point.isInside(triangle: Triangle)` — containment tests (`point_misc.kt` / `point.kt`).
- `Point.ifInside(dimension: Dimension): Point?` — self or null.
- `Point.isOnLine(line: Line): Boolean`.
- `Point.offset(vec: Vec2): Point` — translate by a vector.
- `Point.fromCenter(d: Dimension, fl: Float = 1f): Point` — interpret as offset from canvas center.
- `Point.distanceTo(p: Point): Float` (uses `math.dist`); `Point.squaredDistanceTo(p: Point): Float`.
- Operators: `Point + Point`, `Point - Point`, `Point + Number`, `Point * Number`, plus `component1()/component2()` for destructuring `val (x, y) = p`.
- `dot(a: Point, b: Point): Float` / `Point.dot(b: Point): Float` — dot product.
- `pointBetween(p1: Point, p2: Point): Point` — midpoint (`point_misc.kt`).
- `randomPointBetween(p1: Point, p2: Point): Point` — random point on the segment.
- `isCollinear(point1, point2, point3): Boolean`.
- `class Points` — experimental mutable point accumulator: `+= point`, `path()` builds a path. (`Points.kt`)

### Circles & lines

`Circle` — `gart/src/main/kotlin/dev/oblac/gart/gfx/circle.kt`

- `data class Circle(x: Float, y: Float, radius: Float)`; secondary ctor `Circle(center: Point, radius: Number)`. Exposes `center`, `topPoint`, `bottomPoint`, `leftPoint`, `rightPoint`.
- `Circle.of(center: Point, radius: Number): Circle`; `Circle.of(a: Point, b: Point, c: Point): Circle` — fits a circle through 3 points.
- `contains(x: Float, y: Float)` / `contains(p: Point)` / `contains(c: Circle): Boolean`.
- `rect(): Rect` — bounding box.
- `pointOnCircle(angleRad: Angle): Point` — point at angle on the circumference.
- `tangentAtPoint(pointOnCircle: Point): DLine` — tangent line (perpendicular to radius).
- `movePointAlongCircle(point: Point, angleRadians: Angle): Point` — rotate a point about the center.
- `isInsideOf(other: Circle)` / `isInsideOf(rect: Rect): Boolean`.
- ⭐ `points(count: Int): List<Point>` — `count` evenly-spaced points around the circle (delegates to `createCircleOfPoints`).
- `points(count: Int, startAngle: Angle, sweepAngle: Angle): List<Point>` — points along an arc.
- `toPath(): Path`; `scale(value: Float)`, `resize(newRadius: Float)`, `grow(delta: Float)` — return new `Circle`s.
- `createCircleOfPoints(center: Point, radius: Float, steps: Int): List<Point>` — free function backing `Circle.points`.
- `circleFrom3Points(a, b, c): Circle` — circumscribing circle of 3 points.

`DLine` (infinite parametric line: point + direction `Vec2`) — `dline.kt`

- `data class DLine(p: Point, dvec: Vec2)`.
- `pointFromStart(t: Float)` / `pointFromEnd(t: Float): Point` — march along normalized direction.
- `perpendicularDLine(): DLine`; `toLine(start: Point, distance: Float): Line`.
- `DLine.of(prev, current, next): DLine` — direction from prev→next, anchored at current.
- `DLine.of(point: Point, current: Angle): DLine` — from a point along an angle.

`Line` (finite segment a→b) — `line.kt`

- `data class Line(a: Point, b: Point)`; getters `x1,y1,x2,y2`; `Line.of(x1,y1,x2,y2)`.
- `length()`, `centerPoint()`/`midPoint()`, `reversed()`, `angle(): Angle`, `angleTo(line2: Line): Angle`.
- `pointFromStart(t)` / `pointFromEnd(t)` (t is 0..1 fraction); `pointFromStartLen(len)` / `pointFromEndLen(len)` (len in pixels).
- `shortenByLen(len)`, `extendByLen(len)`, `extendBy(f)`, `lineFromStartLen(length)`.
- `points(count: Int): List<Point>` — `count` evenly-spaced points along the segment.
- `toPath()`, `toDline()`, `toFatLine(thickness): Path`.
- `toBoundingRectangle(gapW, gapH): Rect`, `toWrappingRectangle(gapW): Poly4`.
- `isPointOnLine(point, tolerance = 1f): Boolean`.
- `Line.parallelTo(target, point)`, `Line.fromPointToLine(p, it)` (nearest point on segment), `Line.fromPointAtAngle(startingPoint, angle, length)`.
- `fatLine(x0, y0, x1, y1, thickness): Path` — closed quad path for a thick line.

Polygons & other shapes (geometry side; draw helpers noted briefly, paints are out of scope):

- `createNtagonPoints(n: Int, centerX, centerY, radius, startAngle = 0f, clockwise = false): List<Point>` — regular N-gon vertices (`ntagon.kt`).
- `Poly4(a, b, c, d)` — quad with `points()`, `center()`, `topPoint()`/`bottomPoint()`, `lines()`, prebuilt `path`, `shrink(factor)`, `move(dx, dy)`; companion `rectAroundPoint(c, width, height, angle)`, `squareAroundPoint(c, sideLength, angle)`; free `randomSquareAroundPoint(c, sideLength)` (`poly4.kt`).
- `Triangle(a, b, c)` — `path`, `edges`, `centroid`, `contains(point)`, `calculateArea(): Double`, `calculateCircumcircle(): Circle`, `intersect(triangle)`, `scaled(f)`, `rotateAround(midPoint, angle)`, `flipAcross(line)`, `isInRect(r)`; companion `equilateral(c, radius, angle)`; free `equilateralTriangle`, `randomEquilateralTriangle`, `isoscelesTriangle(c, base, height, angle = Radians(0f))` (`Triangle.kt`).
- `RectIsometric` (sealed) with `RectIsometricTop/Right/Left(x, y, a, b, …angle)` — skewed isometric quads exposing `left/bottom/right/top`, `path()`, `width()`, `height()` (`RectIsometric.kt`).
- `Rect` extensions (`rectangle.kt`): `points(): Array<Point>`, `path(): Path`, `center()`, `contains(rect)/contains(point)`, `thirds()`, `shrink/grow(delta)`, `move(delta)`, `dimension()`, corner accessors, `splitToGrid(cols, rows): List<Rect>`, `diagonal()`, companions `ofXYWH(...)`, `of(...)`, `ofCenter(center, w, h)`, `EMPTY`.
- `gridOfDimension(d: Dimension, cellsX, cellsY): List<GridRect>` (`grid.kt`); `GridRect(rect, row, col)`.
- Shape draw helpers (geometry-flavored, on `Canvas`): `drawCircle(circle/p, …)`, `drawCircleArc(x, y, radius, paint, start, sweep)`, `drawCirclePie(...)`, `drawArc(rect, …)` (`arc.kt`), `createDrawRing(center, radius, radius2, width1, width2, width3, angle): Pair<DrawRing, DrawRing>` (`ring.kt`), `Moon(circle, shadowPaint, moonPaint, moonPhase = 0.5f)` Draw (`moon.kt`), `drawBorder(d, stroke)` / `drawRoundBorder(d, radius, width, color)` (`border.kt`), `drawPoly4`, `drawTriangle`, `drawRectWH`, `drawRotatedRect`, `clipCircle`.

### Path construction & sampling

Source: `gart/src/main/kotlin/dev/oblac/gart/gfx/path.kt` (plus `pathBuilder.kt`, `pathOutline.kt`, `pathVary.kt`, `PathW.kt`)

- ⭐ `pathOf(first: Point, vararg points: Point): Path` — open polyline (moveTo + lineTo).
- `pathOf(list: List<Point>): Path`; `List<Point>.toPath(): Path` — open polyline from a list.
- `pathBuilderOf(points: List<Point>): PathBuilder`; `List<Point>.toPathBuilder()`.
- `closedPathOf(points: List<Point>): Path` / `closedPathOf(first, vararg points)` ; `List<Point>.toClosedPath(): Path` — closed polygon.
- `List<Point>.toQuadPath(): Path` — quadratic-Bézier path: consumes points in pairs as (control, anchor).
- `Point.pathTo(point: Point): Path` — single-segment path.
- `List<Line>.toPath()` / `List<Line>.toClosedPath()` — chain line segments (`@JvmName linesToPath/linesToClosedPath`).
- `Path.points(): List<Point>` — the path's defining control points (`path.points`, filtered non-null).
- ⭐ `pointsOn(path: Path, pointsCount: Int): List<Point>` — sample exactly `pointsCount` points evenly by **arc length** along the path (uses `PathMeasure`; step = length/(count−1), inclusive of both ends).
- `pointsOn(path: Path, pointsCount: Int, ease: EaseFn = EaseFn.Linear): List<Point>` — same, but distance distribution warped by an easing fn (clusters samples).
- `Path.toPoints(pointsCount: Int)` / `Path.toPoints(pointsCount, ease: EaseFn)` — extension wrappers over `pointsOn`.
- `Path.length(): Float` — total arc length (`PathMeasure`).
- `Path.toRegion(): Region`.
- `combinePathsByAppending(vararg paths): Path` — union by appending contours.
- `combinePathsWithOp(operation: PathOp, vararg paths): Path` — boolean ops chained (`Path.makeCombining`).
- `pathsOverlap(c1: Path, c2: Path, minArea: Float = 1f): Boolean` — intersection-area test.
- `deformPath(points: List<Point>, offsetStdDev: Float = 15f): List<Point>` — inserts Gaussian-jittered midpoints per segment (treats list as closed).
- `discretizePath(path: Path, segLength: Float, deviation: Float, seed: Int = 0): Path` — bakes a discrete/jitter effect into real geometry; walks contours at `segLength`, offsets each sample perpendicular to the tangent by uniform `[-deviation, deviation]`.
- `isPointBelowPath(path: Path, point: Point, precision: Float = 1f): Boolean` / `Point.isBelowPath(path, precision = 1f)` — vertical containment vs. sampled path.
- `PathBuilder.addCircle(circle: Circle)` (`pathBuilder.kt`).
- `pathToOutline(path: Path, width: Float): PathOutline` / `Path.toOutline(width): PathOutline` — offsets perpendicular to the tangent on both sides to build a closed stroke outline; `PathOutline(line, width, outline)` (`pathOutline.kt`).
- `List<Point>.withVaryingSplineNoise(minNoise = 0f, maxNoise = 8f, random = Random.Default, preserveEnds = false, noiseAt = { it }): List<Point>` — random-walk node jitter whose amplitude varies along the path; meant to be fed to `toSmoothQuadraticPath()` (`pathVary.kt`).
- `data class PathW(path: Path, width: Float)` — path bundled with stroke width (`PathW.kt`).

### Smoothing & splines

Source: `gart/src/main/kotlin/dev/oblac/gart/smooth/`

- ⭐ `chaikinSmooth(polyline: List<Point>, iterations: Int = 1, closed: Boolean = false, bias: Double = 0.25): List<Point>` — Chaikin corner-cutting (tailrec). `bias` in (0, 0.5): low → cuts near existing vertices, near 0.5 → toward midpoints. `closed` wraps the polyline. (`chaikin.kt`)
- `List<Point>.toChaikinSmooth(iterations = 1, closed = false, bias = 0.25): Path` — smooth then `toClosedPath()`.
- ⭐ `catmullRomSpline(points: List<Point>, segments: Int = 20): Path` — interpolating Catmull-Rom spline through all points; `segments` = subdivisions per span. `List<Point>.toCatmullRomSpline(segments = 20)`. (`catmullRomSpline.kt`)
- `cardinalSpline(points: List<Point>, tension: Float = 0.5f, segments: Int = 20): Path` — Hermite cardinal spline; `tension` 0 = Catmull-Rom, 1 = straight lines. `List<Point>.toCardinalSpline(tension = 0.5f, segments = 20)`. (`cardinalSpline.kt`)
- `bSpline(points: List<Point>, segments: Int = 20): Path` — uniform cubic B-spline (approximating, not through points); **requires ≥ 4 points** (throws otherwise). `List<Point>.toBSpline(segments = 20)`. (`bSpline.kt`)
- `drawSmoothQuadratic(points: List<Point>): Path` — quadratic-Bézier curve using each point as a control and segment midpoints as anchors. `List<Point>.toSmoothQuadraticPath()`. (`smoothQuadratic.kt`)

### Parametric generators

- `createSpiral(center: Point, radius: Float, steps: Int, offset: Angle, loop: Int = 1): List<Point>` — Archimedean spiral; radius grows linearly to `radius` over `steps`, `loop` = number of turns (`gfx/spiral.kt`).
- `createWaveBetweenPoints(start: Point, end: Point, steps: Int, maxRadius: Float, loops: Int = 3, offset: Angle = Degrees.of(0f)): List<Point>` — sine-modulated spiral wave along the line start→end; radius peaks at the midpoint (`gfx/wave.kt`).
- `class Lissajous(center: Point, A: Float, B: Float, a: Float, b: Float, dx: Float = 0f, dy: Float = 0f, t: Float = 0f)` — stateful Lissajous curve; `A/B` = X/Y amplitude, `a/b` = X/Y frequency, `dx/dy` = phase. `step(delta: Float): Point` advances `t` and returns the next point; `position(): Point` returns the current point (`math/Lissajous.kt`).
- `harmongraph2(iterations, delta, a, b, f1, f2, p1, p2, d1, d2): List<Point>` — 2-pendulum harmonograph: `x = a·sin(f1·t+p1)·e^(d1·t)`, `y = b·sin(f2·t+p2)·e^(d2·t)` (damped). (`harmongraph/harmongraph.kt`)
- `harmongraph4(iterations, delta, a1, a2, b1, b2, f1, f2, g1, g2, p1, p2, q1, q2, d1, d2, e1, e2): List<Point>` — 4-pendulum harmonograph (two damped sinusoids summed per axis). (`harmongraph/harmongraph.kt`)
- `createSpirograph(d: Dimension, path: Path, radius: Float, deltaAngle: Angle, samples: Int = 100, repetitions: Int = 20): Spirograph` — rolls a circle of `radius` along a closed `path` (sampled `samples` times via tangents/circumcircles), rotating `deltaAngle` per step for `repetitions` laps. Returns `Spirograph(points: List<Point>, tangents: List<DLine>)`. (`spirograph/spirograph.kt`)
- `data class Knot(x: Float, dy: Float)` — normalized wave node (`x` in 0..1, `dy` offset from baseline); a data carrier for wave/knot curve construction, not a generator itself (`knot/Knot.kt`).
## 6. Paints, Effects & Shaders

All paint/effect helpers live under `dev.oblac.gart`. Skia types are `org.jetbrains.skia.*`. Paths below are relative to `gart/src/main/kotlin/`.

### Paint builders ⭐

Source: `dev/oblac/gart/gfx/paints.kt`

- ⭐ `fillOf(color: Color4f): Paint` / `fillOf(color: Int): Paint` / `fillOf(color: Long): Paint` — anti-aliased FILL paint of the given color.
- ⭐ `strokeOf(color: Color4f, width: Float): Paint` — anti-aliased STROKE paint; color-first ordering.
- ⭐ `strokeOf(color: Long, width: Float)` and reversed `strokeOf(width: Float, color: Long)` — both arg orders exist.
- ⭐ `strokeOf(color: Int, width: Float)` and reversed `strokeOf(width: Float, color: Int)` — both arg orders exist.
- `strokeOfBlack/White/Red/Green/Blue/Yellow/Magenta(width: Number): Paint` — named-color stroke shortcuts.
- `fillOfBlack/White/Yellow/Red/Blue/Green(): Paint` — named-color fill shortcuts (white uses `0xFFFFFFFF`).
- `paint(): Paint` — bare anti-aliased Paint, for manual configuration.
- `hatchPaint(color: Int, density: Float = 5f, dotWidth: Float = 1f, strokeWidth: Float = 3f): Paint` — dotted/hatch fill via `PathEffect.makePath2D` of a small circle tiled by `Matrix33.makeScale(density)`.
- `dashPaint(color: Int, density: Float = 6f, angle: Angle = Degrees(-45f), strokeWidth: Float = 2f): Paint` — diagonal line pattern via `PathEffect.makeLine2D` (scale × rotate matrix).
- `paintOfImageFilter(imageFilter: ImageFilter): Paint` — anti-aliased Paint carrying an `imageFilter` (used by `Canvas.saveLayer`).

Chainable Paint extensions (return `this`):
- `Paint.alpha(a: Int): Paint` — sets alpha (0–255).
- `Paint.blendMode(blendMode: BlendMode): Paint` — sets blend mode.
- `Paint.roundStroke(): Paint` — sets `strokeCap`/`strokeJoin` to ROUND.

Color → shader paint:
- ⭐ `Shader.toPaint(): Paint` — wraps any shader into an anti-aliased Paint. Source: `dev/oblac/gart/shader/shader.kt`.
- `gradientOf(colors: IntArray, positions: FloatArray? = null): Gradient` / `gradientOf(colors: Array<Color4f>, ...)` — builds a gart `Gradient` (CLAMP tile mode), consumed by `Shader.makeLinearGradient(...)` etc. Source: `dev/oblac/gart/color/gradient.kt`.

### Canvas draw helpers

Source: `dev/oblac/gart/gfx/canvas.kt`

- `Canvas.drawBitmap(b: Gartmap)` — draws a Gartmap's image at (0,0).
- `Canvas.drawImage(image: Image)` — draws an Image at (0,0).
- `Canvas.draw(g: Gartvas)` — draws a Gartvas snapshot at (0,0).
- `Canvas.saveLayer(imageFilter: ImageFilter)` — pushes a layer whose paint carries `imageFilter` (the standard way to apply an SkSL filter to subsequent draws).

### Image effects (`fx/`)

- `blur(gartvas: Gartvas, intensity: Float = 2f)` — snapshots and redraws through `ImageFilter.makeBlur(intensity, intensity, FilterTileMode.CLAMP)`. Source: `dev/oblac/gart/fx/blur.kt`.
- `pixelate(gartvas: Gartvas, pixelSize: Int)` — downscale then nearest-neighbor upscale via `drawImageRect`. Source: `dev/oblac/gart/fx/pixelate.kt`.
- `Image.scaleImage(newWidth: Int, newHeight: Int): Image` — resamples an Image onto a new raster surface. Source: `dev/oblac/gart/fx/scale.kt`.
- `borderize(src: GartGG, border: Int, color: Int): GartGG` — clones a Gart into a larger one with a uniform colored border. Source: `dev/oblac/gart/fx/borderize.kt`.

### Raw Skia effects commonly reached for

Verified by grepping `arts/` for real usage. Signatures are `org.jetbrains.skia`.

- ⭐ `ImageFilter.makeBlur(sigmaX: Float, sigmaY: Float, mode: FilterTileMode): ImageFilter` — Gaussian blur as an image filter; set on `Paint.imageFilter`. Real usage: `ImageFilter.makeBlur(3f, 3f, FilterTileMode.DECAL)`, `makeBlur(40f, 40f, DECAL)`, `makeBlur(intensity, intensity, CLAMP)`.
- ⭐ `MaskFilter.makeBlur(mode: FilterBlurMode, sigma: Float): MaskFilter` — soft/glow edges; set on `Paint.maskFilter`. Real usage: `MaskFilter.makeBlur(FilterBlurMode.NORMAL, 20f)` (also 2f, 6f). Use for glow/shadow on shapes rather than whole-image blur.
- ⭐ `Shader.makeLinearGradient(p0: Point, p1: Point, colors, ...)` and the overload `makeLinearGradient(x0, y0, x1, y1, colors, positions, tileMode)` — color ramp along a line; assign to `Paint.shader`. Heavily used with `gradientOf(...)` colors.
- `Shader.makeRadialGradient(x, y, radius, colors, ...)` — radial color ramp (present in arts, mostly via commented templates).
- `Shader.makeSweepGradient(x, y, colors, ...)` — angular/conic ramp; used for `paint.shader = makeSweepGradient(...)`.
- ⭐ `BlendMode.*` on `Paint.blendMode` — most used in arts: `SCREEN` (×11, lighten/glow), `DIFFERENCE` (×6), `OVERLAY` (×4), `MULTIPLY` (×2, darken), `MODULATE` (×2). Also `PLUS`/additive available from Skia.
- `PathEffect.*` on `Paint.pathEffect` — most used in arts: `PathEffect.makeDiscrete` (×26, jitter/roughen lines), `makeDash` (×10, dashed strokes), `makeCorner` (×5, rounded corners). `makePath2D` / `makeLine2D` are used internally by `hatchPaint`/`dashPaint`.

### SkSL runtime shaders & prebuilt filters ⭐

Source: `dev/oblac/gart/shader/`

Core SkSL plumbing (`shader.kt`):
- ⭐ `String.sksl(): RuntimeShaderBuilder` — compiles an SkSL source string via `RuntimeEffect.makeForShader` into a builder; set uniforms with `.uniform(name, ...)`, then `.makeShader()` or `ImageFilter.makeRuntimeShader(...)`.
- ⭐ `Shader.toPaint(): Paint` — wraps a generated shader into a Paint.

Prebuilt image filters — each takes a `Dimension d` (for the `resolution` uniform) and returns an `ImageFilter` built with `ImageFilter.makeRuntimeShader(sksl, shaderName = "image", input = null)`; apply via `Canvas.saveLayer(filter)` or `paintOfImageFilter(filter)`:
- ⭐ `createNoiseGrainFilter(intensity: Float, d: Dimension): ImageFilter` — hash noise subtracted from all RGBA channels (affects alpha). Source: `filterNoiseGrain.kt`.
- ⭐ `createNoiseGrain2Filter(intensity: Float, d: Dimension): ImageFilter` — additive noise on RGB only. Source: `filterNoiseGrain2.kt`.
- ⭐ `createMarbledFilter(intensity: Float, d: Dimension): ImageFilter` — simplex-noise grain + fiber + random specks, paper-like. Source: `filterMarbled.kt`. (Note: takes `intensity` but the SkSL only declares `resolution`/`image`.)
- ⭐ `createRisographFilter(intensity: Float, randomization: Float = 0.5f, randomizationOffset: Float = 0.1f, d: Dimension): ImageFilter` — riso-print channel-max noise overlay. Source: `filterRisograph.kt`.
- ⭐ `createSketchingPaperFilter(contrast1: Float, contrast2: Float, amount: Float, d: Dimension): ImageFilter` — simplex noise mixed with a sine dot pattern, paper texture. Source: `filterSketchingPaper.kt`.

Prebuilt generative shaders (return `Shader`, not a filter):
- `createNeuroShader(time: Float, thickness: Float = 0.1f): Shader` — animated raymarched "neuro" pattern; pair with `Shader.toPaint()`. Source: `shaderNeuro.kt`.

CPU "shader" (pixader):
- `pixdraw(iResolution: Vec2, iTime: Float, pixelFunction: PixelFn)` and `suspend pixdrawAsync(iResolution, iTime, maxConcurrency, pixelFunction)` — per-pixel CPU shading; `PixelFn = (fragCoord: Vec2, iResolution: Vec2, iTime: Float) -> Vec4`; runs as a context-receiver on `Pixels`, blends result over existing pixels. Async version parallelizes via coroutine dispatcher. Source: `dev/oblac/gart/pixader/pixader.kt`.

### Dithering

Source: `dev/oblac/gart/dither/` (29 files). Almost all share the entry shape:

`fun dither<Name>(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256)` — quantizes `bitmap` in place to `colorCount` levels per channel; `pixelSize` groups pixels into NxN blocks (averaged) for chunky output.

Engine + kernels (`DitherKernels.kt`):
- `data class DitherKernelEntry(dx, dy, weight)` and `object DitherKernels` with kernels: `ATKINSON`, `FLOYD_STEINBERG`, `JARVIS_JUDICE_NINKE`, `STUCKI`, `BURKES`, `SIERRA3`, `SIERRA2`, `SIERRA_LITE`, `SHIAU_FAN1/2`, `WONG_ALLEBACH`, `FEDOSEEV`/`FEDOSEEV2/3/4`.
- `ditherErrorDiffusion(bitmap, kernel: Array<DitherKernelEntry>, pixelSize = 1, colorCount = 256)` — generic error-diffusion driver used by the named algorithms.

Named error-diffusion (each delegates to the driver, standard 3-arg signature):
- `ditherFloydSteinberg`, `ditherAtkinson`, `ditherBurkes`, `ditherStucki`, `ditherJarvisJudiceNinke`, `ditherSierra` (Sierra-3), `ditherTwoRowSierra`, `ditherSierraLite`, `ditherShiauFan1`, `ditherShiauFan2`, `ditherWongAllebach`, `ditherFedoseev`, `ditherFedoseev2`, `ditherFedoseev3`, `ditherFedoseev4`.

Serpentine / modulated variants:
- `ditherErrorDiffusionSerpentine(bitmap, kernel, pixelSize = 1, colorCount = 256)` — alternating scan direction.
- `ditherErrorDiffusionModulated(bitmap, kernel, pixelSize = 1, colorCount = 256)` — modulated weights.
- `ditherOstromoukhov(bitmap, pixelSize = 1, colorCount = 256)` — intensity-variable coefficients, serpentine (SIGGRAPH 2001).
- `ditherZhouFang(bitmap, pixelSize = 1, colorCount = 256)`, `ditherZhangPang(bitmap, pixelSize, colorCount, c: Double = 0.013)`.

Ordered / threshold / noise:
- `ditherOrdered2By2Bayer`, `ditherOrdered3By3Bayer`, `ditherOrdered4By4Bayer`, `ditherOrdered8By8Bayer` — Bayer-matrix ordered dithering (standard 3-arg).
- `ditherThreshold(bitmap, pixelSize = 1, colorCount = 256, noise: Double = 0.0)` — plain quantization, optional per-pixel threshold noise.
- `ditherWhiteNoise(bitmap, pixelSize = 1, colorCount = 256)` — white-noise threshold.
- `ditherBlueNoise(bitmap, pixelSize = 1, colorCount = 256, noiseWidth = 64, noiseHeight = 64, sigma = 1.5)` — void-and-cluster blue-noise threshold map (high quality, noise-free).

Advanced / parameterized:
- `ditherMarcu(bitmap, pixelSize = 1, colorCount = 256, kernel = FLOYD_STEINBERG, roadmap: MarcuRoadmap = TRIANGULAR, threshold2 = 0.1, noise = 0.0)`.
- `ditherEntropyConstrained(bitmap, pixelSize, colorCount, kernel = FLOYD_STEINBERG, c: Double = 7.6)`; plus `ditherEntropyConstrainedZhouFang(...)` and `ditherEntropyConstrainedOstromoukhov(...)`.
- `ditherContrastAware(bitmap, pixelSize, colorCount, maskSize: Int = 7, kParameter: Double = 2.6)`.
- `ditherLaplacian(bitmap, pixelSize, colorCount, kernel = FLOYD_STEINBERG, scaleFactor: Double = 0.5)`.
- `ditherVisualDifference(bitmap, pixelSize, colorCount, numIterations: Int = 3, uc: Double = 15.0, q: Double = 5.0, ...)`.

### Halftone & painterly

Halftone (`dev/oblac/gart/halftone/`):
- `renderHalftone(source: Pixels, target: Pixels, angle: Float, dotSize: Int, dotResolution: Int, color: RGBA = BLACK, isLayer: Boolean = false)` — variable-radius dot screen sampled along a rotated grid (brighter source → smaller dot), anti-aliased. Source: `Halftone.kt`.
- `halftoneProcess(source: Pixels, config: HalftoneConfiguration = HalftoneConfiguration()): Pixels` — full CMYK halftone (Y/M/C/K channels at TV screen angles, composited). `processHalftoneWithJoin(...)` is the less memory-efficient join variant. Source: `HalftoneProcess.kt`.
- `HalftoneConfiguration(dotSize = 10, dotResolution = 5, yellowAngle = 82.5f, cyanAngle = 112.5f, magentaAngle = 52.5f, keyAngle = 22.5f)` — Source: `HalftoneConfiguration.kt`.
- `extractColorChannel(source, channel: ColorChannel): Pixels`, `joinGrayscaleCMYKChannels(...)`, sealed `ColorChannel` (`CyanChannel`/`MagentaChannel`/`YellowChannel`/`KeyChannel`, with `ColorChannel.CYAN/MAGENTA/YELLOW/KEY`). Source: `CMYKProcessor.kt`.
- `map(value, minA, maxA, minB, maxB): Double` + `rotatePointAroundPosition(...)` helpers. Source: `HalftoneUtils.kt`.

Painter (`dev/oblac/gart/painter/SprayPainter.kt`):
- `class SprayPainter(width, height = width, bg = white, fg = black, rng = Random.Default)` — float RGBA accumulation buffer for stippling/spray. Key ops: `pixel(x,y)`/`pixel(p)`, `pixels(points, n = -1)`, `circle(cx,cy,radius,n)`, `stroke(a,b,n)`/`line`, `strokes`/`lines`, `path(points, n)`, `clear(color)`, `sample(x,y)`; render via `toBitmap(gamma = 1f): Bitmap` / `drawTo(canvas, gamma = 1f)`; `SprayPainter.loadPng(path)` factory. Porter-Duff "over" compositing, gamma at output.

Pixader (`dev/oblac/gart/pixader/pixader.kt`): CPU shader — see `pixdraw`/`pixdrawAsync` under SkSL section above.
## 7. Simulations & Generative Engines

The algorithmic heart of gart: strange attractors, reaction-diffusion, agent
sims (physarum, cellular, growth), fluid solvers, and N-body gravity. Almost all
engines follow the same loop shape — **construct** (with grid/params) → **step()**
(advance one frame, double-buffered, no per-step allocation) → **read state**
(accessors or `forEach`). Source root: `gart/src/main/kotlin/dev/oblac/gart/`.

Legend: ⭐ = highest-value engine. UNUSED = not referenced by any sketch in
`arts/`, `example/`, `work/`, `ppt/` (a gap = opportunity).

---

### 7.1 Strange attractors ⭐

`gart/.../attractor/` (20 files). A zoo of strange-attractor maps behind one
tiny interface. Each produces a stream of `Point3` you accumulate and plot.

**`Attractor.kt`** — the interface:
```kotlin
interface Attractor {
    fun compute(p: Point3, dt: Float): Point3              // one iteration
    fun computeN(p: Point3, dt: Float, n: Int): List<Point3>  // default: folds compute n times, returns n+1 points
}
```
Construct any attractor with its coefficient params (all have sensible defaults),
then `attr.computeN(Attractor.initialPoint, dt, n)` for a point cloud, or call
`compute` per-frame for live trails. Each class has a companion
`initialPoint: Point3` (good seed) — e.g. `LorenzAttractor.initialPoint = (0,20,12)`.
`dt` is the Euler step for ODE attractors; for 2-D maps `dt` is ignored.

`coefficients.kt`: `buildCoefficients(input: String): Array<Float>` maps each char
to `(code - 77) / 10f`. Used by the string-parameterized `Quadratic`/`Cubic`
attractors (Wolfram-style coefficient encoding).

Two kinds:
- **3-D ODE attractors** — `compute` does explicit forward-Euler integration:
  `x1 = x + dt * f(x,y,z)`. Full xyz evolution.
- **2-D maps** — `compute` is a direct iterated map `(x,y) -> (x',y')`, `z`
  passed through unchanged, `dt` unused.

| Attractor | File | Type | Character note |
|---|---|---|---|
| Lorenz | `LorenzAttractor.kt` | 3-D ODE | the classic butterfly; `sigma/rho/beta` |
| Rossler | `RosslerAttractor.kt` | 3-D ODE | single spiral band that folds — `a/b/c` |
| Chen | `ChenAttractor.kt` | 3-D ODE | double-scroll cousin of Lorenz; `alpha/beta/delta` |
| Dadras | `DadrasAttractor.kt` | 3-D ODE | swirling multi-lobe; `a/b/c/d/e` |
| FourWing | `FourWingAttractor.kt` | 3-D ODE | four-wing butterfly; `a/b/c` |
| Halvorsen | `HalvorsenAttractor.kt` | 3-D ODE | cyclically-symmetric tri-lobe; single `a` |
| LangfordAizawa | `LangfordAizawaAttractor.kt` | 3-D ODE | sphere-with-spike "Aizawa" torus; 6 params `a..f` |
| Lorenz84 | `Lorenz84Attractor.kt` | 3-D ODE | atmospheric-circulation variant; `a/b/f/g` |
| RabinovichFabrikant | `RabinovichFabrikantAttractor.kt` | 3-D ODE | delicate plasma whorls; `alpha/gamma` (numerically touchy) |
| Sprott | `SprottAttractor.kt` | 3-D ODE | minimal quadratic chaos; `a/b` |
| Thomas | `ThomasAttractor.kt` | 3-D ODE | cyclically-symmetric `sin()` flow; single `b` |
| ThreeScrollUnifiedChaotic | `ThreeScrollUnifiedChaoticAttractor.kt` | 3-D ODE | TSUCS, three scrolls; 6 params, large coeffs |
| Duffing | `DuffingAttractor.kt` | 2-D ODE (driven) | forced oscillator; keeps internal `t`, `cos(w*t)` drive |
| Clifford | `CliffordAttractor.kt` | 2-D map | dense filigree web of `sin/cos`; `a/b/c/d` |
| PeterDeJong | `PeterDeJongAttractor.kt` | 2-D map | swirling lace, `sin/cos`; `a/b/c/d` |
| SymmetricIcon | `SymmetricIconAttractor.kt` | 2-D map | rotationally-symmetric "icon" via complex power `d`; `l/a/b/g/o/d` |
| Quadratic | `QuadraticAttractor.kt` | 2-D map | 12-coeff polynomial from a 12-char string; `ONE = "CVQKGHQTPHTE"` |
| Cubic | `CubicAttractor.kt` | 2-D map | 20-coeff polynomial from a 20-char string; `ONE = "ISMHQCHPDFKFBKEALIFD"` |

Representative ODE attractor (Lorenz):
```kotlin
class LorenzAttractor(val sigma=10f, val rho=28f, val beta=8f/3f) : Attractor {
    override fun compute(p, dt) = Point3(
        p.x + dt*(sigma*(p.y-p.x)),
        p.y + dt*(p.x*(rho-p.z)-p.y),
        p.z + dt*(p.x*p.y - beta*p.z))
}
```
Representative 2-D map (Clifford): `x1 = sin(a*y)+c*cos(a*x); y1 = sin(b*x)+d*cos(b*y); z unchanged`.

**Usage:** `example/.../ExampleAttractor.kt` exercises ~17 of them;
`arts/triangular/v2/Triis.kt` uses `LangfordAizawa`. The family is well-explored
but several (Lorenz84, RabinovichFabrikant, ThreeScrollUnified, Sprott, FourWing)
appear only in the example sampler, not in finished art — ripe for a dedicated piece.

---

### 7.2 Reaction-diffusion ⭐

`gart/.../reactiondiffusion/` (6 files). Turing-pattern PDE solvers over a grid.

**`ReactionDiffusion.kt`** — the interface: `val width/height`, `var passes`
(per-step time multiplier), `fun step()`, `fun displayValue(x,y): Float` (≈0..1
for coloring), `fun reset()`. All models hold species as `FloatArray(w*h)` with
an internal double buffer swapped in `step()` — zero allocation per step.

**`GrayScott.kt`** ⭐ — the flagship. Two species U (substrate) / V (activator):
```kotlin
class GrayScott(width, height,
    var feed=0.037f, var kill=0.06f, var Du=0.21f, var Dv=0.105f, var passes=1f)
```
- read: `u(x,y)`, `v(x,y)`; write: `setU/setV(x,y,value)`
- seed: `stampU/stampV(cx,cy,radius,value)` (clipped disc) — perturb the `u=1,v=0`
  equilibrium with a V patch to nucleate coral/mitosis patterns
- `displayValue` returns V. Equations in the file KDoc.

**`FitzHughNagumo.kt`** — excitable-media (neural). Species u (potential) /
v (recovery), params `a0,a1,epsilon,delta,k1,k2,k3`. Integration step is
`passes*0.1`; both species clamped to [0,1]. Same `u/v/setU/setV/stampU/stampV`
API. Patterns: traveling fronts, target waves, spirals.

**`BelousovZhabotinskyContinuous.kt`** — continuous 3-species BZ (distinct from
the cellular BZ in §7.4). Species `a/b/c`, params `alpha,beta,gamma`, uses a 3×3
neighborhood average and cyclic coupling. API: `a/b/c`, `setA/B/C`, `stampA/B/C`;
`displayValue` returns C. Patterns: rotating spirals, target waves.

**`RDLaplacian.kt`** (internal helpers): weighted isotropic 9-point `laplacian(src,x,y,w,h)`
(center −6.828, cardinal 1.0, diagonal 0.707, edge-clamped) shared by GrayScott/FHN;
plus `stampDisc(...)` used by all the stamp helpers.

**`RDRender.kt`**: extension `ReactionDiffusion.renderTo(map: Gartmap, coloring: ColorRamp = ColorRamp.Default)`
— writes `coloring.colorAt(displayValue(x,y))` into a same-size `Gartmap` and
flushes. Typical: `repeat(steps){rd.step()}; rd.renderTo(map)`.

**Usage:** GrayScott + FHN in `example/ExampleReactionDiffusion.kt`,
`arts/orbitr/*` (TriangleInSpace, twelve_monkeys). `BelousovZhabotinskyContinuous`
appears ONLY in the example — UNUSED by finished art.

---

### 7.3 Physarum (slime mold) ⭐

`gart/.../physarum/Physarum.kt`. Jeff Jones agent transport-network model.
SoA agents (parallel `FloatArray` for x/y/heading, no per-agent objects) plus a
two-channel pheromone field; heavy loops run on a parallel `IntStream`. Scales to
hundreds of thousands of agents, GC-free.

```kotlin
class Physarum(val w: Int, val h: Int, agentCount: Int)
```
- **construct** then `init` auto-scatters agents and renders deposits.
- **step()** does: diffuse+decay field (parallel rows) → swap green buffer →
  sense/steer/move agents (parallel) → stamp deposits.
- **read** `val trail: FloatArray` (= the green channel, length `area = w*h`,
  row-major `y*w+x`, values in [0.01, 1]) → map to colors.
- **tunables (live)**: `sensorAngle`, `rotationAngle`, `sensorDistance`,
  `stepSize`, `decay`. Rule of thumb: SA<RA → networks, SA≈RA large → spots,
  RA tiny → flow.
- **presets**: `presetClassicNetwork()`, `presetCoarseHighways()`,
  `presetFineLace()`, `presetCells()`, `presetSilk()`.
- **seed/edit**: `scatter()`, `seedDisc(cx,cy,r)`, `seedLine(x1,y1,x2,y2,thickness,from,to)`,
  `draw(cx,cy,r,count)` (mouse drawing), `clear()`.

**Usage:** `example/ExamplePhysarum.kt`, `arts/cell/cell2/Cell2.kt`.

---

### 7.4 Cellular automata & BZ

`gart/.../cellular/`. A generic CA engine plus rule factories, and elementary
(Wolfram) CA generation.

**`CellAutomata.kt`** — `class CellularAutomata(width, height, ruleEngine: CellularAutomataRules)`.
8-neighbor grid of `Int` states, double-buffered.
- `operator get/set(x,y)`, `step()`, `forEach{ x,y,state -> }`, `cells(): List<Cell>`.
- `data class CellularAutomataRules(computeNextState, validateState, initialState)` —
  three lambdas defining a rule. `data class Cell(x,y,state)`.

**`BelousovZhabotinskyReaction1.kt`** / **`...Reaction2.kt`** — factory functions
`newBelousovZhabotinskyReaction1(n=255,k1=3,k2=5,g=50)` and
`newBelousovZhabotinskyReaction2(q=255,k1=3,k2=5,g=50)` returning
`CellularAutomataRules` (integer healthy/infected/ill BZ variants). Feed into
`CellularAutomata`.

**`rule/rules.kt`** — `generateRuleCellularAutomaton(rule, neighborsAside,
initialRow=List(256){it==128}, generations=256, wrapAround=true): List<List<Boolean>>`.
Wolfram elementary CA (Rule 30, 110, …) as a list of boolean rows; supports wider
neighborhoods via `neighborsAside`.

**Usage:** `arts/cell/cell/Cell1.kt` (BZ + CellularAutomata),
`arts/rule/Rule.kt` (elementary rules).

---

### 7.5 Differential growth

`gart/.../grow/Growth.kt`. A closed (or open) polyline that grows and avoids
self-intersection — the "differential growth" / space-filling worm.

```kotlin
class Growth(maxEdgeLength=5f, rejectionRadius=10f, attractionStrength=0.15f,
    rejectionStrength=0.5f, brownianStrength=0.4f, closed=true, maxNodes=200_000,
    centerX=0f, centerY=0f, maxRadius=+Inf, obstacleStrength=..., obstacleSampleFraction=0.25f)
```
Uniform spatial hash → O(1) neighbor lookup → many thousands of nodes.
- **seed**: `seedCircle(cx,cy,radius,count)`.
- **obstacles**: `setObstacle(points: List<Point>)` or `setObstacle(path: Path)`
  (auto-samples) — curve nodes repelled by obstacle samples.
- **step()**: brownian nudge + attraction to neighbor-midpoint + rejection within
  radius, then split edges longer than `maxEdgeLength` (perimeter grows). Sets
  `done=true` once any node exceeds `maxRadius`.
- **read**: `val xs/ys: FloatArray` + `var size`, `var done`; `toPath(): Path`.

**Usage:** `arts/rugae/Rugae.kt`, `arts/lines/growth/Grow.kt`,
`example/ExampleDynaGraphGrowth.kt`.

---

### 7.6 Particle / walker / midpoint (stateless helpers)

Small functional building blocks (no engine class, no `step()`):

- **`particle/particles.kt`** — `data class Particle(point: Point, vx: Double, vy: Double)`.
  Just a value type. UNUSED by sketches (no `gart.particle` import found).
- **`walker/walker.kt`** — random-walk steppers, all pure functions returning the
  next state:
  - `walkRandom(pos: Point, step=1f): Point` (jitter in disc)
  - `walkMomentum(m: Momentum, accel=0.05f, damping=0.95f): Momentum`
  - `walkRandom3D(pos: Vec3, step=1f): Vec3`, `walkMomentum3D(...)`
  - `data class Momentum(pos: Point, vel=Point(0,0))`, `data class Momentum3D(pos: Vec3, vel=Vec3.ZERO)`.
  UNUSED by sketches — a clean Brownian/momentum-walk primitive sitting idle.
- **`midpoint/midpointDisplacement.kt`** — `midpointDisplacementY(start, end,
  roughness, verticalDisplacement=..., numOfIterations=16): List<Point>`.
  1-D midpoint displacement (fractal terrain/skyline); final point count `2^iters+1`.
  Used by `arts/hills/hills3/Hills3.kt`.

---

### 7.7 Fluid solvers

`gart/.../fluid/` (13 files), three independent families:

**`fluid/all/` — semi-Lagrangian "stable fluids"** ⭐
- **`FluidSolver.kt`** ⭐ — `class FluidSolver(width, height, velocityScale=8,
  numJacobiSteps=3, maxVelocity=30f)`. Velocity field at reduced res
  (`velWidth/velHeight = ceil(w/scale)`), toroidal-wrapping. `step()` =
  advect → divergence → Jacobi pressure solve → subtract gradient (divergence-free).
  Interact: `applyForce(screenX,screenY,fx,fy,radius=30f)`; read:
  `velocityAt(screenX,screenY): Pair<Float,Float>`, `velocityU()/velocityV()/pressure(): FloatArray`,
  `reset()`.
- **`FluidParticles.kt`** — `class FluidParticles(width, height,
  startingParticles: List<Point>, lifetime=1000, numRenderSteps=3)`. SoA particle
  set advected through the solver. `update(solver)` (RK2, ages/respawns particles);
  `forEachParticle{...}`, `forEachParticleWithVelocity(solver){...}`, `reset()`, `count()`.
- **`FluidRenderer.kt`** — `class FluidRenderer(solver, particles, trailLength=15,
  blockSize=2)`; `renderFluid(particleRenderer: ParticleRenderer)` fades+blits
  trails; `clearTrails()`.
- **`ParticleRenderer.kt`** (interface: `clear()`, `renderPixel(x,y,value,blockSize)`)
  with impls **`ParticleRendererPalette`** (Palette-based, draws circles) and
  **`ParticleRendererTwoColors`** (lerp bg↔particle color, draws rects).
- **`renderFluidPressure.kt`** / **`renderFluidVelocityField.kt`** — free functions
  drawing the pressure field (blue↔red) / velocity arrows straight onto a `Canvas`.

**`fluid/navstr/` — Jos Stam "Real-Time Fluid Dynamics for Games"**
- **`NavierStokesSolver.kt`** — `class NavierStokesSolver(nx, ny, dt=1.0,
  viscocity=0.0, fadeSpeed=0.0, solverIterations=10)`. Grid `(nx+2)×(ny+2)` with
  `Double` arrays `r,u,v` (+ `rOld,uOld,vOld`, all public). `update()` advances;
  `addForceAtPos/addForceAtCell`, `randomizeColor()`,
  `indexForCellPosition/indexForNormalizedPosition`, `u(i,j)/v(i,j)` readers.
- **`NSSolverImplicit.kt`** — `class NSSolverImplicit(nx, ny, dx, dy, numIter=3)`.
  Compressible NS, ADI/implicit, staggered grid; `step(dt)` (or `stepVel/stepE`),
  `addEnergy/addPressure/setVelocity/setWall`, readers `u/v/p/e/t/r(i,j)`.
- **`TDMA.kt`** — internal `solveTDMA(n,a,b,c,v)` Thomas tridiagonal solver.

**`fluid/lbh/` — lattice-Boltzmann (D2Q9)**
- **`fluidBoltzmann.kt`** — `class BoltzmannFluid(overallVelocity, viscosity,
  rows, cols)`; `iterate()` step; `setSolid/resetSolids`, readers
  `velocity/velocityX/velocityY/density(r,c)`, `forEach(...)`.
- **`fluidLatticeBoltzmannSimple.kt`** — `class LatticeBoltzmannSimpleFluid(width,
  height, viscosity=0.01, relaxationParam=...)` with `data class Lattice(density,
  velocityX, velocityY, f[9], fEq[9])`. `init{ lattice,x,y -> }`, `simulate()`
  (collide→stream→update), `lattices{...}`, `operator get(x,y)`.

**Usage:** `FluidSolver` family in `example/ExampleFluidSimulation.kt` +
`arts/fluid/*`; `NavierStokesSolver` in `arts/sun/ns1`; `NSSolverImplicit` in
`arts/sun/ns2`; `BoltzmannFluid` in `arts/sun/two/*`; `LatticeBoltzmannSimpleFluid`
in `arts/sun/Sun.kt`. All fluid solvers are in active use.

---

### 7.8 N-body & gravitational

Three approaches: Barnes-Hut (many bodies), WHFAST (few, high accuracy), and
Gravitron (geometric line-bending, not a sim).

**`nbody/` — Barnes-Hut** ⭐
- **`BarnesHutSimulation.kt`** ⭐ — `class BarnesHutSimulation(G=1f, theta=0.7f,
  softening=0.01f)`. O(N log N) via quadtree + KDK leapfrog. Holds a
  `GravityParticles` (`val particles`) and a `QuadTree`.
  - add: `addParticle(x,y,vx,vy,mass)`, `addDisk(...)` (galaxy), `addUniform(...)`,
    `addPlummer(...)` (cluster).
  - `step(dt)` (kick-drift-kick, rebuilds tree each kick); `advance(dt, steps)`.
  - diagnostics: `time`, `count`, `treeNodeCount`, `totalEnergy()` (O(N²)),
    `approximateTotalEnergy()`, `reset()`.
  - companions: `galaxyCollision(...)`, `galaxy(...)`, `collapse(...)` presets.
- **`GravityParticles.kt`** — SoA particle store (`x,y,vx,vy,ax,ay,mass` public
  `FloatArray`, auto-growing). `add(...)`, `addAll(...)`, `bounds()`,
  `centerOfMass()`, `kineticEnergy()`, `kick(dt)`, `drift(dt)`,
  `clearAccelerations()`, `forEach{...}`. Plus `data class BoundingBox`
  (`toSquare/expand/contains`). Designed for 10^6+ particles.
- **`QuadTree.kt`** — flat-array Barnes-Hut tree (node pool, no objects).
  `build(particles, bounds)`, `computeAcceleration(px,py,particleIdx,theta,G,
  softening): Pair<Float,Float>`, `val size/root`. Internal to the sim.

**`whfast/` — Wisdom-Holman symplectic integrator** (orbital mechanics, few bodies)
- **`NBodySystem2D.kt`** — high-level manager. `class NBodySystem2D(G=1f)`.
  `addCentralBody(mass,name)`, `addBody(...)`, `addCircularOrbit(...)`,
  `addFromElements(...)`; `step(dt, scheme=DKD)`, `advance(dt,steps,scheme)`,
  `advanceTo(targetTime,...)`; `bodies`, `size`, `time`, `totalMass`,
  `getOrbitalElements(i)`, `snapshot()/restore()`, `reset()`. Companions
  `innerSolarSystem()`, `twoBody(...)`, `figureEight()`.
- **`WHIntegrator2D.kt`** — core integrator. `class WHIntegrator2D(G=1f,
  corrector=true)`, `enum Scheme{DKD,KDK}`. `step(...)`, `integrate(...)`,
  `integrateWithHistory(...)`; companion `totalEnergy/totalAngularMomentum/
  centerOfMass/centerOfMassVelocity`. Splits Hamiltonian into analytic Kepler
  drift + interaction kicks; excellent long-term energy conservation.
- **`Body2D.kt`** — `data class Body2D(position: Vec2, velocity: Vec2, mass, name)`
  with `kineticEnergy`, `drift/kick/withPosition/...` and companions
  `atOrigin`, `circularOrbit`.
- **`OrbitalElements2D.kt`** — `data class (a,e,omega,M,mu)` Keplerian elements;
  `period`, `fromCartesian(...)`, `circular(...)`.
- **`KeplerSolver.kt`** — `object`: `solveElliptic/solveHyperbolic(M,e)`
  (Newton-Raphson + Danby starter), `eccentricToTrue`, `radiusFromTrue`, etc.
- **`CoordinateTransform.kt`** — `object`: `toCartesian/toOrbitalElements`,
  `advanceOrbit`, `toJacobi/fromJacobi`.
- **`whfast.kt`** — package doc only (file index, no code).

**`gravitron/Gravitron.kt`** — NOT a particle sim. `data class Gravitron(x, y,
radius, angle=180°)`: bends a `Line` that enters its circle into a circular arc
and returns the continuing `Line` (or null). `applyTo(line: Line, path: PathBuilder): Line?`.
A geometric "gravity lens" for path drawing.

**Usage:** `BarnesHutSimulation` in `arts/orbitr/Orbirt.kt`; `Gravitron` in
`example/ExampleGravitron.kt` + `arts/lines/swing2/Swing2.kt`. **WHFAST is UNUSED
by finished art** (only `example/ExampleWhfast.kt`) — a polished symplectic
orbital engine waiting for a sketch. `GravityParticles`/`QuadTree` are only used
internally by Barnes-Hut.

---

#### Gaps / opportunities (unused by finished art)
- `BelousovZhabotinskyContinuous` (continuous BZ spirals) — example only.
- WHFAST orbital system (`NBodySystem2D` / `WHIntegrator2D`) — example only.
- `walker/*` Brownian/momentum walkers — referenced nowhere.
- `particle/Particle` value type — referenced nowhere.
- Several attractors (Lorenz84, RabinovichFabrikant, ThreeScrollUnified, Sprott,
  FourWing) appear only in the example sampler, never in a standalone piece.
## 8. Spatial Structures, Point Algorithms & 3D

Geometry/spatial toolkits: triangulation & Voronoi, distance fields, stippling, circle packing, spatial hashing, a graph-layout model, a 3D rasterizer/ray renderer, and 2D ray reflection. All 2D point types are `org.jetbrains.skia.Point`; 3D uses `dev.oblac.gart.vector.Vec3` (`vector/vector3.kt`). "UNUSED by art" = no callers under `arts/`/`example/`/`work/`/`ppt/` (a usability gap).

### Triangulation & Voronoi

⭐ **`Delaunator(points: List<Point>)`** — Kotlin port of Mapbox Delaunator; very fast 2D Delaunay triangulation. Triangulates in `init`/`update()`; main read: `triangles(): List<Triangle>` (each `gfx.Triangle(a,b,c: Point)`). Internal arrays `_triangles`/`_halfEdges`. Free helpers in same file: `circumradius(...)`, `circumcenter(...): FloatArray`, `quicksort(...)`. Used: triangular, kaleiircle, flowforce/vorflow. `gart/.../triangulation/Delaunator.kt`

⭐ **`delaunayToVoronoi(triangles: List<Triangle>): List<VoronoiCell>`** — builds Voronoi diagram as the dual of a Delaunay triangulation (circumcenters of triangles sharing each edge become Voronoi edges). `data class VoronoiCell(val site: Point, val edges: List<Line>)` with `toPathPoints(): List<Point>` (orders edges into a closed path). Also `normalizeEdge(edge: Pair<Point,Point>)`. Construct→use flow: `Delaunator(pts).triangles()` → `delaunayToVoronoi(...)`. Used: triangular, kaleiircle, flowforce/vorflow. `gart/.../triangulation/voronoi.kt`

- **`voronoi(x: Float): Float`** — 1D *cellular-noise* value function (min distance to cell-noise feature points), unrelated to the diagram above. UNUSED by art. `gart/.../triangulation/voronoi_simple.kt`

### Jump flooding / distance fields

- **`Jfa(d: Dimension)`** — CPU jump-flooding for signed distance fields of a `Path` in O(log N) passes.
  - `computeDistanceField(path: Path): JfaResult` — rasterizes path, seeds edge pixels, JFA passes; returns distances + inside mask.
  - `outlinePath(path: Path, outlineWidth: Float, outerOnly: Boolean = false): Path` — convenience: distance field → contour.
  - **`JfaResult(distances: FloatArray, inside: BooleanArray, width, height: Int)`** with `tracePath(threshold: Float, outerOnly: Boolean = false): Path` (marching-squares contour). Used: pixelmania/cosmic, lines/outline. `gart/.../jfa/Jfa.kt`

### Stippling

Package `stipple`. Public API is 4 top-level functions + `WangTileSet`; `stipple/util/` (Delaunay, GeomUtils, PoissonDisk, SeamGraph, VoronoiDiagram) is internal implementation. The util package has its OWN `Vec2`-based internal Delaunay/Voronoi separate from the `triangulation` package.

⭐ **`stippleVoronoi(pixels: Pixels, pointCount: Int = 5000, iterations: Int = 50, gamma: Float = 1.0f, brightnessThreshold: Float = 0.95f, overshoot: Float = 1.8f, initialJitter: Float = 0.5f, minRadius: Float = 0.5f, maxRadius: Float = 4.0f, seed: Int = 42): List<StippleDot>`** — weighted Voronoi stippling with **Lloyd relaxation**; density follows image darkness, radius follows cell darkness. The only stipple fn that *returns* dots. `data class StippleDot(val x: Float, val y: Float, val radius: Float)`. Used: pixelmania/cosmic, pixelmania/waves, example. `gart/.../stipple/VoronoiStippling.kt`

- **`stippleDots(bitmap: Pixels, dotSize: Int = 8, gap: Float = 0f, foreground = 0xFF000000, background = 0xFFFFFFFF)`** — grid dot-dithering; each cell → circle sized by darkness. Mutates `bitmap` in place (returns Unit). Used: example. `gart/.../stipple/Dots.kt`
- **`stippleNoisyDotDensity(b: Pixels, step: Int = 6, minRadius: Float = 0.8f, maxRadius: Float = 1.8f, density: Float = 0.72f, seed: Int = 42, backgroundColor: Int = Color.WHITE)`** — jittered probabilistic dots, darkness-driven. Mutates in place. Used: pixelmania/waves, pixelmania/light, example. `gart/.../stipple/NoisyDotDensity.kt`
- **`stippleWangTile(bitmap: Pixels, tileSet: WangTileSet, tonalRange: Int = 100_000, maxDepth: Int = 5, minSize: Int = 8, foreground, background)`** — recursive Wang-tile (Poisson) stippling, mutates in place. With **`WangTileSet.generate(numColors: Int = 2, samplesPerTile: Int = 1000, seed: Int = 42): WangTileSet`** (companion factory; tiles = numColors⁴). Used: example only. `gart/.../stipple/WangTileStippling.kt`, `WangTile.kt`
- **`generatePoissonDisk(desiredSamples, initialRadius = 0.15f, attemptsPerRadius = 1000, radiusDecreaseFactor = 0.99f, seed = 42): List<PoissonSample>`** + `data class PoissonSample(x, y, radius, ranking: Float)` — public, tileable toroidal Poisson-disk sampler in unit square. `gart/.../stipple/util/PoissonDisk.kt`

### Circle packing

⭐ **`CirclePacker(width: Float, height: Float, growth: Int = 1, numGrid: Int = 15, padding: Int = 1)`** — grid-accelerated growing-circle packer; keeps a spatial grid + `val items: MutableList<Circle>`.
  - `tryToAddCircle(x, y, minRadius: Float = 0f, maxRadius: Float = 900f): Circle?` — grows a circle at (x,y) until it touches a neighbor; null if it can't even fit at minRadius.
  - `removeCircles(x, y, radius: Float)` — removes circles within a region (for carving holes/animation).
  - `pack(tries: Int, minRadius = 0f, maxRadius = 900f): List<Circle>` — random scatter loop. Used: fluid, bubbles (holes/bubbub/stripe/stripe2). `gart/.../pack/CirclePacker.kt`

- **`simpleCirclePacker(rect: Rect, attempts: Int = 100_000, minRadius = 5f, maxRadius = 20f, growth = 1, padding = 20, isInside: (Circle) -> Boolean = { true }): List<Circle>`** — O(n²) brute-force grow-and-test packer with a containment predicate; no grid. Used: fluid, bubbles. `gart/.../pack/simpleCirclePacker.kt`

### Spatial hashing

⭐ **`HashGrid(radius: Float)`** — uniform spatial hash (cellSize = radius/√2) for O(1) neighbor/free-space queries on points; each point can carry an `owner: Any?`.
  - `insert(point: Point, owner: Any? = null)`, `points(): Sequence<Pair<Point,Any?>>`, `random(random): Point`, `var size`.
  - `isFree(query: Point, ignoreOwners: Set<Any> = emptySet()): Boolean` — is query ≥ radius from all existing points (the core min-distance test).
  - Extensions: `List<Point>.hashGrid(radius): HashGrid`; `List<Point>.filter(radius): List<Point>` (Poisson-style thinning to min spacing). Used internally by `noise/OrxPoissonDiskSamplingNoise.kt` and `flow/StreamlineTracer.kt` — UNUSED directly by art. `gart/.../hashgrid/HashGrid.kt`

### Graph layout

Package `dynagraph` (11 files). NOT an automated force-directed simulator — it is a positioned multi-group undirected graph with builders, graph algorithms, geometry cleanup, and *single-step* relaxation primitives the caller drives in their own loop.

- **`DynaGraph(maxVertices: Int = 100_000, initialGroupCapacity: Int = 16)`** — central type: a fixed pool of positioned vertices + named `Graph` topology groups (`GroupId`, default `DynaGraph.MAIN`). Geometry: `point(v)`, `vec(v)`, `x(v)`/`y(v)`, `setPoint(v, ...)`. Groups: `group(id = MAIN)`, `ensureGroup(id)`. Mutations return `MutationResult` (`Ok`/`Failure`, `isOk`, `newVert`): `addVert(p)`, `addEdge(a, b, group)`, `delEdge`, `moveVert`, `appendEdge`, `vaddEdge`, `splitEdge`, `appendEdgeSegX`. `DynaGraph.kt`, `MutationResult.kt`
- **`Graph(initialCapacity = 16)`** — topology-only adjacency (no geometry): `add/del(a,b)`, `neighbors(v)`, `edges(): Sequence<Edge>`, `loop(): List<Int>?`. `data class Edge(val a, val b: Int)`. `Graph.kt`
- **builders.kt** (DynaGraph extensions): `addCircle(center, radius, count, group, closed=true)`, `addPolygon(...)`, `addLine(a, b)`, `addPath(points, ..., closed=false)`, `addPathByVertices(...)`.
- **relaxation.kt** — the "forces" (each = one pass, caller loops): `relaxSprings(restLen, k = 0.1f, group)` (Hooke per edge), `smoothLaplacian(alpha = 0.2f, group)`, `repelVertices(radius, strength = 0.5f, group)` (O(n²)), `pinned(vararg vs, block)` (anchors).
- **draw.kt**: `Canvas.drawDynaGraph(graph, group, paint)` (auto path-vs-edges), `drawDynaGraphEdges/Vertices`, `DynaGraph.toPath(group): Path`; `SprayPainter` stippled variants.
- **operations.kt** (BFS, `connectedComponents`, `shortestPath`), **mst.kt** (`minimumSpanningTree(targetGroup, maxLen)` Euclidean MST/Kruskal), **queries.kt** (`bounds`, `centroid`, `center`, `relativeNeighborhood`), **cleanup.kt** (`mergeCloseVertices`, `subdivideLongerThan`, `simplifyChain` Douglas-Peucker, etc.), **groupOps.kt** (set algebra `unionInto`/`intersectInto`/`differenceInto`).
- Flow: construct → build (builders/MST) → relax loop (springs/repel/smooth + pinned) → cleanup → `drawDynaGraph`. Has unit tests; in art used only by `example` (ExampleDynaGraph, ExampleDynaGraphGrowth) — UNUSED by real art pieces (gap).

### 3D rendering

Package `tri3d` (10 files). Two visibility engines: a back-face-culling rasterizer (`ZBuffer`) and a two-sided ray renderer (`VolumetricLight`/`RayMesh`). Used by arts/td (mesh/backbone/pandora), example/Example3D, ppt/slide13.

- **domain.kt**: `data class Face(val a, val b, val c: Vec3, val color: Int)` with `normal(): Vec3 = (b-a).cross(c-a)`; `data class Mesh(val faces: List<Face>)`. (Vertex = `Vec3`, triangle = `Face`; no separate Vertex/Triangle3 types.)
- ⭐ **`Camera(screenCx: Float, screenCy: Float, scale: Float, distance: Float)`** — perspective intrinsics; eye at `(0,0,-distance)`.
  - `project(v: Vec3): Point` — perspective divide (`s = distance/(distance+v.z)`).
  - `unproject(screenX, screenY, depth): Vec3`, `rayDirection(screenX, screenY): Vec3`, `depth(v): Float`, `isFrontFacing(face): Boolean`. `Camera.kt`
- **`CameraPose(position: Vec3, yaw = 0f, pitch = 0f)`** — extrinsics; `toCameraSpace(p/face/mesh)` applies translate + rotate. `CameraPose.kt`
- ⭐ **`Scene`** (object) — render facade:
  - `rasterize(camera, mesh, screenWidth, screenHeight, background = 0, shading = Shading.flat): ZBuffer`
  - `render(canvas, camera, mesh, w, h, background = 0, shading = Shading.flat)` — rasterize + draw.
  - `renderVolumetric(camera, mesh, w, h, vl: VolumetricLight): Gartvas`. `Scene.kt`
- **`ZBuffer(width, height, shading = Shading.flat)`** — per-pixel depth buffer (barycentric, perspective-correct, culls back faces): `clear(background = 0)`, `rasterize(camera, mesh)`, `toImage()`, `drawTo(canvas, x = 0f, y = 0f)`. `ZBuffer.kt`
- **`RayMesh`** (object) — ray casting against a `Mesh` (Möller-Trumbore): `firstHit(origin, dir, mesh, maxT = MAX, epsilon = 1e-4f): Hit?` (`data class Hit(val t: Float, val face: Face)`), `isOccluded(origin, dir, mesh, maxT, epsilon): Boolean` (shadow rays). `RayMesh.kt`
- **Lighting** (LightSource.kt): `data class LightSource(position: Vec3, color = white)`; `fun interface Shading { color(face, normal): Int }` with `Shading.flat` and `Shading.diffuse(light, ambient = 0.2f, strength = 1f, falloff = Falloff.NONE): Shading`.
- **VolumetricLight.kt**: `data class VolumetricLight(lights: List<LightSource>, samples = 10, strength = 1f, blendMode = ADD, falloff = INVERSE_SQUARE, maxDistance = 100f, antiAlias = 1, seed = 0L, ambient = 0.1f, background = 0)` with `render(camera, mesh, w, h): Gartvas` (two-sided ray trace + scattering) and `apply(zBuffer, camera, mesh)` (in-place post-pass). Enums `VolumetricBlend{ADD,SCREEN,REPLACE}`, `Falloff{NONE,INVERSE,INVERSE_SQUARE}`.
- **meshes.kt** — only two primitives: `cube(colors: IntArray): Mesh` (unit cube [-1,1]³, colors[0..5] = front/back/left/right/top/bottom), `sphere(stacks: Int, slices: Int, colorFn: (Int,Int) -> Int): Mesh` (UV sphere). No plane/cylinder/cone/torus.
- **rotation.kt** — `rotateX/Y/Z(v: Vec3, angle: Float): Vec3` and `Face.rotateX/Y/Z(angle): Face`.

### Ray tracing (2D mirror reflection)

Package `ray` — 2D ray-vs-mirror reflection (not 3D path tracing).
- `data class Ray(val dline: DLine, val intensity: Float = 1.0f)`; `data class RayTrace(val iteration: Int, val ray: Ray, val from: Point, val to: Point?)`. `ray/ray.kt`
- **`Mirror(p1: Point, p2: Point, reflectivity: Float = 1.0f)`** — `val line = Line(p1,p2)`; `reflect(ray: Ray): Pair<Point, Ray>?` (R = I − 2(I·N)N, intensity × reflectivity). Extension `List<Mirror>.toPath(): Path`. `ray/mirror.kt`
- **`traceRayWithReflections(ray: Ray, mirrors: List<Mirror>, maxReflections: Int): List<RayTrace>`** — iteratively reflects off nearest mirror until max bounces or intensity < 0.01. Used: rayz (mirror/mirror2/mirr). `ray/trace.kt`

### Perspective & glass (brief)

- **`Block3D(left, right: Poly4, top, bottom: Poly4?)`** — two-point-perspective box (3 visible faces). Companion `Block3D.of(vpLeft, vpRight, frontBottom: Point, height, leftWidth, rightWidth: Float): Block3D` picks top/bottom face from horizon. `faces(): List<Poly4>`, `horizontalFace(): Poly4?`. Pure 2D vanishing-point geometry (no camera). Used: skyscraper, lines/nocube, lines/boks, example. `perspective/block3d.kt`
- **`drawGlassBall(g: Gartvas, cx, cy, radius: Float, eta = 1.0/1.5, thickness = 1.2, baseColor = BLACK, whiteSpot = false, rimDarkening = true)`** — per-pixel spherical Snell's-law refraction of existing canvas pixels + Fresnel/highlight overlays. Used: flowforce/Orb1, Orb3. `glass/glassBall.kt`
- **`drawGlassPath(g: Gartvas, path: Path, eta = 1.0/1.5, thickness = 1.2, baseColor = BLACK, whiteSpot = false)`** — same refraction effect over an arbitrary closed path (binary-search effective radius per angle). UNUSED by art (gap — only `drawGlassBall` is used). `glass/glassPath.kt`
---

## 9. Idioms & worked examples

The recurring patterns that turn the primitives above into a piece.

### Pixel-buffer → canvas (the `Gartmap` blit)

For anything computed per-pixel (density plots, noise images, reaction-diffusion, raymarchers):

```kotlin
val map = Gartmap(g.d)                 // in-memory ARGB IntArray; index = y*W + x
for (i in 0 until g.d.area) map.pixels[i] = argbInt   // or: map[x, y] = color
map.drawToCanvas(g)                    // push buffer onto the Gartvas canvas
```

- Colors are **ARGB ints** (`0xAARRGGBB`); `Gartmap` handles the BGRA byte conversion internally — don't swizzle.
- `Pixels` has no `get(offset)`, so read with `map.pixels[i]` (write with `map[i] = c` or `map.pixels[i] = c`).
- For GPU post-fx, snapshot the buffer to a Skia `Image` and re-draw it with a paint:

```kotlin
val img = map.image()                  // immutable Skia Image of current pixels
c.drawImage(img, 0f, 0f, Paint().apply {
    imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
    blendMode = BlendMode.SCREEN       // additive bloom
})
```

### "The process is the picture" — the trilogy

The house style: one simple local rule, run to emergence, where **every mark is colored by *when* or *how much*** — not painted. Three modalities, three reference files:

| Piece | File | Algorithm | "When/how-much" → visual | Lock |
|---|---|---|---|---|
| **rugae** | `arts/cell/src/rugae/Rugae.kt` | differential growth (a folding curve) | snapshots layered oldest→newest, aged color gradient (teal→ember) | `Random(47)` |
| **nervure** | `work/src/nervure/Nervure.kt` | space colonization (a branching network) | color by **birth-step** (age); stroke width by **subtree-tip count** (Murray's law); ember spark on newest tips | seed `9` |
| **corona** | `work/src/corona/Corona.kt` | strange-attractor density bloom (a chaotic field) | **density → luminosity** via `log` tone-map on a solar-ember ramp; cool tint on highest-orbit-velocity filaments | curated params, seed `4` |

Shared recipe: (1) a deterministic engine seeded by a single knob; (2) accumulate history (snapshots / birth-step / visit-density); (3) map that history through `Palette.expand(N).safe(idx)`; (4) finish with glow (`ImageFilter`/`MaskFilter` blur + SCREEN), a single contrasting accent, and an edge vignette (`makeRadialGradient`).

### Density / accumulation rendering (corona pattern)

For attractors and other "plot millions of points" pieces:

1. **Warm up** (discard the transient), then **auto-fit** the bounding box to the frame (sample ~1M points for min/max, build a scale+offset transform).
2. **Bilinearly splat** each visit into a `FloatArray(W*H)` density buffer (spread across the 4 surrounding pixels) → smooth filaments instead of integer-grid grain. Track a parallel buffer (e.g. orbit step-length) for a secondary channel.
3. **Multi-thread** with per-thread buffers merged in fixed order (cheap parallelism; the invariant measure converges regardless).
4. **Tone-map**: `t = ln(1+density)/ln(1+maxDensity)`, then `t.pow(GAMMA)` (≈0.4–0.6) to lift the smoke; sample the palette by `t`.

### Determinism & the parameter sweep

Picking the seed/params is the artistic act. Render many, hand-pick:

```bash
./gradlew :work:classes :work:writeClasspath -q
bash -c 'for i in $(seq 0 9); do java -Dheadless -Dseed=$i -Dout=work/sweep-$i.png \
  @work/build/classpath.txt work.<name>.<Name>Kt; done'
# contact sheet (rows via +append, stacked via -append):
magick work/sweep-0.png work/sweep-1.png work/sweep-2.png work/sweep-3.png work/sweep-4.png \
  -resize 300x300 +append /tmp/r1.png
magick work/sweep-5.png work/sweep-6.png work/sweep-7.png work/sweep-8.png work/sweep-9.png \
  -resize 300x300 +append /tmp/r2.png
magick /tmp/r1.png /tmp/r2.png -append /tmp/sweep.png   # then view it
find work -maxdepth 1 -name 'sweep-*.png' -delete        # clean up after picking
```

### Untapped engines (gaps = opportunities)

The catalogue surfaced powerful primitives **with no finished art piece** — strong starting points for the next "hit":

- **`GrayScott`** reaction-diffusion (`reactiondiffusion/`) — Turing patterns; `renderTo(Gartmap, ColorRamp)` built in. Siblings `FitzHughNagumo`, `BelousovZhabotinskyContinuous` likewise unused.
- The **strange-attractor zoo** beyond the corona maps — 17 attractors in `attractor/` (Clifford/De Jong used by corona; Lorenz, Thomas, Halvorsen, etc. unused as stills).
- **`stippleVoronoi`** + Lloyd relaxation (`stipple/`), **`CirclePacker`** (`pack/`), **`Delaunay`/Voronoi** (`triangulation/`), **`dynagraph`** force-directed layout — graphic/structural aesthetics barely touched.
- **WHFAST** symplectic orbital integrator (`whfast/`) and **`physarum`** slime-mold — only in `example/` demos.
