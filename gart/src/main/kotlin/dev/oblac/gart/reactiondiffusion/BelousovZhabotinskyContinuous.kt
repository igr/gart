package dev.oblac.gart.reactiondiffusion

/**
 * Continuous-valued three-species Belousov-Zhabotinsky reaction.
 *
 * Distinct from the integer cellular automata in
 * `dev.oblac.gart.cellular.BelousovZhabotinskyReaction{1,2}`: this is the
 * continuous PDE variant from the `ofxReactionDiffusion` addon.
 *
 * Species `a`, `b`, `c` evolve under a 3x3 neighborhood average (including
 * the center cell, `avg` below) and cyclic coupling:
 * ```
 * a' = avg(a) + avg(a) * (alpha * avg(b) * gamma - avg(c)) * passes
 * b' = avg(b) + avg(b) * (beta  * avg(c)         - alpha * avg(a)) * passes
 * c' = avg(c) + avg(c) * (gamma * avg(a)         - beta  * avg(b)) * passes
 * ```
 *
 * Typical patterns: rotating spirals, target waves, chaotic mosaics.
 */
class BelousovZhabotinskyContinuous(
    override val width: Int,
    override val height: Int,
    var alpha: Float = 1.0f,
    var beta: Float = 1.0f,
    var gamma: Float = 1.0f,
    override var passes: Float = 1.0f,
) : ReactionDiffusion {

    private var a = FloatArray(width * height)
    private var b = FloatArray(width * height)
    private var c = FloatArray(width * height)
    private var aNext = FloatArray(width * height)
    private var bNext = FloatArray(width * height)
    private var cNext = FloatArray(width * height)

    init {
        reset()
    }

    fun a(x: Int, y: Int): Float = a[y * width + x]
    fun b(x: Int, y: Int): Float = b[y * width + x]
    fun c(x: Int, y: Int): Float = c[y * width + x]

    fun setA(x: Int, y: Int, value: Float) {
        a[y * width + x] = value
    }

    fun setB(x: Int, y: Int, value: Float) {
        b[y * width + x] = value
    }

    fun setC(x: Int, y: Int, value: Float) {
        c[y * width + x] = value
    }

    override fun displayValue(x: Int, y: Int): Float = c[y * width + x]

    override fun reset() {
        a.fill(0f)
        b.fill(0f)
        c.fill(0f)
    }

    override fun step() {
        val w = width
        val h = height
        val p = passes
        for (y in 0 until h) {
            for (x in 0 until w) {
                val sa = avg3x3(a, x, y, w, h)
                val sb = avg3x3(b, x, y, w, h)
                val sc = avg3x3(c, x, y, w, h)
                val i = y * w + x
                aNext[i] = sa + sa * (alpha * sb * gamma - sc) * p
                bNext[i] = sb + sb * (beta * sc - alpha * sa) * p
                cNext[i] = sc + sc * (gamma * sa - beta * sb) * p
            }
        }
        val tmpA = a; a = aNext; aNext = tmpA
        val tmpB = b; b = bNext; bNext = tmpB
        val tmpC = c; c = cNext; cNext = tmpC
    }

    private fun avg3x3(src: FloatArray, x: Int, y: Int, w: Int, h: Int): Float {
        val xm = if (x > 0) x - 1 else 0
        val xp = if (x < w - 1) x + 1 else w - 1
        val ym = if (y > 0) y - 1 else 0
        val yp = if (y < h - 1) y + 1 else h - 1

        val rowM = ym * w
        val row0 = y * w
        val rowP = yp * w

        val sum = src[rowM + xm] + src[rowM + x] + src[rowM + xp] +
            src[row0 + xm] + src[row0 + x] + src[row0 + xp] +
            src[rowP + xm] + src[rowP + x] + src[rowP + xp]
        return sum / 9f
    }
}
