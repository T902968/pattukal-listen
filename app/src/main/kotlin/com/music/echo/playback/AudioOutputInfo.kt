package echo.music.iad1tya.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the device audio output was actually opened with, as reported by Media3 when the
 * AudioTrack is created. Nothing here is estimated; before any track has played it is null.
 * The idea of showing the real output format is credited to Auris / BitChord (GPL-3.0).
 */
object AudioOutputInfo {
  private val _text = MutableStateFlow<String?>(null)
  val text: StateFlow<String?> = _text

  fun describe(encoding: Int, sampleRate: Int, offload: Boolean, tunneling: Boolean): String {
    val enc =
      when (encoding) {
        C.ENCODING_PCM_16BIT -> "16-bit PCM"
        C.ENCODING_PCM_24BIT -> "24-bit PCM"
        C.ENCODING_PCM_32BIT -> "32-bit PCM"
        C.ENCODING_PCM_FLOAT -> "32-bit float"
        else -> "compressed stream (encoding $encoding)"
      }
    return buildString {
      append("AudioTrack · ${"%.1f".format(sampleRate / 1000.0)} kHz · $enc")
      if (offload) append(" · offload")
      if (tunneling) append(" · tunneling")
    }
  }

  @UnstableApi
  val listener =
    object : AnalyticsListener {
      override fun onAudioTrackInitialized(
        eventTime: AnalyticsListener.EventTime,
        audioTrackConfig: AudioSink.AudioTrackConfig,
      ) {
        _text.value =
          describe(
            audioTrackConfig.encoding,
            audioTrackConfig.sampleRate,
            audioTrackConfig.offload,
            audioTrackConfig.tunneling,
          )
      }
    }
}
