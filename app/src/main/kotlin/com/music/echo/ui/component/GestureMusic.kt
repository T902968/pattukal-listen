package echo.music.iad1tya.ui.component

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.GestureMusicEnabledKey
import echo.music.iad1tya.constants.GestureMusicGlitterKey
import echo.music.iad1tya.constants.GestureMusicManualSeenKey
import echo.music.iad1tya.constants.GestureMusicWriteKey
import echo.music.iad1tya.constants.GestureMusicWriteLangKey
import echo.music.iad1tya.constants.HalloweenChoiceKey
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.utils.rememberPreference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Gesture Music. Double-tap and HOLD on any screen, then draw with a glittering glitch trail:
 *  circle = Favorites, wave = Recently played, big diagonal = shuffle, heart = like the current song,
 *  or WRITE a song name (letters, one or many strokes) and the song is found and played.
 * Touches are only taken over after the hold is recognised, so scrolling and normal taps are untouched.
 */
object GestureMusicState {
  val strokes = mutableStateListOf<List<Offset>>()
  val currentPts = mutableStateListOf<Offset>()
  var active by mutableStateOf(false)
  var writing by mutableStateOf(false)
  var message by mutableStateOf<String?>(null)
  var fading by mutableStateOf(false)

  fun begin() { active = true; fading = false }
  fun reset() { strokes.clear(); currentPts.clear(); active = false; writing = false; fading = false }
}

private enum class Shape { Circle, Wave, Diagonal, Heart, Unknown }

private fun recognize(pts: List<Offset>): Pair<Shape, Boolean> {
  if (pts.size < 12) return Shape.Unknown to false
  val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }; val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
  val w = maxX - minX; val h = maxY - minY
  if (max(w, h) < 90f) return Shape.Unknown to false
  var len = 0f
  for (i in 1 until pts.size) len += (pts[i] - pts[i - 1]).getDistance()
  val gap = (pts.first() - pts.last()).getDistance()
  val diag = hypot(w, h)
  val straight = gap / len
  val closed = gap < diag * 0.35f
  var turns = 0
  var lastSign = 0
  for (i in 2 until pts.size step 2) {
    val dy = pts[i].y - pts[i - 2].y
    val sgn = if (dy > 3f) 1 else if (dy < -3f) -1 else 0
    if (sgn != 0 && lastSign != 0 && sgn != lastSign) turns++
    if (sgn != 0) lastSign = sgn
  }
  // heart: closed, a dip at the top centre between two lobes, and the lowest point near the centre
  if (closed && w > 160f && h > 160f && w / h in 0.75f..1.5f) {
    val cx = (minX + maxX) / 2f
    val centre = pts.filter { abs(it.x - cx) < w * 0.1f }
    val left = pts.filter { it.x < cx - w * 0.12f }
    val right = pts.filter { it.x > cx + w * 0.12f }
    if (centre.isNotEmpty() && left.isNotEmpty() && right.isNotEmpty()) {
      val topCentre = centre.minOf { it.y }
      val lobe = min(left.minOf { it.y }, right.minOf { it.y })
      val bottom = pts.maxByOrNull { it.y }!!
      if ((topCentre - lobe) / h > 0.07f && abs(bottom.x - cx) < w * 0.22f) return Shape.Heart to true
    }
  }
  return when {
    closed && w > 200f && h > 200f && w / h in 0.6f..1.7f && len > diag * 2.2f -> Shape.Circle to true
    straight > 0.93f && w > 250f && h > 250f && abs(w / h - 1f) < 0.4f -> Shape.Diagonal to true
    turns >= 3 && w > h * 1.4f && len > w * 1.5f -> Shape.Wave to (turns >= 4)
    else -> Shape.Unknown to false
  }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { c ->
  addOnSuccessListener { c.resume(it) }
  addOnFailureListener { c.resumeWithException(it) }
}

private fun langTag(code: String) = when (code) { "ml" -> "ml-IN"; "hi" -> "hi-IN"; "ta" -> "ta-IN"; "te" -> "te-IN"; "kn" -> "kn-IN"; else -> "en-US" }

/** On-device handwriting. The language model downloads once; after that it works offline. */
private suspend fun readHandwriting(strokes: List<List<Offset>>, code: String): String? {
  val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(langTag(code)) ?: return null
  val model = DigitalInkRecognitionModel.builder(id).build()
  val manager = RemoteModelManager.getInstance()
  if (!manager.isModelDownloaded(model).awaitTask()) manager.download(model, DownloadConditions.Builder().build()).awaitTask()
  val recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
  return try {
    val ink = Ink.builder()
    var t = System.currentTimeMillis()
    strokes.forEach { s ->
      val b = Ink.Stroke.builder()
      s.forEach { p -> b.addPoint(Ink.Point.create(p.x, p.y, t)); t += 8 }
      ink.addStroke(b.build())
      t += 120
    }
    recognizer.recognize(ink.build()).awaitTask().candidates.firstOrNull()?.text?.trim()
  } finally { runCatching { recognizer.close() } }
}

private suspend fun AwaitPointerEventScope.collectStroke(start: Offset, into: MutableList<Offset>) {
  into.add(start)
  GestureMusicState.currentPts.add(start)
  while (true) {
    val ev = awaitPointerEvent(PointerEventPass.Initial)
    ev.changes.forEach { it.consume() }
    val c = ev.changes.firstOrNull() ?: break
    if (!c.pressed) break
    into.add(c.position); GestureMusicState.currentPts.add(c.position)
  }
}

@Composable
fun Modifier.gestureMusic(navController: NavController): Modifier {
  val (enabled, _) = rememberPreference(GestureMusicEnabledKey, true)
  if (!enabled) return this
  val (writeOn, _) = rememberPreference(GestureMusicWriteKey, true)
  val (writeLang, _) = rememberPreference(GestureMusicWriteLangKey, "en")
  val connection = LocalPlayerConnection.current
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val st = GestureMusicState
  var timer by remember { mutableStateOf<Job?>(null) }

  fun say(text: String, endAfter: Long = 1400L) {
    st.message = text
    timer?.cancel()
    timer = scope.launch { delay(endAfter); st.fading = true; delay(650); st.reset(); st.message = null }
  }

  fun finishWriting() {
    val strokes = st.strokes.toList()
    st.writing = false
    scope.launch {
      st.message = "Reading your writing…"
      val text = runCatching { withContext(Dispatchers.Default) { readHandwriting(strokes, writeLang) } }.getOrElse { null }
      if (text.isNullOrBlank()) { say("Couldn't read that. Try writing bigger and clearer", 1800L); return@launch }
      st.message = "Finding “$text”…"
      val item = withContext(Dispatchers.IO) {
        YouTube.search(text, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().firstOrNull()
      }
      if (item == null) { say("No song found for “$text”", 1800L); return@launch }
      connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
      say("Playing ${item.title}", 1800L)
    }
  }

  fun onStroke(pts: List<Offset>) {
    st.strokes.add(pts.toList())
    st.currentPts.clear()
    if (st.strokes.size == 1) {
      val (shape, sure) = recognize(pts)
      if (sure) {
        when (shape) {
          Shape.Circle -> { say("Favorites"); navController.navigate("auto_playlist/liked") }
          Shape.Wave -> { say("Recently played"); navController.navigate("history") }
          Shape.Diagonal -> connection?.let { c -> c.player.shuffleModeEnabled = !c.player.shuffleModeEnabled; say(if (c.player.shuffleModeEnabled) "Shuffle on" else "Shuffle off") }
          Shape.Heart -> {
            val c = connection
            if (c?.mediaMetadata?.value == null) say("Nothing is playing")
            else scope.launch {
              val cur = c.currentSong.first()
              if (cur?.song?.liked == true) say("Already in Favorites ♥") else { c.toggleLike(); say("Liked ♥") }
            }
          }
          Shape.Unknown -> Unit
        }
        return
      }
    }
    if (!writeOn) { say("Not recognised. Try a circle, wave, diagonal or heart", 1600L); return }
    st.writing = true
    st.message = "Keep writing, or pause to play"
    timer?.cancel()
    timer = scope.launch { delay(1300); finishWriting() }
  }

  return this.pointerInput(connection, writeOn, writeLang) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
      val d1 = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
      if (st.writing) {
        // the writing session is open: this touch is another letter stroke
        d1.consume(); timer?.cancel()
        val pts = ArrayList<Offset>(96)
        collectStroke(d1.position, pts)
        onStroke(pts)
        return@awaitEachGesture
      }
      val up1 = withTimeoutOrNull(220) { waitForUpOrCancellation(PointerEventPass.Initial) } ?: return@awaitEachGesture
      if ((up1.position - d1.position).getDistance() > slop * 3f) return@awaitEachGesture
      val d2 = withTimeoutOrNull(280) { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial) } ?: return@awaitEachGesture
      if ((d2.position - d1.position).getDistance() > 80.dp.toPx()) return@awaitEachGesture
      val released = withTimeoutOrNull(360) {
        var stop = false
        while (!stop) {
          val ev = awaitPointerEvent(PointerEventPass.Initial)
          val c = ev.changes.firstOrNull() ?: break
          if (!c.pressed || (c.position - d2.position).getDistance() > slop * 2.5f) stop = true
        }
        true
      }
      if (released != null) return@awaitEachGesture // an ordinary double tap, leave it alone
      // held long enough: gesture mode is on
      timer?.cancel(); st.reset(); st.begin()
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      d2.consume()
      val pts = ArrayList<Offset>(128)
      collectStroke(d2.position, pts)
      onStroke(pts)
    }
  }
}

/** Glitter + glitch trail and the little status bubble. Sits above everything and never takes touches. */
@Composable
fun GestureMusicOverlay() {
  val st = GestureMusicState
  val (glitter, _) = rememberPreference(GestureMusicGlitterKey, true)
  if (!st.active && st.message == null) return
  val alpha = remember { Animatable(1f) }
  LaunchedEffect(st.fading, st.active) { if (st.fading) alpha.animateTo(0f, tween(600)) else alpha.snapTo(1f) }
  val t = rememberInfiniteTransition(label = "gm").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "gmt")
  Box(Modifier.fillMaxSize()) {
    Canvas(Modifier.fillMaxSize()) { drawTrail(alpha.value, t.value, glitter) }
    st.message?.let { m ->
      Box(Modifier.align(Alignment.TopCenter).padding(top = 90.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.72f)).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(m, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
      }
    }
  }
}

private val Cyan = Color(0xFF28F0FF)
private val Magenta = Color(0xFFFF2BD6)
private val Gold = Color(0xFFFFE082)

private fun DrawScope.drawTrail(alpha: Float, t: Float, glitter: Boolean) {
  val st = GestureMusicState
  val all = st.strokes.toList() + listOf(st.currentPts.toList())
  val jit = Random((t * 12f).toInt() + 3)
  all.forEach { pts ->
    if (pts.size < 2) return@forEach
    val path = Path().apply { moveTo(pts[0].x, pts[0].y); for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y) }
    val round = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path, Magenta.copy(alpha = 0.16f * alpha), style = Stroke(width = 26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val dx = 3f + jit.nextFloat() * 5f
    translate(left = -dx, top = jit.nextFloat() * 2f) { drawPath(path, Cyan.copy(alpha = 0.75f * alpha), style = round) }
    translate(left = dx, top = -jit.nextFloat() * 2f) { drawPath(path, Magenta.copy(alpha = 0.75f * alpha), style = round) }
    // glitch: a few horizontal slices of the trail jump sideways
    if (jit.nextFloat() > 0.35f) {
      val b = pts.minOf { it.y }; val e = pts.maxOf { it.y }
      repeat(2) {
        val y0 = b + jit.nextFloat() * max(1f, e - b)
        clipRect(top = y0, bottom = y0 + 10f + jit.nextFloat() * 16f) {
          translate(left = (jit.nextFloat() - 0.5f) * 46f) { drawPath(path, Color.White.copy(alpha = 0.85f * alpha), style = round) }
        }
      }
    }
    drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (glitter) {
      for (i in pts.indices step 2) {
        val r = Random(i * 7919 + pts.size / 40)
        val tw = abs(sin((t * 2f * PI.toFloat()) + i * 0.7f))
        val p = pts[i] + Offset((r.nextFloat() - 0.5f) * 30f, (r.nextFloat() - 0.5f) * 30f)
        val s = (2f + r.nextFloat() * 6f) * (0.4f + tw)
        val c = when (i % 3) { 0 -> Color.White; 1 -> Gold; else -> if (i % 2 == 0) Cyan else Magenta }.copy(alpha = (0.35f + 0.65f * tw) * alpha)
        val star = Path().apply { moveTo(p.x, p.y - s * 1.6f); lineTo(p.x + s * 0.32f, p.y - s * 0.32f); lineTo(p.x + s * 1.6f, p.y); lineTo(p.x + s * 0.32f, p.y + s * 0.32f); lineTo(p.x, p.y + s * 1.6f); lineTo(p.x - s * 0.32f, p.y + s * 0.32f); lineTo(p.x - s * 1.6f, p.y); lineTo(p.x - s * 0.32f, p.y - s * 0.32f); close() }
        drawPath(star, c)
      }
    }
  }
}

/** The how-to, shown the first time and any time from Settings. */
@Composable
fun GestureMusicManual(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(28.dp),
    title = { Text("✨ Gesture Music") },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Double tap, and keep your finger down on the second tap. You'll feel a buzz and a glittering trail will follow your finger.", style = MaterialTheme.typography.bodyMedium)
        Text("2. Draw:", fontWeight = FontWeight.Bold)
        listOf(
          "○  Big circle: open Favorites",
          "〰  Wave, left to right: Recently played",
          "╱  Big diagonal line: shuffle on / off",
          "♥  Heart: like the song that is playing",
          "Aa  Write a song name, one letter at a time. Pause for a moment and that song plays by itself",
        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text("Tips", fontWeight = FontWeight.Bold)
        Text("Start the double tap on an empty spot, not on a song. Draw big. Writing works with several strokes; the first time you write, a small handwriting model downloads, then it works offline. You can turn all of this off, or pick the writing language, in Settings > Gesture Music.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
  )
}

@Composable
fun GestureMusicFirstRun() {
  val (enabled, _) = rememberPreference(GestureMusicEnabledKey, true)
  var seen by rememberPreference(GestureMusicManualSeenKey, false)
  val (choice, _) = rememberPreference(HalloweenChoiceKey, "")
  var shown by remember { mutableStateOf(false) }
  if (!enabled || seen || choice.isEmpty() || shown) return
  GestureMusicManual(onDismiss = { shown = true; seen = true })
}
