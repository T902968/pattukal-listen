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
  // seconds in a 60 s loop; every motion below uses a whole number of cycles per loop so it never jumps
  val clock: State<Float> =
    if (still) remember { mutableStateOf(12f) }
    else rememberInfiniteTransition(label = "haunt").animateFloat(0f, 60f, infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart), label = "t")
  val stars = remember { List(70) { floatArrayOf(Random.nextFloat(), Random.nextFloat() * 0.55f, 0.5f + Random.nextFloat() * 1.4f, Random.nextFloat()) } }
  val embers = remember { List(30) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat(), Random.nextFloat()) } }
  val wisps = remember { List(9) { floatArrayOf(Random.nextFloat(), 0.45f + Random.nextFloat() * 0.45f, Random.nextFloat(), 1f + Random.nextInt(3)) } }
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val t = clock.value
    val tau = 2f * PI.toFloat()
    fun cyc(n: Int, phase: Float = 0f) = sin(tau * (t / 60f * n + phase))
    fun loop(n: Int, phase: Float = 0f): Float = ((t / 60f * n) + phase) % 1f

    // sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF080414), Color(0xFF14081E), Color(0xFF220C1A)), 0f, h))
    stars.forEach { st ->
      val tw = 0.5f + 0.5f * sin(tau * (t / 60f * (2 + (st[3] * 5).toInt()) + st[3]))
      drawCircle(Silver.copy(alpha = 0.15f + 0.5f * tw * st[3]), st[2], Offset(st[0] * w, st[1] * h))
    }
    // huge moon with a breathing halo
    val mc = Offset(w * 0.72f, h * 0.17f)
    val mr = w * 0.19f
    val halo = 1f + 0.05f * cyc(2)
    drawCircle(Brush.radialGradient(listOf(Silver.copy(alpha = 0.30f), Violet.copy(alpha = 0.12f), Color.Transparent), mc, mr * 3.4f * halo), mr * 3.4f * halo, mc)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3D6), Color(0xFFE6E8F4), Color(0xFFB7BED6)), mc - Offset(mr * 0.3f, mr * 0.3f), mr * 1.5f), mr, mc)
    listOf(Triple(-0.35f, -0.2f, 0.20f), Triple(0.3f, 0.25f, 0.14f), Triple(-0.1f, 0.4f, 0.10f), Triple(0.35f, -0.35f, 0.08f)).forEach { (dx, dy, rr) ->
      drawCircle(Color(0xFF8D94AE).copy(alpha = 0.32f), mr * rr, mc + Offset(mr * dx, mr * dy))
    }
    // thin clouds sliding across the moon
    for (i in 0 until 3) {
      val x = -w * 0.6f + loop(1, i * 0.33f) * w * 2.0f
      val y = h * (0.10f + i * 0.07f)
      drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF2A1840).copy(alpha = 0.55f), Color.Transparent), x, x + w * 0.9f), Offset(x, y), androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.04f))
    }
    // bats crossing the moon
    for (b in 0 until 3) {
      val p = loop(2 + b % 2, b * 0.31f)
      val bx = -60f + p * (w + 120f)
      val by = h * (0.08f + 0.07f * b) + sin(p * tau * 3f + b) * 20f
      val flap = sin(t * 14f + b * 2f) * 7f
      val sc = 0.8f + b * 0.25f
      val bat = Path().apply {
        moveTo(bx, by)
        quadraticTo(bx - 14f * sc, by - 12f * sc - flap, bx - 30f * sc, by + 2f * sc)
        quadraticTo(bx - 17f * sc, by - 2f * sc, bx - 8f * sc, by + 8f * sc)
        lineTo(bx, by + 4f * sc)
        lineTo(bx + 8f * sc, by + 8f * sc)
        quadraticTo(bx + 17f * sc, by - 2f * sc, bx + 30f * sc, by + 2f * sc)
        quadraticTo(bx + 14f * sc, by - 12f * sc - flap, bx, by)
      }
      drawPath(bat, Color.Black.copy(alpha = 0.92f))
    }
    // far mountains and hill
    val ground = h * 0.93f
    val far = Path().apply {
      moveTo(0f, ground); lineTo(0f, h * 0.78f)
      quadraticTo(w * 0.15f, h * 0.70f, w * 0.30f, h * 0.78f); quadraticTo(w * 0.45f, h * 0.72f, w * 0.60f, h * 0.80f)
      quadraticTo(w * 0.80f, h * 0.70f, w, h * 0.78f); lineTo(w, ground); close()
    }
    drawPath(far, Color(0xFF0B0615))
    // haunted castle on a hill with flickering windows
    val sil = Color(0xFF030108)
    val cx = w * 0.20f
    val base = h * 0.82f
    val castle = Path().apply {
      moveTo(cx - w * 0.20f, ground)
      lineTo(cx - w * 0.20f, base)
      quadraticTo(cx - w * 0.10f, base - h * 0.02f, cx - w * 0.07f, base - h * 0.03f)
      lineTo(cx - w * 0.07f, base - h * 0.10f); lineTo(cx - w * 0.055f, base - h * 0.13f); lineTo(cx - w * 0.04f, base - h * 0.10f)
      lineTo(cx - w * 0.04f, base - h * 0.06f)
      var x = cx - w * 0.04f
      while (x < cx + w * 0.07f) { lineTo(x, base - h * 0.06f); lineTo(x, base - h * 0.075f); lineTo(x + w * 0.012f, base - h * 0.075f); lineTo(x + w * 0.012f, base - h * 0.06f); x += w * 0.024f }
      lineTo(cx + w * 0.07f, base - h * 0.15f); lineTo(cx + w * 0.085f, base - h * 0.205f); lineTo(cx + w * 0.10f, base - h * 0.15f)
      lineTo(cx + w * 0.10f, base - h * 0.04f); lineTo(cx + w * 0.20f, base); lineTo(cx + w * 0.20f, ground); close()
    }
    drawPath(castle, sil)
    listOf(Offset(cx - w * 0.055f, base - h * 0.085f), Offset(cx + w * 0.085f, base - h * 0.125f), Offset(cx + w * 0.085f, base - h * 0.09f), Offset(cx, base - h * 0.04f)).forEachIndexed { i, o ->
      val fl = 0.6f + 0.4f * sin(tau * (t / 60f * (6 + i) + i * 0.2f))
      drawCircle(Orange.copy(alpha = 0.22f * fl), 14f, o)
      drawCircle(Color(0xFFFFB347).copy(alpha = 0.95f * fl), 4.2f, o)
    }
    // gnarled trees swaying in the wind
    for (k in 0 until 4) {
      val bx = w * (0.55f + k * 0.14f)
      val th = h * (0.13f + (k % 3) * 0.035f)
      val sway = cyc(3 + k % 2, k * 0.2f) * 7f
      val trunk = Path().apply { moveTo(bx - 7f, ground); quadraticTo(bx - 3f + sway * 0.3f, ground - th * 0.5f, bx + sway, ground - th); quadraticTo(bx + 3f + sway * 0.3f, ground - th * 0.5f, bx + 7f, ground); close() }
      drawPath(trunk, sil)
      val limbs = Path().apply {
        moveTo(bx + sway * 0.7f, ground - th * 0.75f); quadraticTo(bx - w * 0.04f + sway, ground - th * 0.85f, bx - w * 0.07f + sway * 1.3f, ground - th * 0.78f - 20f)
        moveTo(bx + sway * 0.6f, ground - th * 0.6f); quadraticTo(bx + w * 0.04f + sway, ground - th * 0.72f, bx + w * 0.075f + sway * 1.3f, ground - th * 0.62f - 26f)
        moveTo(bx + sway, ground - th); quadraticTo(bx + sway * 1.3f - 10f, ground - th - 26f, bx + sway * 1.5f - 22f, ground - th - 44f)
        moveTo(bx + sway, ground - th); quadraticTo(bx + sway * 1.3f + 12f, ground - th - 24f, bx + sway * 1.5f + 26f, ground - th - 40f)
      }
      drawPath(limbs, sil, style = Stroke(width = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
    drawRect(sil, Offset(0f, ground), androidx.compose.ui.geometry.Size(w, h - ground))
    // glowing jack-o-lanterns on the ground
    listOf(0.08f to 1.0f, 0.46f to 0.75f, 0.88f to 1.2f).forEachIndexed { i, (px, sc) ->
      val pc = Offset(w * px, ground + (h - ground) * 0.42f)
      val rr = w * 0.045f * sc
      val fl = 0.75f + 0.25f * sin(tau * (t / 60f * (9 + i * 2) + i))
      drawCircle(Brush.radialGradient(listOf(Orange.copy(alpha = 0.40f * fl), Color.Transparent), pc, rr * 3.4f), rr * 3.4f, pc)
      drawOval(Brush.radialGradient(listOf(Color(0xFFFF9A2E), Color(0xFFE2570C)), pc, rr * 1.3f), pc - Offset(rr * 1.15f, rr), androidx.compose.ui.geometry.Size(rr * 2.3f, rr * 2f))
      val face = Color(0xFF2A0A00).copy(alpha = 0.95f)
      listOf(-0.45f, 0.45f).forEach { dx ->
        val ey = Path().apply { moveTo(pc.x + rr * dx, pc.y - rr * 0.55f); lineTo(pc.x + rr * (dx - 0.2f), pc.y - rr * 0.1f); lineTo(pc.x + rr * (dx + 0.2f), pc.y - rr * 0.1f); close() }
        drawPath(ey, face)
      }
      val mouth = Path().apply { moveTo(pc.x - rr * 0.7f, pc.y + rr * 0.25f); for (z in 0..6) lineTo(pc.x - rr * 0.7f + z * rr * 0.233f, pc.y + rr * (if (z % 2 == 0) 0.25f else 0.62f)); lineTo(pc.x + rr * 0.7f, pc.y + rr * 0.8f); lineTo(pc.x - rr * 0.7f, pc.y + rr * 0.8f); close() }
      drawPath(mouth, face)
      drawRect(Color(0xFF3B5A1E), pc + Offset(-rr * 0.08f, -rr * 1.25f), androidx.compose.ui.geometry.Size(rr * 0.16f, rr * 0.28f))
    }
    // two parallax fog layers
    for (i in 0 until 4) {
      val dir = if (i % 2 == 0) 1f else -1f
      val x = -w * 0.6f + (if (dir > 0) loop(1, i * 0.25f) else 1f - loop(1, i * 0.25f)) * w * 1.6f
      val y = ground - h * (0.02f + i * 0.045f)
      drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFB9A7E8).copy(alpha = 0.11f - i * 0.012f), Color.Transparent), x, x + w), Offset(x, y), androidx.compose.ui.geometry.Size(w, h * 0.08f))
    }
    // will-o-wisps drifting in the mist
    wisps.forEach { ws ->
      val wx = (ws[0] + 0.06f * sin(tau * (t / 60f * ws[3] + ws[2]))) * w
      val wy = ws[1] * h + 14f * sin(tau * (t / 60f * (ws[3] + 1) + ws[2] * 2f))
      val a = 0.35f + 0.35f * sin(tau * (t / 60f * (3 + ws[3]) + ws[2]))
      drawCircle(Brush.radialGradient(listOf(Color(0xFF9DFFB8).copy(alpha = 0.30f * a), Color.Transparent), Offset(wx, wy), 26f), 26f, Offset(wx, wy))
      drawCircle(Color(0xFFD8FFE4).copy(alpha = 0.85f * a), 2.2f, Offset(wx, wy))
    }
    // embers rising from the ground
    embers.forEach { e ->
      val ph = (t / 60f * (4 + (e[2] * 6).toInt()) + e[3]) % 1f
      val ex = (e[0] + sin((ph + e[3]) * tau) * 0.02f) * w
      val ey = ground - ph * h * 0.75f
      drawCircle((if (e[3] > 0.5f) Orange else Violet).copy(alpha = (1f - ph) * 0.7f), 1.5f + e[2] * 1.8f, Offset(ex, ey))
    }
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
      state = "opening"; HalloweenState.phase = HalloweenPhase.CapsuleOpening
      split.snapTo(0f)
      split.animateTo(1f, tween(if (still) 200 else 800, easing = FastOutSlowInEasing))
      if (pool.size < 2) pool.addAll(capsuleSongs(lang, seen + skipped))
      val next = pool.firstOrNull { it.id !in seen && it.id !in skipped }
      if (next == null) { state = "empty"; song = null } else { pool.remove(next); seen += next.id; song = next; HomeCache.put("capsule_song", next); state = "shown" }
      HalloweenState.phase = HalloweenPhase.Idle
      busy = false
    }
  }

  Column(
    modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
      .glassSurface(RoundedCornerShape(26.dp), if (halloween) Color(0xFF2A0F0A) else Color.White)
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
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
      .glassSurface(RoundedCornerShape(22.dp), if (halloween) Color(0xFF1B1030) else Color.White)
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
        if (list.none { it.id == e.song.id } && list.size < 30) list += e.song
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
              YouTube.search("${l.query} $q", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(14)
                .map { FoldSong(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnail, null, it) }
            }
          }
        }.awaitAll().filter { it.second.isNotEmpty() }
      }
      if (value.isNotEmpty()) HomeCache.put(key, value)
    }
    val groups = if (selectedLang == null) history else eraSongs
    AlertDialog(
      onDismissRequest = { open = false; HalloweenState.phase = HalloweenPhase.Idle },
      shape = RoundedCornerShape(28.dp),
      title = { Text("Time Fold") },
      text = {
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
                LazyRow(state = ls, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 70.dp), horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                  items(groups[i].second, key = { "tf_${lang}_${groups[i].first}_${it.id}" }) { s ->
                    val itemKey = "tf_${lang}_${groups[i].first}_${s.id}"
                    Column(
                      Modifier.size(width = 130.dp, height = 176.dp)
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
                      AsyncImage(model = s.thumb?.resize(360, 360), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(130.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Brush.linearGradient(listOf(Orange.copy(alpha = 0.7f), Violet.copy(alpha = 0.7f))), RoundedCornerShape(18.dp)))
                      Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                      Text(s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
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

private class FoldSong(val id: String, val title: String, val artist: String, val thumb: String?, val meta: echo.music.iad1tya.models.MediaMetadata?, val item: SongItem? = null)

@Composable
private fun FoldChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) Violet.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp) }
}

// ---------------------------------------------------------------------------------------------
// 6. Seasonal collections: only shown when real songs were found
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenCollections(refreshKey: Any?) {
  val (langRaw, _) = rememberPreference(MusicLanguagesKey, "")
  val lang = remember(langRaw) { parseMusicLanguages(langRaw).firstOrNull()?.query.orEmpty() }
  val rows by
    produceState<List<Pair<String, List<SongItem>>>>(HomeCache.get("halloween|$lang|$refreshKey") ?: emptyList(), lang, refreshKey) {
      HomeCache.get<List<Pair<String, List<SongItem>>>>("halloween|$lang|$refreshKey")?.let { value = it; return@produceState }
      val names = listOf("Midnight Drive" to "midnight drive", "Haunted Frequencies" to "dark ambient eerie", "Dark Romance" to "dark romantic", "Moonlight Chill" to "moonlight chill", "Ghostly Echoes" to "echo reverb slow", "After Midnight" to "after midnight", "Halloween Mix" to "halloween spooky")
      value = coroutineScope { names.shuffled().take(4).map { (n, q) -> async { n to withContext(Dispatchers.IO) { YouTube.search("$lang $q".trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(14) } } }.awaitAll().filter { it.second.isNotEmpty() } }
      if (value.isNotEmpty()) HomeCache.put("halloween|$lang|$refreshKey", value)
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

// ---------------------------------------------------------------------------------------------
// 9. App-wide Halloween atmosphere: sits above every screen, never takes touches
// ---------------------------------------------------------------------------------------------
@Composable
fun HalloweenGlobalOverlay() {
  if (!rememberHalloweenOn()) return
  val (reducedPref, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val still = reducedPref || rememberReduceMotion()
  val t: State<Float> =
    if (still) remember { mutableStateOf(0.3f) }
    else rememberInfiniteTransition(label = "globalhaunt").animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "gt")
  val sparks = remember { List(if (still) 10 else 22) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat(), Random.nextFloat()) } }
  Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    // cobwebs in the top corners
    val web = Silver.copy(alpha = 0.16f)
    for (corner in 0..1) {
      val ox = if (corner == 0) 0f else w
      val dir = if (corner == 0) 1f else -1f
      val r = w * 0.20f
      for (k in 0..4) {
        val ang = (k * 22.5f) * (PI.toFloat() / 180f)
        drawLine(web, Offset(ox, 0f), Offset(ox + dir * cos(ang) * r, sin(ang) * r), strokeWidth = 1.2f)
      }
      for (ring in 1..3) {
        val rr = r * ring / 3f
        val path = Path()
        for (k in 0..4) {
          val ang = (k * 22.5f) * (PI.toFloat() / 180f)
          val x = ox + dir * cos(ang) * rr
          val y = sin(ang) * rr
          if (k == 0) path.moveTo(x, y) else path.quadraticTo(ox + dir * cos(ang - 0.2f) * rr * 0.85f, sin(ang - 0.2f) * rr * 0.85f, x, y)
        }
        drawPath(path, web, style = Stroke(width = 1f))
      }
    }
    // rising embers and violet sparks
    sparks.forEach { e ->
      val ph = (t.value * (0.6f + e[2]) * 2f + e[3]) % 1f
      val x = (e[0] + sin((ph + e[3]) * 6.283f) * 0.03f) * w
      val y = h - ph * h
      drawCircle((if (e[3] > 0.5f) Orange else Violet).copy(alpha = (1f - ph) * 0.35f), 1.8f + e[2] * 1.4f, Offset(x, y))
    }
    // a bat crossing the top now and then
    if (!still) {
      val f = (t.value * 2f) % 1f
      if (f in 0.05f..0.35f) {
        val p = (f - 0.05f) / 0.3f
        val bx = -40f + p * (w + 80f)
        val by = h * 0.08f + sin(p * 9f) * 18f + p * h * 0.04f
        val flap = sin(p * 60f) * 6f
        val bat = Path().apply {
          moveTo(bx, by)
          quadraticTo(bx - 14f, by - 10f - flap, bx - 26f, by + 2f)
          quadraticTo(bx - 14f, by - 2f, bx, by + 6f)
          quadraticTo(bx + 14f, by - 2f, bx + 26f, by + 2f)
          quadraticTo(bx + 14f, by - 10f - flap, bx, by)
        }
        drawPath(bat, Color.Black.copy(alpha = 0.75f))
      }
    }
  }
}
