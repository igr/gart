package dev.oblac.gart.fx

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.math.hash01
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset

/**
 * One family of fibres lying in a sheet: a noise field stretched along one axis, so its
 * features come out as long threads rather than blobs.
 *
 * The stretch is in the two frequencies. Near pixel scale a thread stops being a thread and
 * reads as speckle, which is the mistake worth avoiding: on a 1200px page `0.035f x 0.45f`
 * gives fibres about 25px long and 2px thick, the scale an eye can still follow.
 *
 * @property weight how much this family counts in the surface
 * @property freqX  frequency across the page
 * @property freqY  frequency down the page
 * @property patch  which patch of the noise field this family is cut from; two families want
 *                  different patches or they move together
 */
data class Fibres(val weight: Float, val freqX: Float, val freqY: Float, val patch: Float)

/**
 * A sheet of paper as a height field: two families of fibres crossing at right angles, a slow
 * round [felt] field under them, and white noise for the fine tooth.
 *
 * [laid] and [chain] are weighted against each other and the pair as a whole is then weighted
 * by [fibre], so the mix is `fibre * (laid + chain) + felt + tooth`. The defaults are a laid
 * stock measured on a 1200px page; [felt] is the one that has to be far slower than you would
 * guess (0.013f) or it turns into a second speckle instead of cloudy unevenness.
 *
 * @property laid  fibres running across the page
 * @property chain fibres running down it
 * @property felt  the slow, round unevenness of the pulp; `null` leaves it out
 * @property fibre how much the two fibre families count against felt and tooth
 * @property tooth how much the fine white-noise tooth counts
 */
data class Paper(
    val laid: Fibres = Fibres(0.55f, 0.035f, 0.45f, 611f),
    val chain: Fibres = Fibres(0.45f, 0.5f, 0.04f, 622f),
    val felt: Fibres? = Fibres(0.32f, 0.013f, 0.014f, 901f),
    val fibre: Float = 0.5f,
    val tooth: Float = 0.42f,
)

/**
 * A [Paper] cut for one seed, sampled per pixel.
 *
 * [heightAt] is signed: positive stands proud of the sheet, negative is a hollow. Nothing here
 * says what the ink then does about it - see [printOnPaper] for the usual answer, or read the
 * height yourself when the response is more than a lerp.
 */
class PaperSurface(private val seed: Int, val paper: Paper = Paper()) {
    private val nz = noiseOffset(seed.toLong())

    /** The fine tooth alone, -0.5..0.5. Handy when the ink reacts to the peaks separately. */
    fun toothAt(x: Int, y: Int) = hash01(x, y, seed) - 0.5f

    fun heightAt(x: Int, y: Int) = heightAt(x, y, toothAt(x, y))

    /** As [heightAt], for a tooth already sampled with [toothAt]. */
    fun heightAt(x: Int, y: Int, tooth: Float): Float {
        var h = paper.fibre * (paper.laid.weight * noise(x, y, paper.laid) + paper.chain.weight * noise(x, y, paper.chain))
        val felt = paper.felt
        if (felt != null) h += felt.weight * noise(x, y, felt)
        return h + paper.tooth * tooth
    }

    private fun noise(x: Int, y: Int, f: Fibres) = SimplexNoise.noise(x * f.freqX + nz, y * f.freqY + f.patch)
}

/**
 * Presses whatever is already on [gartvas] into a sheet of paper, in place: what stands proud
 * of the sheet resists the ink and lerps toward [resist], the hollows let it pool and lerp
 * toward [pool]. Adding one flat noise value to every channel instead - the usual way - reads
 * as digital speckle sitting *on* the print rather than the print sitting on paper.
 *
 * @param resistAmount strength where the surface is raised, per unit of height
 * @param poolAmount   and where it is hollow; defaults to the same
 */
fun printOnPaper(
    gartvas: Gartvas,
    surface: PaperSurface,
    resist: Int,
    pool: Int,
    resistAmount: Float,
    poolAmount: Float = resistAmount,
) {
    Gartmap(gartvas).use { m ->
        val w = gartvas.d.w
        for (y in 0 until gartvas.d.h) for (x in 0 until w) {
            val i = y * w + x
            val high = surface.heightAt(x, y)
            val col = if (high > 0f) lerpColor(m.pixels[i], resist, (resistAmount * high).coerceIn(0f, 1f))
            else lerpColor(m.pixels[i], pool, (poolAmount * -high).coerceIn(0f, 1f))
            m.pixels[i] = col or (255 shl 24)
        }
        m.drawToCanvas()
    }
}
