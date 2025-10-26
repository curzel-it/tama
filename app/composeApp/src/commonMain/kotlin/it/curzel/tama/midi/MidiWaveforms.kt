package it.curzel.tama.midi

import kotlin.math.PI
import kotlin.math.sin

object MidiWaveforms {
    private const val SAMPLE_RATE = 48000

    fun generateWaveformSamples(
        frequency: Double,
        duration: Double,
        volume: Float,
        waveform: Waveform,
        adsr: Boolean = false,
        vibrato: Boolean = false
    ): FloatArray {
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val amplitude = 0.2f * volume
        val samples = FloatArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (vibrato) applyVibrato(t, frequency) else frequency
            val envelope = if (adsr) applyAdsrEnvelope(i, numSamples) else 1.0f

            val sample = when (waveform) {
                Waveform.SQUARE -> generateSquareWave(t, freq, amplitude)
                Waveform.TRIANGLE -> generateTriangleWave(t, freq, amplitude)
                Waveform.SAWTOOTH -> generateSawtoothWave(t, freq, amplitude)
                Waveform.PULSE -> generatePulseWave(t, freq, amplitude)
            }
            samples[i] = sample * envelope
        }

        return samples
    }

    private fun applyAdsrEnvelope(sampleIndex: Int, totalSamples: Int): Float {
        val t = sampleIndex.toFloat() / totalSamples

        val attackTime = 0.05f
        val decayTime = 0.15f
        val sustainLevel = 0.7f
        val releaseTime = 0.20f

        return when {
            t < attackTime -> t / attackTime
            t < attackTime + decayTime -> {
                val decayProgress = (t - attackTime) / decayTime
                1.0f - (1.0f - sustainLevel) * decayProgress
            }
            t < 1.0f - releaseTime -> sustainLevel
            else -> {
                val releaseProgress = (t - (1.0f - releaseTime)) / releaseTime
                sustainLevel * (1.0f - releaseProgress)
            }
        }
    }

    private fun applyVibrato(t: Double, frequency: Double): Double {
        val vibratoRate = 5.0
        val vibratoDepth = 0.02
        val vibratoOffset = sin(t * vibratoRate * 2.0 * PI) * vibratoDepth
        return frequency * (1.0 + vibratoOffset)
    }

    private fun generateSquareWave(t: Double, frequency: Double, amplitude: Float): Float {
        val wave = sin(t * frequency * 2.0 * PI)
        return if (wave >= 0) amplitude else -amplitude
    }

    private fun generateTriangleWave(t: Double, frequency: Double, amplitude: Float): Float {
        val phase = (t * frequency) % 1.0
        val wave = if (phase < 0.5) {
            4.0 * phase - 1.0
        } else {
            3.0 - 4.0 * phase
        }
        return (wave * amplitude).toFloat()
    }

    private fun generateSawtoothWave(t: Double, frequency: Double, amplitude: Float): Float {
        val phase = (t * frequency) % 1.0
        val wave = 2.0 * phase - 1.0
        return (wave * amplitude).toFloat()
    }

    private fun generatePulseWave(t: Double, frequency: Double, amplitude: Float): Float {
        val dutyCycle = 0.25
        val phase = (t * frequency) % 1.0
        val wave = if (phase < dutyCycle) 1.0 else -1.0
        return (wave * amplitude).toFloat()
    }
}
