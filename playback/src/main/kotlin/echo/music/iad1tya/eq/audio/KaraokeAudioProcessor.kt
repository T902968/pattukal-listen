package echo.music.iad1tya.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow

/** Global on/off switch for the instrumental (karaoke) mode, read by every player's processor. */
object KaraokeEngine {
  val enabled = MutableStateFlow(false)
}

/**
 * Vocal remover for karaoke. Lead vocals sit in the centre of a stereo mix, so the centre (mid)
 * component is cancelled and only the side signal is kept. The low end is also centred, so the mid
 * signal is low-passed (~150 Hz) and added back to keep the bass and kick drum, giving a full-bodied
 * instrumental. Stereo 16-bit PCM only; anything else is left untouched.
 */
@UnstableApi
class KaraokeAudioProcessor : BaseAudioProcessor() {
  private var lpState = 0f
  private var alpha = 0.02f

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
      return AudioProcessor.AudioFormat.NOT_SET
    }
    val x = (2.0 * Math.PI * 150.0 / inputAudioFormat.sampleRate).toFloat()
    alpha = x / (x + 1f)
    lpState = 0f
    return inputAudioFormat
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    val size = inputBuffer.remaining()
    if (size == 0) return
    val out = replaceOutputBuffer(size)
    if (!KaraokeEngine.enabled.value) {
      out.put(inputBuffer)
      out.flip()
      return
    }
    var lp = lpState
    while (inputBuffer.remaining() >= 4) {
      val l = inputBuffer.getShort().toInt()
      val r = inputBuffer.getShort().toInt()
      val mid = (l + r) * 0.5f
      val side = (l - r) * 0.5f
      lp += alpha * (mid - lp)
      var o = side * 1.7f + lp * 1.1f
      if (o > 32767f) o = 32767f else if (o < -32768f) o = -32768f
      val s = o.toInt().toShort()
      out.putShort(s)
      out.putShort(s)
    }
    lpState = lp
    out.flip()
  }

  override fun onReset() {
    lpState = 0f
  }
}
