package dev.oblac.gart.skyscraper

import dev.oblac.gart.gfx.RectIsometric

interface WindowsConsumer {
	operator fun invoke(fn: (consumer: RectIsometric) -> Unit)
}
