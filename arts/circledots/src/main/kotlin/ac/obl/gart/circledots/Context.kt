package ac.obl.gart.circledots

import ac.obl.gart.Gartvas
import ac.obl.gart.math.MathCos
import ac.obl.gart.math.MathSin

data class Context(
	val g: Gartvas,
	val msin: MathSin = MathSin(),
	val mcos: MathCos = MathCos()
)
