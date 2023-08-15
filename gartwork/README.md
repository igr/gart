# gȧrtwork

Simple framework for rendering **still images** or **movies**. Displays animation in the window if needed.

Code conventions:

+ we use common single-named variables for the most important objects.

Dependency to manually install:

+ [Humble Video](https://github.com/artclarke/humble-video)

## Usage

`Gartvas` is a _canvas_. Just draw on it.

`Gartmap` is bitmap (`Pixels`) of the canvas.

## Rendering

`Window` is a tool that displays the canvas. The performance are not in the focus here; the purpose is to visualise animation during development.

`VideoRecorder` records frames into the video.

`writeGartvasAsImage()` takes a snapshot of canvas into the image.

Not super-optimized for speed, as the purpose is to render visualisations.

### [Example](arts/example/README.md)
![](../arts/example/example.png)

## Skia implementation

There are two great SKIA connectors:

+ [Skija](https://github.com/HumbleUI/Skija) - for Java
+ [Skiko](https://github.com/JetBrains/skiko) - for Kotlin, by JetBrains 

Gartwork library could work with both, changes are minimal. 
