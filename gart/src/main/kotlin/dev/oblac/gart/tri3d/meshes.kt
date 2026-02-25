package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.vec3

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
