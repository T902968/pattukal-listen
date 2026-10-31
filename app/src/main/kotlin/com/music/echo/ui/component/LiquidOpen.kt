package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.HalloweenReducedMotionKey
import echo.music.iad1tya.constants.LiquidOpenKey
import echo.music.iad1tya.ui.buddy.BuddyLook
import echo.music.iad1tya.ui.buddy.BuddyStageAnchor
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.utils.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Liquid touch effect for opening a song: a soft liquid ripple spreads from where you tapped, then droplets
 * flow into the player. Draws only (no touches), lasts under a second, never delays playback, and is
 * skipped with reduced motion.
 */
@Composable
fun LiquidSongOpenHost(look: BuddyLook) {
  val (on, _) = rememberPreference(LiquidOpenKey, true)
  val (hwReduced, _) = rememberPreference(HalloweenReducedMotionKey, false)
  val reduced = rememberReduceMotion()
  val connection = LocalPlayerConnection.current
  val meta by (connection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
  var lastId by remember { mutableStateOf<String?>(null) }
  var started by remember { mutableStateOf(false) }
  var origin by remember { mutableStateOf(Offset.Zero) }
  var running by remember { mutableStateOf(false) }
  val p = remember { Animatable(0f) }
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary

  LaunchedEffect(meta?.id) {
    val m = meta ?: return@LaunchedEffect
    if (!started) { started = true; lastId = m.id; return@LaunchedEffect } // restored song at launch: no effect
    if (m.id == lastId) return@LaunchedEffect
    lastId = m.id
    if (!on || reduced || hwReduced) return@LaunchedEffect
    val ping = look.ping ?: return@LaunchedEffect
    if (System.nanoTime() - ping.id > 2_500_000_000L) return@LaunchedEffect // automatic next song, not a tap
    origin = ping.pos
    running = true
    try {
      p.snapTo(0f)
      p.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    } finally { running = false }
  }
  if (!running) return
  Canvas(Modifier.fillMaxSize()) { drawLiquid(origin, p.value, primary, tertiary) }
}

private fun DrawScope.drawLiquid(o: Offset, p: Float, c1: Color, c2: Color) {
  val maxR = hypot(size.width, size.height) * 0.5f
  val grow = FastOutSlowInEasing.transform((p / 0.55f).coerceIn(0f, 1f))
  val fade = 1f - ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
  val r = maxR * grow
  val t = p * 11f
  // the wobbling liquid blob
  if (r > 2f) {
    val path = Path()
    val n = 56
    for (i in 0..n) {
      val a = i * 2f * PI.toFloat() / n
      val rr = r * (1f + 0.045f * sin(5f * a + t) + 0.03f * sin(3f * a - 1.7f * t) + 0.02f * sin(8f * a + 2.3f * t))
      val x = o.x + cos(a) * rr
      val y = o.y + sin(a) * rr
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color.Transparent, c1.copy(alpha = 0.10f * fade), c2.copy(alpha = 0.34f * fade)), o, r))
    drawPath(path, Color.White.copy(alpha = 0.55f * fade), style = Stroke(width = 3f))
    drawPath(path, c1.copy(alpha = 0.35f * fade), style = Stroke(width = 12f))
    // glossy highlight arc, like light on a liquid surface
    drawArc(Color.White.copy(alpha = 0.35f * fade), 200f, 50f, false, o - Offset(r * 0.92f, r * 0.92f), androidx.compose.ui.geometry.Size(r * 1.84f, r * 1.84f), style = Stroke(width = 5f))
    // a second, trailing ripple
    val r2 = r * 0.72f
    drawCircle(Color.White.copy(alpha = 0.22f * fade), r2, o, style = Stroke(width = 2f))
  }
  // droplets flow into the player
  val anchor = BuddyStageAnchor.rect
  val target = if (anchor != null && anchor.width > 80f && anchor.bottom in 1f..size.height) anchor.center else Offset(size.width * 0.5f, size.height * 0.9f)
  val flow = ((p - 0.3f) / 0.65f).coerceIn(0f, 1f)
  if (flow > 0f) {
    val e = FastOutSlowInEasing.transform(flow)
    for (i in 0 until 9) {
      val rnd = Random(i * 31 + 5)
      val a = rnd.nextFloat() * 2f * PI.toFloat()
      val start = Offset(o.x + cos(a) * maxR * 0.55f * grow, o.y + sin(a) * maxR * 0.55f * grow)
      val ctrl = Offset((start.x + target.x) / 2f + (rnd.nextFloat() - 0.5f) * 160f, (start.y + target.y) / 2f - 80f)
      val u = e
      val x = (1 - u) * (1 - u) * start.x + 2 * (1 - u) * u * ctrl.x + u * u * target.x
      val y = (1 - u) * (1 - u) * start.y + 2 * (1 - u) * u * ctrl.y + u * u * target.y
      val rad = (5f + rnd.nextFloat() * 8f) * (1f - 0.6f * e)
      drawCircle(c2.copy(alpha = 0.5f * (1f - flow * 0.6f)), rad * 1.8f, Offset(x, y))
      drawCircle(Color.White.copy(alpha = 0.75f * (1f - flow * 0.5f)), rad, Offset(x, y))
    }
    // the splash where they land
    val land = ((flow - 0.55f) / 0.45f).coerceIn(0f, 1f)
    if (land > 0f) {
      val sr = 20f + 70f * land
      drawCircle(c1.copy(alpha = 0.35f * (1f - land)), sr, target, style = Stroke(width = 4f))
      drawCircle(Color.White.copy(alpha = 0.25f * (1f - land)), sr * 0.6f, target)
    }
  }
}
