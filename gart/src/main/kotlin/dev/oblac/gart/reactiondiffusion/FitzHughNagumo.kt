package dev.oblac.gart.reactiondiffusion

/**
 * FitzHugh-Nagumo reaction-diffusion model (excitable-media neural dynamics).
 *
 * Two species `u` (membrane potential) and `v` (recovery) evolve under:
 * ```
 * du = k1 * u - k2 * u^2 - u^3 - v + lap(u)
 * dv = epsilon * (k3 * u - a1 * v - a0) + delta * lap(v)
 * ```
 *
 * The integration step is `passes * 0.1` (matches the original GLSL shader),
 * and both species are clamped to `[0, 1]` after each update. Initial state
 * is `u = 0`, `v = 0`; perturb by seeding either species in a small region.
 *
 * Typical patterns: traveling wavefronts, target patterns, spirals.
 */
class FitzHughNagumo(
    override val width: Int,
    override val height: Int,
    var a0: Float = 0.459184f,
    var a1: Float = 0.780612f,
    var epsilon: Float = 0.642857f,
    var delta: Float = 3.0f,
    var k1: Float = 1.63776f,
    var k2: Float = 0.336735f,
    var k3: Float = 2.29592f,
    override var passes: Float = 1.0f,
) : ReactionDiffusion {

    private var u = FloatArray(width * height)
    private var v = FloatArray(width * height)
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
        u.fill(0f)
        v.fill(0f)
    }

    override fun step() {
        val w = width
        val h = height
        val dt = passes * 0.1f
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val i = row + x
                val ui = u[i]
                val vi = v[i]
                val lapU = laplacian(u, x, y, w, h)
                val lapV = laplacian(v, x, y, w, h)
                val du = k1 * ui - k2 * ui * ui - ui * ui * ui - vi + lapU
                val dv = epsilon * (k3 * ui - a1 * vi - a0) + delta * lapV
                uNext[i] = (ui + du * dt).coerceIn(0f, 1f)
                vNext[i] = (vi + dv * dt).coerceIn(0f, 1f)
            }
        }
        val tmpU = u; u = uNext; uNext = tmpU
        val tmpV = v; v = vNext; vNext = tmpV
    }
}
