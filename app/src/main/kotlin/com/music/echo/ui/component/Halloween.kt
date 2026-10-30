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
import echo.music.iad1tya.constants.HalloweenAmbientKey
import echo.music.iad1tya.constants.HalloweenChoiceKey
import echo.music.iad1tya.constants.HalloweenPortalKey
import echo.music.iad1tya.constants.HalloweenReducedMotionKey
import echo.music.iad1tya.constants.MUSIC_LANGUAGE_SKIPPED
import echo.music.iad1tya.constants.MusicLanguagesKey
import echo.music.iad1tya.models.toMediaMetadata
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

private val Orange = Color(0xFFFF7A1A)
private val Violet = Color(0xFF9B6BFF)
private val Silver = Color(0xFFDDE3F0)
private val Crimson = Color(0xFF8B1A2B)

/** True when the Halloween edition is on. */
@Composable
fun rememberHalloweenOn(): Boolean {
  val (choice, _) = rememberPreference(HalloweenChoiceKey, "")
  return choice == "halloween"
}

/** One coordinated state for the big transitions: only one runs at a time. */
enum class HalloweenPhase { Idle, CapsuleOpening, RealityTear, ParticleFormation, PlayerArrival, TimeFold, Cancelled }

object HalloweenState {
  var phase by mutableStateOf(HalloweenPhase.Idle)
}

// ---------------------------------------------------------------------------------------------
// 1. Haunted backdrop: moon, fog, trees, castle, embers. Static when reduced motion is on.
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenBackdrop(modifier: Modifier = Modifier) {
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val still = reducedPref || rememberReduceMotion()
  val t: State<Float> =
    if (still) remember { mutableStateOf(0f) }
    else rememberInfiniteTransition(label = "haunt").animateFloat(0f, 1f, infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), label = "t")
  val embers = remember { List(26) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat(), Random.nextFloat()) } }
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF0B0616), Color(0xFF1A0B12))))
    // stars
    for (i in 0 until 40) {
      val sx = ((i * 97) % 100) / 100f * w
      val sy = ((i * 53) % 60) / 100f * h
      drawCircle(Silver.copy(alpha = 0.25f + (i % 4) * 0.06f), 1.2f, Offset(sx, sy))
    }
    // moon
    val mc = Offset(w * 0.78f, h * 0.13f)
    drawCircle(Brush.radialGradient(listOf(Silver.copy(alpha = 0.35f), Color.Transparent), mc, w * 0.32f), w * 0.32f, mc)
    drawCircle(Silver.copy(alpha = 0.92f), w * 0.09f, mc)
    drawCircle(Color(0xFFB9C0D4).copy(alpha = 0.7f), w * 0.02f, mc + Offset(-w * 0.025f, w * 0.01f))
    // castle + trees silhouettes along the bottom
    val ground = h * 0.93f
    val sil = Color(0xFF040208)
    val castle = Path().apply {
      moveTo(w * 0.05f, ground); lineTo(w * 0.05f, ground - h * 0.10f); lineTo(w * 0.08f, ground - h * 0.10f)
      lineTo(w * 0.08f, ground - h * 0.16f); lineTo(w * 0.095f, ground - h * 0.19f); lineTo(w * 0.11f, ground - h * 0.16f)
      lineTo(w * 0.11f, ground - h * 0.08f); lineTo(w * 0.17f, ground - h * 0.08f); lineTo(w * 0.17f, ground)
      close()
    }
    drawPath(castle, sil)
    for (k in 0 until 6) {
      val bx = w * (0.45f + k * 0.10f)
      val th = h * (0.07f + (k % 3) * 0.025f)
      val tree = Path().apply { moveTo(bx, ground); lineTo(bx, ground - th); lineTo(bx - w * 0.03f, ground - th * 0.6f); moveTo(bx, ground - th * 0.8f); lineTo(bx + w * 0.035f, ground - th * 0.55f); moveTo(bx, ground - th * 0.5f); lineTo(bx - w * 0.025f, ground - th * 0.3f) }
      drawPath(tree, sil, style = Stroke(width = 5f))
    }
    drawRect(sil, Offset(0f, ground), androidx.compose.ui.geometry.Size(w, h - ground))
    // drifting fog bands
    for (i in 0 until 3) {
      val shift = ((t.value * (1 + i) + i * 0.33f) % 1f)
      val fx = -w * 0.5f + shift * w * 1.5f
      drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Violet.copy(alpha = 0.10f), Color.Transparent), fx, fx + w * 0.9f), Offset(fx, ground - h * (0.05f + i * 0.035f)), androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.07f))
    }
    // embers
    embers.forEach { e ->
      val ph = (t.value * (0.5f + e[2]) * 3f + e[3]) % 1f
      val ex = (e[0] + sin((ph + e[3]) * 2f * PI.toFloat()) * 0.02f) * w
      val ey = h - ph * h * 0.8f
      drawCircle((if (e[3] > 0.5f) Orange else Violet).copy(alpha = (1f - ph) * 0.65f), 1.6f + e[2] * 1.6f, Offset(ex, ey))
    }
  }
}

// ---------------------------------------------------------------------------------------------
// 2. Reality Tear + Song Manifestation overlay (plays when the current song changes)
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenPortalHost() {
  val on = rememberHalloweenOn()
  val (portalOn, _) = rememberPreference(HalloweenPortalKey, true)
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val lite = rememberReduceMotion()
  if (!on || !portalOn) return
  val connection = LocalPlayerConnection.current ?: return
  val meta by connection.mediaMetadata.collectAsState()
  var shownId by remember { mutableStateOf<String?>(null) }
  var firstSeen by remember { mutableStateOf(false) }
  val progress = remember { Animatable(0f) }
  var art by remember { mutableStateOf<String?>(null) }
  val seed = remember { Random(7) }

  LaunchedEffect(meta?.id) {
    val m = meta ?: return@LaunchedEffect
    if (!firstSeen) { firstSeen = true; shownId = m.id; return@LaunchedEffect } // no animation for the restored song at start
    if (m.id == shownId) return@LaunchedEffect
    shownId = m.id
    art = m.thumbnailUrl
    if (HalloweenState.phase == HalloweenPhase.CapsuleOpening || HalloweenState.phase == HalloweenPhase.TimeFold) { /* still allowed: tear supersedes */ }
    HalloweenState.phase = HalloweenPhase.RealityTear
    progress.snapTo(0f)
    // short and interruptible: a newer song restarts this effect, which cancels this animation
    progress.animateTo(1f, tween(if (reducedPref || lite) 450 else 1100, easing = FastOutSlowInEasing))
    HalloweenState.phase = HalloweenPhase.Idle
    art = null
  }
  val p = progress.value
  if (art == null || p <= 0f || p >= 1f) return
  val particles = remember(art) { List(if (lite) 24 else 72) { floatArrayOf(seed.nextFloat() * 6.283f, 0.35f + seed.nextFloat() * 0.65f, seed.nextFloat()) } }
  Box(Modifier.fillMaxSize()) {
    Canvas(Modifier.fillMaxSize()) {
      val c = Offset(size.width / 2f, size.height * 0.42f)
      val dim = if (p < 0.5f) p * 2f else (1f - p) * 2f
      drawRect(Color.Black.copy(alpha = 0.55f * dim))
      // Phase 1: thin orange-violet crack
      val crack = (p / 0.3f).coerceIn(0f, 1f)
      if (crack > 0f && p < 0.7f) {
        val path = Path().apply {
          moveTo(c.x, c.y - size.height * 0.2f * crack)
          var y = c.y - size.height * 0.2f * crack
          val steps = 8
          for (i in 1..steps) { y += size.height * 0.4f * crack / steps; lineTo(c.x + (if (i % 2 == 0) 14f else -14f) * crack, y) }
        }
        drawPath(path, Brush.verticalGradient(listOf(Orange, Violet)), style = Stroke(width = 3f + 4f * crack))
      }
      // Phase 2/3: portal opens, particles spiral inward and form the artwork
      val open = ((p - 0.2f) / 0.5f).coerceIn(0f, 1f)
      val r = size.minDimension * 0.52f * open
      if (open > 0f) {
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = 0.55f * (1f - p)), Orange.copy(alpha = 0.25f * (1f - p)), Color.Transparent), c, r * 1.3f + 1f), r * 1.3f, c)
        drawCircle(Brush.sweepGradient(listOf(Orange, Violet, Crimson, Orange), c), r, c, style = Stroke(width = 4f))
        particles.forEach { pt ->
          val conv = ((p - 0.15f) / 0.6f).coerceIn(0f, 1f)
          val ang = pt[0] + conv * 6f * pt[1] * 3f
          val rad = r * (1.35f - conv * 1.3f) * pt[1]
          drawCircle((if (pt[2] > 0.5f) Orange else Violet).copy(alpha = (1f - conv * 0.7f) * (1f - p)), 2.2f, Offset(c.x + cos(ang) * rad, c.y + sin(ang) * rad))
        }
      }
    }
    // the real album art forms inside the portal, then settles away into the player
    val form = ((p - 0.45f) / 0.35f).coerceIn(0f, 1f)
    val fade = if (p > 0.8f) (1f - (p - 0.8f) / 0.2f) else 1f
    AsyncImage(
      model = art?.resize(544, 544),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.align(Alignment.Center).padding(bottom = 140.dp).size(210.dp).graphicsLayer { alpha = form * fade; scaleX = 0.5f + 0.5f * form; scaleY = 0.5f + 0.5f * form }
        .clip(RoundedCornerShape(24.dp)).border(1.5.dp, Brush.sweepGradient(listOf(Orange, Violet, Orange)), RoundedCornerShape(24.dp)),
    )
  }
}

// ---------------------------------------------------------------------------------------------
// 3. Mystery Capsule: Trick or Track (works with or without the Halloween theme)
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
  val halloween = rememberHalloweenOn()
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val still = reducedPref || rememberReduceMotion()
  val (langRaw, _) = rememberPreference(MusicLanguagesKey, "")
  val skipped = rememberSkippedIds()
  val lang = remember(langRaw) { parseMusicLanguages(langRaw).firstOrNull()?.query }
  var state by remember { mutableStateOf("idle") } // idle, opening, shown, empty
  var song by remember { mutableStateOf<SongItem?>(null) }
  val pool = remember { mutableStateListOf<SongItem>() }
  val seen = remember { mutableSetOf<String>() }
  var busy by remember { mutableStateOf(false) }
  val split = remember { Animatable(0f) }
  val spin = rememberInfiniteTransition(label = "cap").animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")

  fun reveal() {
    if (busy) return
    busy = true
    scope.launch {
      state = "opening"; HalloweenState.phase = HalloweenPhase.CapsuleOpening
      split.snapTo(0f)
      split.animateTo(1f, tween(if (still) 200 else 800, easing = FastOutSlowInEasing))
      if (pool.size < 2) pool.addAll(capsuleSongs(lang, seen + skipped))
      val next = pool.firstOrNull { it.id !in seen && it.id !in skipped }
      if (next == null) { state = "empty"; song = null } else { pool.remove(next); seen += next.id; song = next; state = "shown" }
      HalloweenState.phase = HalloweenPhase.Idle
      busy = false
    }
  }

  Column(
    modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(26.dp))
      .background(Brush.linearGradient(if (halloween) listOf(Color(0xCC12081F), Color(0xCC2A0F0A)) else listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))))
      .border(1.dp, if (halloween) Brush.linearGradient(listOf(Orange.copy(alpha = 0.8f), Violet.copy(alpha = 0.8f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))), RoundedCornerShape(26.dp))
      .padding(14.dp)
  ) {
    when (state) {
      "shown" -> {
        val s = song!!
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          AsyncImage(model = s.thumbnail.resize(360, 360), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)))
          Column(Modifier.weight(1f)) {
            Text("TRICK OR TRACK", fontSize = 10.sp, letterSpacing = 1.6.sp, color = if (halloween) Orange else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(s.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(s.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CapsuleButton("Play", true) { connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = s.id), s.toMediaMetadata())) }
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
          Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
            val tremble = if (state == "opening" && !still) sin(split.value * 40f) * 3f else 0f
            Canvas(Modifier.size(70.dp).graphicsLayer { rotationZ = if (still) 0f else (if (state == "opening") split.value * 90f else spin.value * 0.2f); translationX = tremble }) {
              val gap = split.value * 18f
              val cx = size.width / 2f
              val cy = size.height / 2f
              drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1B1030), Color(0xFF07040E))), Offset(cx - 18f, cy - 32f - gap), androidx.compose.ui.geometry.Size(36f, 32f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
              drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF07040E), Color(0xFF1B1030))), Offset(cx - 18f, cy + gap), androidx.compose.ui.geometry.Size(36f, 32f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
              drawLine(Orange, Offset(cx - 18f, cy), Offset(cx + 18f, cy), strokeWidth = 3f + split.value * 5f)
              drawCircle(Violet.copy(alpha = 0.25f + split.value * 0.4f), 36f, Offset(cx, cy))
              if (split.value > 0.3f) for (i in 0 until 10) { val a = i * 0.628f + split.value * 3f; drawCircle(Silver.copy(alpha = 0.8f - split.value * 0.5f), 2f, Offset(cx + cos(a) * split.value * 34f, cy + sin(a) * split.value * 34f)) }
            }
          }
          Column {
            Text("TRICK OR TRACK", fontSize = 10.sp, letterSpacing = 1.6.sp, color = if (halloween) Orange else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
  val database = LocalDatabase.current
  val connection = LocalPlayerConnection.current
  val halloween = rememberHalloweenOn()
  var open by remember { mutableStateOf(false) }
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(22.dp))
      .background(Color.White.copy(alpha = 0.08f)).border(1.dp, if (halloween) Violet.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
      .clickable { open = true }.padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("⏳", fontSize = 26.sp)
    Column { Text("Time Fold", fontWeight = FontWeight.Bold); Text("Explore what you played, by day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  if (open) {
    val events by database.events().collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf(0) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    val groups = remember(events) {
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
        if (list.none { it.id == e.song.id } && list.size < 30) list += e.song
      }
      byPeriod.filterValues { it.isNotEmpty() }.entries.toList()
    }
    AlertDialog(
      onDismissRequest = { open = false; HalloweenState.phase = HalloweenPhase.Idle },
      shape = RoundedCornerShape(28.dp),
      title = { Text("Time Fold") },
      text = {
        if (groups.isEmpty()) {
          Text("No listening history yet. Play some songs and they will appear here.")
        } else {
          val i = tab.coerceIn(0, groups.lastIndex)
          Column(Modifier.pointerInput(groups.size) {
            var dx = 0f
            detectDragGestures(onDragStart = { dx = 0f }, onDragEnd = { if (dx < -80f) tab = min(groups.lastIndex, i + 1) else if (dx > 80f) tab = max(0, i - 1) }) { c, d -> dx += d.x }
          }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              groups.forEachIndexed { idx, g ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (idx == i) Violet.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)).clickable { tab = idx }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text(g.key, fontSize = 12.sp) }
              }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(groups[i].value, key = { "tf_${groups[i].key}_${it.id}" }) { s ->
                Column(Modifier.size(width = 110.dp, height = 150.dp).clip(RoundedCornerShape(14.dp)).clickable {
                  connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = s.id), s.toMediaMetadata()))
                  open = false
                }) {
                  AsyncImage(model = s.song.thumbnailUrl?.resize(360, 360), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)))
                  Text(s.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                  Text(s.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
    )
  }
}

// ---------------------------------------------------------------------------------------------
// 5. Gesture Music Language: draw in the pad; ambiguous shapes only suggest an action
// ---------------------------------------------------------------------------------------------
private enum class Shape { Circle, Wave, Diagonal, Unknown }

private fun recognize(pts: List<Offset>): Pair<Shape, Boolean> {
  if (pts.size < 12) return Shape.Unknown to false
  val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }; val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
  val w = maxX - minX; val h = maxY - minY
  if (max(w, h) < 90f) return Shape.Unknown to false
  var len = 0f
  for (i in 1 until pts.size) len += (pts[i] - pts[i - 1]).getDistance()
  val gap = (pts.first() - pts.last()).getDistance()
  val diag = kotlin.math.hypot(w, h)
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
  return when {
    closed && w / h in 0.6f..1.7f && len > diag * 2.2f -> Shape.Circle to true
    straight > 0.88f && w > 60f && h > 60f && abs(w / h - 1f) < 0.7f -> Shape.Diagonal to true
    turns >= 3 && w > h * 1.4f -> Shape.Wave to (turns >= 4)
    straight > 0.8f && w > 60f && h > 60f -> Shape.Diagonal to false
    closed -> Shape.Circle to false
    else -> Shape.Unknown to false
  }
}

@Composable
fun GesturePad(navController: NavController) {
  val connection = LocalPlayerConnection.current
  val trail = remember { mutableStateListOf<Offset>() }
  var hint by remember { mutableStateOf("Draw a circle (Favorites), a wave (Recently played) or a diagonal flick (Shuffle)") }
  var suggestion by remember { mutableStateOf<Shape?>(null) }
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)

  fun run(s: Shape) {
    when (s) {
      Shape.Circle -> navController.navigate("auto_playlist/liked")
      Shape.Wave -> navController.navigate("history")
      Shape.Diagonal -> connection?.let { it.player.shuffleModeEnabled = !it.player.shuffleModeEnabled }
      Shape.Unknown -> Unit
    }
  }
  fun name(s: Shape) = when (s) { Shape.Circle -> "Open Favorites"; Shape.Wave -> "Open Recently played"; Shape.Diagonal -> "Toggle shuffle"; Shape.Unknown -> "" }

  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Box(
      Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { trail.clear(); suggestion = null },
            onDragEnd = {
              val (shape, sure) = recognize(trail.toList())
              if (shape == Shape.Unknown) hint = "Not recognised. Use the buttons below."
              else if (sure) { hint = name(shape); run(shape) } else { suggestion = shape; hint = "Did you mean: ${name(shape)}?" }
              trail.clear()
            },
            onDragCancel = { trail.clear() },
          ) { change, _ -> change.consume(); trail.add(change.position); if (trail.size > 240) trail.removeAt(0) }
        },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(Modifier.fillMaxSize()) {
        if (trail.size > 1) {
          val path = Path().apply { moveTo(trail[0].x, trail[0].y); trail.drop(1).forEach { lineTo(it.x, it.y) } }
          drawPath(path, Brush.linearGradient(listOf(Orange, Violet)), style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
      }
      if (trail.isEmpty()) Text(hint, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp))
    }
    suggestion?.let { s ->
      Row(Modifier.padding(top = 6.dp)) { CapsuleButton(name(s), true) { run(s); suggestion = null; hint = name(s) } }
    }
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      CapsuleButton("Favorites", false) { run(Shape.Circle) }
      CapsuleButton("Recent", false) { run(Shape.Wave) }
      CapsuleButton("Shuffle", false) { run(Shape.Diagonal) }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// 6. Seasonal collections: only shown when real songs were found
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenCollections(refreshKey: Any?) {
  val (langRaw, _) = rememberPreference(MusicLanguagesKey, "")
  val lang = remember(langRaw) { parseMusicLanguages(langRaw).firstOrNull()?.query.orEmpty() }
  val rows by
    produceState<List<Pair<String, List<SongItem>>>>(emptyList(), lang, refreshKey) {
      val names = listOf("Midnight Drive" to "midnight drive", "Haunted Frequencies" to "dark ambient eerie", "Dark Romance" to "dark romantic", "Moonlight Chill" to "moonlight chill", "Ghostly Echoes" to "echo reverb slow", "After Midnight" to "after midnight", "Halloween Mix" to "halloween spooky")
      value = coroutineScope { names.shuffled().take(4).map { (n, q) -> async { n to withContext(Dispatchers.IO) { YouTube.search("$lang $q".trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(14) } } }.awaitAll().filter { it.second.isNotEmpty() } }
    }
  Column { rows.forEach { (t, s) -> SongCardRow(title = t, songs = s, subtitle = "Halloween collection") } }
}

// ---------------------------------------------------------------------------------------------
// 7. Optional ambient wind (generated, no copyrighted audio, off by default)
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenAmbientWind(active: Boolean) {
  DisposableEffect(active) {
    if (!active) return@DisposableEffect onDispose {}
    var running = true
    val rate = 22050
    val buf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
    val track = runCatching {
      AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(buf).build()
    }.getOrNull()
    val th = Thread {
      track ?: return@Thread
      track.setVolume(0.12f)
      track.play()
      val chunk = ShortArray(1024)
      var lp = 0.0
      var ph = 0.0
      while (running) {
        for (i in chunk.indices) {
          lp += 0.04 * ((Random.nextDouble() * 2 - 1) - lp)
          ph += 0.00004
          val gust = 0.55 + 0.45 * sin(ph * 2 * PI)
          chunk[i] = (lp * gust * 9000).toInt().toShort()
        }
        track.write(chunk, 0, chunk.size)
      }
      runCatching { track.stop(); track.release() }
    }
    th.isDaemon = true
    th.start()
    onDispose { running = false }
  }
}

// ---------------------------------------------------------------------------------------------
// 8. Theme choice for new and existing users
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenChoiceDialog() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val (choice, _) = rememberPreference(HalloweenChoiceKey, "")
  val (langs, _) = rememberPreference(MusicLanguagesKey, "")
  // new users: wait until the language selector has been answered (saved or skipped)
  if (choice.isNotEmpty() || langs.isEmpty()) return
  var dismissed by remember { mutableStateOf(false) }
  if (dismissed) return
  fun pick(v: String) { dismissed = true; scope.launch { context.dataStore.edit { it[HalloweenChoiceKey] = v } } }
  AlertDialog(
    onDismissRequest = { dismissed = true },
    shape = RoundedCornerShape(28.dp),
    title = { Text("🎃 Halloween Edition") },
    text = { Text("Try the haunted Home with the cursed Magic Cube, portals and Mystery Capsule, or keep the original Pattukal look. Your songs, favourites and settings stay exactly as they are. You can change this any time in Settings > Seasonal Halloween Test.") },
    confirmButton = { TextButton(onClick = { pick("halloween") }) { Text("Halloween effects") } },
    dismissButton = { TextButton(onClick = { pick("original") }) { Text("Original theme") } },
  )
}
