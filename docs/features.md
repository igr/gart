# Gart Features

There is really **a lot of features**, and I'm not sure how to organize them, but here's an attempt at categorization:

## Core Framework

- Gart — Factory/entry point (Gart.of("name", w, h))
- Gartvas — Canvas wrapper (Skia Surface)
- Gartmap — Bitmap-backed canvas with raw pixel access
- GartSnapshot — State capture, freeze/replay, saveImage
- Window / WindowView — Swing display with keyboard/mouse handlers
- Movie — MP4/GIF video recording (FFmpeg + GifSequenceWriter)
- Frames — Frame timing, FPS control
- GartRand — Deterministic random with replay
- Sprite — Image with chainable transforms (rotate, translate, scale, flip)
- DrawFrame / Drawing — Functional draw interface and abstract base class for rendering

## Color

- Color spaces: RGBA, HSL, HSV, HSI, LAB, LCH, OKLAB, OKLCH, CMYK
- Palettes: 173 cool + 15 mix + 112 colormaps (Carto, CET, ColorBrewer, Matplotlib, Ocean, Plotly, Tableau, etc.)
- Named colors: CssColors, NipponColors, RetroColors, MidCenturyColors, CyanotypeColors, BgColors
- Functions: blendColors, lerpColor, colorDistance, colorMatrix, toFillPaint, toStrokePaint
- PaletteGenerator — Dynamic palette generation
- NoiseColor — Noise-driven color generation

## Geometry & Graphics

- Primitives: Point, Line, DLine, Circle, Triangle, Poly4, Rect, GridRect, RectIsometric
- DLine — Parametric line (point + direction), perpendicular, pointFromStart/End
- Collections: Points, PointsTrail
- Intersections: line-line, line-circle, dline-line
- Drawing: drawCircle, drawLine, drawPoly4, drawTriangle, drawRotatedRect, drawPointsAsCircles, fatLine, n-gon, arc, ring, spiral, wave, grid, border, moon, tree, human
- Paint helpers: strokeOf, fillOf, hatchPaint, dashPaint
- Path utilities: pathOf, closedPathOf, toQuadPath, toPath, toClosedPath, pointsOn (with easing), combinePathsWithOp (boolean ops), deformPath, pathOutline
- Point operations: randomPoint, moveTowards, rotate, isCloseTo, isInside, dot product, operator overloads
- Easing functions: Linear, Quad, Cubic, Quart, Sine, Expo, Circ, Back, Elastic, Bounce (In/Out/InOut)
- Knot — Control points for wave/curve deformation

## Math & Vectors

- Vectors: Vector2/3/4, Vec2 (with dot, cross, rotation, normalization, angle), Matrix2, Matrix3
- Complex numbers: Complex, ComplexField, ComplexPolynomial, transcendental functions
- Curves: Lissajous, GaussianFunction
- Utilities: clamp, map, lerp, smoothstep, frac, mod, distance, primes, fastSqrt, stdev
- Angles: Radians, Degrees with trig functions, middleAngle
- Polar coordinates, affine transforms, tangent calculations
- Precalculated trig tables: MathCos, MathSin, MathPrecalcTable
- Z-function iteration for fractals (zfunc, ZFuncResult, Convergence)
- Binary entropy function

## Noise & Sampling

- Noise: Perlin, multi-octave PerlinNoise, SimplexNoise, OpenSimplexNoise, FBM
- Sampling: Halton sequence, Poisson disk sampling
- Pixel sampling: sampleNearest, sampleBilinear with SampleMode (CLAMP, TILE, BACKGROUND)

## Curve Smoothing

- B-spline, Cardinal spline, Catmull-Rom spline, Chaikin, quadratic smoothing

## Physics & Simulation

- Attractors (18): Lorenz, Lorenz84, Clifford, Rossler, Duffing, Thomas, Chen, Sprott, Langford-Aizawa, Halvorsen,
  Rabinovitch-Fabrikant, Dadras, Four-Wing, Symmetric Icon, Three-Scroll, Cubic, Quadratic, Peter de Jong
- N-body: BarnesHut simulation, QuadTree, GravityParticles (10^6+)
- Orbital mechanics (WHFast): Wisdom-Holman integrator, Kepler solver, orbital elements
- Fluid dynamics: Navier-Stokes solver, Lattice Boltzmann, FluidSolver with particle rendering
- Cellular automata: Elementary rules, Belousov-Zhabotinsky reaction
- Flow fields: Flow, FlowField, flow generators, StreamlineTracer (evenly-spaced streamlines)
- Particles: Particle system, Gravitron
- Box2D integration (gart-box2d module): JBox2D rigid body and particle physics, createContainer, World.particles()

## Spatial & Triangulation

- Delaunay triangulation (Mapbox Delaunator port)
- Voronoi diagrams
- HashGrid — Spatial hash for fast neighbor queries
- Circle packing — CirclePacker
- Jump Flooding Algorithm — Distance fields, outlinePath, marching squares contour tracing

## Stippling

- stippleDots — Dot-based dithering with configurable cell size and gap
- stippleVoronoi — Weighted Voronoi stippling using Lloyd relaxation
- stippleWangTile — Wang Tile-based recursive stippling for blue noise generation
- NoisyDotDensity, VoronoiStippling, WangTile/WangTileSet

## Pixel Processing

- Filters: Gaussian blur, motion blur, grayscale, liquify, moire, pixel sorting, flood fill, scaling
- Advanced filters: uniformFilter, laplacianFilter, maximumFilter, minimumFilter, adaptiveMedianFilter, PadMode (REFLECT, WRAP)
- Dithering: Floyd-Steinberg, Bayer (2x2-8x8), Atkinson, Burkes, Jarvis-Judice-Ninke, Sierra, Stucki, Threshold, WhiteNoise, ShiauFan, Marcu, Fedoseev, Ostromoukhov, ZhangPang, ZhouFang, WongAllebach, EntropyConstrained, VisualDifference, ContrastAware, BlueNoise, ErrorDiffusionSerpentine, ErrorDiffusionModulated
- Halftone: CMYK separation, configurable screening
- Conformal warp — Log-polar ring mapping using complex plane transformations with bilinear interpolation

## Shaders & Effects

- Shaders: neuro, marbled texture, noise grain, risograph, sketching paper
- Pixader — CPU pixel shader (shader-like per-pixel computation)
- FX: blur, borderize, pixelate, scale
- Glass/refraction: glassBall, glassPath
- Ray tracing: Ray, Mirror, traceRayWithReflections

## 3D Graphics

- Perspective: block3d (two-point perspective)
- Scene rendering: Scene, Camera, LightSource, ZBuffer (per-pixel depth buffer)
- Meshes: cube, sphere (UV), Face/Mesh primitives, 3D rotation matrices

## Generators

- Spirograph, harmonograph, midpoint displacement (terrain)

## Hot Reload

- GartLauncher — Classloader-based hot restart: watches `.class` files, re-invokes `main()` with a fresh
  `URLClassLoader` while reusing the existing Swing window (no flicker)

## Utilities

- Font loading, text rendering
- Image conversion, resource loading
- Array/list/loop/range/sequence helpers (countSequence, forSequence, repeatSequence)
