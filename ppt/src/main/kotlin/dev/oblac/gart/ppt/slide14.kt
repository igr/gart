package dev.oblac.gart.ppt

import dev.oblac.gart.Dimension
import dev.oblac.gart.DrawFrame
import dev.oblac.gart.Frames
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.smooth.chaikinSmooth
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawMultilineStringInRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.max

val slide14 = DrawFrame { c, d, f ->
    draw(c, d, f)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01

// Pre-computed random values for cloud
private val cloudLineY1 = rndf(100f, 200f)
private val cloudLineY2 = rndf(100f, 200f)
private val cloudPointOffsets = FloatArray(10) { rndf(-50f, 250f) }
private val cloudCircleRadii = FloatArray(200) { rndGaussian(50f, 20f) }

// Pre-computed random values for rays (120 rays)
private val rayOffsetsX = FloatArray(120) { rndf(-1f, 1f) }
private val rayOffsetsY = FloatArray(120) { rndf(-1f, 1f) }
private val rayStrokeWidths = FloatArray(120) { rndf(2f, 3f) }

// Target values for easing transitions
private var targetCloudLineY1 = cloudLineY1
private var targetCloudLineY2 = cloudLineY2
private var targetCloudPointOffsets = cloudPointOffsets.copyOf()
private var targetCloudCircleRadii = cloudCircleRadii.copyOf()
private var targetRayOffsetsX = rayOffsetsX.copyOf()
private var targetRayOffsetsY = rayOffsetsY.copyOf()
private var targetRayStrokeWidths = rayStrokeWidths.copyOf()

// Current (eased) values
private var currentCloudLineY1 = cloudLineY1
private var currentCloudLineY2 = cloudLineY2
private var currentCloudPointOffsets = cloudPointOffsets.copyOf()
private var currentCloudCircleRadii = cloudCircleRadii.copyOf()
private var currentRayOffsetsX = rayOffsetsX.copyOf()
private var currentRayOffsetsY = rayOffsetsY.copyOf()
private var currentRayStrokeWidths = rayStrokeWidths.copyOf()

private var transitionStart = -1L
private const val TRANSITION_DURATION = 2f  // seconds

private fun generateNewTargets() {
    targetCloudLineY1 = rndf(100f, 200f)
    targetCloudLineY2 = rndf(100f, 200f)
    targetCloudPointOffsets = FloatArray(10) { rndf(-50f, 250f) }
    targetCloudCircleRadii = FloatArray(200) { rndGaussian(50f, 20f) }
    targetRayOffsetsX = FloatArray(120) { rndf(-1f, 1f) }
    targetRayOffsetsY = FloatArray(120) { rndf(-1f, 1f) }
    targetRayStrokeWidths = FloatArray(120) { rndf(2f, 3f) }
}

private fun easeValues(t: Float) {
    val st = smoothstep(0f, 1f, t)
    currentCloudLineY1 = lerp(currentCloudLineY1, targetCloudLineY1, st)
    currentCloudLineY2 = lerp(currentCloudLineY2, targetCloudLineY2, st)
    for (i in currentCloudPointOffsets.indices) {
        currentCloudPointOffsets[i] = lerp(currentCloudPointOffsets[i], targetCloudPointOffsets[i], st)
    }
    for (i in currentCloudCircleRadii.indices) {
        currentCloudCircleRadii[i] = lerp(currentCloudCircleRadii[i], targetCloudCircleRadii[i], st)
    }
    for (i in currentRayOffsetsX.indices) {
        currentRayOffsetsX[i] = lerp(currentRayOffsetsX[i], targetRayOffsetsX[i], st)
    }
    for (i in currentRayOffsetsY.indices) {
        currentRayOffsetsY[i] = lerp(currentRayOffsetsY[i], targetRayOffsetsY[i], st)
    }
    for (i in currentRayStrokeWidths.indices) {
        currentRayStrokeWidths[i] = lerp(currentRayStrokeWidths[i], targetRayStrokeWidths[i], st)
    }
}

private fun draw(c: Canvas, d: Dimension, f: Frames) {
    // Start a new transition every TRANSITION_DURATION seconds
    if (transitionStart < 0) {
        transitionStart = f.frame
    }
    val frameSinceStart = f.frame - transitionStart
    val t = (frameSinceStart * f.frameDurationSeconds) / TRANSITION_DURATION

    if (t >= 1f) {
        // Transition complete - current values snap to target, generate new targets
        currentCloudLineY1 = targetCloudLineY1
        currentCloudLineY2 = targetCloudLineY2
        targetCloudPointOffsets.copyInto(currentCloudPointOffsets)
        targetCloudCircleRadii.copyInto(currentCloudCircleRadii)
        targetRayOffsetsX.copyInto(currentRayOffsetsX)
        targetRayOffsetsY.copyInto(currentRayOffsetsY)
        targetRayStrokeWidths.copyInto(currentRayStrokeWidths)
        generateNewTargets()
        transitionStart = f.frame
    } else {
        easeValues(t)
    }

    c.clear(colorBack)

    val circle1 = Circle(d.center.offset(0f, -d.hf * 0.08f), d.hf * 0.2f)
    val circle2 = Circle(d.center.offset(0f, -d.hf * 0.08f), max(d.w, d.h))

    val points = circle2.points(120)
    points.forEachIndexed { i, pt -> drawRay(c, d, circle1, pt, i) }

    drawCloud(c, d)

    c.drawCircle(circle1, fillOf(colorBack))

    c.drawRoundBorder(d, 10f, 40f, colorInk)

    c.drawBottom("Igor Spasic\n@github: gart\nigo.rs")
}

private fun Canvas.drawBottom(text: String) =
    this.drawMultilineStringInRect(
        text,
        bottomBox,
        textFont,
        colorBack.toFillPaint(),
        HorizontalAlign.CENTER
    )


// see SF 8

private fun drawCloud(c: Canvas, d: Dimension) {
    val y = d.cy
    val line = Line(Point(0f, y + currentCloudLineY1), Point(d.w, y + currentCloudLineY2))
    val p = line.points(10).mapIndexed { i, pt ->
        pt.offset(0f, currentCloudPointOffsets[i])
    }

    val p2 = chaikinSmooth(p, iterations = 4, closed = false, bias = 0.2)
    p2.forEachIndexed { i, pt ->
        val radius = if (i < currentCloudCircleRadii.size) currentCloudCircleRadii[i] else 50f
        c.drawCircle(Circle(pt, radius), fillOf(colorInk))
    }

    val path = p.toPathBuilder()
    path.lineTo(d.rightBottom)
    path.lineTo(d.leftBottom)
    path.closePath()
    c.drawPath(path.detach(), fillOf(colorInk))
}

private fun drawRay(c: Canvas, d: Dimension, circle: Circle, lastPoint: Point, index: Int) {
    val line = Line(
        circle.center,
        lastPoint.offset(currentRayOffsetsX[index] * d.wf, currentRayOffsetsY[index] * d.hf)
    )
    c.drawLine(line, strokeOf(colorInk, currentRayStrokeWidths[index]))
}
