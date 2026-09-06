package dev.oblac.gart.fx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.pixels.boxDownsample
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

/*
 * Supersampling - why the pieces draw big and shrink back.
 *
 * Skia anti-aliases each edge on its own, with a per-pixel coverage estimate. That holds up for
 * a few shapes and falls apart on what these pieces are mostly made of: hairlines under a pixel
 * wide, thousands of strokes crossing, fields of dots or lines spaced close to the pixel grid.
 * Line weight comes out uneven, near-parallel lines beat against the grid into moire, and a
 * stroke thinner than a pixel drops out or goes grey depending on where it happens to land.
 *
 * Drawing at ss times the output size and averaging every ss x ss block down to one pixel
 * measures area coverage instead of estimating it: every edge, crossing and hairline is sampled
 * ss*ss times per output pixel, and they all come down together, so overlaps and gaps are
 * weighted the way they would be on paper.
 *
 * The protocol:
 *  - [supersampled] gives the big blank canvas, [downsample] brings the finished drawing back to
 *    the piece's own size. Same ss both ways. The box filter reads every source pixel exactly
 *    once and writes the plain block mean - it is a measurement, not a resample.
 *  - draw in big-canvas pixels. Sizes given as a share of the width or height scale on their own;
 *    anything in absolute px (a 2 px stroke, a 22 px grid) is multiplied by ss.
 *  - passes that belong to the paper - grain, print mottle, dither, a blur in output px - run
 *    after the downsample on the small canvas, or the box averages them away.
 *  - ss is a knob, `pi("ss", 3, 1..4)`: 3 for the hero, 2 in sweeps, 1 to iterate. It costs
 *    ss*ss in memory and fill; 1200 x 1800 at 3 is a 3600 x 5400 canvas.
 *  - the block mean is integer arithmetic in a fixed order, so a seeded piece renders bit for bit
 *    the same on every run and machine. Skia's samplers ([scaleImage]) are not that, and they
 *    only ever look at a fixed few source pixels whatever the ratio, so hairlines keep their
 *    aliasing. Never shrink a render with them.
 */

/**
 * A blank canvas [ss] times bigger than this gart in each direction, to draw on before
 * [downsample] brings it back. The header of this file says why.
 */
fun Gart.supersampled(ss: Int): Gartvas = Gartvas(Dimension(d.w * ss, d.h * ss))

/**
 * Shrinks a supersampled canvas by [ss] with a plain box average - see [boxDownsample]. Use this
 * rather than [scaleImage] to bring a 2x/3x render down: Skia's samplers only ever look at a
 * fixed couple of source px whatever the ratio, so hairlines drawn at that spacing keep their
 * aliasing and beat against the grid into ripples. The box takes every source px once.
 */
fun Gartvas.downsample(ss: Int): Gartvas {
    val out = Gartvas(Dimension(d.w / ss, d.h / ss))
    val dst = Gartmap(out)
    boxDownsample(Gartmap(this).pixels, ss, dst)
    dst.drawToCanvas()
    return out
}

/**
 * Resamples an image to a new size with Skia's default sampler. Fine for thumbnails and for
 * going up; not for bringing a supersampled render down - see the header, use [downsample].
 */
fun Image.scaleImage(newWidth: Int, newHeight: Int): Image {
    val surface = Surface.makeRasterN32Premul(newWidth, newHeight)
    val canvas = surface.canvas

    canvas.drawImageRect(
        this,
        Rect(0f, 0f, width.toFloat(), height.toFloat()),
        Rect(0f, 0f, newWidth.toFloat(), newHeight.toFloat()),
        SamplingMode.DEFAULT,
        null,
        true
    )
    return surface.makeImageSnapshot()
}
