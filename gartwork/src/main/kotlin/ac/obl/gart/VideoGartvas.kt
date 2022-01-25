package ac.obl.gart

/**
 * Function that creates a video recording of provided gartvas.
 */
class VideoGartvas(private val g: Gartvas) {
	fun start(fileName: String, framesPerSecond: Int = 25): Video {
		return VideoDefinition(fileName, g.w, g.h, framesPerSecond)
			.let { VideoRecorder(it) }
			.start { g.snapshot() }
	}
}