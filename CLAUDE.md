## Project Overview

**Gȧrt** is a Kotlin micro-framework for generating and rendering generative art (still images and movies). Uses Skiko (
Skia bindings), JBox2D for physics, FFmpeg for video encoding.

## Build Commands

```bash
just build      # Build the project
just test       # Run tests
just clean      # Clean build artifacts
```

## Module Structure

- **`gart`** - Core framework library (Skiko graphics, colors, math, noise, particles, etc.)
- **`gart-box2d`** - JBox2D physics integration
- **`example`** - Interactive examples and templates
- **`arts/*`** - Individual generative art projects (45+ modules)

## Core Architecture

Entry point: `Gart.of("name", width, height)`. Key classes: `Gart` (factory), `Gartvas` (canvas wrapper), `Window` (
Swing display), `Movie` (video recording), `Frames` (animation timing), `GartRand` (deterministic random).

Key packages in `dev.oblac.gart`: `color` (palettes), `gfx` (primitives), `perspective` (3D), `noise` (Perlin/Simplex),
`attractor` (strange attractors), `math`, `vector`.

## Conventions

- Examples go in `example/src/main/kotlin/dev/oblac/gart/`
- Art projects go in `arts/<project-name>/`
- Use `toFillPaint()` to convert Int colors to Paint
