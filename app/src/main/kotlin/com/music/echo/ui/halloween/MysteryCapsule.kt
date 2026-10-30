package echo.music.iad1tya.ui.halloween

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.MusicLanguageOption
import echo.music.iad1tya.constants.MusicLanguageOptions
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A real, playable track. Built only from the app's catalog (Quick Picks or a YouTube Music search). */
class CapsuleTrack(val id: String, val title: String, val artist: String, val thumb: String?, val meta: MediaMetadata)

private fun Song.toCapsule() = CapsuleTrack(id, song.title, artists.joinToString { it.name }, song.thumbnailUrl, toMediaMetadata())
private fun SongItem.toCapsule() = CapsuleTrack(id, title, artists.joinToString { it.name }, thumbnail, toMediaMetadata())

private enum class CapState { Idle, Opening, Revealed, Empty, Offline }

/**
 * Mystery Capsule, "TRICK OR TRACK". Opens to a real song from the catalog. Play uses the existing playback engine,
 * Save uses the existing Favorites, Another reveals a different track. Taps are ignored while it is opening.
 * Works with the Halloween theme off (it then uses the app's own accent colour).
 */
@Composable
fun MysteryCapsuleCard(quickPicks: List<Song>, openSignal: Int, modifier: Modifier = Modifier) {
  val hw = rememberHalloweenOn()
  val fx = rememberHwFx()
  val visible = rememberAppVisible()
  val conn = LocalPlayerConnection.current
  val database = LocalDatabase.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val accent = if (hw) HW.Orange else MaterialTheme.colorScheme.primary
  val glow = if (hw) HW.Violet else MaterialTheme.colorScheme.tertiary

  var state by remember { mutableStateOf(CapState.Idle) }
  var lang by remember { mutableStateOf<MusicLanguageOption?>(null) }
  var track by remember { mutableStateOf<CapsuleTrack?>(null) }
  var saved by remember { mutableStateOf(false) }
  val shown = remember { mutableSetOf<String>() }
  val pools = remember { mutableMapOf<String, List<CapsuleTrack>?>() }
  val open = remember { Animatable(0f) }
  var lastPlay by remember { mutableStateOf(0L) }
  var lastSignal by remember { mutableIntStateOf(openSignal) }

  val infinite = rememberInfiniteTransition(label = "capsule")
  val spin by infinite.animateFloat(-9f, 9f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse), label = "spin")
  val pulse by infinite.animateFloat(0.4f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
  val idleMotion = visible && !fx.reduced && state == CapState.Idle

  suspend fun poolFor(l: MusicLanguageOption?): List<CapsuleTrack>? {
    val key = l?.code ?: "all"
    if (pools.containsKey(key) && pools[key]?.isNotEmpty() == true) return pools[key]
    val list =
      if (l == null && quickPicks.isNotEmpty()) quickPicks.distinctBy { it.id }.map { it.toCapsule() }
      else
        withContext(Dispatchers.IO) {
          YouTube.search(l?.query ?: "popular songs", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty()
            .filterIsInstance<SongItem>().map { it.toCapsule() }
        }
    pools[key] = list
    return list
  }

  fun reveal() {
    if (state == CapState.Opening) return // no duplicate taps / overlapping animations
    if (!HauntedStage.enter(StagePhase.CapsuleOpening)) return
    state = CapState.Opening
    saved = false
    scope.launch {
      try {
        open.snapTo(0f)
        if (fx.sound) HauntedSound.chime(context)
        val loading = async { runCatching { poolFor(lang) }.getOrNull() }
        open.animateTo(1f, tween(if (fx.reduced) 250 else 1000, easing = LinearEasing))
        val pool = loading.await()
        val fresh = pool?.filter { it.id !in shown }.orEmpty()
        val pick = (fresh.ifEmpty { pool.orEmpty().also { shown.clear() } }).randomOrNull()
        if (pick == null) {
          state = if (pool == null) CapState.Offline else CapState.Empty
        } else {
          shown += pick.id
          track = pick
          state = CapState.Revealed
        }
      } finally {
        HauntedStage.release(StagePhase.CapsuleOpening)
        if (state == CapState.Opening) state = CapState.Idle
      }
    }
  }

  LaunchedEffect(openSignal) {
    if (openSignal != lastSignal) { lastSignal = openSignal; reveal() }
  }

  val shape = RoundedCornerShape(28.dp)
  Column(
    modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(shape)
      .background(Brush.linearGradient(listOf(Color.Black.copy(alpha = if (hw) 0.55f else 0.25f), glow.copy(alpha = 0.14f))))
      .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), glow.copy(alpha = 0.5f))), shape)
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("TRICK OR TRACK", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
      Text("Mystery Capsule", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      val opts = listOf<MusicLanguageOption?>(null) + MusicLanguageOptions.filter { it.code in setOf("ml", "ta", "hi", "en") }
      opts.forEach { o ->
        val sel = lang?.code == o?.code
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(if (sel) accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f))
            .clickable(enabled = state != CapState.Opening) { lang = o; if (state != CapState.Opening) { state = CapState.Idle } }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) { Text(o?.label ?: "All languages", color = if (sel) Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) }
      }
    }

    when (state) {
      CapState.Idle, CapState.Opening -> {
        val o = open.value
        Box(
          Modifier.fillMaxWidth().height(150.dp)
            .semantics { contentDescription = "Mystery capsule. Double tap to open and reveal a song." }
            .clickable(enabled = state == CapState.Idle) { reveal() },
          contentAlignment = Alignment.Center,
        ) {
          Canvas(Modifier.size(150.dp)) {
            val c = center
            val tremble = if (state == CapState.Opening && !fx.reduced) sin(o * 60f) * 3f * (1f - o) else 0f
            val ang = (if (idleMotion) spin else 0f) + o * 180f + tremble
            val len = size.width * 0.62f
            val thick = size.width * 0.36f
            val sep = o * size.width * 0.16f
            rotate(ang, c) {
              // inner glow between the shells
              drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f + 0.4f * (if (state == CapState.Opening) o else pulse)), Color.Transparent), c, size.width * 0.5f), size.width * 0.5f, c)
              // two shells separating along the seam
              val half = len / 2f
              drawRoundRect(Color(0xFF0E0B14), Offset(c.x - half - sep, c.y - thick / 2f), Size(half, thick), CornerRadius(thick / 2f))
              drawRoundRect(Color(0xFF0E0B14), Offset(c.x + sep, c.y - thick / 2f), Size(half, thick), CornerRadius(thick / 2f))
              drawRoundRect(glow.copy(alpha = 0.5f), Offset(c.x - half - sep, c.y - thick / 2f), Size(half, thick), CornerRadius(thick / 2f), style = Stroke(2f))
              drawRoundRect(glow.copy(alpha = 0.5f), Offset(c.x + sep, c.y - thick / 2f), Size(half, thick), CornerRadius(thick / 2f), style = Stroke(2f))
              drawLine(accent.copy(alpha = 0.55f + 0.45f * o), Offset(c.x, c.y - thick / 2f), Offset(c.x, c.y + thick / 2f), strokeWidth = 3f + 5f * o)
            }
            // particles leaking from the seam, bursting when it opens
            val count = (14 * fx.particleScale).toInt()
            for (i in 0 until count) {
              val r = Random(i * 17)
              val a = r.nextFloat() * 2f * PI.toFloat()
              val d = (size.width * 0.1f + r.nextFloat() * size.width * 0.34f) * (0.2f + o)
              drawCircle(accent.copy(alpha = (0.2f + 0.6f * (1f - o) * pulse).coerceIn(0f, 1f)), 2f + r.nextFloat() * 2f, Offset(c.x + cos(a) * d, c.y + sin(a) * d))
            }
          }
        }
        Text(
          if (state == CapState.Opening) "Something stirs…" else "Tap the capsule",
          color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
      }
      CapState.Revealed -> {
        val t = track
        if (t != null) {
          AnimatedVisibility(true, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 6 }) {
            Column(Modifier.padding(top = 12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AsyncImage(
                  model = t.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                  modifier = Modifier.size(96.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                )
                Column(Modifier.weight(1f)) {
                  Text(t.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                  Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
              Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CapButton("Play", accent, Color.Black) {
                  val now = System.currentTimeMillis()
                  if (now - lastPlay > 1500) { lastPlay = now; conn?.playQueue(YouTubeQueue.radio(t.meta)) }
                }
                CapButton(if (saved) "Saved" else "Save", Color.White.copy(alpha = 0.12f), MaterialTheme.colorScheme.onSurface) {
                  if (!saved) {
                    saved = true
                    scope.launch(Dispatchers.IO) {
                      val ex = database.song(t.id).first()
                      if (ex == null) database.query { insert(t.meta) { it.copy(liked = true, likedDate = LocalDateTime.now(), inLibrary = LocalDateTime.now()) } }
                      else if (!ex.song.liked) database.query { update(ex.song.toggleLike()) }
                    }
                  }
                }
                CapButton("Another", Color.White.copy(alpha = 0.12f), MaterialTheme.colorScheme.onSurface) { state = CapState.Idle; reveal() }
              }
            }
          }
        }
      }
      CapState.Empty, CapState.Offline -> {
        Text(
          if (state == CapState.Offline) "The capsule can't reach the catalog right now. Check your connection and try again." else "No songs found for this language yet. Try another.",
          color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp),
        )
        Row(Modifier.padding(top = 10.dp)) { CapButton("Try again", accent, Color.Black) { state = CapState.Idle; reveal() } }
      }
    }
  }
}

@Composable
private fun CapButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
  Box(
    Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) { Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}
