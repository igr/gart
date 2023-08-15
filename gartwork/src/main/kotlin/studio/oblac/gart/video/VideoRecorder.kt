package studio.oblac.gart.video

import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVIOContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swscale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer

/**
 * Video recorder.
 */
class VideoRecorder(width: Int,
                    height: Int,
                    framesPerSecond: Int = 25) {

    private val frame: AVFrame
    private val rgbFrame: AVFrame
    private val swsCtx: SwsContext
    private val codecContext: AVCodecContext
    private val formatContext: AVFormatContext
    private val packet: AVPacket
    private val stream: AVStream
    private val codec: AVCodec
    private var frameCount: Long = 0

    init {
        formatContext = AVFormatContext()
        avformat_alloc_output_context2(formatContext, null, "mp4", null).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to allocate format context: ${av_err2str(err)}")
            }
        }

        stream = avformat_new_stream(formatContext, null).also { stream ->
            if (stream.isNull) {
                throw RuntimeException("Failed to allocate stream")
            }
        }

        codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_H264).also { codec ->
            if (codec.isNull) {
                throw RuntimeException("Failed to find encoder")
            }
        }
        codecContext = avcodec.avcodec_alloc_context3(codec).also { codecContext ->
            if (codecContext.isNull) {
                throw RuntimeException("Failed to allocate codec context")
            }
        }

        packet = avcodec.av_packet_alloc().also { packet ->
            if (packet.isNull) {
                throw RuntimeException("Failed to allocate packet")
            }
        }

        codecContext.width(width)
        codecContext.height(height)
        val targetFormat = AV_PIX_FMT_YUV420P
        codecContext.pix_fmt(targetFormat)

        codecContext.time_base(AVRational())
        codecContext.time_base().num(1)
        codecContext.time_base().den(framesPerSecond)
        codecContext.framerate(AVRational())
        codecContext.framerate().num(framesPerSecond)
        codecContext.framerate().den(1)

        // Some formats want stream headers to be separate.
        if ((formatContext.oformat().flags() and AVFMT_GLOBALHEADER) != 0) {
            codecContext.flags(codecContext.flags() or avcodec.AV_CODEC_FLAG_GLOBAL_HEADER)
        }

        val codecOptions = AVDictionary()
        // mpg4 preset
        //av_dict_set(codecOptions, "preset", "icon", 0)
        // h264 presets: ultrafast superfast veryfast faster fast medium slow slower veryslow placebo
        av_dict_set(codecOptions, "preset", "slow", 0)

        // Open codec
        avcodec.avcodec_open2(codecContext, codec, codecOptions).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to open codec: ${av_err2str(err)}")
            }
        }

        val codecpar = stream.codecpar()
        avcodec.avcodec_parameters_from_context(codecpar, codecContext).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to copy codec parameters: ${av_err2str(err)}")
            }
        }
        stream.codecpar(codecpar)

        stream.time_base(AVRational())
        stream.time_base().num(1)
        stream.time_base().den(framesPerSecond)


        // Frame used to push data into the codec
        frame = av_frame_alloc().also { frame ->
            if (frame.isNull) {
                throw RuntimeException("Failed to allocate frame")
            }
        }
        frame.format(codecContext.pix_fmt())
        frame.width(codecContext.width())
        frame.height(codecContext.height())
        av_frame_get_buffer(frame, 0).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to allocate picture: ${av_err2str(err)}")
            }
        }

        // Input frame in RGBA format, we copy the rendered images to this for
        // conversion to the pixel format used by the codec
        rgbFrame = av_frame_alloc().also { rgbFrame ->
            if (rgbFrame.isNull) {
                throw RuntimeException("Failed to allocate frame")
            }
        }
        rgbFrame.format(AV_PIX_FMT_RGBA)
        rgbFrame.width(codecContext.width())
        rgbFrame.height(codecContext.height())
        av_frame_get_buffer(rgbFrame, 0)

        // We use in-memory dynamic buffers
        val pb = AVIOContext(null)
        avio_open_dyn_buf(pb).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to open dynamic buffer: ${av_err2str(err)}")
            }
        }
        formatContext.pb(pb)

        // Conversion context for RGBA->YUV
        swsCtx = swscale.sws_getContext(
            rgbFrame.width(), rgbFrame.height(), rgbFrame.format(),
            frame.width(), frame.height(), frame.format(),
            swscale.SWS_FAST_BILINEAR, null, null, null as DoublePointer?
        )
            .also { swsCtx ->
                if (swsCtx.isNull) {
                    throw RuntimeException("Failed to allocate sws context")
                }
            }

        // Write header
        val formatOptions = AVDictionary()
        // Infinite loop
        av_dict_set_int(formatOptions, "loop", 0, 0)
        avformat_write_header(formatContext, formatOptions).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to write header: ${av_err2str(err)}")
            }
        }
    }

    private fun av_err2str(err: Int): String {
        val buffer = ByteArray(256)
        if (av_strerror(err, buffer, buffer.size.toLong()) < 0) {
            return "Unknown error $err"
        }
        return buffer.toString(Charsets.UTF_8)
    }

    /**
     * Adds the video frame defined as a byte array of RGB pixels.
     */
    fun writeFrame(pixels: ByteArray) {
        av_frame_make_writable(frame)
        val data = rgbFrame.data(0)
        for ((ndx, p) in pixels.withIndex()) {
            data.put(ndx.toLong(), p)
        }

        // Convert from in-memory pixel format to format required by codec
        swscale.sws_scale(swsCtx,
            rgbFrame.data(), rgbFrame.linesize(), 0,
            frame.height(), frame.data(), frame.linesize()
        )
        frame.pts(frameCount)
        encode(formatContext, codecContext, packet, stream, frame)

        frameCount++
    }

    fun finish(): ByteArray {
        encode(formatContext, codecContext, packet, stream, null)

        // Write the end of the file
        av_write_trailer(formatContext)

        // Get the current output buffer
        val pb = BytePointer()
        val pbSize = avio_get_dyn_buf(formatContext.pb(), pb)
        val data = ByteArray(pbSize)
        pb[data]
        return data
    }

    fun close() {
        avcodec.avcodec_free_context(codecContext)
        av_frame_free(frame)
        av_frame_free(rgbFrame)
        codec.close()
        if (!formatContext.pb().isNull) {
            val pb = BytePointer()
            avio_close_dyn_buf(formatContext.pb(), pb)
            av_free(pb)
        }
        avformat_free_context(formatContext)
    }

    private fun encode(formatContext: AVFormatContext, codecContext: AVCodecContext, packet: AVPacket, stream: AVStream, frame: AVFrame?) {
        // Send frame to codec
        avcodec.avcodec_send_frame(codecContext, frame).let { err ->
            if (err < 0) {
                throw RuntimeException("Failed to send frame to codec: ${av_err2str(err)}")
            }
        }

        // Get the output packet, if any (codec may buffer frames)
        while (true) {
            val err = avcodec.avcodec_receive_packet(codecContext, packet)
            if (err == AVERROR_EAGAIN() || err == AVERROR_EOF) {
                return
            }
            if (err < 0) {
                throw RuntimeException("Failed to receive packet from codec: ${av_err2str(err)}")
            }

            // Rescale output packet timestamp values from codec to stream timebase
            avcodec.av_packet_rescale_ts(packet, codecContext.time_base(), stream.time_base())
            packet.stream_index(stream.index())

            // Write packet
            av_interleaved_write_frame(formatContext, packet).let {
                if (it < 0) {
                    throw RuntimeException("Failed to write packet: ${av_err2str(it)}")
                }
            }

            avcodec.av_packet_unref(packet)
        }
    }
}
