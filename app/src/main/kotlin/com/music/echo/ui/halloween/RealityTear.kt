package echo.music.iad1tya.ui.halloween

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.ui.buddy.BuddyLook
import echo.music.iad1tya.ui.buddy.BuddyStageAnchor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private class TearSession(
  val id: String,
  val bitmap: Bitmap?,
  val colors: List<Color>,
  val origin: Offset,
  val title: String,
  val artist: String,
)

/**
 * Haunted Reality Tear + Soul Particles. Watches the existing player for a song the user just opened (a touch
 * within the last 2.5 s) and plays a short overlay: crack, portal, particles forming the song's real artwork,
 * then a light pulse. It only draws. It takes no touches, never touches playback and always removes itself.
 * Reduced motion = soft fade; low-end = dissolve and glow without particles.
 */
@Composable
fun HauntedPortalOverlay(look: BuddyLook) {
  val fx = rememberHwFx()
  val conn = LocalPlayerConnection.current
  val context = LocalContext.current
  val visible = rememberAppVisible()
  val meta = conn?.mediaMetadata?.collectAsState()?.value
  var lastId by remember { mutableStateOf(meta?.id) }
  var session by remember { mutableStateOf<TearSession?>(null) }
  val prog = remember { Animatable(0f) }

  LaunchedEffect(meta?.id, fx.on) {
    val m = meta ?: return@LaunchedEffect
    if (m.id == lastId) return@LaunchedEffect
    lastId = m.id
    if (!fx.on || !visible) return@LaunchedEffect
    val ping = look.ping ?: return@LaunchedEffect
    if (System.nanoTime() - ping.id > 2_500_000_000L) return@LaunchedEffect // auto-advance, not a tap
    val bmp =
      withTimeoutOrNull(700) {
        withContext(Dispatchers.IO) {
          runCatching {
              val r = context.imageLoader.execute(ImageRequest.Builder(context).data(m.thumbnailUrl).size(420, 420).allowHardware(false).build())
              (r as? SuccessResult)?.image?.toBitmap()
            }
            .getOrNull()
        }
      }
    val colors =
      bmp?.let {
        withContext(Dispatchers.Default) {
          runCatching {
              val p = Palette.from(it).maximumColorCount(12).generate()
              listOfNotNull(p.vibrantSwatch?.rgb, p.dominantSwatch?.rgb, p.lightVibrantSwatch?.rgb, p.mutedSwatch?.rgb).map { c -> Color(c) }
            }
            .getOrDefault(emptyList())
        }
      }.orEmpty()
    session = TearSession(m.id, bmp, colors.ifEmpty { listOf(HW.Orange, HW.Violet) }, ping.pos, m.title, m.artists.joinToString { it.name })
  }

  LaunchedEffect(session) {
    val s = session ?: return@LaunchedEffect
    val total = if (fx.reduced) 420 else if (fx.lowEnd) 800 else 1300
    try {
      if (fx.sound) HauntedSound.portal(context)
      prog.snapTo(0f)
      HauntedStage.enter(StagePhase.RealityTear)
      prog.animateTo(0.45f, tween((total * 0.45f).toInt(), easing = LinearEasing))
      HauntedStage.set(StagePhase.ParticleFormation)
      prog.animateTo(0.85f, tween((total * 0.40f).toInt(), easing = LinearEasing))
      HauntedStage.set(StagePhase.PlayerArrival)
      prog.animateTo(1f, tween((total * 0.15f).toInt(), easing = LinearEasing))
    } finally {
      // completion and cancellation both release everything
      HauntedStage.release(StagePhase.RealityTear)
      if (session === s) session = null
    }
  }

  val s = session ?: return
  Canvas(Modifier.fillMaxSize()) { drawTear(s, prog.value, fx) }
}

private fun DrawScope.drawTear(s: TearSession, p: Float, fx: HwFx) {
  val w = size.width
  val h = size.height
  val fade = 1f - seg(p, 0.93f, 1f)
  val anchor = BuddyStageAnchor.rect
  val target =
    if (anchor != null && anchor.width > 80f && anchor.bottom in 1f..h) anchor
    else {
      val side = min(w * 0.78f, h * 0.40f)
      Rect(Offset((w - side) / 2f, h * 0.16f), Size(side, side))
    }
  val o = Offset(s.origin.x.coerceIn(0f, w), s.origin.y.coerceIn(0f, h))
  val srcSide = w * 0.28f
  val src = Rect(o - Offset(srcSide / 2f, srcSide / 2f), Size(srcSide, srcSide))
  val move = ease(seg(p, 0.40f, 0.90f))
  val rect = Rect(
    Offset(src.left + (target.left - src.left) * move, src.top + (target.top - src.top) * move),
    Size(src.width + (target.width - src.width) * move, src.height + (target.height - src.height) * move),
  )
  val c1 = s.colors[0]
  val c2 = s.colors.getOrElse(1) { HW.Violet }

  // --- reduced motion: a soft fade of the artwork and a faint glow
  if (fx.reduced) {
    val a = (if (p < 0.5f) ease(seg(p, 0f, 0.4f)) else 1f - ease(seg(p, 0.6f, 1f))) * 0.9f
    drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = 0.35f * a), Color.Transparent), rect.center, target.width), target.width, rect.center)
    s.bitmap?.let { drawArt(it, target, a, 1f) }
    return
  }

  // dim around the song
  val dim = 0.5f * ease(seg(p, 0f, 0.15f)) * (1f - ease(seg(p, 0.6f, 0.9f))) * fade
  drawRect(Color.Black.copy(alpha = dim))

  // Phase 1: contact ripple, glow and crack
  val contact = ease(seg(p, 0f, 0.2f))
  if (p < 0.5f) {
    val ra = (1f - seg(p, 0.05f, 0.35f)) * 0.7f
    drawCircle(HW.Orange.copy(alpha = ra), 24f + 150f * contact, o, style = Stroke(3f))
    drawCircle(Brush.radialGradient(listOf(HW.Orange.copy(alpha = 0.35f * (1f - seg(p, 0.2f, 0.45f))), Color.Transparent), o, srcSide), srcSide, o)
  }
  val rnd = Random(s.id.hashCode())
  val crackLen = ease(seg(p, 0.05f, 0.3f))
  if (crackLen > 0f && p < 0.62f) {
    val path = Path().apply {
      var x = o.x - w * 0.18f * crackLen
      var y = o.y - srcSide * 0.6f * crackLen
      moveTo(x, y)
      val steps = 8
      for (i in 1..steps) {
        x += w * 0.36f * crackLen / steps
        y += srcSide * 1.2f * crackLen / steps + (rnd.nextFloat() - 0.5f) * 36f * crackLen
        lineTo(x, y)
      }
    }
    val ca = (1f - seg(p, 0.45f, 0.62f))
    drawPath(path, HW.Violet.copy(alpha = 0.35f * ca), style = Stroke(9f))
    drawPath(path, HW.Orange.copy(alpha = 0.95f * ca), style = Stroke(3f))
  }

  // Phase 2: curved portal opening with the real artwork inside, glass shards and fog
  val open = ease(seg(p, 0.15f, 0.5f))
  if (open > 0f) {
    val pc = Offset(o.x + (target.center.x - o.x) * ease(seg(p, 0.2f, 0.6f)), o.y + (target.center.y - o.y) * ease(seg(p, 0.2f, 0.6f)))
    val rx = 10f + (w * 0.60f - 10f) * open * (1f - 0.55f * ease(seg(p, 0.6f, 0.9f)))
    val ry = rx * 1.25f
    val oval = Path().apply { addOval(Rect(pc.x - rx, pc.y - ry, pc.x + rx, pc.y + ry)) }
    val pa = (1f - ease(seg(p, 0.62f, 0.88f))) * fade
    clipPath(oval) {
      drawRect(Brush.radialGradient(listOf(Color(0xFF1B0F2E).copy(alpha = pa), Color.Black.copy(alpha = pa)), pc, rx.coerceAtLeast(1f)))
      s.bitmap?.let { drawArt(it, Rect(pc.x - rx * 0.7f, pc.y - rx * 0.7f, pc.x + rx * 0.7f, pc.y + rx * 0.7f), 0.75f * pa, 1f) }
      drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFB9A7E8).copy(alpha = 0.28f * pa)), pc.y, pc.y + ry))
    }
    drawPath(oval, HW.Violet.copy(alpha = 0.45f * pa), style = Stroke(10f))
    drawPath(oval, HW.Orange.copy(alpha = 0.9f * pa), style = Stroke(2.5f))
    if (!fx.lowEnd) {
      val outward = ease(seg(p, 0.2f, 0.65f))
      for (i in 0 until 10) {
        val ang = (i / 10f) * 2f * PI.toFloat() + rnd.nextFloat()
        val d = (rx * 0.8f + rx * 0.45f * outward)
        val q = Offset(pc.x + cos(ang) * d, pc.y + sin(ang) * d * 1.25f)
        val sz = 8f + rnd.nextFloat() * 12f
        val shard = Path().apply { moveTo(q.x, q.y - sz); lineTo(q.x + sz * 0.7f, q.y + sz * 0.6f); lineTo(q.x - sz * 0.6f, q.y + sz * 0.5f); close() }
        val sa = (1f - seg(p, 0.45f, 0.7f)) * 0.9f
        drawPath(shard, Color(0xFF15101F).copy(alpha = sa))
        drawPath(shard, HW.Orange.copy(alpha = sa), style = Stroke(1.5f))
      }
    }
  }

  // Phase 3: soul particles spiral in and form the artwork from the centre outward
  val form = seg(p, 0.40f, 0.85f)
  val n = (110 * fx.particleScale).toInt()
  if (n > 0 && s.bitmap != null) {
    val cc = target.center
    val c = ease(form)
    for (i in 0 until n) {
      val r = Random(i * 977 + s.id.hashCode())
      val layer = 0.5f + r.nextFloat() * 0.5f
      val a0 = r.nextFloat() * 2f * PI.toFloat()
      val r0 = (0.35f + r.nextFloat() * 0.75f) * w * 0.5f
      val ang = a0 + c * 4.2f * layer
      val rad = r0 * (1f - c)
      val spiral = Offset(cc.x + cos(ang) * rad, cc.y + sin(ang) * rad * 0.8f)
      val tgt = Offset(target.left + r.nextFloat() * target.width, target.top + r.nextFloat() * target.height)
      val pos = Offset(spiral.x + (tgt.x - spiral.x) * ease(seg(c, 0.6f, 1f)), spiral.y + (tgt.y - spiral.y) * ease(seg(c, 0.6f, 1f)))
      val al = (sin(c * PI.toFloat()).coerceIn(0f, 1f)) * 0.85f * fade * layer
      drawCircle(s.colors[i % s.colors.size].copy(alpha = al), 1.5f + 2.5f * layer, pos)
    }
    if (form in 0.05f..0.95f) {
      drawCircle(Brush.sweepGradient(listOf(HW.Violet.copy(alpha = 0f), HW.Orange.copy(alpha = 0.6f), HW.Violet.copy(alpha = 0f)), cc), target.width * 0.55f * (1f - 0.35f * c), cc, style = Stroke(2f))
    }
  }

  // the exact artwork resolves from its centre toward its edges, then settles
  s.bitmap?.let { bmp ->
    val reveal = ease(seg(p, 0.48f, 0.85f))
    if (reveal > 0f) {
      val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(rect.width * 0.08f))) }
      val circle = Path().apply { addOval(Rect(rect.center, reveal * hypot(rect.width, rect.height) / 2f)) }
      clipPath(clip) { clipPath(circle) { drawArt(bmp, rect, fade, 1f) } }
    }
  }

  // Phase 4: spectral outline, restrained light pulse, title rising in
  val pulse = sin(PI.toFloat() * seg(p, 0.8f, 1f))
  if (p > 0.8f) {
    drawRoundRect(HW.Silver.copy(alpha = 0.7f * pulse * fade), rect.topLeft, rect.size, CornerRadius(rect.width * 0.08f), style = Stroke(2f))
    drawCircle(Brush.radialGradient(listOf(c2.copy(alpha = 0.22f * pulse), HW.Orange.copy(alpha = 0.10f * pulse), Color.Transparent), rect.center, rect.width), rect.width, rect.center)
  }
  val ta = ease(seg(p, 0.78f, 0.95f)) * fade
  if (ta > 0f && s.title.isNotBlank()) {
    drawIntoCanvas { cv ->
      val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.copy(alpha = ta).toArgb(); textSize = 17f * density * 1.0f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
        setShadowLayer(8f, 0f, 2f, Color.Black.toArgb())
      }
      val y = rect.bottom + 34f * density * 0.9f + (1f - ta) * 14f
      cv.nativeCanvas.drawText(s.title.take(38), rect.center.x, y, paint)
      paint.textSize = 13f * density; paint.isFakeBoldText = false; paint.color = HW.Silver.copy(alpha = ta * 0.85f).toArgb()
      cv.nativeCanvas.drawText(s.artist.take(42), rect.center.x, y + 20f * density, paint)
    }
  }
}

private fun DrawScope.drawArt(bmp: Bitmap, dst: Rect, alpha: Float, @Suppress("UNUSED_PARAMETER") scale: Float) {
  val side = min(bmp.width, bmp.height)
  drawImage(
    bmp.asImageBitmap(),
    srcOffset = IntOffset((bmp.width - side) / 2, (bmp.height - side) / 2),
    srcSize = IntSize(side, side),
    dstOffset = IntOffset(dst.left.toInt(), dst.top.toInt()),
    dstSize = IntSize(dst.width.toInt().coerceAtLeast(1), dst.height.toInt().coerceAtLeast(1)),
    alpha = alpha.coerceIn(0f, 1f),
  )
}
