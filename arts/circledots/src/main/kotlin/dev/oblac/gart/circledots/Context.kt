package dev.oblac.gart.circledots

import dev.oblac.gart.Gartvas
import dev.oblac.gart.math.MathCos
import dev.oblac.gart.math.MathSin

data class Context(
	val g: Gartvas,
	val msin: MathSin = MathSin(),
	val mcos: MathCos = MathCos()
)
