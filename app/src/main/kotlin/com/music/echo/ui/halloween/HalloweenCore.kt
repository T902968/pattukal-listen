package echo.music.iad1tya.ui.halloween

import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import echo.music.iad1tya.constants.HalloweenAmbientSoundKey
import echo.music.iad1tya.constants.HalloweenEditionKey
import echo.music.iad1tya.constants.HalloweenGesturesKey
import echo.music.iad1tya.constants.HalloweenReducedMotionKey
import echo.music.iad1tya.ui.immersive.rememberMotionSettings
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Halloween palette: burnt orange, ghostly violet, moonlit silver, dark crimson on AMOLED black. */
object HW {
  val Orange = Color(0xFFFF7A1A)
  val Violet = Color(0xFF9B5CFF)
  val Silver = Color(0xFFD9DEEA)
  val Crimson = Color(0xFF9E1B32)
  val Ink = Color(0xFF07060C)
}

/** One coordinated state machine for every major animation. Only one full-screen transition runs at a time. */
enum class StagePhase { Idle, GestureRecognition, CapsuleOpening, RealityTear, ParticleFormation, PlayerArrival, TimeFoldExploration, Cancelled }

object HauntedStage {
  var phase by mutableStateOf(StagePhase.Idle)
    private set

  private fun isTear(p: StagePhase) = p == StagePhase.RealityTear || p == StagePhase.ParticleFormation || p == StagePhase.PlayerArrival

  /** The portal transition always wins and supersedes anything else. Inline features wait while it runs. */
  fun enter(p: StagePhase): Boolean {
    if (!isTear(p) && (isTear(phase) || phase == StagePhase.CapsuleOpening && p == StagePhase.CapsuleOpening)) return false
    phase = p
    return true
  }

  fun set(p: StagePhase) { phase = p }

  /** Releases the stage only if [expected] still owns it. */
  fun release(expected: StagePhase) {
    if (phase == expected || isTear(expected) && isTear(phase)) phase = StagePhase.Idle
  }

  fun cancel() { phase = StagePhase.Cancelled; phase = StagePhase.Idle }
}

class HwFx(val on: Boolean, val reduced: Boolean, val lowEnd: Boolean, val sound: Boolean, val gestures: Boolean) {
  /** 0 = no particles, 1 = full. */
  val particleScale: Float get() = if (reduced) 0f else if (lowEnd) 0.35f else 1f
}

@Composable
fun rememberHalloweenOn(): Boolean {
  val (on, _) = rememberPreference(HalloweenEditionKey, true)
  return on
}

@Composable
fun rememberHwFx(): HwFx {
  val context = LocalContext.current
  val (on, _) = rememberPreference(HalloweenEditionKey, true)
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val (sound, _) = rememberPreference(HalloweenAmbientSoundKey, false)
  val (gestures, _) = rememberPreference(HalloweenGesturesKey, true)
  val motion = rememberMotionSettings()
  val lowRam =
    remember {
      runCatching { (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice }.getOrDefault(false)
    }
  return remember(on, reducedPref, sound, gestures, motion, lowRam) {
    HwFx(on, reducedPref || motion.reduced, lowRam || motion.battery, sound, gestures)
  }
}

/** True while the app is on screen. Decorative loops stop when this is false. */
@Composable
fun rememberAppVisible(): Boolean {
  val owner = LocalLifecycleOwner.current
  var visible by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
  DisposableEffect(owner) {
    val o = LifecycleEventObserver { _, _ -> visible = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
    owner.lifecycle.addObserver(o)
    onDispose { owner.lifecycle.removeObserver(o) }
  }
  return visible
}

/** Frame clock in nanoseconds. Read `.value` inside draw lambdas so only drawing is invalidated. */
@Composable
fun rememberFrameTime(active: Boolean, gapNanos: Long = 33_000_000L): State<Long> {
  val t = remember { mutableLongStateOf(0L) }
  LaunchedEffect(active) {
    if (!active) return@LaunchedEffect
    var last = 0L
    while (true) {
      withFrameNanos { n ->
        if (n - last >= gapNanos) {
          last = n
          t.longValue = n
        }
      }
    }
  }
  return t
}

fun ease(x: Float): Float { val c = x.coerceIn(0f, 1f); return c * c * (3f - 2f * c) }
fun seg(p: Float, a: Float, b: Float): Float = ((p - a) / (b - a)).coerceIn(0f, 1f)

/**
 * Optional sound effects, generated in code so no copyrighted audio is bundled. Quiet, short, and skipped
 * when the phone is muted or the music volume is zero. Never plays unless the user enabled ambient effects.
 */
object HauntedSound {
  private const val RATE = 22050

  private fun allowed(context: Context): Boolean {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    return am.ringerMode == AudioManager.RINGER_MODE_NORMAL && am.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
  }

  private fun play(context: Context, pcm: ShortArray, volume: Float) {
    if (!allowed(context)) return
    runCatching {
      val track =
        AudioTrack.Builder()
          .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
          .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
          .setBufferSizeInBytes(pcm.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      track.write(pcm, 0, pcm.size)
      track.setVolume(volume)
      track.setNotificationMarkerPosition(pcm.size - 1)
      track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(t: AudioTrack?) { runCatching { t?.release() } }
        override fun onPeriodicNotification(t: AudioTrack?) {}
      })
      track.play()
    }
  }

  /** Soft three-note chime for the Mystery Capsule. */
  fun chime(context: Context) {
    val n = (RATE * 0.9f).toInt()
    val pcm = ShortArray(n) { i ->
      val t = i / RATE.toFloat()
      val env = exp(-3.2f * t)
      val s = sin(2 * PI * 880 * t) + 0.6 * sin(2 * PI * 1320 * t) + 0.35 * sin(2 * PI * 1760 * t)
      (s * env * 0.28 * Short.MAX_VALUE / 2).toInt().toShort()
    }
    play(context, pcm, 0.35f)
  }

  /** Low, short whoosh for the portal. */
  fun portal(context: Context) {
    val n = (RATE * 0.7f).toInt()
    val pcm = ShortArray(n) { i ->
      val t = i / RATE.toFloat()
      val f = 140 + 420 * t
      val env = (sin(PI * (t / 0.7f)) * 0.9).toFloat()
      val s = sin(2 * PI * f * t) * 0.7 + sin(2 * PI * f * 1.51 * t) * 0.3
      (s * env * 0.25 * Short.MAX_VALUE).toInt().toShort()
    }
    play(context, pcm, 0.3f)
  }
}
