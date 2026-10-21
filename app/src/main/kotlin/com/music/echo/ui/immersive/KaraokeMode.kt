package echo.music.iad1tya.ui.immersive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.eq.audio.KaraokeEngine
import echo.music.iad1tya.ui.player.InlineLyricsView
import kotlinx.coroutines.delay

/** Karaoke state: turning it on removes the vocals (instrumental) and opens the lyrics stage. */
object KaraokeState {
  var visible by mutableStateOf(false)
  fun open() {
    KaraokeEngine.enabled.value = true
    visible = true
  }
  fun close() {
    KaraokeEngine.enabled.value = false
    visible = false
  }
}

private class KLine(val timeMs: Long, val text: String)

private val lrcStamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
private val wordStamp = Regex("<\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?>")

private fun parseLrc(raw: String?): List<KLine> {
  if (raw.isNullOrBlank()) return emptyList()
  val out = ArrayList<KLine>()
  for (line in raw.lines()) {
    val stamps = lrcStamp.findAll(line).toList()
    if (stamps.isEmpty()) continue
    val text = wordStamp.replace(lrcStamp.replace(line, ""), "").trim()
    if (text.isEmpty()) continue
    for (m in stamps) {
      val min = m.groupValues[1].toLong()
      val sec = m.groupValues[2].toLong()
      val frac = m.groupValues[3]
      val ms = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
      out.add(KLine((min * 60 + sec) * 1000 + ms, text))
    }
  }
  return out.sortedBy { it.timeMs }
}

private val vowelFix =
  mapOf(
    'ā' to "aa", 'ī' to "ee", 'ū' to "oo", 'ē' to "e", 'ō' to "o", 'ṭ' to "t", 'ḍ' to "d",
    'ṇ' to "n", 'ṅ' to "ng", 'ñ' to "ny", 'ṟ' to "r", 'ṛ' to "ru", 'ḷ' to "l", 'ḻ' to "zh",
    'ś' to "sh", 'ṣ' to "sh", 'ṃ' to "m", 'ṁ' to "m", 'ḥ' to "h", 'ṉ' to "n", 'ṝ' to "ru",
  )

/** Writes lyrics of any Indian script (Malayalam, Hindi, Tamil, Telugu...) in plain English letters. */
private fun romanize(text: String): String =
  try {
    val t = android.icu.text.Transliterator.getInstance("Any-Latin; NFC")
    val latin = t.transliterate(text)
    val sb = StringBuilder()
    for (ch in latin.lowercase()) {
      val rep = vowelFix[ch]
      if (rep != null) sb.append(rep) else sb.append(ch)
    }
    val ascii = android.icu.text.Transliterator.getInstance("Latin-ASCII").transliterate(sb.toString())
    ascii.replace(Regex("\\s+"), " ").trim()
  } catch (_: Throwable) {
    text
  }

private fun clock(ms: Long): String {
  val s = (ms / 1000).coerceAtLeast(0)
  return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun KaraokeOverlay() {
  AnimatedVisibility(
    visible = KaraokeState.visible,
    enter = fadeIn(tween(380)) + scaleIn(initialScale = 0.94f, animationSpec = tween(420, easing = FastOutSlowInEasing)),
    exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.96f, animationSpec = tween(260)),
  ) {
    KaraokeContent()
  }
}

@Composable
private fun KaraokeContent() {
  val playerConnection = LocalPlayerConnection.current
  if (playerConnection == null) {
    KaraokeState.close()
    return
  }
  BackHandler { KaraokeState.close() }

  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val isPlaying by playerConnection.isPlaying.collectAsState()
  val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
  val instrumental by KaraokeEngine.enabled.collectAsState()
  val player = playerConnection.player
  var romanized by remember { mutableStateOf(true) }
  var position by remember { mutableLongStateOf(player.currentPosition) }
  var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
  var scrubbing by remember { mutableStateOf(false) }
  var scrubValue by remember { mutableStateOf(0f) }
  LaunchedEffect(player) {
    while (true) {
      if (!scrubbing) {
        position = player.currentPosition
        duration = player.duration.coerceAtLeast(0L)
      }
      delay(if (isPlaying) 80 else 400)
    }
  }

  val lines = remember(currentLyrics) { parseLrc(currentLyrics?.lyrics) }
  val shown = remember(lines, romanized) { lines.map { if (romanized) romanize(it.text) else it.text } }
  val activeIndex by derivedStateOf {
    var idx = -1
    for (i in lines.indices) if (lines[i].timeMs <= position + 120) idx = i else break
    idx
  }
  val palette = rememberAlbumPalette(mediaMetadata?.thumbnailUrl)
  val listState = rememberLazyListState()
  LaunchedEffect(activeIndex) {
    if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
  }
  val white = Color.White

  Box(
    Modifier.fillMaxSize()
      .background(Color.Black)
      .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
  ) {
    Crossfade(targetState = mediaMetadata?.thumbnailUrl, animationSpec = tween(900), label = "karaokeBg") { url ->
      Box(Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }) {
        if (url != null) {
          AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(80.dp))
        } else {
          Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.a, palette.c))))
        }
      }
    }
    Box(
      Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.85f)))
      )
    )
    // keeps lyrics fetching (and saving) working exactly like the normal player does
    Box(Modifier.size(1.dp).alpha(0f)) {
      InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = true, positionProvider = { position })
    }

    Column(
      Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          Modifier.size(44.dp).clip(CircleShape).background(white.copy(alpha = 0.14f)).clickable { KaraokeState.close() },
          contentAlignment = Alignment.Center,
        ) {
          Icon(painterResource(R.drawable.close), contentDescription = "Close karaoke", tint = white, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("KARAOKE", color = white.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
          Text(
            mediaMetadata?.title ?: "",
            color = white, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
          )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(44.dp))
      }

      Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("Instrumental", instrumental) { KaraokeEngine.enabled.value = !instrumental }
        Pill("English letters", romanized) { romanized = !romanized }
      }

      Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        if (lines.isEmpty()) {
          Text(
            if (currentLyrics == null) "Finding lyrics…" else "Synced lyrics aren't available for this song",
            color = white.copy(alpha = 0.7f), fontSize = 16.sp, textAlign = TextAlign.Center,
          )
        } else {
          LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Spacer(Modifier.height(60.dp)) }
            itemsIndexed(shown) { i, text ->
              val active = i == activeIndex
              val dist = kotlin.math.abs(i - activeIndex)
              val a by animateFloatAsState(if (active) 1f else (0.5f - dist * 0.1f).coerceAtLeast(0.18f), tween(350), label = "ka")
              val sc by animateFloatAsState(if (active) 1f else 0.9f, tween(350), label = "ks")
              // sweep progress through the active line
              val next = lines.getOrNull(i + 1)?.timeMs ?: (lines[i].timeMs + 4000)
              val prog = if (active) ((position - lines[i].timeMs).toFloat() / (next - lines[i].timeMs).coerceAtLeast(1)).coerceIn(0f, 1f) else 0f
              val style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, textAlign = TextAlign.Center)
              Box(
                Modifier.fillMaxWidth().graphicsLayer { alpha = a; scaleX = sc; scaleY = sc }
                  .clickable { player.seekTo(lines[i].timeMs) },
                contentAlignment = Alignment.Center,
              ) {
                if (active) {
                  Text(
                    text,
                    style = style.copy(
                      brush = Brush.horizontalGradient(
                        0f to palette.b.copy(alpha = 1f),
                        (prog).coerceIn(0.001f, 0.999f) to Color.White,
                        (prog + 0.02f).coerceIn(0.002f, 1f) to Color.White.copy(alpha = 0.45f),
                        1f to Color.White.copy(alpha = 0.45f),
                      )
                    ),
                  )
                } else {
                  Text(text, style = style, color = white)
                }
              }
            }
            item { Spacer(Modifier.height(120.dp)) }
          }
        }
      }

      val dur = duration.coerceAtLeast(1L)
      Slider(
        value = if (scrubbing) scrubValue else (position.toFloat() / dur).coerceIn(0f, 1f),
        onValueChange = { scrubbing = true; scrubValue = it },
        onValueChangeFinished = { player.seekTo((scrubValue * dur).toLong()); position = (scrubValue * dur).toLong(); scrubbing = false },
      )
      Row(Modifier.fillMaxWidth()) {
        Text(clock(position), color = white.copy(alpha = 0.7f), fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(clock(duration), color = white.copy(alpha = 0.7f), fontSize = 12.sp)
      }
      Spacer(Modifier.height(10.dp))
      Box(
        Modifier.size(64.dp).clip(CircleShape).background(white).clickable { if (isPlaying) player.pause() else player.play() }.align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
      ) {
        Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = Color.Black, modifier = Modifier.size(30.dp))
      }
      Spacer(Modifier.height(18.dp))
    }
  }
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
  Box(
    Modifier.clip(RoundedCornerShape(50))
      .background(if (on) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = if (on) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}
