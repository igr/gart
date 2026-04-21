package dev.oblac.gart.reactiondiffusion

/**
 * Gray-Scott reaction-diffusion model.
 *
 * Two species `U` (substrate) and `V` (activator) evolve under:
 * ```
 * du = Du * lap(u) - u * v * v + feed * (1 - u)
 * dv = Dv * lap(v) + u * v * v - (feed + kill) * v
 * ```
 *
 * Initial state is the equilibrium `u = 1`, `v = 0`. Perturb by seeding
 * `v > 0` in a small region (see the interface KDoc for recipes).
 *
 * Defaults come from the original `ofxReactionDiffusion` constructor and
 * produce classic coral / mitosis patterns.
 */
class GrayScott(
    override val width: Int,
    override val height: Int,
    var feed: Float = 0.037f,
    var kill: Float = 0.06f,
    var Du: Float = 0.21f,
    var Dv: Float = 0.105f,
    override var passes: Float = 1.0f,
) : ReactionDiffusion {

    private var u = FloatArray(width * height) { 1f }
    private var v = FloatArray(width * height) { 0f }
    private var uNext = FloatArray(width * height)
    private var vNext = FloatArray(width * height)

    fun u(x: Int, y: Int): Float = u[y * width + x]
    fun v(x: Int, y: Int): Float = v[y * width + x]

    fun setU(x: Int, y: Int, value: Float) {
        u[y * width + x] = value
    }

    fun setV(x: Int, y: Int, value: Float) {
        v[y * width + x] = value
    }

    override fun displayValue(x: Int, y: Int): Float = v[y * width + x]

    override fun reset() {
        u.fill(1f)
        v.fill(0f)
    }

    override fun step() {
        val w = width
        val h = height
        val p = passes
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val i = row + x
                val ui = u[i]
                val vi = v[i]
                val reaction = ui * vi * vi
                val lapU = laplacian(u, x, y, w, h)
                val lapV = laplacian(v, x, y, w, h)
                val du = Du * lapU - reaction + feed * (1f - ui)
                val dv = Dv * lapV + reaction - (feed + kill) * vi
                uNext[i] = ui + p * du
                vNext[i] = vi + p * dv
            }
        }
        val tmpU = u; u = uNext; uNext = tmpU
        val tmpV = v; v = vNext; vNext = tmpV
    }
}
