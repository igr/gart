package studio.oblac.gart

/**
 * Function that creates a video recording of provided gartvas.
 */
class VideoGartvas(private val g: Gartvas) {
	fun start(fileName: String, framesPerSecond: Int = 25): Video {
		return VideoDefinition(fileName, g.box.w, g.box.h, framesPerSecond)
			.let { VideoRecorder(it) }
			.start { g.snapshot() }
	}
}
