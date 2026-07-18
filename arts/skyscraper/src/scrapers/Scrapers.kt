package scrapers

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.*
import org.jetbrains.skia.*
import kotlin.math.*
import kotlin.random.Random

private const val W = 860
private const val H = 1200
private val BLACK = RetroColors.black01
private val CREAM = RetroColors.white01
private val RED = RetroColors.red01
private val YELLOW = RetroColors.yellow01

private const val ART_LEFT = 25f
private const val ART_TOP = 25f
private const val ART_RIGHT = W - ART_LEFT
private const val ART_BOTTOM = H - ART_TOP
private const val DX = 27f
private const val DY = 16f
private const val LEVEL = 63f
private const val HALF_LEVEL = LEVEL / 2f
private const val GRID_ORIGIN_X = 55f
private const val GRID_COLUMNS = ((ART_RIGHT - GRID_ORIGIN_X) / DX).toInt() + 1
private val DENSITY_COLUMNS = ((ART_RIGHT - ART_LEFT) / 78f).roundToInt()
private const val DENSITY_ROWS = 8
private const val TRIANGLE_SIZE = 18f
private const val TRIANGLE_CLEARANCE = 22f
private const val RAIL_WIDTH = 3f

private data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    val count: Int,
    val largeShare: Float,
    val mediumShare: Float,
    val reds: Int,
    val triangles: Int,
    val edgeChance: Float,
)

private enum class PlaneKind(val span: Float) {
    SMALL(1f),
    MEDIUM(1.5f),
    LARGE(2f),
    ;

    val rx: Float get() = DX * span
    val ry: Float get() = DY * span
}

private enum class Face {
    TOP_RIGHT,
    RIGHT_BOTTOM,
    BOTTOM_LEFT,
    LEFT_TOP,
}

private enum class AccentTone {
    RED,
    YELLOW,
}

private data class Plane(
    val x: Float,
    val y: Float,
    val kind: PlaneKind,
    val ribbon: Int,
    val accentTone: AccentTone? = null,
) {
    val rx: Float get() = kind.rx
    val ry: Float get() = kind.ry
}

private data class Rail(val x: Float, val y1: Float, val y2: Float)
private data class Vertex(val x: Float, val y: Float)
private data class BoundaryAccent(val side: Int, val y: Float, val tone: AccentTone)
private data class RailTriangle(val x: Float, val y: Float)

private data class Composition(
    val planes: List<Plane>,
    val rails: List<Rail>,
    val boundaryAccents: List<BoundaryAccent>,
    val triangles: List<RailTriangle>,
)

private class CompositionBuilder {
    val planes = mutableListOf<Plane>()
    val rails = mutableListOf<Rail>()
    var nextRibbon = 0
}

private data class Leg(
    val next: Plane,
    val rails: List<Rail>,
    val face: Face,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val params = resolveParams()
    val composition = generateComposition(params)
    val gart = Gart.of("scrapers", W, H)
    println(
        "seed=${params.seed} planes=${composition.planes.size} " +
            "shafts=${composition.rails.size / 2} " +
            "reds=${composition.planes.count { it.accentTone == AccentTone.RED }} " +
            "yellows=${composition.planes.count { it.accentTone == AccentTone.YELLOW }} " +
            "triangles=${composition.triangles.size}",
    )

    val big = Gartvas(Dimension(W * params.ss, H * params.ss))
    big.canvas.scale(params.ss.toFloat(), params.ss.toFloat())
    render(big.canvas, composition)

    val g = gart.gartvas()
    val snapshot = big.snapshot()
    g.canvas.drawImageRect(
        snapshot,
        Rect.makeWH((W * params.ss).toFloat(), (H * params.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snapshot.close()

    val output = if (params.out.endsWith(".png", ignoreCase = true)) params.out else "${params.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun resolveParams(): Params {
    val params = Params(
        seed = pl("seed", 5321),       //5310L+120
        count = pi("count", 120),
        out = ps("out", "work/scrapers"),
        ss = pi("ss", 2),
        largeShare = pf("large", 0.22f),
        mediumShare = pf("medium", 0.10f),
        reds = pi("reds", 12),
        triangles = pi("triangles", pi("halfCircles", 9)),
        edgeChance = pf("edge", 0.11f),
    )
    require(params.ss in 1..4) { "ss must be between 1 and 4" }
    require(params.count in 30..130) { "count must be between 30 and 130" }
    require(params.largeShare in 0f..0.4f) { "large must be between 0 and 0.4" }
    require(params.mediumShare in 0f..0.3f) { "medium must be between 0 and 0.3" }
    require(params.largeShare + params.mediumShare < 0.65f) { "large + medium must be below 0.65" }
    require(params.reds in 0..params.count) { "reds must be between 0 and count" }
    require(params.triangles in 0..30) { "triangles must be between 0 and 30" }
    require(params.edgeChance in 0f..0.35f) { "edge must be between 0 and 0.35" }
    return params
}

private fun generateComposition(params: Params): Composition {
    val layoutRng = Random(params.seed)
    val styleRng = Random(params.seed * 7919L + 17L)
    val colorRng = Random(params.seed * 65537L + 23L)
    val detailRng = Random(params.seed * 99991L + 101L)
    val builder = CompositionBuilder()

    val largeTarget = (params.count * params.largeShare).roundToInt()
    val mediumTarget = (params.count * params.mediumShare).roundToInt()
    val smallTarget = params.count - largeTarget - mediumTarget

    growFamily(builder, layoutRng, params, PlaneKind.LARGE, largeTarget)
    growFamily(builder, layoutRng, params, PlaneKind.MEDIUM, mediumTarget)
    growFamily(builder, layoutRng, params, PlaneKind.SMALL, smallTarget)

    val styled = selectAccents(builder.planes, styleRng, colorRng, params.reds)
    val triangles = selectTriangles(builder.rails, styled, detailRng, params.triangles)
    val boundaryAccents = listOf(
        BoundaryAccent(
            -1,
            styleRng.between(ART_TOP + 520f, ART_TOP + 760f),
            colorRng.nextAccentTone(),
        ),
        BoundaryAccent(
            +1,
            styleRng.between(ART_TOP + 650f, ART_TOP + 900f),
            colorRng.nextAccentTone(),
        ),
    )
    return Composition(styled, builder.rails.toList(), boundaryAccents, triangles)
}

private fun growFamily(
    builder: CompositionBuilder,
    rng: Random,
    params: Params,
    kind: PlaneKind,
    target: Int,
) {
    var attempts = 0
    val maxAttempts = target.coerceAtLeast(1) * 100

    while (builder.planes.count { it.kind == kind } < target && attempts++ < maxAttempts) {
        val placed = builder.planes.count { it.kind == kind }
        val remaining = target - placed
        val pathGoal = min(remaining, rng.nextInt(2, 5))
        val ribbon = builder.nextRibbon++
        val start = findStart(builder, rng, params, kind, ribbon, pathGoal) ?: continue
        builder.planes += start

        var current = start
        var previousFace: Face? = null
        var pathSize = 1
        maybeAddEntrance(builder, current, rng)

        while (pathSize < pathGoal) {
            val leg = findLeg(builder, rng, current, previousFace) ?: break
            builder.rails += leg.rails
            builder.planes += leg.next
            current = leg.next
            previousFace = leg.face
            pathSize++
        }
        maybeAddExit(builder, current, rng)
    }
}

private fun findStart(
    builder: CompositionBuilder,
    rng: Random,
    params: Params,
    kind: PlaneKind,
    ribbon: Int,
    pathGoal: Int,
): Plane? {
    var best: Plane? = null
    var bestScore = Float.POSITIVE_INFINITY
    val minimumAdvance = when (kind) {
        PlaneKind.LARGE -> 92f
        PlaneKind.MEDIUM -> 78f
        PlaneKind.SMALL -> 70f
    }
    val maxStartY = (ART_BOTTOM - (pathGoal - 1) * minimumAdvance).coerceAtLeast(ART_TOP + 40f)
    val edgeSeed = rng.nextFloat() < params.edgeChance

    repeat(96) {
        val x = randomStartX(rng, kind, edgeSeed)
        val rows = ((maxStartY - ART_TOP) / HALF_LEVEL).toInt().coerceAtLeast(1)
        val y = ART_TOP + rng.nextInt(rows + 1) * HALF_LEVEL + rng.between(-5f, 5f)
        val candidate = Plane(x, y, kind, ribbon)
        if (!canPlace(candidate, builder.planes)) return@repeat

        val score = densityScore(candidate, builder.planes)
        if (score < bestScore) {
            best = candidate
            bestScore = score
        }
    }
    return best
}

private fun randomStartX(rng: Random, kind: PlaneKind, edgeSeed: Boolean): Float {
    if (edgeSeed) {
        val side = if (rng.nextFloat() < 0.18f) -1 else +1
        return if (side < 0) {
            ART_LEFT - rng.between(0f, kind.rx * 0.55f)
        } else {
            ART_RIGHT + rng.between(0f, kind.rx * 0.55f)
        }
    }

    val phase = if (kind == PlaneKind.MEDIUM) 0.5f else 0f
    val candidates = (0 until GRID_COLUMNS)
        .map { GRID_ORIGIN_X + (it + phase) * DX }
        .filter { it - kind.rx >= ART_LEFT - 3f && it + kind.rx <= ART_RIGHT + 3f }
    return candidates[rng.nextInt(candidates.size)]
}

private fun densityScore(candidate: Plane, planes: List<Plane>): Float {
    val cell = densityCell(candidate.x, candidate.y)
    val inCell = planes
        .filter { densityCell(it.x, it.y) == cell }
        .sumOf { it.visualWeight().toDouble() }
        .toFloat()
    val local = planes
        .filter { abs(it.x - candidate.x) < 150f && abs(it.y - candidate.y) < 170f }
        .sumOf { it.visualWeight().toDouble() }
        .toFloat()
    val clearance = planes.minOfOrNull { normalizedDistance(candidate, it) } ?: 8f
    return inCell * 100f + local * 12f - clearance.coerceAtMost(8f)
}

private fun Plane.visualWeight(): Float = kind.span * kind.span

private fun densityCell(x: Float, y: Float): Int {
    val col = (((x - ART_LEFT) / (ART_RIGHT - ART_LEFT)) * DENSITY_COLUMNS)
        .toInt()
        .coerceIn(0, DENSITY_COLUMNS - 1)
    val row = (((y - ART_TOP) / (ART_BOTTOM - ART_TOP)) * DENSITY_ROWS)
        .toInt()
        .coerceIn(0, DENSITY_ROWS - 1)
    return row * DENSITY_COLUMNS + col
}

private fun findLeg(
    builder: CompositionBuilder,
    rng: Random,
    current: Plane,
    previousFace: Face?,
): Leg? {
    var best: Leg? = null
    var bestScore = Float.POSITIVE_INFINITY
    repeat(24) {
        val fold = rng.nextFloat() < 0.38f
        val leg = if (fold) {
            proposeFold(rng, current)
        } else {
            proposeVerticalFace(rng, current, previousFace)
        }
        if (!canPlace(leg.next, builder.planes)) return@repeat

        val score = densityScore(leg.next, builder.planes) + rng.nextFloat() * 5f
        if (score < bestScore) {
            best = leg
            bestScore = score
        }
    }
    return best
}

private fun proposeVerticalFace(rng: Random, current: Plane, previousFace: Face?): Leg {
    val face = pickFace(rng, previousFace)
    val drop = pickDrop(rng)
    val next = current.copy(y = current.y + drop, accentTone = null)
    val (a, b) = edgeVertices(current, face)
    return Leg(
        next,
        listOf(
            Rail(a.x, a.y, a.y + drop),
            Rail(b.x, b.y, b.y + drop),
        ),
        face,
    )
}

private fun proposeFold(rng: Random, current: Plane): Leg {
    val direction = if (rng.nextBoolean()) 1 else -1
    val drop = pickDrop(rng)
    val next = current.copy(
        x = current.x + direction * current.rx,
        y = current.y + current.ry + drop,
        accentTone = null,
    )
    val face = if (direction > 0) Face.RIGHT_BOTTOM else Face.BOTTOM_LEFT
    return Leg(
        next,
        listOf(
            Rail(current.x + direction * current.rx, current.y, current.y + drop),
            Rail(current.x, current.y + current.ry, next.y),
        ),
        face,
    )
}

private fun pickFace(rng: Random, previousFace: Face?): Face {
    repeat(8) {
        val face = when (rng.nextFloat()) {
            in 0f..<0.35f -> Face.RIGHT_BOTTOM
            in 0.35f..<0.70f -> Face.BOTTOM_LEFT
            in 0.70f..<0.85f -> Face.TOP_RIGHT
            else -> Face.LEFT_TOP
        }
        if (face != previousFace) return face
    }
    return if (previousFace == Face.RIGHT_BOTTOM) Face.BOTTOM_LEFT else Face.RIGHT_BOTTOM
}

private fun pickDrop(rng: Random): Float {
    val levels = when (rng.nextFloat()) {
        in 0f..<0.34f -> 1
        in 0.34f..<0.78f -> 2
        in 0.78f..<0.95f -> 3
        else -> 4
    }
    return levels * LEVEL + rng.between(-7f, 7f)
}

private fun edgeVertices(plane: Plane, face: Face): Pair<Vertex, Vertex> {
    val top = Vertex(plane.x, plane.y - plane.ry)
    val right = Vertex(plane.x + plane.rx, plane.y)
    val bottom = Vertex(plane.x, plane.y + plane.ry)
    val left = Vertex(plane.x - plane.rx, plane.y)
    return when (face) {
        Face.TOP_RIGHT -> top to right
        Face.RIGHT_BOTTOM -> right to bottom
        Face.BOTTOM_LEFT -> bottom to left
        Face.LEFT_TOP -> left to top
    }
}

private fun maybeAddEntrance(builder: CompositionBuilder, plane: Plane, rng: Random) {
    if (plane.y > ART_TOP + 260f) return
    val direction = if (rng.nextBoolean()) 1 else -1
    val outerX = plane.x - direction * plane.rx
    builder.rails += Rail(plane.x, ART_TOP - 20f, plane.y - plane.ry)
    builder.rails += Rail(outerX, ART_TOP - 20f, plane.y)
}

private fun maybeAddExit(builder: CompositionBuilder, plane: Plane, rng: Random) {
    if (plane.y < ART_BOTTOM - 250f) return
    val direction = if (rng.nextBoolean()) 1 else -1
    builder.rails += Rail(plane.x + direction * plane.rx, plane.y, ART_BOTTOM + 20f)
    builder.rails += Rail(plane.x, plane.y + plane.ry, ART_BOTTOM + 20f)
}

private fun canPlace(candidate: Plane, planes: List<Plane>): Boolean {
    if (candidate.x + candidate.rx < ART_LEFT - 2f) return false
    if (candidate.x - candidate.rx > ART_RIGHT + 2f) return false
    if (candidate.y + candidate.ry < ART_TOP - 2f) return false
    if (candidate.y - candidate.ry > ART_BOTTOM + 2f) return false
    return planes.none { normalizedDistance(candidate, it) < 1f }
}

private fun normalizedDistance(a: Plane, b: Plane): Float {
    val gap = if (a.kind == PlaneKind.LARGE || b.kind == PlaneKind.LARGE) 10f else 6f
    val nx = abs(a.x - b.x) / (a.rx + b.rx + gap)
    val ny = abs(a.y - b.y) / (a.ry + b.ry + gap * 0.55f)
    return nx + ny
}

private fun selectTriangles(
    rails: List<Rail>,
    planes: List<Plane>,
    rng: Random,
    requested: Int,
): List<RailTriangle> {
    if (requested == 0) return emptyList()
    val endInset = TRIANGLE_SIZE + TRIANGLE_CLEARANCE
    val candidates = rails
        .filter { abs(it.y2 - it.y1) > endInset * 2f + TRIANGLE_SIZE }
        .shuffled(rng)
    val selected = mutableListOf<RailTriangle>()

    for (rail in candidates) {
        if (selected.size == requested) break
        findTriangleOnRail(rail, rails, planes, selected, rng, endInset)?.let(selected::add)
    }
    return selected
}

private fun findTriangleOnRail(
    rail: Rail,
    rails: List<Rail>,
    planes: List<Plane>,
    selected: List<RailTriangle>,
    rng: Random,
    endInset: Float,
): RailTriangle? {
    val minY = minOf(rail.y1, rail.y2) + endInset
    val maxY = maxOf(rail.y1, rail.y2) - endInset
    repeat(6) {
        val candidate = RailTriangle(
            x = rail.x,
            y = rng.between(minY, maxY),
        )
        if (canPlaceTriangle(candidate, rails, planes, selected)) return candidate
    }
    return null
}

private fun canPlaceTriangle(
    candidate: RailTriangle,
    rails: List<Rail>,
    planes: List<Plane>,
    selected: List<RailTriangle>,
): Boolean {
    if (candidate.x < ART_LEFT + 2f) return false
    if (candidate.x + TRIANGLE_SIZE > ART_RIGHT - 2f) return false
    if (candidate.y - TRIANGLE_SIZE < ART_TOP + 2f) return false
    if (candidate.y + TRIANGLE_SIZE > ART_BOTTOM - 2f) return false
    if (planes.any { isNearPlane(candidate, it) }) return false
    if (overlapsVerticalRail(candidate, rails)) return false

    val minimumGap = TRIANGLE_SIZE * 2f + TRIANGLE_CLEARANCE
    return selected.none {
        hypot((candidate.x - it.x).toDouble(), (candidate.y - it.y).toDouble()) < minimumGap
    }
}

private fun overlapsVerticalRail(triangle: RailTriangle, rails: List<Rail>): Boolean {
    val halfStroke = RAIL_WIDTH / 2f
    return rails.any { rail ->
        val dx = rail.x - triangle.x
        if (dx <= 0.5f || dx - halfStroke >= TRIANGLE_SIZE) return@any false

        val effectiveDx = (dx - halfStroke).coerceAtLeast(0f)
        val halfHeight = TRIANGLE_SIZE * (1f - effectiveDx / TRIANGLE_SIZE) + halfStroke
        val railTop = minOf(rail.y1, rail.y2)
        val railBottom = maxOf(rail.y1, rail.y2)
        railBottom >= triangle.y - halfHeight && railTop <= triangle.y + halfHeight
    }
}

private fun isNearPlane(triangle: RailTriangle, plane: Plane): Boolean {
    val margin = TRIANGLE_SIZE + TRIANGLE_CLEARANCE
    val nx = abs(triangle.x - plane.x) / (plane.rx + margin)
    val ny = abs(triangle.y - plane.y) / (plane.ry + margin)
    return nx + ny < 1f
}

private fun selectAccents(
    planes: List<Plane>,
    placementRng: Random,
    colorRng: Random,
    requested: Int,
): List<Plane> {
    if (requested == 0) return planes
    val candidates = planes.indices.filter { index ->
        val plane = planes[index]
        plane.kind != PlaneKind.LARGE &&
            plane.x in ART_LEFT + plane.rx..ART_RIGHT - plane.rx &&
            plane.y in ART_TOP + 35f..ART_BOTTOM - 35f
    }
    val target = min(requested, candidates.size)
    val selected = mutableListOf<Int>()

    val mediumLower = candidates.filter {
        planes[it].kind == PlaneKind.MEDIUM && planes[it].y > (ART_TOP + ART_BOTTOM) / 2f
    }
    if (mediumLower.isNotEmpty() && target > 0) selected += mediumLower[placementRng.nextInt(mediumLower.size)]

    while (selected.size < target) {
        val next = candidates
            .asSequence()
            .filter { it !in selected }
            .maxByOrNull { index -> accentScore(planes[index], selected.map(planes::get), placementRng) }
            ?: break
        selected += next
    }

    val selectedSet = selected.toSet()
    return planes.mapIndexed { index, plane ->
        plane.copy(accentTone = if (index in selectedSet) colorRng.nextAccentTone() else null)
    }
}

private fun accentScore(candidate: Plane, selected: List<Plane>, rng: Random): Float {
    if (selected.isEmpty()) return rng.nextFloat()
    val minDistance = selected.minOf {
        hypot((candidate.x - it.x).toDouble(), (candidate.y - it.y).toDouble()).toFloat()
    }
    val sameRibbon = selected.any { it.ribbon == candidate.ribbon }
    val candidateCell = accentCell(candidate)
    val newCellBonus = if (selected.none { accentCell(it) == candidateCell }) 115f else 0f
    val ribbonPenalty = if (sameRibbon) 70f else 0f
    return minDistance + newCellBonus - ribbonPenalty + rng.nextFloat() * 18f
}

private fun accentCell(plane: Plane): Int {
    val xThird = (((plane.x - ART_LEFT) / (ART_RIGHT - ART_LEFT)) * 3f).toInt().coerceIn(0, 2)
    val yQuarter = (((plane.y - ART_TOP) / (ART_BOTTOM - ART_TOP)) * 4f).toInt().coerceIn(0, 3)
    return yQuarter * 3 + xThird
}

private fun render(c: Canvas, composition: Composition) {
    c.clear(BLACK)
    c.save()
    c.clipRect(Rect.makeLTRB(ART_LEFT, ART_TOP, ART_RIGHT, ART_BOTTOM))

    val railPaint = strokeOf(CREAM, RAIL_WIDTH).apply { strokeCap = PaintStrokeCap.BUTT }
    composition.rails.forEach { rail -> c.drawLine(rail.x, rail.y1, rail.x, rail.y2, railPaint) }

    val creamPaint = fillOf(CREAM)
    composition.triangles.forEach { triangle -> drawRailTriangle(c, triangle, creamPaint) }
    composition.planes.forEach { plane ->
        c.drawPath(diamond(plane.x, plane.y, plane.rx, plane.ry), creamPaint)
    }

    val redPaint = fillOf(RED)
    val yellowPaint = fillOf(YELLOW)
    composition.planes.filter { it.accentTone != null }.forEach { plane ->
        val paint = if (plane.accentTone == AccentTone.RED) redPaint else yellowPaint
        c.drawPath(diamond(plane.x, plane.y, plane.rx * 0.84f, plane.ry * 0.82f), paint)
    }
    composition.boundaryAccents.forEach { drawBoundaryAccent(c, it) }
    c.restore()
}

private fun drawRailTriangle(c: Canvas, triangle: RailTriangle, paint: Paint) {
    val path = PathBuilder()
        .moveTo(triangle.x, triangle.y - TRIANGLE_SIZE)
        .lineTo(triangle.x + TRIANGLE_SIZE, triangle.y)
        .lineTo(triangle.x, triangle.y + TRIANGLE_SIZE)
        .closePath()
        .detach()
    c.drawPath(path, paint)
}

private fun drawBoundaryAccent(c: Canvas, accent: BoundaryAccent) {
    val x = if (accent.side < 0) ART_LEFT else ART_RIGHT
    val direction = if (accent.side < 0) 1f else -1f
    val outer = PathBuilder()
        .moveTo(x, accent.y - DY)
        .lineTo(x + direction * DX, accent.y)
        .lineTo(x, accent.y + DY)
        .closePath()
        .detach()
    val inner = PathBuilder()
        .moveTo(x + direction * 2f, accent.y - DY * 0.76f)
        .lineTo(x + direction * DX * 0.84f, accent.y)
        .lineTo(x + direction * 2f, accent.y + DY * 0.76f)
        .closePath()
        .detach()
    c.drawPath(outer, fillOf(CREAM))
    c.drawPath(inner, fillOf(if (accent.tone == AccentTone.RED) RED else YELLOW))
}

private fun diamond(cx: Float, cy: Float, rx: Float, ry: Float): Path = PathBuilder()
    .moveTo(cx, cy - ry)
    .lineTo(cx + rx, cy)
    .lineTo(cx, cy + ry)
    .lineTo(cx - rx, cy)
    .closePath()
    .detach()

private fun Random.between(min: Float, max: Float): Float = min + nextFloat() * (max - min)

private fun Random.nextAccentTone(): AccentTone = if (nextBoolean()) AccentTone.RED else AccentTone.YELLOW
