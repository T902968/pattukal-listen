package echo.music.iad1tya.ui.component

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import echo.music.iad1tya.ui.component.glassSurface
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.constants.MUSIC_LANGUAGE_SKIPPED
import echo.music.iad1tya.constants.MusicLanguagesKey
import echo.music.iad1tya.constants.WinterEnabledKey
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.utils.resize
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.utils.rememberReduceMotion
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Orange = Color(0xFF7FD6FF)
private val Violet = Color(0xFFB8A6FF)
private val Silver = Color(0xFFEAF6FF)
private val Crimson = Color(0xFF2A5BD7)

/** True when the Winter edition is on (it is the default theme). */
@Composable
fun rememberWinterOn(): Boolean {
  val (on, _) = rememberPreference(WinterEnabledKey, false)
  return on
}

/** One coordinated state for the big transitions: only one runs at a time. */
enum class SeasonPhase { Idle, CapsuleOpening, RealityTear, ParticleFormation, PlayerArrival, TimeFold, Cancelled }

object SeasonState {
  var phase by mutableStateOf(SeasonPhase.Idle)
}

// ---------------------------------------------------------------------------------------------
// 3. Mystery Capsule: Mystery Song (works with or without the Halloween theme)
// ---------------------------------------------------------------------------------------------
private suspend fun capsuleSongs(languageQuery: String?, avoid: Set<String>): List<SongItem> =
  withContext(Dispatchers.IO) {
    val q = listOf("hits", "popular songs", "melody", "party songs", "romantic songs").random()
    YouTube.search("${languageQuery.orEmpty()} $q".trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty()
      .filterIsInstance<SongItem>().filter { it.id !in avoid }
  }

@Composable
fun MysteryCapsule(modifier: Modifier = Modifier) {
  val connection = LocalPlayerConnection.current
  val database = LocalDatabase.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val halloween = rememberWinterOn()
  val (reducedPref, _) = rememberPreference(WinterLowPerfKey, false)
  val still = reducedPref || rememberReduceMotion()
  val (langRaw, _) = rememberPreference(MusicLanguagesKey, "")
  val skipped = rememberSkippedIds()
  val lang = remember(langRaw) { parseMusicLanguages(langRaw).firstOrNull()?.query }
  var song by remember { mutableStateOf(HomeCache.get<SongItem>("capsule_song")) }
  var state by remember { mutableStateOf(if (song != null) "shown" else "idle") } // idle, opening, shown, empty
  val pool = remember { mutableStateListOf<SongItem>() }
  val seen = remember { mutableSetOf<String>() }
  var busy by remember { mutableStateOf(false) }
  val split = remember { Animatable(0f) }
  val spin = rememberInfiniteTransition(label = "cap").animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")

  fun reveal() {
    if (busy) return
    busy = true
    scope.launch {
      state = "opening"; SeasonState.phase = SeasonPhase.CapsuleOpening
      split.snapTo(0f)
      split.animateTo(1f, tween(if (still) 200 else 800, easing = FastOutSlowInEasing))
      if (pool.size < 2) pool.addAll(capsuleSongs(lang, seen + skipped))
      val next = pool.firstOrNull { it.id !in seen && it.id !in skipped }
      if (next == null) { state = "empty"; song = null } else { pool.remove(next); seen += next.id; song = next; HomeCache.put("capsule_song", next); state = "shown" }
      SeasonState.phase = SeasonPhase.Idle
      busy = false
    }
  }

  Column(
    modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
      .glassSurface(RoundedCornerShape(26.dp), if (halloween) Color(0xFF0B1E3D) else Color.White)
      .border(1.dp, if (halloween) Brush.linearGradient(listOf(Orange.copy(alpha = 0.8f), Violet.copy(alpha = 0.8f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))), RoundedCornerShape(26.dp))
      .padding(14.dp)
  ) {
    when (state) {
      "shown" -> {
        val s = song!!
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          AsyncImage(model = s.thumbnail.resize(360, 360), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)))
          Column(Modifier.weight(1f)) {
            Text("MYSTERY SONG", fontSize = 10.sp, letterSpacing = 1.6.sp, color = if (halloween) Orange else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(s.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(s.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CapsuleButton("Play", true) { connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = s.id), s.toMediaMetadata())) }
          CapsuleButton("Play next", false) { connection?.playNext(listOf(s.toMediaMetadata().toMediaItem())); Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show() }
          CapsuleButton("Save", false) {
            scope.launch(Dispatchers.IO) {
              val existing = database.song(s.id).first()
              if (existing?.song?.liked == true) { withContext(Dispatchers.Main) { Toast.makeText(context, "Already in favourites", Toast.LENGTH_SHORT).show() }; return@launch }
              database.transaction {
                if (existing == null) insert(s.toMediaMetadata(), echo.music.iad1tya.db.entities.SongEntity::toggleLike)
                else update(existing.song.toggleLike())
              }
              withContext(Dispatchers.Main) { Toast.makeText(context, "Saved to favourites", Toast.LENGTH_SHORT).show() }
            }
          }
          CapsuleButton("Another", false) { reveal() }
        }
      }
      "empty" -> {
        Text("No songs found right now. Check your connection and try again.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        CapsuleButton("Try again", true) { reveal() }
      }
      else -> {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { reveal() }) {
          Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            val tremble = if (state == "opening" && !still) sin(split.value * 40f) * 3f else 0f
            val sloshT = rememberInfiniteTransition(label = "slosh").animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "slosh")
            val accent = if (halloween) Orange else MaterialTheme.colorScheme.primary
            val liquid = if (halloween) Violet else MaterialTheme.colorScheme.tertiary
            Canvas(Modifier.size(78.dp).graphicsLayer { rotationZ = if (still) 0f else (if (state == "opening") split.value * 90f else 18f * sin(sloshT.value * 2f * PI.toFloat())); translationX = tremble }) {
              val sp = split.value
              val cx = size.width / 2f
              val cy = size.height / 2f
              val cw = size.width * 0.46f
              val ch = size.height * 0.86f
              val gap = sp * 16f
              // glass shell halves (top and bottom) that part along the glowing seam
              val top = androidx.compose.ui.geometry.Rect(cx - cw / 2f, cy - ch / 2f - gap, cx + cw / 2f, cy - gap)
              val bot = androidx.compose.ui.geometry.Rect(cx - cw / 2f, cy + gap, cx + cw / 2f, cy + ch / 2f + gap)
              val rad = androidx.compose.ui.geometry.CornerRadius(cw / 2f, cw / 2f)
              // liquid inside, with a moving wave surface and bubbles
              val fillTop = cy - ch * 0.18f
              val wave = Path().apply {
                moveTo(cx - cw / 2f, fillTop)
                val steps = 14
                for (i in 0..steps) {
                  val x = cx - cw / 2f + cw * i / steps
                  lineTo(x, fillTop + sin((i / steps.toFloat()) * 2f * PI.toFloat() * 1.3f + sloshT.value * 2f * PI.toFloat()) * 4f)
                }
                lineTo(cx + cw / 2f, cy + ch / 2f - 4f); lineTo(cx - cw / 2f, cy + ch / 2f - 4f); close()
              }
              drawRoundRect(Color.White.copy(alpha = 0.06f), top.topLeft, androidx.compose.ui.geometry.Size(cw, ch + 2 * gap), rad)
              clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f, rad)) }) {
                drawPath(wave, Brush.verticalGradient(listOf(liquid.copy(alpha = 0.85f), accent.copy(alpha = 0.75f)), fillTop, cy + ch / 2f))
                for (i in 0 until 5) {
                  val ph = (sloshT.value * (1 + i % 3) + i * 0.21f) % 1f
                  drawCircle(Color.White.copy(alpha = 0.55f * (1f - ph)), 1.6f + (i % 3), Offset(cx - cw * 0.25f + (i % 3) * cw * 0.22f, cy + ch * 0.42f - ph * ch * 0.5f))
                }
              }
              drawRoundRect(Color.White.copy(alpha = 0.55f), top.topLeft, androidx.compose.ui.geometry.Size(cw, ch / 2f - gap), rad, style = Stroke(2f))
              drawRoundRect(Color.White.copy(alpha = 0.55f), bot.topLeft, androidx.compose.ui.geometry.Size(cw, ch / 2f - gap + gap), rad, style = Stroke(2f))
              // specular highlight on the glass
              drawLine(Color.White.copy(alpha = 0.5f), Offset(cx - cw * 0.28f, cy - ch * 0.38f), Offset(cx - cw * 0.28f, cy + ch * 0.1f), strokeWidth = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
              // glowing seam
              drawLine(accent, Offset(cx - cw / 2f - 2f, cy), Offset(cx + cw / 2f + 2f, cy), strokeWidth = 3f + sp * 6f)
              // liquid droplets burst out when it opens
              if (sp > 0.2f) for (i in 0 until 12) {
                val a = i * 0.5236f + sp * 2f
                val d = sp * (size.width * 0.5f)
                drawCircle(liquid.copy(alpha = 0.9f - sp * 0.6f), 3f + (i % 3) * 1.5f, Offset(cx + cos(a) * d, cy + sin(a) * d))
              }
            }
          }
          Column {
            Text("MYSTERY SONG", fontSize = 10.sp, letterSpacing = 1.6.sp, color = if (halloween) Orange else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Mystery Capsule", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(if (state == "opening") "Opening…" else "Tap to reveal a random song", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@Composable
private fun CapsuleButton(label: String, primary: Boolean, onClick: () -> Unit) {
  Box(
    Modifier.clip(RoundedCornerShape(50)).background(if (primary) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp)
  ) { Text(label, color = if (primary) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

// ---------------------------------------------------------------------------------------------
// 4. Time Fold: real listening history, grouped into real periods
// ---------------------------------------------------------------------------------------------
@Composable
fun TimeFoldEntry() {
  val context = LocalContext.current
  val database = LocalDatabase.current
  val connection = LocalPlayerConnection.current
  val halloween = rememberWinterOn()
  var open by remember { mutableStateOf(false) }
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
      .glassSurface(RoundedCornerShape(22.dp), if (halloween) Color(0xFF0B1E3D) else Color.White)
      .border(1.dp, if (halloween) Violet.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
      .clickable { open = true }.padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("⏳", fontSize = 26.sp)
    Column { Text("Time Fold", fontWeight = FontWeight.Bold); Text("Explore what you played, by day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  if (open) {
    val events by database.events().collectAsState(initial = emptyList())
    val (langRaw, _) = rememberPreference(MusicLanguagesKey, "")
    val languages = remember(langRaw) { parseMusicLanguages(langRaw) }
    var lang by remember { mutableStateOf<String?>(null) } // null = my history
    var tab by remember { mutableStateOf(0) }
    val today = remember { LocalDate.now() }
    val history = remember(events) {
      val byPeriod = linkedMapOf("Today" to mutableListOf<echo.music.iad1tya.db.entities.Song>(), "Yesterday" to mutableListOf(), "Earlier this week" to mutableListOf(), "Older" to mutableListOf())
      events.forEach { e ->
        val d = e.event.timestamp.toLocalDate()
        val key = when {
          d == today -> "Today"
          d == today.minusDays(1) -> "Yesterday"
          d.isAfter(today.minusDays(7)) -> "Earlier this week"
          else -> "Older"
        }
        val list = byPeriod.getValue(key)
        if (list.none { it.id == e.song.id } && list.size < 120) list += e.song
      }
      byPeriod.filterValues { it.isNotEmpty() }.entries.map { it.key to it.value.map { s -> FoldSong(s.id, s.song.title, s.artists.joinToString { a -> a.name }, s.song.thumbnailUrl, s.toMediaMetadata()) } }
    }
    val eras = remember { listOf("Latest" to "new", "2010s" to "2010s hits", "2000s" to "2000s hits", "90s" to "90s hits") }
    val selectedLang = languages.firstOrNull { it.code == lang }
    val eraSongs by produceState<List<Pair<String, List<FoldSong>>>>(emptyList(), selectedLang?.code) {
      val l = selectedLang
      if (l == null) { value = emptyList(); return@produceState }
      val key = "fold|${l.code}"
      HomeCache.get<List<Pair<String, List<FoldSong>>>>(key)?.let { value = it; return@produceState }
      value = coroutineScope {
        eras.map { (name, q) ->
          async {
            name to withContext(Dispatchers.IO) {
              listOf("${l.query} $q", "${l.query} $q songs", "best of ${l.query} $q").map { query ->
                async { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>() }
              }.awaitAll().flatten().distinctBy { it.id }.take(60)
                .map { FoldSong(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnail, null, it) }
            }
          }
        }.awaitAll().filter { it.second.isNotEmpty() }
      }
      if (value.isNotEmpty()) HomeCache.put(key, value)
    }
    val groups = if (selectedLang == null) history else eraSongs
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { open = false; SeasonState.phase = SeasonPhase.Idle },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
      Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF05060F), Color(0xFF14083A), Color(0xFF0A1B3D))))) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
          drawCircle(Brush.radialGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.45f), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.2f), size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.2f, size.height * 0.2f))
          drawCircle(Brush.radialGradient(listOf(Color(0xFF22D3EE).copy(alpha = 0.35f), Color.Transparent), Offset(size.width * 0.85f, size.height * 0.75f), size.width * 0.75f), size.width * 0.75f, Offset(size.width * 0.85f, size.height * 0.75f))
        }
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text("PATTUKAL", fontSize = 11.sp, letterSpacing = 4.sp, color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
              Text("Time Fold", fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = { open = false }) { Text("Close", color = Color(0xFF8B5CF6)) }
          }
          Spacer(Modifier.height(10.dp))
          Box(
            Modifier.fillMaxWidth().weight(1f).glassSurface(RoundedCornerShape(30.dp), Color.White)
              .border(1.dp, Brush.linearGradient(listOf(Color(0xFF22D3EE).copy(alpha = 0.7f), Color(0xFF8B5CF6).copy(alpha = 0.7f))), RoundedCornerShape(30.dp)).padding(16.dp),
          ) {
        Column {
          if (languages.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item("hist") { FoldChip("My history", lang == null) { lang = null; tab = 0 } }
              items(languages, key = { it.code }) { l -> FoldChip(l.label, lang == l.code) { lang = l.code; tab = 0 } }
            }
            Spacer(Modifier.height(10.dp))
          }
          if (groups.isEmpty()) {
            Text(if (selectedLang == null) "No listening history yet. Play some songs and they will appear here." else "Loading ${selectedLang.label} songs through time…")
          } else {
            val i = tab.coerceIn(0, groups.lastIndex)
            Column(Modifier.pointerInput(groups.size) {
              var dx = 0f
              detectDragGestures(onDragStart = { dx = 0f }, onDragEnd = { if (dx < -80f) tab = min(groups.lastIndex, i + 1) else if (dx > 80f) tab = max(0, i - 1) }) { _, d -> dx += d.x }
            }) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(groups) { idx, g -> FoldChip(g.first, idx == i) { tab = idx } }
              }
              Spacer(Modifier.height(10.dp))
              Text("${groups[i].second.size} songs · swipe the cards", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              androidx.compose.runtime.key(groups[i].first, lang) {
                val ls = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyRow(state = ls, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 80.dp), horizontalArrangement = Arrangement.spacedBy((-24).dp)) {
                  items(groups[i].second, key = { "tf_${lang}_${groups[i].first}_${it.id}" }) { s ->
                    val itemKey = "tf_${lang}_${groups[i].first}_${s.id}"
                    Column(
                      Modifier.size(width = 190.dp, height = 300.dp)
                        .graphicsLayer {
                          // 3D cover-flow: cards fold away from the centre like layers of time
                          val info = ls.layoutInfo
                          val me = info.visibleItemsInfo.firstOrNull { it.key == itemKey }
                          val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                          val d = if (me == null) 0f else ((me.offset + me.size / 2f) - centre) / me.size
                          rotationY = (-d * 38f).coerceIn(-60f, 60f)
                          val k = 1f - 0.14f * min(abs(d), 1.6f)
                          scaleX = k; scaleY = k
                          alpha = 1f - 0.28f * min(abs(d), 2f)
                          cameraDistance = 14f * density
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                          connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = s.id), s.meta ?: s.item!!.toMediaMetadata()))
                          open = false
                        }
                    ) {
                      AsyncImage(model = s.thumb?.resize(520, 520), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(190.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Brush.linearGradient(listOf(Orange.copy(alpha = 0.7f), Violet.copy(alpha = 0.7f))), RoundedCornerShape(18.dp)))
                      Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                      Text(s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text("＋ Play next", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                          connection?.playNext(listOf((s.meta ?: s.item!!.toMediaMetadata()).toMediaItem()))
                          Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
                        }.padding(vertical = 4.dp))
                    }
                  }
                }
              }
            }
          }
        }
          }
        }
        }
      }
    }
  }
}

private class FoldSong(val id: String, val title: String, val artist: String, val thumb: String?, val meta: echo.music.iad1tya.models.MediaMetadata?, val item: SongItem? = null)

@Composable
private fun FoldChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) Violet.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp) }
}


/** Home entry that opens Pattukal Wrapped. */
@Composable
fun WrappedEntry(navController: NavController) {
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
      .glassSurface(RoundedCornerShape(22.dp), Color(0xFF0B1E3D))
      .border(1.dp, Orange.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
      .clickable { navController.navigate("wrapped") }.padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("🎁", fontSize = 26.sp)
    Column { Text("Pattukal Wrapped", fontWeight = FontWeight.Bold); Text("Your year in music", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}
