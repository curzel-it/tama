package it.curzel.tama.midi

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class MidiComposerIos : NativeMidiComposer {
    private var audioEngine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var isCurrentlyPlaying = false
    private var shouldLoop = false
    private var audioBuffer: AVAudioPCMBuffer? = null

    override fun play(samples: FloatArray, sampleRate: Int, loop: Boolean) {
        stop()

        shouldLoop = loop
        isCurrentlyPlaying = true

        // Create audio format (mono, 16-bit PCM, signed integer)
        val audioFormat = AVAudioFormat(
            commonFormat = AVAudioPCMFormatInt16,
            sampleRate = sampleRate.toDouble(),
            channels = 1u,
            interleaved = true
        ) ?: run {
            println("iOS: Failed to create audio format")
            isCurrentlyPlaying = false
            return
        }

        // Create engine and player node
        val engine = AVAudioEngine()
        val player = AVAudioPlayerNode()

        audioEngine = engine
        playerNode = player

        // Attach player to engine
        engine.attachNode(player)

        // Connect player to main mixer
        engine.connect(
            player,
            engine.mainMixerNode,
            audioFormat
        )

        // Create audio buffer
        val buffer = AVAudioPCMBuffer(
            pCMFormat = audioFormat,
            frameCapacity = samples.size.toUInt()
        ) ?: run {
            println("iOS: Failed to create audio buffer")
            isCurrentlyPlaying = false
            return
        }

        buffer.frameLength = samples.size.toUInt()
        audioBuffer = buffer

        // Convert float samples to int16 and copy to buffer
        val channelData = buffer.int16ChannelData?.pointed?.value ?: run {
            println("iOS: Failed to get channel data")
            isCurrentlyPlaying = false
            return
        }

        for (i in samples.indices) {
            val sample = (samples[i] * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            channelData[i] = sample
        }

        // Start engine
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val started = engine.startAndReturnError(error.ptr)
            if (!started) {
                println("iOS: Failed to start audio engine: ${error.value?.localizedDescription}")
                isCurrentlyPlaying = false
                return
            }
        }

        // Schedule buffer with completion handler for looping
        scheduleBuffer()

        // Start player
        player.play()
    }

    private fun scheduleBuffer() {
        val buffer = audioBuffer ?: return
        val player = playerNode ?: return

        if (!isCurrentlyPlaying) return

        player.scheduleBuffer(buffer) {
            if (this.shouldLoop && this.isCurrentlyPlaying) {
                dispatch_async(dispatch_get_main_queue()) {
                    this.scheduleBuffer()
                }
            }
        }
    }

    override fun stop() {
        isCurrentlyPlaying = false
        playerNode?.stop()
        audioEngine?.stop()
        audioEngine = null
        playerNode = null
        audioBuffer = null
    }

    override fun isPlaying(): Boolean {
        return isCurrentlyPlaying
    }
}
