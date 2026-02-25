# gȧrt

🧧 gënérative ȧrt made by pragmatic [kotlin](WhyKotlin.md) micro-framework.

> ❤️ [Instagram](https://www.instagram.com/gart_173) / [Online Gallery](https://igo.rs/gart)

⭐️ **Gȧrt** is a pragmatical framework and set of utilities for generating and rendering
**still images** and **movies**.

+ uses [Skiko](https://github.com/JetBrains/skiko) (by JetBrains) for [Skia](https://skia.org) binding.
+ `box-2d` for physics simulation.
+ `ffmpeg` for video encoding.

🤷‍♂️ Honestly, I put the framework together for my own needs, so it’s a bit rough around the edges and the naming isn’t
great. _It’s not how I normally write code._

♻️ Continuous build for **Hot Reload**:

```shell
just hotReload
```

## 🚀 Features

There is really **a lot of features**, and I’m not sure how to organize them, but here’s an attempt at categorization:

Core Framework

- Gart — Factory/entry point (Gart.of("name", w, h))
- Gartvas — Canvas wrapper (Skia Surface)
- Window / WindowView — Swing display with keyboard/mouse handlers
- Movie — MP4/GIF video recording (FFmpeg + GifSequenceWriter)
- Frames — Frame timing, FPS control
- GartRand — Deterministic random with replay
- Sprite — Image with chainable transforms (rotate, translate, scale, flip)

Color

- Color spaces: RGBA, HSL, HSV, HSI, LAB, LCH, OKLAB, OKLCH, CMYK
- Palettes: 76 cool + 15 mix + 133 colormaps (Carto, CET, ColorBrewer, Matplotlib, Plotly, Tableau, etc.)
- Named colors: CssColors, NipponColors, RetroColors, MidCenturyColors
- Functions: blendColors, lerpColor, colorDistance, toFillPaint, toStrokePaint

Geometry & Graphics

- Primitives: Point, Line, DLine, Circle, Triangle, Poly4, Rect, GridRect, RectIsometric
- Collections: Points, PointsTrail
- Drawing: drawCircle, drawLine, drawPoly4, drawTriangle, drawRotatedRect, fatLine, n-gon, arc, ring, spiral, wave,
  grid, border, moon, tree, human
- Paint helpers: strokeOf, fillOf, hatchPaint, dashPaint

Math & Vectors

- Vectors: Vector2/3/4, Matrix2, dot/cross product
- Complex numbers: Complex, ComplexField, ComplexPolynomial, transcendental functions
- Curves: Lissajous, GaussianFunction
- Utilities: clamp, map, lerp, smoothstep, frac, mod, distance, primes, fastSqrt
- Angles: Radians, Degrees with trig functions

Noise & Sampling

- Noise: Perlin, multi-octave PerlinNoise, cell noise, FBM
- Sampling: Halton sequence, Poisson disk sampling
- NoiseColor — Noise-driven color generation

Curve Smoothing

- B-spline, Cardinal spline, Catmull-Rom spline, Chaikin, quadratic smoothing

Physics & Simulation

- Attractors (19): Lorenz, Clifford, Rössler, Duffing, Thomas, Chen, Sprott, etc.
- N-body: BarnesHut simulation, QuadTree, GravityParticles (10^6+)
- Orbital mechanics (WHFast): Wisdom-Holman integrator, Kepler solver, orbital elements
- Fluid dynamics: Navier-Stokes solver, Lattice Boltzmann, FluidSolver with particle rendering
- Cellular automata: Elementary rules, Belousov-Zhabotinsky reaction
- Force fields: ForceField, Flow, ForceGenerator
- Particles: Particle system, Gravitron

Spatial & Triangulation

- Delaunay triangulation (Mapbox Delaunator port)
- Voronoi diagrams
- HashGrid — Spatial hash for fast neighbor queries
- Circle packing — CirclePacker
- Jump Flooding Algorithm — Distance fields

Pixel Processing

- Filters: Gaussian blur, motion blur, grayscale, liquify, moiré, pixel sorting, flood fill, scaling
- Dithering (12): Floyd-Steinberg, Bayer (2x2–8x8), Atkinson, Burkes, Jarvis-Judice-Ninke, Sierra, Stucki
- Halftone: CMYK separation, configurable screening

Shaders & Effects

- Shaders: neuro, marbled texture, noise grain, risograph, sketching paper
- FX: blur, borderize, pixelate, scale
- Glass/refraction: glassBall, glassPath
- Ray tracing: ray, trace, mirror
- 3D perspective: block3d (two-point perspective)

Generators

- Spirograph, harmonograph, midpoint displacement (terrain)

Utilities

- Hot reload (file watcher + dynamic class loader)
- Font loading, text rendering
- Image conversion, resource loading
- Array/list/loop/range/sequence helpers

## 🧪 Example

Example folder contains several small projects demonstrating various features of the framework.

+ `Example*` - demonstrations of various features, usually interactive.
+ `Template` - minimal project template to start with.
+ `TemplateHotReload` - same as above, but with hot-reload enabled.
+ Tools:
  + `GradientGenerator` - interactive gradient palette generator.
  + `FunGraph` - interactive function plotter.

## 🍭 Gȧlléry



A collection of generative art pieces (ordered by name).

## Alien

[<img src="arts/alien/alien-letters-v1_thumb.png" hspace="2" vspace="2" align="left">](arts/alien/alien-letters-v1.png)
[<img src="arts/alien/alien-letters-v2_thumb.png" hspace="2" vspace="2" align="left">](arts/alien/alien-letters-v2.png)
[<img src="arts/alien/alien-letters-v3_thumb.png" hspace="2" vspace="2" align="left">](arts/alien/alien-letters-v3.png)
[<img src="arts/alien/alien2_thumb.png" hspace="2" vspace="2" align="left">](arts/alien/alien2.png)
<br clear="both">

## Blob

[<img src="arts/blob/blob_thumb.jpg" hspace="2" vspace="2" align="left">](arts/blob/blob.jpg)
<br clear="both">

## Bubbles

[<img src="arts/bubbles/BubbleStripe_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/BubbleStripe.png)
[<img src="arts/bubbles/Bubbles2_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/Bubbles2.png)
[<img src="arts/bubbles/bubble-stripe-2_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/bubble-stripe-2.png)
[<img src="arts/bubbles/bubbub_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/bubbub.png)
[<img src="arts/bubbles/pebble1_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/pebble1.png)
[<img src="arts/bubbles/pebble2_thumb.png" hspace="2" vspace="2" align="left">](arts/bubbles/pebble2.png)
<br clear="both">

## Cell

[<img src="arts/cell/cell1_thumb.png" hspace="2" vspace="2" align="left">](arts/cell/cell1.png)
<br clear="both">

## Circledots

[<img src="arts/circledots/awaking_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/awaking.png)
[<img src="arts/circledots/circledots_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/circledots.png)
[<img src="arts/circledots/fbf1_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/fbf1.png)
[<img src="arts/circledots/fbf2_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/fbf2.png)
[<img src="arts/circledots/gentle_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/gentle.png)
[<img src="arts/circledots/guzv_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/guzv.png)
[<img src="arts/circledots/xy-1_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/xy-1.png)
[<img src="arts/circledots/xy-2_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/xy-2.png)
[<img src="arts/circledots/xy-3_thumb.png" hspace="2" vspace="2" align="left">](arts/circledots/xy-3.png)
<br clear="both">

## Cotton

[<img src="arts/cotton/cotton-circles2_thumb.png" hspace="2" vspace="2" align="left">](arts/cotton/cotton-circles2.png)
[<img src="arts/cotton/cotton-circles_thumb.png" hspace="2" vspace="2" align="left">](arts/cotton/cotton-circles.png)
[<img src="arts/cotton/cotton1_thumb.png" hspace="2" vspace="2" align="left">](arts/cotton/cotton1.png)
[<img src="arts/cotton/cotton2_thumb.png" hspace="2" vspace="2" align="left">](arts/cotton/cotton2.png)
[<img src="arts/cotton/cotton3_thumb.png" hspace="2" vspace="2" align="left">](arts/cotton/cotton3.png)
<br clear="both">

## Falllines

[<img src="arts/falllines/falllines_thumb.png" hspace="2" vspace="2" align="left">](arts/falllines/falllines.png)
<br clear="both">

## Flamebrush

[<img src="arts/flamebrush/flamebrush1_thumb.png" hspace="2" vspace="2" align="left">](arts/flamebrush/flamebrush1.png)
<br clear="both">

## Flowforce

[<img src="arts/flowforce/Eclectic2_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/Eclectic2.png)
[<img src="arts/flowforce/Eclectic_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/Eclectic.png)
[<img src="arts/flowforce/Eclipse_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/Eclipse.png)
[<img src="arts/flowforce/Spring_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/Spring.png)
[<img src="arts/flowforce/circlex_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/circlex.png)
[<img src="arts/flowforce/emergence_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/emergence.png)
[<img src="arts/flowforce/flowforce1_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/flowforce1.png)
[<img src="arts/flowforce/flowforce2_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/flowforce2.png)
[<img src="arts/flowforce/flowforce3_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/flowforce3.png)
[<img src="arts/flowforce/flowforce4_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/flowforce4.png)
[<img src="arts/flowforce/interruption_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/interruption.png)
[<img src="arts/flowforce/vorflow_thumb.png" hspace="2" vspace="2" align="left">](arts/flowforce/vorflow.png)
<br clear="both">

## Fluid

[<img src="arts/fluid/fluid-pack_thumb.png" hspace="2" vspace="2" align="left">](arts/fluid/fluid-pack.png)
[<img src="arts/fluid/fluid-wind_thumb.png" hspace="2" vspace="2" align="left">](arts/fluid/fluid-wind.png)
<br clear="both">

## Harmongraph

[<img src="arts/harmongraph/hA_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hA.png)
[<img src="arts/harmongraph/hB_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hB.png)
[<img src="arts/harmongraph/hC_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hC.png)
[<img src="arts/harmongraph/hD_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hD.png)
[<img src="arts/harmongraph/hE_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hE.png)
[<img src="arts/harmongraph/hF_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hF.png)
[<img src="arts/harmongraph/hG_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hG.png)
[<img src="arts/harmongraph/hH_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/hH.png)
[<img src="arts/harmongraph/harmongraph0_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/harmongraph0.png)
[<img src="arts/harmongraph/harmongraph1_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/harmongraph1.png)
[<img src="arts/harmongraph/harmongraph2_thumb.png" hspace="2" vspace="2" align="left">](arts/harmongraph/harmongraph2.png)
<br clear="both">

## Hills

[<img src="arts/hills/february_thumb.png" hspace="2" vspace="2" align="left">](arts/hills/february.png)
[<img src="arts/hills/hills_thumb.png" hspace="2" vspace="2" align="left">](arts/hills/hills.png)
[<img src="arts/hills/horizons_thumb.png" hspace="2" vspace="2" align="left">](arts/hills/horizons.png)
<br clear="both">

## Igor

[<img src="arts/igor/igor_thumb.png" hspace="2" vspace="2" align="left">](arts/igor/igor.png)
<br clear="both">

## Joydiv

[<img src="arts/joydiv/joydiv_thumb.png" hspace="2" vspace="2" align="left">](arts/joydiv/joydiv.png)
<br clear="both">

## Kaleiircle

[<img src="arts/kaleiircle/kaleidoscope2-1_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleidoscope2-1.png)
[<img src="arts/kaleiircle/kaleidoscope2-2_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleidoscope2-2.png)
[<img src="arts/kaleiircle/kaleidoscope2_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleidoscope2.png)
[<img src="arts/kaleiircle/kaleidoscope3_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleidoscope3.png)
[<img src="arts/kaleiircle/kaleidoscope_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleidoscope.png)
[<img src="arts/kaleiircle/kaleiircle_thumb.png" hspace="2" vspace="2" align="left">](arts/kaleiircle/kaleiircle.png)
<br clear="both">

## Legoo

[<img src="arts/legoo/Legoo12_thumb.png" hspace="2" vspace="2" align="left">](arts/legoo/Legoo12.png)
[<img src="arts/legoo/Legoo1_thumb.png" hspace="2" vspace="2" align="left">](arts/legoo/Legoo1.png)
[<img src="arts/legoo/Legoo2_thumb.png" hspace="2" vspace="2" align="left">](arts/legoo/Legoo2.png)
<br clear="both">

## Lettero

[<img src="arts/lettero/LetterO_thumb.png" hspace="2" vspace="2" align="left">](arts/lettero/LetterO.png)
[<img src="arts/lettero/lettero2_thumb.png" hspace="2" vspace="2" align="left">](arts/lettero/lettero2.png)
[<img src="arts/lettero/lettero3-1_thumb.png" hspace="2" vspace="2" align="left">](arts/lettero/lettero3-1.png)
[<img src="arts/lettero/lettero3-2_thumb.png" hspace="2" vspace="2" align="left">](arts/lettero/lettero3-2.png)
[<img src="arts/lettero/lettero3_thumb.png" hspace="2" vspace="2" align="left">](arts/lettero/lettero3.png)
<br clear="both">

## Lines

[<img src="arts/lines/citymap_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/citymap.png)
[<img src="arts/lines/heapspace_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/heapspace.png)
[<img src="arts/lines/ngons_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/ngons.png)
[<img src="arts/lines/outline_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/outline.png)
[<img src="arts/lines/stripes1_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/stripes1.png)
[<img src="arts/lines/stripes2_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/stripes2.png)
[<img src="arts/lines/swing2_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/swing2.png)
[<img src="arts/lines/swing3_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/swing3.png)
[<img src="arts/lines/swing_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/swing.png)
[<img src="arts/lines/tapesA_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/tapesA.png)
[<img src="arts/lines/tapesB_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/tapesB.png)
[<img src="arts/lines/triadance2_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/triadance2.png)
[<img src="arts/lines/triangles_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/triangles.png)
[<img src="arts/lines/tridance_thumb.png" hspace="2" vspace="2" align="left">](arts/lines/tridance.png)
<br clear="both">

## Lissajous

[<img src="arts/lissajous/lissajous_thumb.png" hspace="2" vspace="2" align="left">](arts/lissajous/lissajous.png)
[<img src="arts/lissajous/moire_thumb.png" hspace="2" vspace="2" align="left">](arts/lissajous/moire.png)
<br clear="both">

## Metro

[<img src="arts/metro/metro2_thumb.png" hspace="2" vspace="2" align="left">](arts/metro/metro2.png)
[<img src="arts/metro/metro_thumb.png" hspace="2" vspace="2" align="left">](arts/metro/metro.png)
<br clear="both">

## Monet

[<img src="arts/monet/monet1-0_thumb.png" hspace="2" vspace="2" align="left">](arts/monet/monet1-0.png)
[<img src="arts/monet/monet1_thumb.png" hspace="2" vspace="2" align="left">](arts/monet/monet1.png)
[<img src="arts/monet/monet2_thumb.png" hspace="2" vspace="2" align="left">](arts/monet/monet2.png)
<br clear="both">

## Neuromancer

[<img src="arts/neuromancer/nm1_thumb.png" hspace="2" vspace="2" align="left">](arts/neuromancer/nm1.png)
<br clear="both">

## Orbitr

[<img src="arts/orbitr/orbitr_thumb.png" hspace="2" vspace="2" align="left">](arts/orbitr/orbitr.png)
<br clear="both">

## Palecircles

[<img src="arts/palecircles/all-circles_thumb.png" hspace="2" vspace="2" align="left">](arts/palecircles/all-circles.png)
[<img src="arts/palecircles/palecircles_thumb.png" hspace="2" vspace="2" align="left">](arts/palecircles/palecircles.png)
<br clear="both">

## Pixelmania

[<img src="arts/pixelmania/circl_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/circl.png)
[<img src="arts/pixelmania/glass_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/glass.png)
[<img src="arts/pixelmania/liqf_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/liqf.png)
[<img src="arts/pixelmania/rastersin_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/rastersin.png)
[<img src="arts/pixelmania/romb_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/romb.png)
[<img src="arts/pixelmania/stripo_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/stripo.png)
[<img src="arts/pixelmania/tower01_thumb.png" hspace="2" vspace="2" align="left">](arts/pixelmania/tower01.png)
<br clear="both">

## Plasma

[<img src="arts/plasma/plasma2_thumb.png" hspace="2" vspace="2" align="left">](arts/plasma/plasma2.png)
[<img src="arts/plasma/plasma3_thumb.png" hspace="2" vspace="2" align="left">](arts/plasma/plasma3.png)
[<img src="arts/plasma/plasma4_thumb.png" hspace="2" vspace="2" align="left">](arts/plasma/plasma4.png)
[<img src="arts/plasma/plasma_thumb.png" hspace="2" vspace="2" align="left">](arts/plasma/plasma.png)
<br clear="both">

## Rayz

[<img src="arts/rayz/mirrorz2_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/mirrorz2.png)
[<img src="arts/rayz/mirrorz_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/mirrorz.png)
[<img src="arts/rayz/rayz2-1_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/rayz2-1.png)
[<img src="arts/rayz/rayz2-2_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/rayz2-2.png)
[<img src="arts/rayz/rayz2-3_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/rayz2-3.png)
[<img src="arts/rayz/rayz_thumb.png" hspace="2" vspace="2" align="left">](arts/rayz/rayz.png)
<br clear="both">

## Rectapart

[<img src="arts/rectapart/fall-squares_thumb.png" hspace="2" vspace="2" align="left">](arts/rectapart/fall-squares.png)
[<img src="arts/rectapart/rectApart_thumb.png" hspace="2" vspace="2" align="left">](arts/rectapart/rectApart.png)
[<img src="arts/rectapart/rectapart3_thumb.png" hspace="2" vspace="2" align="left">](arts/rectapart/rectapart3.png)
[<img src="arts/rectapart/rectpack1_thumb.png" hspace="2" vspace="2" align="left">](arts/rectapart/rectpack1.png)
[<img src="arts/rectapart/rectpack2_thumb.png" hspace="2" vspace="2" align="left">](arts/rectapart/rectpack2.png)
<br clear="both">

## Rects

[<img src="arts/rects/cells_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/cells.png)
[<img src="arts/rects/divine-divide_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/divine-divide.png)
[<img src="arts/rects/impossible-rubik-one_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/impossible-rubik-one.png)
[<img src="arts/rects/impossible-rubik-three_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/impossible-rubik-three.png)
[<img src="arts/rects/impossible-rubik-two_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/impossible-rubik-two.png)
[<img src="arts/rects/mondrian-01_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/mondrian-01.png)
[<img src="arts/rects/mondrian-02_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/mondrian-02.png)
[<img src="arts/rects/mondrian-03_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/mondrian-03.png)
[<img src="arts/rects/rects-over_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/rects-over.png)
[<img src="arts/rects/rects1_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/rects1.png)
[<img src="arts/rects/rects2_thumb.png" hspace="2" vspace="2" align="left">](arts/rects/rects2.png)
<br clear="both">

## Repetition

[<img src="arts/repetition/Repetition1_thumb.png" hspace="2" vspace="2" align="left">](arts/repetition/Repetition1.png)
[<img src="arts/repetition/Repetition2_thumb.png" hspace="2" vspace="2" align="left">](arts/repetition/Repetition2.png)
<br clear="both">

## Rotoro

[<img src="arts/rotoro/rotoro1_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro1.png)
[<img src="arts/rotoro/rotoro2-0_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro2-0.png)
[<img src="arts/rotoro/rotoro2-1_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro2-1.png)
[<img src="arts/rotoro/rotoro2-2_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro2-2.png)
[<img src="arts/rotoro/rotoro2_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro2.png)
[<img src="arts/rotoro/rotoro3_thumb.png" hspace="2" vspace="2" align="left">](arts/rotoro/rotoro3.png)
<br clear="both">

## Roundrects

[<img src="arts/roundrects/roundrects_thumb.png" hspace="2" vspace="2" align="left">](arts/roundrects/roundrects.png)
<br clear="both">

## Rule

[<img src="arts/rule/rulez01_thumb.png" hspace="2" vspace="2" align="left">](arts/rule/rulez01.png)
[<img src="arts/rule/rulez02_thumb.png" hspace="2" vspace="2" align="left">](arts/rule/rulez02.png)
<br clear="both">

## Sea

[<img src="arts/sea/sea_thumb.png" hspace="2" vspace="2" align="left">](arts/sea/sea.png)
<br clear="both">

## Sf

[<img src="arts/sf/sf10_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf10.png)
[<img src="arts/sf/sf1_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf1.png)
[<img src="arts/sf/sf2_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf2.png)
[<img src="arts/sf/sf3_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf3.png)
[<img src="arts/sf/sf4_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf4.png)
[<img src="arts/sf/sf5_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf5.png)
[<img src="arts/sf/sf6_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf6.png)
[<img src="arts/sf/sf7_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf7.png)
[<img src="arts/sf/sf8_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf8.png)
[<img src="arts/sf/sf9_thumb.png" hspace="2" vspace="2" align="left">](arts/sf/sf9.png)
<br clear="both">

## Shad

[<img src="arts/shad/earth2_thumb.png" hspace="2" vspace="2" align="left">](arts/shad/earth2.png)
[<img src="arts/shad/foo_thumb.png" hspace="2" vspace="2" align="left">](arts/shad/foo.png)
<br clear="both">

## Sixsix

[<img src="arts/sixsix/moon-over-window_thumb.png" hspace="2" vspace="2" align="left">](arts/sixsix/moon-over-window.png)
[<img src="arts/sixsix/six_0_thumb.png" hspace="2" vspace="2" align="left">](arts/sixsix/six_0.png)
[<img src="arts/sixsix/sixsix_1_thumb.png" hspace="2" vspace="2" align="left">](arts/sixsix/sixsix_1.png)
[<img src="arts/sixsix/sixsix_2_thumb.png" hspace="2" vspace="2" align="left">](arts/sixsix/sixsix_2.png)
<br clear="both">

## Skyscraper

[<img src="arts/skyscraper/dualcity_thumb.png" hspace="2" vspace="2" align="left">](arts/skyscraper/dualcity.png)
[<img src="arts/skyscraper/perspective01_thumb.png" hspace="2" vspace="2" align="left">](arts/skyscraper/perspective01.png)
[<img src="arts/skyscraper/perspective02_thumb.png" hspace="2" vspace="2" align="left">](arts/skyscraper/perspective02.png)
[<img src="arts/skyscraper/skyscraper2_thumb.png" hspace="2" vspace="2" align="left">](arts/skyscraper/skyscraper2.png)
[<img src="arts/skyscraper/skyscraper_thumb.png" hspace="2" vspace="2" align="left">](arts/skyscraper/skyscraper.png)
<br clear="both">

## Spiral

[<img src="arts/spiral/spiral2_thumb.png" hspace="2" vspace="2" align="left">](arts/spiral/spiral2.png)
[<img src="arts/spiral/spiral3_thumb.png" hspace="2" vspace="2" align="left">](arts/spiral/spiral3.png)
[<img src="arts/spiral/spiral_thumb.png" hspace="2" vspace="2" align="left">](arts/spiral/spiral.png)
<br clear="both">

## Spirograph

[<img src="arts/spirograph/spirograph1_thumb.png" hspace="2" vspace="2" align="left">](arts/spirograph/spirograph1.png)
[<img src="arts/spirograph/spirograph2_thumb.png" hspace="2" vspace="2" align="left">](arts/spirograph/spirograph2.png)
<br clear="both">

## Stripes

[<img src="arts/stripes/s2_thumb.png" hspace="2" vspace="2" align="left">](arts/stripes/s2.png)
[<img src="arts/stripes/stripes1_thumb.png" hspace="2" vspace="2" align="left">](arts/stripes/stripes1.png)
[<img src="arts/stripes/stripes_thumb.png" hspace="2" vspace="2" align="left">](arts/stripes/stripes.png)
[<img src="arts/stripes/tolerance_thumb.png" hspace="2" vspace="2" align="left">](arts/stripes/tolerance.png)
<br clear="both">

## Sun

[<img src="arts/sun/echoes1_thumb.png" hspace="2" vspace="2" align="left">](arts/sun/echoes1.png)
[<img src="arts/sun/echoes2_thumb.png" hspace="2" vspace="2" align="left">](arts/sun/echoes2.png)
[<img src="arts/sun/sunNS1_thumb.png" hspace="2" vspace="2" align="left">](arts/sun/sunNS1.png)
[<img src="arts/sun/sunlines_thumb.png" hspace="2" vspace="2" align="left">](arts/sun/sunlines.png)
<br clear="both">

## Switchboard

[<img src="arts/switchboard/switchboard_thumb.png" hspace="2" vspace="2" align="left">](arts/switchboard/switchboard.png)
<br clear="both">

## Thre3

[<img src="arts/thre3/noisepads_thumb.png" hspace="2" vspace="2" align="left">](arts/thre3/noisepads.png)
[<img src="arts/thre3/surfing_thumb.png" hspace="2" vspace="2" align="left">](arts/thre3/surfing.png)
<br clear="both">

## Ticktiletock

[<img src="arts/ticktiletock/ticktiletock_thumb.png" hspace="2" vspace="2" align="left">](arts/ticktiletock/ticktiletock.png)
<br clear="both">

## Triangular

[<img src="arts/triangular/SaharaDiamond_thumb.png" hspace="2" vspace="2" align="left">](arts/triangular/SaharaDiamond.png)
[<img src="arts/triangular/Triage_thumb.png" hspace="2" vspace="2" align="left">](arts/triangular/Triage.png)
<br clear="both">

## Z

[<img src="arts/z/z1_thumb.png" hspace="2" vspace="2" align="left">](arts/z/z1.png)
[<img src="arts/z/z2_thumb.png" hspace="2" vspace="2" align="left">](arts/z/z2.png)
[<img src="arts/z/z3_thumb.png" hspace="2" vspace="2" align="left">](arts/z/z3.png)
[<img src="arts/z/z4_thumb.png" hspace="2" vspace="2" align="left">](arts/z/z4.png)
[<img src="arts/z/z5_thumb.png" hspace="2" vspace="2" align="left">](arts/z/z5.png)
<br clear="both">

---

**Total: 187 works across 47 collections**
