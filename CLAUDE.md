# CLAUDE.md

## Project Overview

**Gȧrt** is a Kotlin micro-framework for generating and rendering generative art - still images and movies. It uses Skiko (Skia bindings), JBox2D for physics, and FFmpeg for video encoding.

## Build Commands

```bash
just build      # Build the project
just test       # Run tests
just hotreload  # Continuous compilation for hot reload development
just readme     # Generate README gallery
just thumbs     # Generate thumbnail images
just clean      # Clean build artifacts
```

Direct Gradle commands:
```bash
./gradlew test                    # Run all tests
./gradlew :gart:test              # Run tests for core module only
./gradlew :example:compileKotlin  # Compile examples
```

## Module Structure

- **`gart`** - Core framework library (Skiko graphics, colors, math, noise, particles, etc.)
- **`gart-box2d`** - JBox2D physics integration
- **`example`** - Interactive examples and templates
- **`arts/*`** - Individual generative art projects (45+ modules).

## Core Architecture

**Entry Point:** `Gart.of("name", width, height)` creates a framework instance.

**Key Classes:**
- `Gart` - Factory and main entry point
- `Gartvas` - In-memory canvas wrapper around Skia Canvas
- `Window` - Swing window for interactive display
- `Movie` - Frame buffer for MP4/GIF video recording
- `Frames` - Frame timing and animation control
- `GartRand` - Deterministic random with replay capability

**Drawing Patterns:**
```kotlin
// Static image
val gart = Gart.of("name", 512, 512)
val g = gart.gartvas()
g.canvas.clear(Colors.white)
// ... draw ...
gart.saveImage(g)

// Interactive window
gart.window().show { canvas, dimension, frames ->
    // ... animation code ...
}

// Hot reload development
gart.window().hotReload(gart.gartvas())
```

## Key Packages (in `dev.oblac.gart`)

| Package | Purpose |
|---------|---------|
| `color` | Colors, palettes (Palettes.cool1-76, NipponColors, RetroColors) |
| `gfx` | Graphics primitives (Point, Line, Poly4, Circle, etc.) |
| `perspective` | 3D perspective (Block3D for two-point perspective) |
| `noise` | Perlin/Simplex noise generation |
| `attractor` | Strange attractors (Lorenz, Clifford, etc.) |
| `math` | Mathematical utilities |
| `vector` | 2D/3D vector operations |

## Conventions

- Examples go in `example/src/main/kotlin/dev/oblac/gart/`
- Art projects go in `arts/<project-name>/`
- Use `Palettes.coolN` or `Palettes.mixN` for color palettes
- Use `toFillPaint()` extension to convert Int colors to Paint
- Use `drawPoly4()` for drawing quadrilateral polygons
