package echo.music.iad1tya.ui.halloween

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.HalloweenEditionKey
import echo.music.iad1tya.constants.MusicLanguagesKey
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.parseMusicLanguages
import echo.music.iad1tya.ui.screens.Screens
import echo.music.iad1tya.utils.rememberPreference
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val accentOrange @Composable get() = if (rememberHalloweenOn()) HW.Orange else MaterialTheme.colorScheme.primary

// ---------------------------------------------------------------------------------------------------------------
// Doors: every cube destination as a visible button (also the accessible alternative to gestures)
// ---------------------------------------------------------------------------------------------------------------

@Composable
fun HauntedDoors(navController: NavController, onCapsule: () -> Unit, onTimeFold: () -> Unit, onCollections: () -> Unit) {
  val (_, setEdition) = rememberPreference(HalloweenEditionKey, true)
  data class Door(val label: String, val go: () -> Unit)
  val doors =
    listOf(
      Door("Haunted Discover") { navController.navigate("mood_and_genres") },
      Door("Midnight Favorites") { navController.navigate("auto_playlist/liked") },
      Door("Mystery Capsule", onCapsule),
      Door("Recent Shadows") { navController.navigate("history") },
      Door("Time Fold", onTimeFold),
      Door("Normal Library") { navController.navigate(Screens.Library.route) },
      Door("Original theme") { setEdition(false) },
    )
  Column(Modifier.padding(top = 4.dp)) {
    Text("Doors", color = HW.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      doors.forEach { d ->
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Brush.linearGradient(listOf(HW.Orange.copy(alpha = 0.8f), HW.Violet.copy(alpha = 0.8f))), RoundedCornerShape(50))
            .clickable(onClick = d.go)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text(d.label, color = Color.White, fontSize = 13.sp) }
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Haunted gestures: a dedicated rune pad, so nothing fights scrolling, carousels, back or seeking
// ---------------------------------------------------------------------------------------------------------------

enum class Rune(val label: String) {
  Favorites("Open Favorites"),
  Recents("Show Recently Played"),
  Shuffle("Shuffle"),
  Discover("Open Discover"),
}

class Recognition(val rune: Rune?, val confidence: Float)

/** Clear thresholds: a gesture only runs at >= 0.8 confidence; 0.5 to 0.8 shows a suggestion to confirm. */
fun recognise(pts: List<Offset>, dp: Float): Recognition {
  if (pts.size < 8) return Recognition(null, 0f)
  var len = 0f
  for (i in 1 until pts.size) len += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
  val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
  val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
  val bw = maxX - minX; val bh = maxY - minY
  if (len < 70f * dp) return Recognition(null, 0f)
  val first = pts.first(); val last = pts.last()
  val chord = hypot(last.x - first.x, last.y - first.y)
  val scored = mutableListOf<Recognition>()

  // circle: winds around its centre almost fully and ends near where it started
  val cx = pts.map { it.x }.average().toFloat(); val cy = pts.map { it.y }.average().toFloat()
  var wind = 0f
  for (i in 1 until pts.size) {
    var d = atan2(pts[i].y - cy, pts[i].x - cx) - atan2(pts[i - 1].y - cy, pts[i - 1].x - cx)
    while (d > PI) d -= 2 * PI.toFloat(); while (d < -PI) d += 2 * PI.toFloat()
    wind += d
  }
  val rs = pts.map { hypot(it.x - cx, it.y - cy) }
  val meanR = rs.average().toFloat()
  val sd = sqrt(rs.map { (it - meanR) * (it - meanR) }.average()).toFloat()
  val round = (1f - (sd / max(meanR, 1f)) * 2.5f).coerceIn(0f, 1f)
  val closed = (1f - chord / max(max(bw, bh), 1f)).coerceIn(0f, 1f)
  if (abs(wind) > 4.6f && bw > 40f * dp && bh > 40f * dp) scored += Recognition(Rune.Favorites, (abs(wind) / (2 * PI.toFloat())).coerceAtMost(1f) * (0.4f + 0.6f * round) * (0.5f + 0.5f * closed))

  // wave: mostly one horizontal direction with several up/down reversals
  var rev = 0; var dir = 0; var ref = pts[0].y
  for (p in pts) {
    val dy = p.y - ref
    if (abs(dy) > 10f * dp) { val nd = if (dy > 0) 1 else -1; if (dir != 0 && nd != dir) rev++; dir = nd; ref = p.y }
  }
  val forward = pts.zipWithNext().count { (a, b) -> (b.x - a.x) * (last.x - first.x) >= 0 }.toFloat() / (pts.size - 1)
  if (rev >= 3 && bw > 110f * dp && bw > bh * 1.3f) scored += Recognition(Rune.Recents, (rev / 4f).coerceAtMost(1f) * forward)

  // straight strokes
  val straight = if (chord > 0f) len / chord else 99f
  if (straight < 1.3f && chord > 90f * dp) {
    val ang = Math.toDegrees(atan2(last.y - first.y, last.x - first.x).toDouble()).toFloat()
    val a = abs(ang)
    val straightScore = (1f - (straight - 1f) * 2.5f).coerceIn(0f, 1f)
    if ((a in 25f..65f) || (a in 115f..155f)) scored += Recognition(Rune.Shuffle, straightScore)
    if (ang in -110f..-70f) scored += Recognition(Rune.Discover, straightScore)
  }
  return scored.maxByOrNull { it.confidence } ?: Recognition(null, 0f)
}

@Composable
fun HauntedGesturePad(navController: NavController, onDiscover: () -> Unit) {
  val fx = rememberHwFx()
  if (!fx.gestures) return
  val conn = LocalPlayerConnection.current
  val dp = LocalDensity.current.density
  val trail = remember { mutableStateListOf<Offset>() }
  var suggestion by remember { mutableStateOf<Rune?>(null) }
  var hint by remember { mutableStateOf<String?>(null) }
  var fade by remember { mutableStateOf(0f) }
  val scope = rememberCoroutineScope()
  val currentSong by (conn?.currentSong?.collectAsState(initial = null) ?: remember { mutableStateOf(null) })

  fun run(r: Rune) {
    suggestion = null
    when (r) {
      Rune.Favorites -> navController.navigate("auto_playlist/liked")
      Rune.Recents -> navController.navigate("history")
      Rune.Shuffle -> conn?.player?.shuffleModeEnabled = true
      Rune.Discover -> onDiscover()
    }
    hint = r.label
  }

  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("HAUNTED GESTURES", color = HW.Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
      Text(hint ?: "Draw a rune below", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    Box(
      Modifier.fillMaxWidth().padding(top = 6.dp).height(110.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(Color.Black.copy(alpha = 0.5f))
        .border(1.dp, HW.Violet.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
        .pointerInput(Unit) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            HauntedStage.enter(StagePhase.GestureRecognition)
            trail.clear(); trail += down.position; suggestion = null; hint = null; fade = 1f
            down.consume()
            do {
              val ev = awaitPointerEvent()
              ev.changes.forEach { c -> if (c.pressed) { trail += c.position; c.consume() } }
            } while (ev.changes.any { it.pressed })
            val rec = recognise(trail.toList(), dp)
            when {
              rec.rune != null && rec.confidence >= 0.8f -> run(rec.rune)
              rec.rune != null && rec.confidence >= 0.5f -> { suggestion = rec.rune; hint = "Did you mean: ${rec.rune.label}?" }
              else -> hint = "Not recognised. Use the buttons below."
            }
            HauntedStage.release(StagePhase.GestureRecognition)
            scope.launch { delay(450); trail.clear() }
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(Modifier.matchParentSizeCompat()) {
        if (trail.size > 1) {
          val path = Path().apply { moveTo(trail[0].x, trail[0].y); for (i in 1 until trail.size) lineTo(trail[i].x, trail[i].y) }
          drawPath(path, HW.Violet.copy(alpha = 0.35f), style = Stroke(14f))
          drawPath(path, HW.Orange.copy(alpha = 0.95f), style = Stroke(4f))
        }
      }
      if (trail.isEmpty()) Text("circle · wave · diagonal flick · swipe up", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
    }
    AnimatedVisibility(suggestion != null) {
      Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PadButton("Yes: ${suggestion?.label.orEmpty()}", HW.Orange, Color.Black) { suggestion?.let { run(it) } }
        PadButton("Cancel", Color.White.copy(alpha = 0.12f), Color.White) { suggestion = null; hint = null }
      }
    }
    Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Rune.values().forEach { r -> PadButton(r.label, Color.White.copy(alpha = 0.10f), Color.White) { run(r) } }
      PadButton("Add current song to Favorites", Color.White.copy(alpha = 0.10f), Color.White) {
        if (currentSong?.song?.liked == false) conn?.toggleLike()
        hint = if (currentSong == null) "Nothing is playing" else "Added to Favorites"
      }
    }
  }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().height(110.dp))

@Composable
private fun PadButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
  Box(Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)) {
    Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Time Fold: real listening history from the app's own database, grouped by real dates
// ---------------------------------------------------------------------------------------------------------------

private class FoldSong(val meta: MediaMetadata, val thumb: String?, val title: String)
private class FoldPeriod(val name: String, val songs: List<FoldSong>)

@Composable
fun TimeFoldCard(navController: NavController, openSignal: Int) {
  val database = LocalDatabase.current
  val conn = LocalPlayerConnection.current
  val fx = rememberHwFx()
  var open by remember { mutableStateOf(false) }
  var lastSignal by remember { mutableStateOf(openSignal) }
  LaunchedEffect(openSignal) { if (openSignal != lastSignal) { lastSignal = openSignal; open = true } }
  DisposableEffect(open) {
    if (open) HauntedStage.enter(StagePhase.TimeFoldExploration)
    onDispose { HauntedStage.release(StagePhase.TimeFoldExploration) }
  }

  val periods by produceState<List<FoldPeriod>?>(null, open) {
    if (!open) return@produceState
    database.events().collect { events ->
      value = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val buckets = linkedMapOf("Today" to mutableListOf<FoldSong>(), "Yesterday" to mutableListOf(), "Earlier this week" to mutableListOf(), "Older" to mutableListOf())
        val seen = HashSet<String>()
        for (e in events) {
          val day = e.event.timestamp.toLocalDate()
          val key = when {
            day == today -> "Today"
            day == today.minusDays(1) -> "Yesterday"
            day.isAfter(today.minusDays(7)) -> "Earlier this week"
            else -> "Older"
          }
          if (!seen.add(key + "|" + e.song.id)) continue
          val b = buckets.getValue(key)
          if (b.size < 20) b += FoldSong(e.song.toMediaMetadata(), e.song.song.thumbnailUrl, e.song.song.title)
        }
        buckets.filter { it.value.isNotEmpty() }.map { FoldPeriod(it.key, it.value) }
      }
    }
  }

  val shape = RoundedCornerShape(28.dp)
  Column(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(shape)
      .background(Color.Black.copy(alpha = 0.5f))
      .border(1.dp, Brush.linearGradient(listOf(HW.Violet.copy(alpha = 0.7f), HW.Orange.copy(alpha = 0.6f))), shape)
      .animateContentSize()
      .padding(14.dp)
  ) {
    Row(Modifier.clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
      Canvas(Modifier.size(34.dp)) { // spectral hourglass
        val p = Path().apply { moveTo(size.width * 0.15f, 0f); lineTo(size.width * 0.85f, 0f); lineTo(size.width * 0.5f, size.height * 0.5f); lineTo(size.width * 0.85f, size.height); lineTo(size.width * 0.15f, size.height); lineTo(size.width * 0.5f, size.height * 0.5f); close() }
        drawPath(p, Brush.verticalGradient(listOf(HW.Violet.copy(alpha = 0.5f), HW.Orange.copy(alpha = 0.5f))))
        drawPath(p, HW.Silver.copy(alpha = 0.8f), style = Stroke(2f))
      }
      Column(Modifier.padding(start = 12.dp).weight(1f)) {
        Text("TIME FOLD", color = HW.Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Your listening history, folded by day", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
      }
      Text(if (open) "Fold" else "Unfold", color = HW.Orange, fontSize = 13.sp)
    }
    if (open) {
      val list = periods
      when {
        list == null -> Text("Reading your history…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        list.isEmpty() -> {
          Text("No listening history yet. Play something and it will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
          Box(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).background(HW.Orange).clickable { navController.navigate("history") }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("Open Recently Played", color = Color.Black, fontSize = 13.sp) }
        }
        else -> {
          val pager = rememberPagerState { list.size }
          val scope = rememberCoroutineScope()
          Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEachIndexed { i, p ->
              val sel = pager.currentPage == i
              Box(Modifier.clip(RoundedCornerShape(50)).background(if (sel) HW.Violet else Color.White.copy(alpha = 0.1f)).clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("${p.name} · ${p.songs.size}", color = Color.White, fontSize = 12.sp)
              }
            }
          }
          HorizontalPager(pager, Modifier.fillMaxWidth().padding(top = 10.dp), pageSpacing = 12.dp) { page ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(list[page].songs, key = { list[page].name + it.meta.id }) { s ->
                Column(Modifier.width(92.dp).clickable { conn?.playQueue(YouTubeQueue.radio(s.meta)) }) {
                  AsyncImage(
                    model = s.thumb, contentDescription = s.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Brush.linearGradient(listOf(HW.Orange.copy(alpha = 0.7f), HW.Violet.copy(alpha = 0.7f))), RoundedCornerShape(16.dp)),
                  )
                  Text(s.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Collections: filled from real catalog searches; a collection with too few songs is not shown
// ---------------------------------------------------------------------------------------------------------------

private val collectionNames = listOf("Midnight Drive", "Haunted Frequencies", "Dark Romance", "Moonlight Chill", "Ghostly Echoes", "After Midnight", "Halloween Mix")

private class Collection(val name: String, val songs: List<SongItem>)

@Composable
fun HauntedCollections() {
  val conn = LocalPlayerConnection.current
  val (rawLang, _) = rememberPreference(MusicLanguagesKey, "")
  val lang = remember(rawLang) { parseMusicLanguages(rawLang).firstOrNull() }
  val loaded = remember(lang?.code) { mutableStateListOf<Collection>() }
  var done by remember(lang?.code) { mutableStateOf(false) }
  LaunchedEffect(lang?.code) {
    for (name in collectionNames) {
      val items = withContext(Dispatchers.IO) {
        YouTube.search("$name ${lang?.label.orEmpty()} songs".trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(12)
      }
      if (items.size >= 4) loaded += Collection(name, items) // only collections that can really be filled
    }
    done = true
  }
  if (loaded.isEmpty()) return
  Column(Modifier.padding(vertical = 6.dp)) {
    Text("HAUNTED COLLECTIONS", color = HW.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
    loaded.forEach { c ->
      Text(c.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
      LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(c.songs, key = { c.name + it.id }) { s ->
          Column(Modifier.width(96.dp).clickable { conn?.playQueue(YouTubeQueue.radio(s.toMediaMetadata())) }) {
            Box {
              AsyncImage(
                model = s.thumbnail, contentDescription = s.title, contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Brush.linearGradient(listOf(HW.Orange.copy(alpha = 0.7f), HW.Violet.copy(alpha = 0.6f))), RoundedCornerShape(16.dp)),
              )
              Box(Modifier.matchParentSizeBox().background(Brush.verticalGradient(listOf(Color.Transparent, HW.Violet.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.35f)))))
            }
            Text(s.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
          }
        }
      }
    }
  }
}

private fun Modifier.matchParentSizeBox(): Modifier = this.size(96.dp).clip(RoundedCornerShape(16.dp))
