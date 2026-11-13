package echo.music.iad1tya.ui.component

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import echo.music.iad1tya.constants.EasterEggsFoundKey
import echo.music.iad1tya.constants.EffectKindKey
import echo.music.iad1tya.constants.EffectTouchKey
import echo.music.iad1tya.constants.FrostEdgesKey
import echo.music.iad1tya.constants.SnowIntensityKey
import echo.music.iad1tya.constants.SnowModeKey
import echo.music.iad1tya.constants.WaterFillKey
import echo.music.iad1tya.constants.WinterEnabledKey
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.constants.WinterVariantKey
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.utils.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Variants (Frozen, Aurora, Midnight, Snow White, Christmas, New Year)
// ---------------------------------------------------------------------------------------------
enum class WinterVariant(val key: String, val label: String, val seed: Color, val sky: List<Color>, val bands: List<Color>) {
  FROZEN("frozen", "Frozen", Color(0xFF58B8FF), listOf(Color(0xFF030A1C), Color(0xFF0A2347), Color(0xFF14406E)), listOf(Color(0xFF7FE3FF), Color(0xFF8AB6FF), Color(0xFFBFF3FF))),
  AURORA("aurora", "Aurora", Color(0xFF35E0B5), listOf(Color(0xFF020B16), Color(0xFF062235), Color(0xFF0B3B3F)), listOf(Color(0xFF3DFFB4), Color(0xFF4DB8FF), Color(0xFFB070FF))),
  MIDNIGHT("midnight", "Midnight", Color(0xFF4A63FF), listOf(Color(0xFF01030D), Color(0xFF060C27), Color(0xFF0E1745)), listOf(Color(0xFF5B7BFF), Color(0xFF8F6BFF), Color(0xFF4DD0FF))),
  SNOW_WHITE("snowwhite", "Snow White", Color(0xFFD6EEFF), listOf(Color(0xFF0E1B33), Color(0xFF1D3358), Color(0xFF39608F)), listOf(Color(0xFFFFFFFF), Color(0xFFD6EEFF), Color(0xFFB9DCFF))),
  CHRISTMAS("christmas", "Christmas", Color(0xFFE0394F), listOf(Color(0xFF07130F), Color(0xFF0F2A1E), Color(0xFF3A1018)), listOf(Color(0xFFFF5468), Color(0xFF3FD07A), Color(0xFFFFE08A))),
  NEW_YEAR("newyear", "New Year", Color(0xFFFFC94D), listOf(Color(0xFF05060F), Color(0xFF14123A), Color(0xFF2D1B4F)), listOf(Color(0xFFFFD36B), Color(0xFFFF7BD0), Color(0xFF7BD8FF)));

  companion object {
    fun from(key: String) = entries.firstOrNull { it.key == key } ?: FROZEN
  }
}

@Composable
fun rememberWinterVariant(): WinterVariant {
  val (key, _) = rememberPreference(WinterVariantKey, "frozen")
  return WinterVariant.from(key)
}

/** Latest touch, written by an observer that never consumes events. */
object WinterTouch {
  var x by mutableFloatStateOf(-1000f)
  var y by mutableFloatStateOf(-1000f)
  var down by mutableStateOf(false)
  var downTick by mutableIntStateOf(0)
}

/** Watches touches without consuming them, so flakes can react and hidden flakes can be found. */
fun Modifier.winterTouchObserver(): Modifier =
  this.pointerInput(Unit) {
    awaitPointerEventScope {
      while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull() ?: continue
        WinterTouch.x = c.position.x
        WinterTouch.y = c.position.y
        val nowDown = c.pressed
        if (nowDown && !c.previousPressed) WinterTouch.downTick++
        WinterTouch.down = nowDown
      }
    }
  }

@Composable
private fun rememberAppResumed(): Boolean {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
  DisposableEffect(lifecycle) {
    val obs = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
    lifecycle.addObserver(obs)
    onDispose { lifecycle.removeObserver(obs) }
  }
  return resumed
}

// ---------------------------------------------------------------------------------------------
// Particle overlay: snow, fire sparks, falling leaves, spin and water drops. Pauses in background.
// ---------------------------------------------------------------------------------------------
private class P(
  var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float,
  var rot: Float, var vrot: Float, var phase: Float, var egg: Boolean = false, var hue: Int = 0,
)

private val LeafColors = listOf(Color(0xFFE8892B), Color(0xFFC9541C), Color(0xFFE2B33C), Color(0xFF9C3A1A), Color(0xFF8FA83A))

private fun spawn(kind: String, w: Float, h: Float, anywhere: Boolean, d: Float, storm: Boolean): P {
  val r = Random
  val x = r.nextFloat() * w
  return when (kind) {
    "fire" -> P(x, if (anywhere) r.nextFloat() * h else h + 10f, (r.nextFloat() - 0.5f) * 30f * d, -(60f + r.nextFloat() * 150f) * d, (1.2f + r.nextFloat() * 2.8f) * d, 0f, 0f, r.nextFloat() * 6.28f, hue = r.nextInt(3))
    "leaves" -> P(x, if (anywhere) r.nextFloat() * h else -30f, (r.nextFloat() - 0.3f) * 25f * d, (40f + r.nextFloat() * 50f) * d, (7f + r.nextFloat() * 8f) * d, r.nextFloat() * 6.28f, (r.nextFloat() - 0.5f) * 3f, r.nextFloat() * 6.28f, hue = r.nextInt(LeafColors.size))
    "spin" -> P(x, if (anywhere) r.nextFloat() * h else -20f, (r.nextFloat() - 0.5f) * 20f * d, (30f + r.nextFloat() * 60f) * d, (6f + r.nextFloat() * 10f) * d, r.nextFloat() * 6.28f, (2f + r.nextFloat() * 5f) * (if (r.nextBoolean()) 1 else -1), r.nextFloat() * 6.28f, hue = r.nextInt(3))
    "water" -> P(x, if (anywhere) r.nextFloat() * h else -20f, 0f, (160f + r.nextFloat() * 260f) * d, (1.5f + r.nextFloat() * 2.5f) * d, 0f, 0f, r.nextFloat() * 6.28f)
    else -> { // snow
      val big = r.nextFloat()
      val size = (if (big > 0.85f) 5.5f + r.nextFloat() * 3f else 1.6f + r.nextFloat() * 3.2f) * d
      P(x, if (anywhere) r.nextFloat() * h else -10f, (r.nextFloat() - 0.5f) * 14f * d + (if (storm) 90f * d else 0f), (28f + size * 9f) * (if (storm) 2.1f else 1f) * d, size, r.nextFloat() * 6.28f, (r.nextFloat() - 0.5f) * 2f, r.nextFloat() * 6.28f)
    }
  }
}

@Composable
fun WinterEffectsOverlay() {
  val (winterOn, _) = rememberPreference(WinterEnabledKey, true)
  val (kind, _) = rememberPreference(EffectKindKey, "snow")
  val (intensity, _) = rememberPreference(SnowIntensityKey, 60)
  val (mode, _) = rememberPreference(SnowModeKey, "gentle")
  val (lowPerf, _) = rememberPreference(WinterLowPerfKey, false)
  val (touchOn, _) = rememberPreference(EffectTouchKey, true)
  val (frost, _) = rememberPreference(FrostEdgesKey, true)
  val (eggs, setEggs) = rememberPreference(EasterEggsFoundKey, 0)
  val variant = rememberWinterVariant()
  val reduced = rememberReduceMotion()
  val resumed = rememberAppResumed()
  val context = LocalContext.current
  var size by remember { mutableStateOf(IntSize.Zero) }
  val effectKind = if (!winterOn) "off" else kind
  val storm = mode == "storm" && effectKind == "snow"
  val perfScale = if (lowPerf || reduced) 0.35f else 1f
  val base = when (effectKind) { "snow" -> 120; "fire" -> 70; "leaves" -> 36; "spin" -> 34; "water" -> 90; else -> 0 }
  val count = (base * (intensity.coerceIn(5, 100) / 60f) * perfScale * (if (storm) 2.2f else 1f)).toInt().coerceIn(0, 420)
  val d = LocalDensity.current.density
  val particles = remember(effectKind, count, size, storm) {
    if (size.width == 0 || count == 0) emptyList<P>()
    else List(count) { spawn(effectKind, size.width.toFloat(), size.height.toFloat(), true, d, storm) }
  }
  // hidden golden snowflakes (Easter eggs) live among the snow
  val golden = remember(effectKind, size) {
    if (effectKind == "snow" && size.width > 0) List(2) { P(Random.nextFloat() * size.width, Random.nextFloat() * size.height, 6f * d, 38f * d, 7f * d, 0f, 0.8f, Random.nextFloat() * 6f, egg = true) } else emptyList()
  }
  var frame by remember { mutableLongStateOf(0L) }
  var clock by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(resumed, particles, reduced, touchOn) {
    if (!resumed || reduced || (particles.isEmpty() && golden.isEmpty())) return@LaunchedEffect
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    var last = 0L
    var acc = 0f
    val step = if (lowPerf) 1f / 30f else 0f
    while (true) {
      androidx.compose.runtime.withFrameNanos { t ->
        if (last == 0L) last = t
        acc += ((t - last) / 1e9f).coerceAtMost(0.05f)
        last = t
        if (acc < step) return@withFrameNanos
        val dt = acc
        acc = 0f
        clock += dt
        val wind = sin(clock * 0.35f) * (if (storm) 120f else 22f) * d
        val tx = WinterTouch.x
        val ty = WinterTouch.y
        val pushing = touchOn && WinterTouch.down
        fun update(p: P) {
          when (effectKind) {
            "fire" -> { p.x += (p.vx + sin(clock * 3f + p.phase) * 18f * d) * dt; p.y += p.vy * dt; if (p.y < -10f) { p.y = h + 10f; p.x = Random.nextFloat() * w } }
            "water" -> { p.y += p.vy * dt; if (p.y > h + 20f) { p.y = -20f; p.x = Random.nextFloat() * w } }
            else -> {
              p.x += (p.vx + wind + sin(clock * 1.2f + p.phase) * 18f * d) * dt
              p.y += p.vy * dt
              p.rot += p.vrot * dt
              if (p.y > h + 20f) { p.y = -20f; p.x = Random.nextFloat() * w }
              if (p.x > w + 30f) p.x = -20f
              if (p.x < -30f) p.x = w + 20f
            }
          }
          if (pushing) {
            val dx = p.x - tx
            val dy = p.y - ty
            val dist = hypot(dx, dy)
            if (dist < 170f * d && dist > 1f) { val f = (1f - dist / (170f * d)) * 420f * d * dt; p.x += dx / dist * f; p.y += dy / dist * f }
          }
        }
        particles.forEach(::update)
        golden.forEach(::update)
        frame = t
      }
    }
  }

  // Easter eggs: tap a golden flake
  LaunchedEffect(WinterTouch.downTick) {
    if (golden.isEmpty() || WinterTouch.downTick == 0) return@LaunchedEffect
    golden.forEach { g ->
      if (hypot(g.x - WinterTouch.x, g.y - WinterTouch.y) < 46f * d) {
        val n = eggs + 1
        setEggs(n)
        Toast.makeText(context, "Hidden snowflake found  $n", Toast.LENGTH_SHORT).show()
        g.y = -40f; g.x = Random.nextFloat() * size.width
      }
    }
  }

  Canvas(Modifier.fillMaxSize().onSizeChanged { size = it }) {
    @Suppress("UNUSED_EXPRESSION") frame
    if (frost && winterOn) drawFrost(variant)
    when (effectKind) {
      "water" -> drawWaterMist(clock, particles)
      else -> particles.forEach { drawParticle(effectKind, it, clock, variant) }
    }
    golden.forEach { drawSnowflake(Offset(it.x, it.y), it.size, it.rot, Color(0xFFFFE08A).copy(alpha = 0.55f), 1.6f) }
  }
}

private fun DrawScope.drawSnowflake(c: Offset, r: Float, rot: Float, color: Color, stroke: Float) {
  rotate(rot * 57.3f, c) {
    for (i in 0 until 3) {
      val a = i * (PI.toFloat() / 3f)
      drawLine(color, Offset(c.x - cos(a) * r, c.y - sin(a) * r), Offset(c.x + cos(a) * r, c.y + sin(a) * r), strokeWidth = stroke, cap = StrokeCap.Round)
    }
  }
}

private fun DrawScope.drawParticle(kind: String, p: P, clock: Float, v: WinterVariant) {
  val c = Offset(p.x, p.y)
  when (kind) {
    "fire" -> {
      val life = 1f - (p.y / size.height).coerceIn(0f, 1f)
      val col = when (p.hue) { 0 -> Color(0xFFFF8A1F); 1 -> Color(0xFFFFC23D); else -> Color(0xFFFF4D1A) }
      val a = (0.25f + 0.75f * (1f - life)) * (0.6f + 0.4f * sin(clock * 9f + p.phase))
      drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a), Color.Transparent), c, p.size * 3.2f), p.size * 3.2f, c)
      drawCircle(Color.White.copy(alpha = a * 0.8f), p.size * 0.5f, c)
    }
    "leaves" -> rotate(p.rot * 57.3f, c) {
      val path = Path().apply {
        moveTo(c.x, c.y - p.size)
        quadraticTo(c.x + p.size * 0.9f, c.y, c.x, c.y + p.size)
        quadraticTo(c.x - p.size * 0.9f, c.y, c.x, c.y - p.size)
      }
      drawPath(path, LeafColors[p.hue].copy(alpha = 0.85f))
      drawLine(Color(0x66401E0A), Offset(c.x, c.y - p.size), Offset(c.x, c.y + p.size), strokeWidth = 1f)
    }
    "spin" -> rotate(p.rot * 57.3f, c) {
      val col = v.bands[p.hue % v.bands.size].copy(alpha = 0.8f)
      for (i in 0 until 4) {
        val a = i * (PI.toFloat() / 2f)
        val tip = Offset(c.x + cos(a) * p.size, c.y + sin(a) * p.size)
        val side = Offset(c.x + cos(a + 0.6f) * p.size * 0.45f, c.y + sin(a + 0.6f) * p.size * 0.45f)
        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(side.x, side.y); lineTo(tip.x, tip.y); close() }, col)
      }
      drawCircle(Color.White.copy(alpha = 0.9f), p.size * 0.14f, c)
    }
    else -> {
      val a = 0.55f + 0.4f * (p.size / (9f * 2f)).coerceAtMost(0.5f)
      if (p.size > 5f) drawSnowflake(c, p.size, p.rot, Color.White.copy(alpha = 0.85f), 1.4f)
      else drawCircle(Color.White.copy(alpha = a), p.size, c)
    }
  }
}

private fun DrawScope.drawWaterMist(clock: Float, drops: List<P>) {
  val w = size.width
  val h = size.height
  // cloudy fog: slow drifting soft blobs
  for (i in 0 until 5) {
    val cx = ((i * 0.27f + clock * 0.012f * (1 + i % 2)) % 1.3f - 0.15f) * w
    val cy = h * (0.12f + 0.2f * i)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFCFE6FF).copy(alpha = 0.10f), Color.Transparent), Offset(cx, cy), w * 0.55f), w * 0.55f, Offset(cx, cy))
  }
  drawRect(Color(0xFFBFDFFF).copy(alpha = 0.035f))
  drops.forEach { p ->
    val c = Offset(p.x, p.y)
    drawLine(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFDDF1FF).copy(alpha = 0.55f)), p.y - p.size * 9f, p.y), Offset(p.x, p.y - p.size * 9f), c, strokeWidth = p.size * 0.7f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFEFF9FF).copy(alpha = 0.7f), p.size, c)
    drawCircle(Color.White.copy(alpha = 0.9f), p.size * 0.35f, c + Offset(-p.size * 0.3f, -p.size * 0.3f))
  }
}

private fun DrawScope.drawFrost(v: WinterVariant) {
  val w = size.width
  val h = size.height
  val f = Color(0xFFE6F6FF)
  val t = min(w, h) * 0.10f
  drawRect(Brush.verticalGradient(listOf(f.copy(alpha = 0.20f), Color.Transparent), 0f, t), Offset.Zero, Size(w, t))
  drawRect(Brush.verticalGradient(listOf(Color.Transparent, f.copy(alpha = 0.22f)), h - t, h), Offset(0f, h - t), Size(w, t))
  drawRect(Brush.horizontalGradient(listOf(f.copy(alpha = 0.16f), Color.Transparent), 0f, t), Offset.Zero, Size(t, h))
  drawRect(Brush.horizontalGradient(listOf(Color.Transparent, f.copy(alpha = 0.16f)), w - t, w), Offset(w - t, 0f), Size(t, h))
  drawRect(Brush.verticalGradient(listOf(Color.Transparent, v.bands[0].copy(alpha = 0.25f)), h - 2f, h), Offset(0f, h - 2f), Size(w, 2f))
  // ice crystals in the corners
  for ((cx, cy) in listOf(0f to 0f, w to 0f)) {
    for (k in 0..6) {
      val a = (k * 13f + 6f) * (PI.toFloat() / 180f)
      val dir = if (cx == 0f) 1f else -1f
      val len = t * (1.6f + (k % 3) * 0.5f)
      drawLine(f.copy(alpha = 0.22f), Offset(cx, cy), Offset(cx + dir * cos(a) * len, sin(a) * len), strokeWidth = 1.2f)
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Aurora / winter backdrop for Home. Static when animations are reduced.
// ---------------------------------------------------------------------------------------------
@Composable
fun WinterBackdrop(modifier: Modifier = Modifier) {
  val v = rememberWinterVariant()
  val (lowPerf, _) = rememberPreference(WinterLowPerfKey, false)
  val still = lowPerf || rememberReduceMotion() || !rememberAppResumed()
  val t: androidx.compose.runtime.State<Float> =
    if (still) remember { mutableStateOf(0.25f) }
    else rememberInfiniteTransition(label = "aurora").animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart), label = "t")
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(v.sky))
    for (i in 0 until 46) {
      val sx = ((i * 97) % 100) / 100f * w
      val sy = ((i * 53) % 55) / 100f * h
      val tw = 0.5f + 0.5f * sin(t.value * 6.283f * 2f + i)
      drawCircle(Color.White.copy(alpha = 0.18f + 0.35f * tw), 1.2f + (i % 3) * 0.5f, Offset(sx, sy))
    }
    // aurora ribbons
    v.bands.forEachIndexed { i, col ->
      val path = Path()
      val baseY = h * (0.16f + i * 0.09f)
      val steps = 24
      for (s in 0..steps) {
        val x = w * s / steps
        val y = baseY + sin(s * 0.42f + t.value * 6.283f * (1 + i * 0.4f) + i) * h * 0.045f + sin(s * 0.17f + t.value * 6.283f) * h * 0.03f
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      path.lineTo(w, baseY + h * 0.30f)
      path.lineTo(0f, baseY + h * 0.30f)
      path.close()
      drawPath(path, Brush.verticalGradient(listOf(col.copy(alpha = 0.30f), col.copy(alpha = 0.10f), Color.Transparent), baseY - h * 0.05f, baseY + h * 0.30f))
    }
    // snowy hills and pine trees
    val ground = h * 0.94f
    val hill = Path().apply {
      moveTo(0f, h); lineTo(0f, ground)
      cubicTo(w * 0.25f, ground - h * 0.06f, w * 0.5f, ground + h * 0.01f, w * 0.75f, ground - h * 0.045f)
      cubicTo(w * 0.88f, ground - h * 0.07f, w * 0.95f, ground - h * 0.03f, w, ground - h * 0.04f)
      lineTo(w, h); close()
    }
    drawPath(hill, Brush.verticalGradient(listOf(Color(0xFFDCEEFF).copy(alpha = 0.55f), Color(0xFF8DB7E8).copy(alpha = 0.35f)), ground - h * 0.06f, h))
    for (k in 0 until 5) {
      val bx = w * (0.58f + k * 0.085f)
      val th = h * (0.05f + (k % 3) * 0.018f)
      val gy = ground - h * 0.02f
      val tree = Path().apply { moveTo(bx, gy - th); lineTo(bx - th * 0.38f, gy); lineTo(bx + th * 0.38f, gy); close() }
      drawPath(tree, Color(0xFF06142B).copy(alpha = 0.85f))
      drawCircle(Color.White.copy(alpha = 0.5f), 2f, Offset(bx, gy - th))
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Interactive snow globe
// ---------------------------------------------------------------------------------------------
@Composable
fun SnowGlobe(modifier: Modifier = Modifier) {
  val v = rememberWinterVariant()
  val (lowPerf, _) = rememberPreference(WinterLowPerfKey, false)
  val reduced = rememberReduceMotion() || lowPerf
  val resumed = rememberAppResumed()
  val flakes = remember { List(if (reduced) 40 else 90) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat(), 0f, 0f) } } // x,y,size,vx,vy (unit space)
  var frame by remember { mutableLongStateOf(0L) }
  var tiltX by remember { mutableFloatStateOf(0f) }
  var tiltY by remember { mutableFloatStateOf(0f) }
  val scope = rememberCoroutineScope()
  val settle = remember { Animatable(0f) }
  LaunchedEffect(resumed, reduced) {
    if (!resumed) return@LaunchedEffect
    var last = 0L
    while (true) {
      androidx.compose.runtime.withFrameNanos { t ->
        if (last == 0L) last = t
        val dt = ((t - last) / 1e9f).coerceAtMost(0.05f)
        last = t
        flakes.forEach { f ->
          f[3] *= (1f - 1.6f * dt)
          f[4] += (0.06f * f[2] - f[4]) * 1.2f * dt
          f[0] += f[3] * dt + sin(t / 1e9f + f[2] * 9f) * 0.01f * dt
          f[1] += f[4] * dt
          if (f[1] > 0.93f) { f[1] = 0.05f; f[0] = Random.nextFloat() }
          if (f[0] < 0.05f) f[0] = 0.95f
          if (f[0] > 0.95f) f[0] = 0.05f
        }
        frame = t
      }
    }
  }
  Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).glassSurface(RoundedCornerShape(26.dp), Color(0xFF0B1E3D)).border(1.dp, Brush.linearGradient(listOf(v.bands[0].copy(alpha = 0.7f), v.bands[1].copy(alpha = 0.4f))), RoundedCornerShape(26.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text("SNOW GLOBE", fontSize = 10.sp, letterSpacing = 1.6.sp, color = v.bands[0], fontWeight = FontWeight.Bold)
    Text("Drag to shake it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    Box(
      Modifier.padding(top = 8.dp).size(210.dp)
        .graphicsLayer { rotationY = tiltX; rotationX = -tiltY; cameraDistance = 14f * density }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragEnd = { scope.launch { val sx = tiltX; val sy = tiltY; settle.snapTo(1f); settle.animateTo(0f, spring(0.35f, 120f)) { tiltX = sx * value; tiltY = sy * value } } },
          ) { change, drag ->
            change.consume()
            tiltX = (tiltX + drag.x * 0.2f).coerceIn(-22f, 22f)
            tiltY = (tiltY + drag.y * 0.2f).coerceIn(-22f, 22f)
            flakes.forEach { f -> f[3] += drag.x * 0.0009f * (0.5f + f[2]); f[4] += drag.y * 0.0007f - Random.nextFloat() * 0.002f }
          }
        },
    ) {
      Canvas(Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION") frame
        val c = Offset(size.width / 2f, size.height * 0.46f)
        val r = size.minDimension * 0.46f
        // base
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2B3E66), Color(0xFF0E1A33))), Offset(size.width * 0.18f, size.height * 0.80f), Size(size.width * 0.64f, size.height * 0.16f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
        // glass
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1C3C70).copy(alpha = 0.55f), Color(0xFF071326).copy(alpha = 0.85f)), c, r), r, c)
        // scene: snowy ground, house and tree
        val ground = c.y + r * 0.55f
        drawCircle(Color(0xFFE8F4FF).copy(alpha = 0.9f), r * 0.75f, Offset(c.x, ground + r * 0.62f))
        drawRect(Color(0xFF3B2A22), Offset(c.x - r * 0.28f, ground - r * 0.30f), Size(r * 0.30f, r * 0.30f))
        drawPath(Path().apply { moveTo(c.x - r * 0.33f, ground - r * 0.30f); lineTo(c.x - r * 0.13f, ground - r * 0.50f); lineTo(c.x + r * 0.07f, ground - r * 0.30f); close() }, Color(0xFFF2F8FF))
        drawRect(Color(0xFFFFD36B), Offset(c.x - r * 0.18f, ground - r * 0.22f), Size(r * 0.09f, r * 0.09f))
        drawPath(Path().apply { moveTo(c.x + r * 0.30f, ground - r * 0.62f); lineTo(c.x + r * 0.14f, ground - r * 0.10f); lineTo(c.x + r * 0.46f, ground - r * 0.10f); close() }, Color(0xFF0E5A3A))
        drawCircle(Color(0xFFFFE08A), 3f, Offset(c.x + r * 0.30f, ground - r * 0.62f))
        // flakes inside the circle
        flakes.forEach { f ->
          val px = c.x + (f[0] - 0.5f) * 2f * r
          val py = c.y + (f[1] - 0.5f) * 2f * r
          if (hypot(px - c.x, py - c.y) < r - 4f) drawCircle(Color.White.copy(alpha = 0.55f + 0.3f * f[2] / 1.4f), 1.2f + f[2] * 1.6f, Offset(px, py))
        }
        // glass rim and highlights
        drawCircle(Brush.sweepGradient(listOf(v.bands[0].copy(alpha = 0.8f), v.bands[1].copy(alpha = 0.3f), v.bands[2].copy(alpha = 0.8f), v.bands[0].copy(alpha = 0.8f)), c), r, c, style = Stroke(width = 3f))
        drawOval(Color.White.copy(alpha = 0.22f), Offset(c.x - r * 0.62f, c.y - r * 0.78f), Size(r * 0.7f, r * 0.34f))
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Water fill: pull the cord on Home to fill the screen, albums float on the water
// ---------------------------------------------------------------------------------------------
object WaterFill {
  var level by mutableFloatStateOf(0f)
  var time by mutableFloatStateOf(0f)
}

/** Makes an item bob on the water. No cost when the water is down. */
fun Modifier.waterFloat(seed: Int = 0): Modifier =
  this.graphicsLayer {
    val l = WaterFill.level
    if (l > 0.04f) {
      val t = WaterFill.time
      translationY = sin(t * 1.7f + seed * 1.3f) * 7f * density * l
      translationX = cos(t * 1.1f + seed * 0.7f) * 3f * density * l
      rotationZ = sin(t * 1.3f + seed) * 1.6f * l
    }
  }

@Composable
fun WaterFillLayer(modifier: Modifier = Modifier) {
  val (enabled, _) = rememberPreference(WaterFillKey, false)
  if (!enabled) { WaterFill.level = 0f; return }
  val v = rememberWinterVariant()
  val (lowPerf, _) = rememberPreference(WinterLowPerfKey, false)
  val reduced = rememberReduceMotion() || lowPerf
  val resumed = rememberAppResumed()
  var size by remember { mutableStateOf(IntSize.Zero) }
  val scope = rememberCoroutineScope()
  val anim = remember { Animatable(WaterFill.level) }
  val bubbles = remember { List(18) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat()) } }

  LaunchedEffect(resumed, WaterFill.level > 0.01f, reduced) {
    if (!resumed || WaterFill.level <= 0.01f) return@LaunchedEffect
    var last = 0L
    while (true) {
      androidx.compose.runtime.withFrameNanos { t ->
        if (last == 0L) last = t
        val dt = ((t - last) / 1e9f).coerceAtMost(0.05f)
        last = t
        if (!reduced) WaterFill.time += dt
        bubbles.forEach { b -> b[1] -= dt * 0.12f * b[2]; if (b[1] < 0f) { b[1] = 1f; b[0] = Random.nextFloat() } }
      }
    }
  }
  Box(modifier.fillMaxSize().onSizeChanged { size = it }) {
    val maxPull = size.height * 0.30f
    Canvas(Modifier.fillMaxSize()) {
      val level = WaterFill.level
      val t = WaterFill.time
      if (level > 0.005f) {
        val top = size.height * (1f - level * 0.92f)
        val w = size.width
        fun wave(amp: Float, k: Float, sp: Float, ph: Float) = Path().apply {
          moveTo(0f, size.height)
          var x = 0f
          while (x <= w) { lineTo(x, top + sin(x / w * k * 6.283f + t * sp + ph) * amp * level.coerceAtLeast(0.2f)); x += 12f }
          lineTo(w, size.height); close()
        }
        drawPath(wave(14f, 1.6f, 1.6f, 0f), Brush.verticalGradient(listOf(v.bands[0].copy(alpha = 0.22f), Color(0xFF1A6BD0).copy(alpha = 0.40f)), top, size.height))
        drawPath(wave(10f, 2.4f, -1.1f, 2f), Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color(0xFF0B3B8A).copy(alpha = 0.30f)), top, size.height))
        bubbles.forEach { b ->
          val by = top + (size.height - top) * b[1]
          drawCircle(Color.White.copy(alpha = 0.25f), 2f + b[2] * 3f, Offset(b[0] * w + sin(t + b[2] * 5f) * 8f, by), style = Stroke(1f))
        }
      }
    }
    // the cord: hangs from the top edge, drag it down to fill, drag it back up to drain
    val knobY = WaterFill.level * maxPull + 40f
    Canvas(Modifier.align(Alignment.TopEnd).padding(end = 28.dp).size(44.dp, with(LocalDensity.current) { (knobY + 34f).toDp() })) {
      val cx = size.width / 2f
      drawLine(Color(0xFFCFE6FF).copy(alpha = 0.85f), Offset(cx, 0f), Offset(cx, knobY), strokeWidth = 3f, cap = StrokeCap.Round)
      drawCircle(Brush.radialGradient(listOf(Color.White, v.bands[0]), Offset(cx, knobY), 18f), 15f, Offset(cx, knobY))
      drawCircle(Color.White.copy(alpha = 0.6f), 15f, Offset(cx, knobY), style = Stroke(2f))
    }
    Box(
      Modifier.align(Alignment.TopEnd).padding(end = 12.dp).size(76.dp, with(LocalDensity.current) { (knobY + 70f).toDp() })
        .pointerInput(maxPull) {
          detectVerticalDragGestures(
            onDragStart = { scope.launch { anim.stop() } },
            onDragEnd = {
              val target = if (WaterFill.level > 0.45f) 1f else 0f
              scope.launch { anim.snapTo(WaterFill.level); anim.animateTo(target, spring(0.6f, 60f)) { WaterFill.level = value } }
            },
          ) { change, dy ->
            change.consume()
            WaterFill.level = (WaterFill.level + dy / maxPull.coerceAtLeast(1f)).coerceIn(0f, 1f)
          }
        },
    )
  }
}
