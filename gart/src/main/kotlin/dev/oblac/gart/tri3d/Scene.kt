package dev.oblac.gart.tri3d

import dev.oblac.gart.color.toFillPaint
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas

object Scene {

    fun render(canvas: Canvas, camera: Camera, mesh: Mesh) {
        val visible = mesh.faces
            .withIndex()
            .filter { (_, face) -> camera.isFrontFacing(face) }

        val sorted = visible.sortedWith(
            camera.painterComparator(
                vertices = { (_, face) ->
                    Triple(face.a, face.b, face.c)
                },
                id = { (i, _) -> i },
            )
        )
        for ((_, face) in sorted) {
            val pa = camera.project(face.a)
            val pb = camera.project(face.b)
            val pc = camera.project(face.c)
            canvas.drawTriangles(
                arrayOf(pa, pb, pc),
                null, null, null,
                BlendMode.SRC_OVER,
                face.color.toFillPaint()
            )
        }
    }
}
