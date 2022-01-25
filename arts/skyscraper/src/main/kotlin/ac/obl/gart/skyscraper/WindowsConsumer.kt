package ac.obl.gart.skyscraper

import ac.obl.gart.gfx.RectIsometric

interface WindowsConsumer {
	operator fun invoke(fn: (consumer: RectIsometric) -> Unit)
}