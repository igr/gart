package glossa

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * alien, but the letters got a language.
 *
 * one construction rule - a spine hanging off a headline bar - makes the alphabet,
 * alphabet makes a lexicon, lexicon gets set as prose. thats the whole trick really.
 * the page should read as *writing* and not as decoration, and it turns out that comes
 * mostly from repetition + zipf, not from the glyphs themselves being clever.
 */
private const val W = 1024
private const val H = 1448 // a4-ish. close enough

private data class Params(
    val seed: Long,
    val out: String,
    // margins, clockwise from the top
    val mt: Float, val mb: Float, val ml: Float, val mr: Float,
    // type
    val em: Float,
    val lead: Float,
    val weight: Float,
    val track: Float, // gap between letters, in em
    val space: Float, // and between words
    // the script itself
    val alphabet: Int,
    val lexicon: Int,
    val zipfG: Float, val zipfW: Float, // how hard the letter/word frequency falls off
    val wordMin: Int, val wordMax: Int,
    val descend: Float,
    val marks: Float,
    val bowls: Float,
    // page furniture
    val title: Int,
    val paraMin: Int, val paraMax: Int,
    val dropcap: Int,
    val rubric: Int, // 1 word in N goes red
    val punct: Int, //  1 in N gets a tick after it
    val margins: Int,
    // ink and paper
    val ruling: Int,
    val paper: Int,
    val ink: Int,
    val red: Int,
    val foxing: Int,
    val grain: Float,
    val vignette: Float,
    val flow: Int,
    val jitter: Float,
)

// the impressions. paper / ink / red - and the red is the only accent the whole page gets,
// so picking a palette is really just deciding how loud that second colour is allowed to be.
// 0 is the original and stays put, everything already printed came off it.
// (has to sit above `p` down there, top-level vals init in file order and i got a very
// confusing NPE out of having it next to resolveParams where it belongs)
private val PRESSES = arrayOf(
    // torinoko paper, aisumicha ink, ginsyu for the red. dont touch, took ages
    intArrayOf(NipponColors.col105_TORINOKO, NipponColors.col245_AISUMICHA, NipponColors.col029_GINSYU),
    // sumi only. rubric drops to a second grey, so theres no colour on the page at all
    intArrayOf(NipponColors.col233_SHIRONERI, NipponColors.col248_SUMI, NipponColors.col247_KESHIZUMI),
    // one earth hue for the lot - bengara over kogecha on tanned stock. reads much older
    intArrayOf(NipponColors.col100_SHIROTSURUBAMI, NipponColors.col057_KOGECHA, NipponColors.col046_BENGARA),
    // cold. grey stock, indigo hand, plum accent - comes out official rather than devotional
    intArrayOf(NipponColors.col235_SHIRONEZUMI, NipponColors.col195_KON, NipponColors.col003_SUOH),
    // a handbill and not a manuscript. shouldnt work and mostly doesnt, but keep rubric low
    // and the vermilion lands on maybe two words a page, which is worth the trouble
    intArrayOf(NipponColors.col106_USUKI, NipponColors.col250_RO, NipponColors.col037_SYOJYOHI),
)

private val p = resolveParams()
private val rnd = Random(p.seed)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("glossa", W, H)

    println(gart)
    // sweeping this thing without the knobs echoed back is a nightmare, i never
    // remember which seed made which page
    println(
        "seed=${p.seed} alphabet=${p.alphabet} lexicon=${p.lexicon} em=${p.em} " +
            "lead=${p.lead} weight=${p.weight} rubric=${p.rubric}",
    )

    val g = gart.gartvas()
    render(g)

    val output = if (p.out.endsWith(".png", ignoreCase = true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)

    if (!headless) gart.window().showImage(g)
}

// the script /////////

// a letter, in em units. x runs 0..width, y=0 is the headline that the word bar rides on
// and y=1 is the baseline, so marks live above 0 / below 1. ink is stroked, blobs filled -
// two paths because one paint cant do both and i didnt want to split the class.
private class Glyph(val width: Float, val ink: Path, val blobs: Path)

/** little scribe. collects strokes in em units, hands back a glyph when its done */
private class Pen {
    private val ink = PathBuilder()
    private val blobs = PathBuilder()

    fun seg(x1: Float, y1: Float, x2: Float, y2: Float) {
        ink.moveTo(x1, y1).lineTo(x2, y2)
    }

    // polyline. all one subpath so the corners join round instead of reading as two
    // seperate caps sat next to each other
    fun run(vararg xy: Float) {
        ink.moveTo(xy[0], xy[1])
        var i = 2
        while (i < xy.size) {
            ink.lineTo(xy[i], xy[i + 1])
            i += 2
        }
    }

    fun dot(x: Float, y: Float, r: Float) { blobs.addCircle(x, y, r) }
    fun ring(x: Float, y: Float, r: Float) { ink.addCircle(x, y, r) }

    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) {
        ink.arcTo(Rect.makeXYWH(cx - r, cy - r, r * 2, r * 2), start, sweep, true)
    }

    fun glyph(width: Float) = Glyph(width, ink.detach(), blobs.detach())
}

// arms only ever land on these heights. quantising them is honestly most of what makes
// the alphabet look related instead of just random - dont be tempted to make it continuous
private val LEVELS = floatArrayOf(0.32f, 0.62f, 1f)

// one letter. every glyph hangs at least one vertical off the headline and everything
// else - arms, hooks, bowls, terminals, diacritics - is optional decoration on that spine.
// same rule ~26 times and it reads as one hand.
private fun makeGlyph(): Glyph {
    val w = listOf(0.5f, 0.72f, 0.72f, 0.96f).random(rnd) // 0.72 twice on purpose, it's the common one
    val pen = Pen()

    // the spine
    val sx = listOf(0f, w / 2, w).random(rnd)
    val foot = if (chance(p.descend)) 1.3f else listOf(0.62f, 1f, 1f).random(rnd)
    pen.seg(sx, 0f, sx, foot)

    // a second, usually shorter vertical. gives the wide glyphs something to hold onto,
    // narrow ones look better without
    val far = if (sx == 0f) w else 0f
    var hasSecond = false
    if (w > 0.6f && chance(0.45f)) {
        hasSecond = true
        pen.seg(far, 0f, far, listOf(0.32f, 0.62f, 1f).random(rnd))
    }

    // arms off the spine, sometimes hooked at the end. two arms is rare on purpose,
    // any more than that and the letters stop being distinguishable
    repeat(if (chance(0.25f)) 2 else 1) {
        val y = LEVELS.toList().random(rnd)
        val to = if (sx == w / 2 && chance(0.5f)) 0f else far
        val hook = if (chance(0.4f)) listOf(-0.26f, 0.26f).random(rnd) else 0f
        if (hook == 0f) pen.seg(sx, y, to, y) else pen.run(sx, y, to, y, to, y + hook)
    }

    // a slash across the body. the coerce is so it cant dive past the descenders
    if (chance(0.22f)) {
        val y1 = LEVELS.random(rnd)
        pen.seg(sx, y1, far, (y1 + 0.38f).coerceAtMost(1.15f))
    }

    // the bowl - closed counter hung on the spine. basically the only curved thing in
    // the whole script, so it does a lot of work
    if (chance(p.bowls)) {
        val cy = listOf(0.42f, 0.72f).random(rnd)
        val r = 0.2f
        val cx = if (sx == 0f) r else if (sx == w) w - r else sx
        when (ri(0, 3)) {
            0 -> pen.ring(cx, cy, r)
            1 -> pen.dot(cx, cy, r * 0.62f)
            2 -> pen.arc(cx, cy, r, if (sx == 0f) -90f else 90f, 180f)
            else -> pen.run(cx - r, cy - r, cx + r, cy - r, cx + r, cy + r, cx - r, cy + r, cx - r, cy - r)
        }
    }

    // terminal on the foot of the spine. 0..4 not 0..2, so bare wins about half the time
    when (ri(0, 4)) {
        0 -> pen.dot(sx, foot, 0.09f)
        1 -> pen.seg(sx - 0.13f, foot, sx + 0.13f, foot)
        2 -> pen.run(sx - 0.15f, foot + 0.16f, sx, foot, sx + 0.15f, foot + 0.16f)
        else -> {} // bare
    }

    // diacritic above the headline. sits between the two verticals if there are two,
    // otherwise straight over the spine
    if (chance(p.marks)) {
        val mx = if (hasSecond) (sx + far) / 2 else sx
        when (ri(0, 3)) {
            0 -> pen.dot(mx, -0.3f, 0.075f)
            1 -> {
                pen.dot(mx - 0.13f, -0.3f, 0.07f); pen.dot(mx + 0.13f, -0.3f, 0.07f)
            }
            2 -> pen.seg(mx - 0.16f, -0.36f, mx + 0.16f, -0.22f)
            else -> pen.run(mx - 0.15f, -0.24f, mx, -0.4f, mx + 0.15f, -0.24f)
        }
    }

    // and one below the baseline, but only if the spine didnt allready descend down there
    if (foot <= 1f && chance(p.marks * 0.55f)) {
        when (ri(0, 2)) {
            0 -> pen.dot(sx, 1.28f, 0.075f)
            1 -> pen.seg(sx, 1.14f, sx, 1.34f)
            else -> pen.run(sx - 0.14f, 1.16f, sx, 1.32f, sx + 0.14f, 1.16f)
        }
    }

    return pen.glyph(w)
}

// a word is just a run of letter indices. the bar over it gets drawn at set time, not here
private class Word(val letters: IntArray, val width: Float)

private fun makeWord(alphabet: List<Glyph>, pick: Zipf): Word {
    // two draws averaged so the lengths pile up in the middle instead of spreading flat.
    // poor mans normal distribution
    val n = (ri(p.wordMin, p.wordMax) + ri(p.wordMin, p.wordMax) + 1) / 2
    val letters = IntArray(n) { pick.next() }
    var w = 0f
    letters.forEachIndexed { i, l ->
        w += alphabet[l].width
        if (i < n - 1) w += p.track
    }
    return Word(letters, w)
}

// zipf-ish sampler. a handful of letters and words carry the whole page and the rest are
// rare, which is what real language does and what makes this look like language.
// linear scan over the cumulative table, n is tiny so who cares
private class Zipf(n: Int, s: Float) {
    private val cum = FloatArray(n)

    init {
        var t = 0f
        for (i in 0 until n) {
            t += 1f / (i + 1f).pow(s)
            cum[i] = t
        }
        for (i in cum.indices) cum[i] /= t // normalise
    }

    fun next(): Int {
        val u = rnd.nextFloat()
        for (i in cum.indices) if (u <= cum[i]) return i
        return cum.size - 1 // float slop, shouldnt really get here
    }
}

// ---- setting type -------------------------------------------------

// a size of the hand. nib is the pen width in device px and it grows with the em but not
// linearly, same as a real nib would, otherwise the big letters turn into slabs.
// 0.62 by eye - tried 1.0 first and the masthead looked like a woodcut
private class Ink(val em: Float, color: Int, private val alpha: Int = 255) {
    val nib = p.weight * (em / p.em).pow(0.62f)

    // glyph paths are in em units so the pen has to be divided back down
    val stroke: Paint = strokeOf(nib / em, color).apply {
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
        this.alpha = alpha
    }
    val fill: Paint = fillOf(color).apply { this.alpha = alpha }

    // ...but the word bar is set in device space, so it gets its own width. this bit me twice
    val bar: Paint = strokeOf(nib, color).apply {
        strokeCap = PaintStrokeCap.ROUND
        this.alpha = alpha
    }

    // how much ink the nib is carrying right now. re-dipped once per word
    fun flow(a: Int) {
        val v = a * alpha / 255
        stroke.alpha = v
        fill.alpha = v
        bar.alpha = v
    }
}

// one letter, headline sitting on `head`
private fun Canvas.glyph(gl: Glyph, x: Float, head: Float, ink: Ink, tilt: Float = 0f) {
    save()
    translate(x, head)
    if (tilt != 0f) rotate(tilt)
    scale(ink.em, ink.em)
    drawPath(gl.ink, ink.stroke)
    if (!gl.blobs.isEmpty) drawPath(gl.blobs, ink.fill)
    restore()
}

// sets one word, runs the bar over the top of it, returns the advance
private fun Canvas.word(word: Word, alphabet: List<Glyph>, x: Float, base: Float, ink: Ink): Float {
    ink.flow(ri(p.flow, 255)) // re-dip

    val head = base - ink.em
    drawLine(x, head, x + word.width * ink.em, head, ink.bar)

    var cx = x
    word.letters.forEach { l ->
        val gl = alphabet[l]
        // the jitter is the whole difference between "font" and "hand". tilt gets less of
        // it than the vertical does, a wonky baseline reads fine but wonky letters dont
        glyph(gl, cx, head + rf(-p.jitter, p.jitter), ink, rf(-p.jitter, p.jitter) * 0.7f)
        cx += (gl.width + p.track) * ink.em
    }
    return word.width * ink.em
}

// sentence break - pair of ticks hanging off where the bar would have been
private fun Canvas.mark(x: Float, base: Float, ink: Ink) {
    val e = ink.em
    drawLine(x, base - e * 0.75f, x, base - e * 0.15f, ink.bar)
    drawLine(x + e * 0.22f, base - e * 0.9f, x + e * 0.22f, base - e * 0.3f, ink.bar)
}

// the page ==========================================================

private fun render(g: Gartvas) {
    val c = g.canvas
    val d = g.d

    // build the language up front: glyphs -> words -> and then two samplers over both
    val alphabet = List(p.alphabet) { makeGlyph() }
    val pickLetter = Zipf(p.alphabet, p.zipfG)
    val lexicon = List(p.lexicon) { makeWord(alphabet, pickLetter) }
    val pickWord = Zipf(p.lexicon, p.zipfW)

    c.clear(p.paper)
    foxing(c, d)

    val body = Ink(p.em, p.ink)
    val red = Ink(p.em, p.red)

    val left = p.ml
    val right = d.wf - p.mr
    val bottom = d.hf - p.mb

    // ruling. scored before anything gets written on top, like a real ruled page
    val ruled = strokeOf(1.1f, p.red).apply { alpha = p.ruling }

    // masthead - few big words and a rule under them
    val titleInk = Ink(p.em * 1.85f, p.ink)
    var tx = left
    repeat(p.title) {
        val word = lexicon[pickWord.next()]
        tx += c.word(word, alphabet, tx, p.mt, titleInk) + p.space * titleInk.em * 0.8f
    }
    // the -16/+16 is the rules hanging out past the text block. shows up a few more times below
    c.drawLine(left - 16f, p.mt + 26f, right + 16f, p.mt + 26f, strokeOf(2.2f, p.ink).apply { alpha = 190 })

    // and now the body. paragraphs until the page runs out of room
    val baselines = mutableListOf<Float>()
    var base = p.mt + p.lead * 1.55f
    var line = 0
    var first = true

    while (base <= bottom) {
        // first para has to clear the seal, hence the coerce
        val lines = ri(p.paraMin, p.paraMax).coerceAtLeast(if (first) p.dropcap + 1 else 1)

        for (i in 0 until lines) {
            if (base > bottom) break

            // the drop cap pushes the opening lines over to the right
            val indent = if (first && line < p.dropcap) dropcapBox() + p.space * p.em else 0f
            var x = left + indent
            // last line of a para stops short somewhere random, everything else runs to the edge
            val target = if (i == lines - 1) left + indent + (right - left - indent) * rf(0.3f, 0.92f) else right

            if (p.ruling > 0) c.drawLine(left - 16f, base, right + 16f, base, ruled)

            // paragraphs after the first one open with a red pilcrow
            if (i == 0 && !first) {
                c.mark(x, base, red)
                x += p.em * 0.75f
            }

            // set words until we hit the target. `set` guards the first word so a line is
            // never completely empty, even if that word overshoots
            var set = 0
            while (true) {
                val word = lexicon[pickWord.next()]
                val ww = word.width * p.em
                if (set > 0 && x + ww > target) break
                // never rubricate the very first word, it just merges into the seal
                val ink = if (line > 0 && ri(1, p.rubric) == 1) red else body
                x += c.word(word, alphabet, x, base, ink)
                set++
                if (ri(1, p.punct) == 1 && x + p.em * 1.4f < right) { // no ticks hanging off the edge
                    c.mark(x + p.em * 0.2f, base, body)
                    x += p.em * 0.55f
                }
                x += p.space * p.em
                if (x > target) break
            }

            baselines += base // marginalia needs these later
            base += p.lead
            line++
        }

        base += p.lead * 0.55f // para gap
        first = false
    }

    // side rules, from the masthead down to wherever the text actually stopped. drawn after
    // the body because i only know where it stopped by now
    if (p.ruling > 0 && baselines.isNotEmpty()) {
        val top = p.mt - p.em * 1.9f
        val end = baselines.last() + p.lead * 0.45f
        c.drawLine(left - 16f, top, left - 16f, end, ruled)
        c.drawLine(right + 16f, top, right + 16f, end, ruled)
    }

    // seal gets the busiest letter in the alphabet, it has to carry that whole corner.
    // area of the bounds + a nudge for anything with a filled blob in it
    val ornate = alphabet.indices.maxBy {
        val b = alphabet[it].ink.bounds
        b.width * b.height + if (alphabet[it].blobs.isEmpty) 0f else 0.2f
    }
    dropcap(c, alphabet, ornate, left, p.mt + p.lead * 1.55f)
    marginalia(c, alphabet, lexicon, pickWord, d, baselines)

    // folio, down in the bottom outer corner
    val folio = Ink(p.em * 0.62f, p.ink, 165)
    val fw = lexicon[pickWord.next()]
    c.word(fw, alphabet, right - fw.width * folio.em, d.hf - p.mb + p.lead * 1.1f, folio)

    // and finally the aging pass over everything
    c.drawVignette(d, p.vignette, 0x2A1F12, radius = 0.95f, innerStop = 0.42f)
    if (p.grain > 0f) addGrain(g, p.grain, (p.seed and 0x7fffffff).toInt())
}

// the seal is square and this is its side. opening lines indent by exactly this much,
// which is why its a function and not just inlined in two places
private fun dropcapBox() = p.dropcap * p.lead * 0.78f

// the opening seal - one letter blown right up inside a broken box, in the red
private fun dropcap(c: Canvas, alphabet: List<Glyph>, letter: Int, left: Float, base: Float) {
    if (p.dropcap <= 0) return

    val box = dropcapBox()
    val l = left // just so the four corners below read as l/t/r/b
    val t = base - p.em - 12f
    val r = l + box
    val b = t + box

    // 0.56 so the marks and the descender still land inside the frame
    val gl = alphabet[letter]
    val em = box * 0.56f
    val ink = Ink(em, p.red)
    c.glyph(gl, l + (box - gl.width * em) / 2, t + box * 0.25f, ink)

    // the enclosure. four corners instead of a closed frame, a full box was way too heavy
    val f = strokeOf(ink.nib * 0.55f, p.red).apply {
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
        alpha = 210
    }
    val arm = box * 0.3f
    listOf(
        floatArrayOf(l, t + arm, l, t, l + arm, t),
        floatArrayOf(r - arm, t, r, t, r, t + arm),
        floatArrayOf(r, b - arm, r, b, r - arm, b),
        floatArrayOf(l + arm, b, l, b, l, b - arm),
    ).forEach { a ->
        val path = PathBuilder().moveTo(a[0], a[1]).lineTo(a[2], a[3]).lineTo(a[4], a[5]).detach()
        c.drawPath(path, f)
    }
    c.drawCircle(r, t, f.strokeWidth * 1.5f, fillOf(p.red).apply { alpha = 210 }) // pin, top right
}

// notes out in the wide margin. smaller, lighter, tied back to their line with a tick
private fun marginalia(
    c: Canvas,
    alphabet: List<Glyph>,
    lexicon: List<Word>,
    pick: Zipf,
    d: Dimension,
    baselines: List<Float>,
) {
    if (baselines.size < 6) return
    val ink = Ink(p.em * 0.55f, p.ink, 150)
    val x = d.wf - p.mr + 22f
    val used = mutableListOf<Float>()

    repeat(p.margins) {
        // keep the notes off each other. rejection sampling, a re-roll is way cheaper than
        // actually laying them out - and if 24 tries cant find a gap then just give up on
        // this one, theres no room for it anyway
        var at = baselines[ri(2, baselines.size - 3)]
        var tries = 0
        while (tries++ < 24 && used.any { abs(it - at) < p.lead * 2.8f }) {
            at = baselines[ri(2, baselines.size - 3)]
        }
        if (used.any { abs(it - at) < p.lead * 2.8f }) return@repeat
        used += at

        var cy = at
        repeat(ri(1, 2)) {
            val word = lexicon[pick.next()]
            c.word(word, alphabet, x, cy, ink)
            cy += p.lead * 0.62f
        }
        c.drawLine(x - 12f, at - ink.em * 0.5f, x - 3f, at - ink.em * 0.5f, ink.bar)
    }
}

// age spots. dead cheap but it stops the paper reading as a flat fill
private fun foxing(c: Canvas, d: Dimension) {
    repeat(p.foxing) {
        val r = rf(3f, 17f)
        c.drawCircle(
            rf(0f, d.wf), rf(0f, d.hf), r,
            fillOf(NipponColors.col050_MOMOSHIOCHA).apply { alpha = ri(6, 20) }, // barely there
        )
    }
}

// plumbing

private fun rf(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
private fun ri(a: Int, b: Int) = a + rnd.nextInt(b - a + 1) // inclusive both ends!
private fun chance(f: Float) = rnd.nextFloat() < f
private fun FloatArray.random(r: Random) = this[r.nextInt(size)] // stdlib has it for List but not this

private fun resolveParams(): Params {
    // the preset only hands over the *defaults* - paper/ink/red are still each overridable
    // on their own, which is how you find the next preset
    val press = PRESSES[pi("pal", 0).coerceIn(PRESSES.indices)]
    return Params(
        seed = pl("seed", 41L),
        out = ps("out", "glossa.png"),
        mt = pf("mt", 150f), mb = pf("mb", 168f), ml = pf("ml", 116f), mr = pf("mr", 196f),
        em = pf("em", 27f),
        lead = pf("lead", 62f),
        weight = pf("weight", 4.4f),
        track = pf("track", 0.2f),
        space = pf("space", 0.74f),
        alphabet = pi("alphabet", 30), // 30ish is the sweet spot, 12 reads as a code, 60 as noise
        lexicon = pi("lexicon", 130),
        zipfG = pf("zipfG", 0.6f), zipfW = pf("zipfW", 0.75f),
        wordMin = pi("wordMin", 1), wordMax = pi("wordMax", 5),
        descend = pf("descend", 0.2f),
        marks = pf("marks", 0.34f),
        bowls = pf("bowls", 0.32f),
        title = pi("title", 3),
        paraMin = pi("paraMin", 3), paraMax = pi("paraMax", 6),
        dropcap = pi("dropcap", 3),
        rubric = pi("rubric", 24),
        punct = pi("punct", 11),
        margins = pi("margins", 3),
        ruling = pi("ruling", 26),
        paper = pi("paper", press[0]),
        ink = pi("ink", press[1]),
        red = pi("red", press[2]),
        foxing = pi("foxing", 70),
        grain = pf("grain", 0.13f),
        vignette = pf("vignette", 0.22f),
        flow = pi("flow", 205),
        jitter = pf("jitter", 0.7f),
    )
}
