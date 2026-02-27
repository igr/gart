package dev.oblac.gart.tri3d

import dev.oblac.gart.math.PIf
import dev.oblac.gart.vector.Vec3
import dev.oblac.gart.vector.vec3
import kotlin.math.cos
import kotlin.math.sin

fun cube(colors: IntArray): Mesh {
    val v = arrayOf(
        vec3(-1, -1, -1), vec3(1, -1, -1),
        vec3(1, 1, -1), vec3(-1, 1, -1),
        vec3(-1, -1, 1), vec3(1, -1, 1),
        vec3(1, 1, 1), vec3(-1, 1, 1),
    )

    // 6 sides, 2 triangles each
    val faces = listOf(
        // front (z = -1)
        Face(v[0], v[1], v[2], colors[0]),
        Face(v[0], v[2], v[3], colors[0]),
        // back (z = 1)
        Face(v[5], v[4], v[7], colors[1]),
        Face(v[5], v[7], v[6], colors[1]),
        // left (x = -1)
        Face(v[4], v[0], v[3], colors[2]),
        Face(v[4], v[3], v[7], colors[2]),
        // right (x = 1)
        Face(v[1], v[5], v[6], colors[3]),
        Face(v[1], v[6], v[2], colors[3]),
        // top (y = 1)
        Face(v[3], v[2], v[6], colors[4]),
        Face(v[3], v[6], v[7], colors[4]),
        // bottom (y = -1)
        Face(v[4], v[5], v[1], colors[5]),
        Face(v[4], v[1], v[0], colors[5]),
    )

    return Mesh(faces)
}

/**
 * Generates a UV sphere of radius 1, centered at origin.
 *
 * @param stacks  number of horizontal slices (latitude bands)
 * @param slices  number of vertical slices (longitude bands)
 * @param colorFn maps (stack index, slice index) to an ARGB color
 */
fun sphere(stacks: Int, slices: Int, colorFn: (Int, Int) -> Int): Mesh {
    // generate vertex grid: (stacks+1) rows x (slices+1) columns
    val verts = Array(stacks + 1) { i ->
        val phi = PIf * i / stacks   // 0..PI  (north pole to south pole)
        val sp = sin(phi)
        val cp = cos(phi)
        Array(slices + 1) { j ->
            val theta = 2f * PIf * j / slices  // 0..2PI
            Vec3(sp * cos(theta), cp, sp * sin(theta))
        }
    }

    val faces = mutableListOf<Face>()
    for (i in 0 until stacks) {
        for (j in 0 until slices) {
            val color = colorFn(i, j)
            val v00 = verts[i][j]
            val v10 = verts[i + 1][j]
            val v11 = verts[i + 1][j + 1]
            val v01 = verts[i][j + 1]

            // two triangles per quad (skip degenerate caps)
            if (i != 0) {
                faces.add(Face(v00, v10, v01, color))
            }
            if (i != stacks - 1) {
                faces.add(Face(v01, v10, v11, color))
            }
        }
    }
    return Mesh(faces)
}
