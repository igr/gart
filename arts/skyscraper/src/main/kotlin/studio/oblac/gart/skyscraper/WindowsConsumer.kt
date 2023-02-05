package studio.oblac.gart.skyscraper

import studio.oblac.gart.gfx.RectIsometric

interface WindowsConsumer {
	operator fun invoke(fn: (consumer: RectIsometric) -> Unit)
}
