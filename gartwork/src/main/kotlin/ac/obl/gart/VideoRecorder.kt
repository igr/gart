package ac.obl.gart

import ac.obl.gart.skia.Image
import io.humble.video.*
import io.humble.video.Codec.findEncodingCodec
import io.humble.video.Coder.Flag
import io.humble.video.awt.MediaPictureConverter
import io.humble.video.awt.MediaPictureConverterFactory
import io.humble.video.awt.MediaPictureConverterFactory.convertToType
import java.awt.image.BufferedImage

// https://stackoverflow.com/a/38015682/511837

data class VideoDefinition(
	val fileName: String,
	val width: Int,
	val height: Int,
	val framesPerSecond: Int = 25,
)

class VideoRecorder(internal val def: VideoDefinition) {
	private val codec: Codec
	internal val encoder: Encoder
	internal val picture: MediaPicture
	internal val packet: MediaPacket
	internal val muxer: Muxer
    internal val framesCount = FramesCount(def.framesPerSecond)

	init {
		val framerate: Rational = Rational.make(1, framesCount.rate())

		// first we create a muxer using the passed in filename and format name
		muxer = Muxer.make(def.fileName, null, def.fileName.substringAfterLast('.'))

		// Now, we need to decide what type of codec to use to encode video. Muxers
		// have limited sets of codecs they can use.
		val format = muxer.format
		codec = findEncodingCodec(format.defaultVideoCodecId)

		// Now that we know what codec, we need to create an encoder
		encoder = Encoder.make(codec)

		// Video encoders need to know at a minimum:
		//    width
		//    height
		//    pixel format
		encoder.width = def.width
		encoder.height = def.height
		// We are going to use 420P as the format because that's what most video formats these days use
		val pixelformat = PixelFormat.Type.PIX_FMT_YUV420P
		encoder.pixelFormat = pixelformat
		encoder.timeBase = framerate

		// An annoyance of some formats is that they need global (rather than per-stream) headers,
		// and in that case you have to tell the encoder. And since Encoders are decoupled from
		// Muxers, there is no easy way to know this beyond
		if (format.getFlag(ContainerFormat.Flag.GLOBAL_HEADER)) {
			encoder.setFlag(Flag.FLAG_GLOBAL_HEADER, true)
		}

		// Start

		encoder.open(null, null)
		muxer.addNewStream(encoder)
		muxer.open(null, null)

		// Next, we need to make sure we have the right MediaPicture format objects
		// to encode data with. Java (and most on-screen graphics programs) use some
		// variant of Red-Green-Blue image encoding (a.k.a. RGB or BGR). Most video
		// codecs use some variant of YCrCb formatting. So we're going to have to
		// convert. To do that, we'll introduce a MediaPictureConverter object later.

		picture = MediaPicture.make(encoder.width, encoder.height, pixelformat)
		picture.timeBase = framerate

		// the main loop
		packet = MediaPacket.make()
	}

	/**
	 * Starts with the recordings.
	 */
	fun start(imageProvider: () -> Image): Video {
        println("Video recoding started")
		return Video(this, imageProvider)
	}
}

/**
 * Represents a running video, that is being recorded.
 */
class Video(private val vcr: VideoRecorder, private val imageProvider: () -> Image) {

	private var timestamp: Long = 0
	private var converter: MediaPictureConverter? = null
    val frames: Frames = vcr.framesCount
	/**
	 * Adds frame to the movie.
	 */
	fun addFrame() {
        vcr.framesCount.tick()
		val image = imageProvider()

		// convert image to TYPE_3BYTE_BGR
		val screen = convertToType(image.toBufferedImage(), BufferedImage.TYPE_3BYTE_BGR)

		if (converter == null) {
			converter = MediaPictureConverterFactory.createConverter(screen, vcr.picture)
		}
		converter!!.toPicture(vcr.picture, screen, timestamp)

		do {
			vcr.encoder.encode(vcr.packet, vcr.picture)
			if (vcr.packet.isComplete) {
				vcr.muxer.write(vcr.packet, false)
			}
		} while (vcr.packet.isComplete)

		timestamp++
	}

	/**
	 * Returns true while video is saving.
	 */
	var running = true
		private set

	fun save() {
        if (!running) {
            return
        }
		running = false
		// Encoders, like decoders, sometimes cache pictures, so it can do the right key-frame optimizations.
		// So, they need to be flushed as well. As with the decoders, the convention is to pass in a null
		// input until the output is not complete.
		do {
			vcr.encoder.encode(vcr.packet, null)
			if (vcr.packet.isComplete) {
				vcr.muxer.write(vcr.packet, false)
			}
		} while (vcr.packet.isComplete)

		// done
		vcr.muxer.close()

		println("Video saved: ${vcr.def.fileName}")
	}
}
