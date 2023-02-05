package studio.oblac.gart.circledots

import studio.oblac.gart.Gartvas
import studio.oblac.gart.math.MathCos
import studio.oblac.gart.math.MathSin

data class Context(
	val g: Gartvas,
	val msin: MathSin = MathSin(),
	val mcos: MathCos = MathCos()
)
