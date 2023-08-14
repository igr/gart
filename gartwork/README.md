# gȧrtwork

Simple framework for rendering **still images** or **movies**. Displays animation in the window if needed.

## Usage

`Gartvas` is a _canvas_. Just draw on it.

`Gartmap` is bitmap (`Pixels`) of the canvas.

## Rendering

`Window` is a tool that displays the canvas. The performance are not in the focus here; the purpose is to visualise animation during development.

`VideoRecorder` records frames into the video.

`ImageWritter` stores a canvas snapshot into the image.

### [Example](arts/example/README.md)
![](../arts/example/example.png)

## Skia implementation

There are two great SKIA connectors:

+ [Skija](https://github.com/HumbleUI/Skija) - for Java
+ [Skiko](https://github.com/JetBrains/skiko) - for Kotlin, by JetBrains 

Gartwork library could work with both, changes are minimal. 
