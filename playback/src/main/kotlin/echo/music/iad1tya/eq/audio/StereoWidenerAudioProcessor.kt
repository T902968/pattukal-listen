package echo.music.iad1tya.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mid-side stereo widener ("spatial audio" toggle). Decomposes each frame into a mono mid component
 * and a side (difference) component, then boosts the side component before recombining — this
 * pushes the L/R channels further apart without changing the mono-compatible center image, unlike a
 * naive per-channel gain change.
 *
 * Mono and non-16-bit-PCM sources are passed through unchanged: widening a mono signal is
 * meaningless (there is no side component), and the mid/side math below assumes 16-bit samples.
 */
@UnstableApi
class StereoWidenerAudioProcessor : AudioProcessor {

  /** 1 = unchanged, >1 widens. Clamped low enough that recombined peaks won't clip often. */
  @Volatile
  var width: Float = 1f
    set(value) {
      field = value.coerceIn(1f, 2f)
    }

  /**
   * Karaoke mode: reduces centre-panned content (where lead vocals normally sit) by keeping the stereo
   * side signal and only the low bass of the mid signal. A real-time approximation, not studio stem
   * separation: songs with wide-stereo vocals or reverb keep some voice.
   */
  @Volatile var vocalRemoval: Boolean = false

  // Two cascaded one-pole filters per band edge (12 dB/octave). Coefficients are recomputed per sample rate.
  private var bassA = 0.03
  private var bassState = 0.0
  private var bassState2 = 0.0
  private var airA = 0.45
  private var airState = 0.0
  private var airState2 = 0.0

  // Song analyzer: how much of the song sits in the centre (where vocals are) versus the stereo sides.
  private var midEnergy = 0.0
  private var sideEnergy = 0.0
  private var voiceEnergy = 0.0
  @Volatile private var analyzedSamples = 0L

  /** Clears the analysis (call when karaoke turns on or the track changes). */
  fun resetAnalysis() {
    midEnergy = 0.0; sideEnergy = 0.0; voiceEnergy = 0.0; analyzedSamples = 0L
  }

  /** Short human text about how well vocals can be removed from the audio analysed so far, or null if too little audio. */
  fun analysisSummary(): String? {
    if (analyzedSamples < 44100L * 3) return null
    val total = midEnergy + sideEnergy
    if (total <= 0.0) return null
    val sideRatio = sideEnergy / total
    val voiceShare = voiceEnergy / total
    return when {
      sideRatio < 0.02 -> "Karaoke: this song is almost mono, so the voice can't be separated"
      voiceShare > 0.30 && sideRatio > 0.08 -> "Karaoke: clean centre vocal found, strong removal"
      voiceShare > 0.15 -> "Karaoke: vocals mostly removed, a little may remain"
      else -> "Karaoke: vocals are spread wide or mixed with reverb, partial removal only"
    }
  }

  private var channelCount = 0
  private var encoding = C.ENCODING_INVALID
  private var isActive = false

  private var outputBuffer: ByteBuffer = EMPTY_BUFFER
  private var inputEnded = false

  companion object {
    private val EMPTY_BUFFER: ByteBuffer =
      ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
  }

  override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    channelCount = inputAudioFormat.channelCount
    encoding = inputAudioFormat.encoding
    val rate = inputAudioFormat.sampleRate.coerceAtLeast(8000).toDouble()
    bassA = 1.0 - Math.exp(-2.0 * Math.PI * 130.0 / rate) // ~130 Hz
    airA = 1.0 - Math.exp(-2.0 * Math.PI * 9500.0 / rate) // ~9.5 kHz

    if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
      throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
    }

    isActive = true
    return inputAudioFormat
  }

  // Always active once configured for stereo PCM16 — at width=1 the mid/side
  // decomposition recombines losslessly, so there's no separate bypass path needed, and
  // toggling stays glitch-free since Media3 evaluates isActive() only around configure(),
  // not per buffer.
  override fun isActive(): Boolean = isActive

  override fun queueInput(inputBuffer: ByteBuffer) {
    val inputSize = inputBuffer.remaining()
    if (inputSize == 0) return

    if (outputBuffer.capacity() < inputSize) {
      outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
    } else {
      outputBuffer.clear()
    }

    val widthNow = width
    val karaoke = vocalRemoval
    val sampleCount = inputSize / 2
    when (channelCount) {
      2 ->
        repeat(sampleCount / 2) {
          val left = inputBuffer.getShort().toInt()
          val right = inputBuffer.getShort().toInt()

          var mid = (left + right) * 0.5
          var side = (left - right) * 0.5 * widthNow

          if (karaoke) {
            // Split the centre signal into bass (<~130 Hz), the voice band and air (>~9.5 kHz). The whole voice
            // band is removed; bass and air stay so the instrumental keeps its punch and shimmer. Stereo sides
            // (guitars, pads, reverb) are lifted a little to fill the gap.
            bassState += bassA * (mid - bassState)
            bassState2 += bassA * (bassState - bassState2)
            airState += airA * (mid - airState)
            airState2 += airA * (airState - airState2)
            val voice = airState2 - bassState2
            val air = mid - airState2
            midEnergy += mid * mid
            sideEnergy += side * side
            voiceEnergy += voice * voice
            analyzedSamples++
            mid = bassState2 + air * 0.9
            side *= 1.5
          }

          var outLeft = mid + side
          var outRight = mid - side

          // Boosting the side channel can push a hard-panned peak past full scale
          // (width=1.4 on left=32767/right=-32768 overshoots by ~40%). Clamping each
          // channel independently at that point would distort the stereo image
          // asymmetrically; scaling both channels down together by the same factor
          // preserves the balance the widening introduced while staying in range.
          val peak = maxOf(kotlin.math.abs(outLeft), kotlin.math.abs(outRight))
          if (peak > 32767.0) {
            val scale = 32767.0 / peak
            outLeft *= scale
            outRight *= scale
          }

          outputBuffer.putShort(outLeft.toInt().toShort())
          outputBuffer.putShort(outRight.toInt().toShort())
        }
      // Mono has no side component to widen — pass through unchanged.
      else -> repeat(sampleCount) { outputBuffer.putShort(inputBuffer.getShort()) }
    }

    outputBuffer.flip()
  }

  override fun getOutput(): ByteBuffer {
    val buffer = outputBuffer
    outputBuffer = EMPTY_BUFFER
    return buffer
  }

  override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

  @Deprecated("Deprecated in Java")
  override fun flush() {
    resetAnalysis()
    outputBuffer = EMPTY_BUFFER
    inputEnded = false
  }

  override fun reset() {
    @Suppress("DEPRECATION") flush()
    channelCount = 0
    encoding = C.ENCODING_INVALID
    isActive = false
  }

  override fun queueEndOfStream() {
    inputEnded = true
  }
}
