package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.mediumPurple
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.orange
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import dev.oblac.gart.tri3d.*
import dev.oblac.gart.vector.vec3
import org.jetbrains.skia.*
import kotlin.math.cos
import kotlin.math.sin

val slide13 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.020f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 40f, rect.right, rect.bottom - 5f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    c.clear(CssColors.darkSlateGray)
    c.drawTitle("Vertices")

    val grid = contentBox.shrink(20f).splitToGrid(2, 2)

    // 1. drawVertices — indexed triangle mesh
    val g1 = grid[0].shrink(8f)
    val cx1 = g1.left + g1.width / 2
    val cy1 = g1.top + g1.height / 2 - 15f
    val r1 = g1.height * 0.35f
    //--- src: 1 drawVertices
    // vertices: center + 6 points on a hexagon
    val positions = mutableListOf(cx1, cy1)
    val vertColors = mutableListOf(white)
    val hexCols = intArrayOf(coral, deepSkyBlue, mediumSeaGreen, gold, coral, deepSkyBlue)
    for (i in 0 until 6) {
        val a = (i.toFloat() / 6) * Math.PI.toFloat() * 2
        positions.add(cx1 + cos(a) * r1)
        positions.add(cy1 + sin(a) * r1)
        vertColors.add(hexCols[i])
    }
    // indices: 6 triangles sharing the center vertex
    val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3, 0, 3, 4,
        0, 4, 5, 0, 5, 6, 0, 6, 1,
    )
    c.drawVertices(
        VertexMode.TRIANGLES,
        positions.toFloatArray(),
        vertColors.toIntArray(),
        null, indices,
        BlendMode.MODULATE,
        white.toFillPaint()
    )
    //--- crs: 1
    c.drawLabel(g1, "drawVertices")

    // 2. drawTriangleStrip — alternating colors
    val g2 = grid[1].shrink(8f)
    //--- src: 2 drawTriangleStrip colors
    val stripPts2 = mutableListOf<Point>()
    val n2 = 8
    for (i in 0..n2) {
        val t = i.toFloat() / n2
        val x = g2.left + g2.width * (0.1f + t * 0.8f)
        val yMid = g2.top + g2.height * 0.45f
        val amp = g2.height * 0.25f
        val wave = sin(t * Math.PI * 2).toFloat() * amp * 0.3f
        stripPts2.add(Point(x, yMid - amp * 0.5f + wave))
        stripPts2.add(Point(x, yMid + amp * 0.5f + wave))
    }
    val stripArr = stripPts2.toTypedArray()
    val stripCols = intArrayOf(coral, mediumSeaGreen, deepSkyBlue, gold)
    for (i in 0 until stripArr.size - 2) {
        c.drawTriangles(
            arrayOf(stripArr[i], stripArr[i + 1], stripArr[i + 2]),
            null, null, null,
            BlendMode.SRC_OVER,
            stripCols[i % stripCols.size].toFillPaint()
        )
    }
    //--- crs: 2
    c.drawLabel(g2, "strip with colors")

    // 3. drawTriangleFan — radial fan from center
    val g3 = grid[2].shrink(8f)
    val cx3 = g3.left + g3.width / 2
    val cy3 = g3.top + g3.height / 2 - 15f
    val r3 = g3.height * 0.33f
    //--- src: 3 drawTriangleFan
    val fanPts = mutableListOf(Point(cx3, cy3))
    val fanColors = mutableListOf(white)
    val segments = 12
    val rimCols = intArrayOf(coral, deepSkyBlue, mediumSeaGreen, gold)
    for (i in 0..segments) {
        val a = (i.toFloat() / segments) * Math.PI.toFloat() * 2
        fanPts.add(Point(cx3 + cos(a) * r3, cy3 + sin(a) * r3))
        fanColors.add(rimCols[i % rimCols.size])
    }
    c.drawTriangleFan(
        fanPts.toTypedArray(),
        fanColors.toIntArray(),
        null, null,
        BlendMode.MODULATE,
        white.toFillPaint()
    )
    //--- crs: 3
    c.drawLabel(g3, "drawTriangleFan")

    // 4. Rotating 3D cube projected to 2D via drawTriangles
    val g4 = grid[3].shrink(8f)
    val cx4 = g4.left + g4.width / 2
    val cy4 = g4.top + g4.height / 2 - 15f
    val size = g4.height * 0.28f
    //--- src: 4 3D cube
    val t = f.timeSeconds
    val cubeMesh = cube(
        intArrayOf(coral, deepSkyBlue, mediumSeaGreen, gold, mediumPurple, orange)
    )
    val rotated = Mesh(cubeMesh.faces.map { face ->
        face.rotateY(t * 0.8f).rotateX(t * 0.5f)
    })
    val camera = Camera(cx4, cy4, size, 4f)
    Scene.render(c, camera, rotated)

    // wireframe overlay
    val cubeVerts = arrayOf(
        vec3(-1, -1, -1), vec3(1, -1, -1),
        vec3(1, 1, -1), vec3(-1, 1, -1),
        vec3(-1, -1, 1), vec3(1, -1, 1),
        vec3(1, 1, 1), vec3(-1, 1, 1),
    ).map { camera.project(rotateX(rotateY(it, t * 0.8f), t * 0.5f)) }
    val edges = arrayOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,
        4 to 5, 5 to 6, 6 to 7, 7 to 4,
        0 to 4, 1 to 5, 2 to 6, 3 to 7,
    )
    val wirePaint = white.toStrokePaint(1.5f)
    for ((a, b) in edges) {
        c.drawLine(cubeVerts[a].x, cubeVerts[a].y, cubeVerts[b].x, cubeVerts[b].y, wirePaint)
    }
    //--- crs: 4
    c.drawLabel(g4, "3D cube projection")
}
