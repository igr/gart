package studio.oblac.gart

fun scrollPixelsUp(p:Pixels, delta: Int) {
    for (y in delta until p.d.h) {
        for (x in 0 until p.d.w) {
            p[x, y - delta] = p[x, y]
        }
    }
}
