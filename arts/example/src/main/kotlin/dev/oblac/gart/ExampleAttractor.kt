package dev.oblac.gart

import dev.oblac.gart.attractor.*
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.drawPoints
import dev.oblac.gart.gfx.fromCenter
import dev.oblac.gart.gfx.strokeOfBlack
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("ExampleAttractor", 600, 600, 30)
    println(gart.name)

    val w = gart.window()
    var p = lorenzAttractor(gart.d)

    w.show { c, _, _ ->
        c.clear(Colors.white)
        c.drawPoints(p, strokeOfBlack(1f))
    }.onKey { k ->
        p = when (k) {
            Key.KEY_1 -> lorenzAttractor(gart.d)
            Key.KEY_2 -> thomasAttractor(gart.d)
            Key.KEY_3 -> aizawaAttractor(gart.d)
            Key.KEY_4 -> dadrasAttractor(gart.d)
            Key.KEY_5 -> chenAttractor(gart.d)
            Key.KEY_6 -> lorenz84Attractor(gart.d)
            Key.KEY_7 -> rosslerAttractor(gart.d)
            Key.KEY_8 -> halvorsenAttractor(gart.d)
            Key.KEY_9 -> rabinovichFabrikantAttractor(gart.d)
            Key.KEY_0 -> threeScrollUnifiedChaoticAttractor(gart.d)
            Key.KEY_Q -> sprottAttractor(gart.d)
            Key.KEY_W -> fourWingAttractor(gart.d)
            Key.KEY_E -> fourWingAttractor(gart.d)
            Key.KEY_R -> cliffordAttractor(gart.d)
            Key.KEY_T -> peterDeJongAttractor(gart.d)
            Key.KEY_Y -> duffingAttractor(gart.d)
            Key.KEY_U -> symmetricIconAttractor(gart.d)
            Key.KEY_I -> quadraticAttractor(gart.d)
            Key.KEY_O -> qubicAttractor(gart.d)
            else -> p
        }
        println(p.size)
    }


}

private fun lorenzAttractor(d: Dimension): List<Point> =
    LorenzAttractor().computeN(LorenzAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(d, 10f) }
        .map { it.offset(0f, -250f) }

private fun thomasAttractor(d: Dimension): List<Point> =
    ThomasAttractor().computeN(ThomasAttractor.initialPoint, 0.01f, 50000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(-100f, -180f) }

private fun aizawaAttractor(d: Dimension) =
    LangfordAizawaAttractor().computeN(LangfordAizawaAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(d, 150f) }
        .map { it.offset(0f, -100f) }

private fun dadrasAttractor(d: Dimension) =
    DadrasAttractor().computeN(DadrasAttractor.initialPoint, 0.01f, 50000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 20f) }
        .map { it.offset(0f, -50f) }

private fun chenAttractor(d: Dimension) =
    ChenAttractor().computeN(ChenAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.y) }
        .filter { it.x.isFinite() && it.y.isFinite() }
        .map { it.fromCenter(d, 10f) }
        .map { it.offset(0f, -100f) }

private fun lorenz84Attractor(d: Dimension) =
    Lorenz84Attractor().computeN(Lorenz84Attractor.initialPoint, 0.005f, 50000)
        .map { Point(it.z, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(0f, -50f) }

private fun rosslerAttractor(d: Dimension) =
    RosslerAttractor().computeN(RosslerAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(d, 10f) }
        .map { it.offset(0f, -50f) }

private fun halvorsenAttractor(d: Dimension) =
    HalvorsenAttractor().computeN(HalvorsenAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.z) }
        .map { it.fromCenter(d, 20f) }
        .map { it.offset(50f, -0f) }

private fun rabinovichFabrikantAttractor(d: Dimension) =
    RabinovichFabrikantAttractor().computeN(RabinovichFabrikantAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(0f, -50f) }

private fun threeScrollUnifiedChaoticAttractor(d: Dimension) =
    ThreeScrollUnifiedChaoticAttractor().computeN(ThreeScrollUnifiedChaoticAttractor.initialPoint, 0.0001f, 20000)
        .map { Point(it.x, it.y) }
        .filter { it.x.isFinite() && it.y.isFinite() }
        .map { it.fromCenter(d, 2f) }
        .map { it.offset(0f, -50f) }

private fun sprottAttractor(d: Dimension) =
    SprottAttractor().computeN(SprottAttractor.initialPoint, 0.01f, 10000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 200f) }
        .map { it.offset(-100f, -50f) }

private fun fourWingAttractor(d: Dimension) =
    FourWingAttractor().computeN(FourWingAttractor.initialPoint, 0.01f, 30000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(0f, -50f) }

private fun cliffordAttractor(d: Dimension) =
    CliffordAttractor().computeN(CliffordAttractor.initialPoint, 0.01f, 50000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(0f, -50f) }

private fun peterDeJongAttractor(d: Dimension) =
    PeterDeJongAttractor().computeN(PeterDeJongAttractor.initialPoint, 0.01f, 50000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(0f, -50f) }

private fun duffingAttractor(d: Dimension) =
    DuffingAttractor().computeN(DuffingAttractor.initialPoint, 0.01f, 100000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 200f) }
        .map { it.offset(0f, -50f) }

private fun symmetricIconAttractor(d: Dimension) =
    SymmetricIconAttractor().computeN(SymmetricIconAttractor.initialPoint, 0.01f, 100_000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 200f) }
        .map { it.offset(0f, -50f) }

private fun quadraticAttractor(d: Dimension) =
    QuadraticAttractor.ONE.computeN(QuadraticAttractor.initialPoint, 0.001f, 50_000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 100f) }
        .map { it.offset(100f, -50f) }

private fun qubicAttractor(d: Dimension) =
    CubicAttractor.ONE.computeN(CubicAttractor.initialPoint, 0.001f, 50_000)
        .map { Point(it.x, it.y) }
        .map { it.fromCenter(d, 200f) }
        .map { it.offset(100f, -50f) }
