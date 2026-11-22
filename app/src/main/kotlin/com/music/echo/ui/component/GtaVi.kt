package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated GTA VI inspired Miami-style sunset, drawn live (resolution independent, so it is sharp on any display).
 * Time comes from the wall clock, so the Home and Player instances are in the same phase and read as one connected world.
 * It is original procedural art, not footage from the game.
 */
@Composable
fun GtaViBackground(modifier: Modifier = Modifier, scrim: Float = 0.28f) {
  var t by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    while (true) withFrameNanos { t = (System.currentTimeMillis() % 3_600_000L) / 1000f }
  }
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.66f
    val breathe = (sin(t * 0.18) * 0.5 + 0.5).toFloat()

    // sky
    val top = lerp(Color(0xFF14043A), Color(0xFF2A0A5E), breathe)
    val mid = lerp(Color(0xFFB0208F), Color(0xFFFF3FA4), breathe)
    val low = lerp(Color(0xFFFF7A3C), Color(0xFFFFB347), breathe)
    drawRect(Brush.verticalGradient(listOf(top, mid, low, Color(0xFFFFD86B)), 0f, horizon))

    // stars near the top
    for (i in 0 until 46) {
      val x = ((i * 7919) % 1000) / 1000f * w
      val y = ((i * 104729) % 1000) / 1000f * horizon * 0.5f
      val tw = (sin(t * 1.6 + i) * 0.5 + 0.5).toFloat()
      drawCircle(Color.White.copy(alpha = 0.15f + 0.5f * tw), 1.2f + tw * 1.2f, Offset(x, y))
    }

    // drifting clouds
    for (i in 0 until 6) {
      val speed = 6f + i * 3f
      val cw = w * (0.35f + 0.08f * (i % 3))
      val x = ((t * speed + i * w * 0.4f) % (w + cw)) - cw
      val y = horizon * (0.18f + 0.11f * i)
      drawRoundRect(Color(0xFFFF8AC8).copy(alpha = 0.16f), Offset(x, y), Size(cw, h * 0.022f), CornerRadius(h * 0.011f))
      drawRoundRect(Color(0xFFFFC2E6).copy(alpha = 0.10f), Offset(x + cw * 0.2f, y - h * 0.012f), Size(cw * 0.6f, h * 0.02f), CornerRadius(h * 0.01f))
    }

    // sun with retro stripes
    val sunC = Offset(w * 0.5f, horizon - w * 0.06f + sin(t * 0.3).toFloat() * h * 0.004f)
    val sunR = w * 0.24f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1B8).copy(alpha = 0.55f), Color.Transparent), sunC, sunR * 2.1f), sunR * 2.1f, sunC)
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFF5FA8)), sunC.y - sunR, sunC.y + sunR), sunR, sunC)
    for (k in 0 until 7) {
      val yy = sunC.y + sunR * (0.05f + k * 0.14f)
      drawRect(mid.copy(alpha = 0.95f), Offset(sunC.x - sunR, yy), Size(sunR * 2, sunR * (0.012f + k * 0.012f)))
    }

    // far skyline, slow parallax
    val shift = (t * 3f) % (w * 0.5f)
    for (rep in 0..2) {
      for (i in 0 until 16) {
        val bw = w * (0.035f + ((i * 37) % 5) * 0.012f)
        val bh = h * (0.05f + ((i * 53) % 9) * 0.012f)
        val x = i * w * 0.062f + rep * w * 0.5f - shift
        drawRect(Color(0xFF2A0840), Offset(x, horizon - bh), Size(bw, bh))
        for (wi in 0 until 3) {
          val on = ((i * 31 + wi * 17 + (t * 0.5f).toInt()) % 5) != 0
          if (on) drawRect(Color(0xFFFF8AD8).copy(alpha = 0.7f), Offset(x + bw * 0.2f, horizon - bh + h * 0.01f + wi * h * 0.012f), Size(bw * 0.18f, h * 0.005f))
        }
      }
    }

    // ocean with shimmering sun reflection
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3B0F6B), Color(0xFF12041F)), horizon, h), Offset(0f, horizon), Size(w, h - horizon))
    for (i in 0 until 16) {
      val yy = horizon + (h - horizon) * (i / 16f) * (i / 16f) + 6f
      val len = w * (0.5f - i * 0.022f) * (0.7f + 0.3f * sin(t * 1.4 + i).toFloat())
      drawRoundRect(Color(0xFFFFB6DD).copy(alpha = 0.42f - i * 0.02f), Offset(sunC.x - len / 2f, yy), Size(len, 2.5f + i * 0.5f), CornerRadius(4f))
    }

    // swaying palms
    fun palm(x: Float, height: Float, lean: Float, phase: Float) {
      val sway = sin(t * 0.8 + phase).toFloat() * 10f
      val base = Offset(x, h * 0.99f)
      val topP = Offset(x + lean + sway, base.y - height)
      val trunk = Path().apply { moveTo(base.x - 7f, base.y); lineTo(base.x + 7f, base.y); lineTo(topP.x + 3f, topP.y); lineTo(topP.x - 3f, topP.y); close() }
      drawPath(trunk, Color(0xFF0B0214))
      for (a in listOf(-170f, -150f, -125f, -100f, -80f, -55f, -30f, -10f)) {
        val rad = a * PI / 180.0 + sin(t * 1.1 + phase + a) * 0.04
        val len = height * 0.42f
        val end = Offset(topP.x + (cos(rad) * len).toFloat(), topP.y + (sin(rad) * len * 0.55f).toFloat() + len * 0.3f)
        val c = Offset((topP.x + end.x) / 2f, topP.y - len * 0.25f)
        val leaf = Path().apply { moveTo(topP.x, topP.y); quadraticBezierTo(c.x, c.y, end.x, end.y); quadraticBezierTo(c.x, c.y + 16f, topP.x, topP.y + 9f); close() }
        drawPath(leaf, Color(0xFF0B0214))
      }
    }
    palm(w * 0.10f, h * 0.46f, 46f, 0f)
    palm(w * 0.92f, h * 0.40f, -40f, 2f)
    palm(w * 0.02f, h * 0.30f, 26f, 4f)
    palm(w * 1.0f, h * 0.26f, -22f, 1f)

    // neon sparkles drifting up
    for (i in 0 until 26) {
      val p = ((t * (0.02f + (i % 5) * 0.006f) + i * 0.137f) % 1f)
      val x = ((i * 6007) % 1000) / 1000f * w + sin(t + i).toFloat() * 12f
      val y = h - p * h * 0.9f
      drawCircle(Color(0xFFFF6FC4).copy(alpha = (1f - p) * 0.6f), 2f + (i % 3), Offset(x, y))
    }

    // readability scrim
    drawRect(Color.Black.copy(alpha = scrim))
  }
}

/**
 * Player version: a neon Vice-style night drive. Skyline with lit windows, a big pink moon, a perspective road grid
 * rushing toward you and light streaks of passing cars. Original procedural art, in phase with the Home sunset.
 */
@Composable
fun GtaViNightBackground(modifier: Modifier = Modifier, scrim: Float = 0.3f) {
  var t by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) { while (true) withFrameNanos { t = (System.currentTimeMillis() % 3_600_000L) / 1000f } }
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.58f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF05010F), Color(0xFF1A0540), Color(0xFF5B1285), Color(0xFFFF3FA4)), 0f, horizon))
    for (i in 0 until 60) {
      val x = ((i * 7919) % 1000) / 1000f * w
      val y = ((i * 104729) % 1000) / 1000f * horizon * 0.7f
      val tw = (sin(t * 1.4 + i) * 0.5 + 0.5).toFloat()
      drawCircle(Color.White.copy(alpha = 0.12f + 0.55f * tw), 1f + tw, Offset(x, y))
    }
    val moon = Offset(w * 0.5f, horizon * 0.52f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF7AC8).copy(alpha = 0.5f), Color.Transparent), moon, w * 0.5f), w * 0.5f, moon)
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFB3E1), Color(0xFFFF5FA8)), moon.y - w * 0.16f, moon.y + w * 0.16f), w * 0.16f, moon)
    // skyline
    for (layer in 0..1) {
      val shift = (t * (2f + layer * 3f)) % (w * 0.6f)
      val col = if (layer == 0) Color(0xFF1B0638) else Color(0xFF0B0214)
      for (rep in 0..2) for (i in 0 until 14) {
        val bw = w * (0.05f + ((i * 37 + layer * 11) % 5) * 0.014f)
        val bh = h * (0.07f + ((i * 53 + layer * 7) % 9) * 0.017f) * (if (layer == 0) 1.0f else 0.7f)
        val x = i * w * 0.075f + rep * w * 0.6f - shift - layer * 20f
        drawRect(col, Offset(x, horizon - bh), Size(bw, bh))
        for (wi in 0 until 4) {
          val on = ((i * 31 + wi * 17 + layer * 5 + (t * 0.4f).toInt()) % 4) != 0
          if (on) drawRect((if (wi % 2 == 0) Color(0xFF22D3EE) else Color(0xFFFF6FC4)).copy(alpha = 0.75f), Offset(x + bw * 0.18f, horizon - bh + h * 0.012f + wi * h * 0.016f), Size(bw * 0.2f, h * 0.006f))
        }
      }
    }
    // road
    drawRect(Brush.verticalGradient(listOf(Color(0xFF16062C), Color(0xFF05010F)), horizon, h), Offset(0f, horizon), Size(w, h - horizon))
    val vp = Offset(w * 0.5f, horizon)
    for (i in -8..8) { // converging lines
      drawLine(Color(0xFF22D3EE).copy(alpha = 0.35f), vp, Offset(w * 0.5f + i * w * 0.32f, h), 2f)
    }
    for (k in 0 until 9) { // lines rushing toward the viewer
      val p = ((k / 9f) + t * 0.18f) % 1f
      val y = horizon + (h - horizon) * p * p
      drawLine(Color(0xFFFF4FA3).copy(alpha = 0.15f + 0.5f * p), Offset(0f, y), Offset(w, y), 1.5f + 3f * p)
    }
    // passing car lights
    for (i in 0 until 5) {
      val p = ((t * (0.12f + i * 0.03f) + i * 0.21f) % 1f)
      val y = horizon + (h - horizon) * p * p
      val lane = (if (i % 2 == 0) -1 else 1) * (0.06f + p * 0.28f) * w
      val r = 2f + 7f * p
      drawCircle(if (i % 2 == 0) Color(0xFFFF2D55) else Color(0xFFFFF1B8), r, Offset(w * 0.5f + lane - r * 2f, y))
      drawCircle(if (i % 2 == 0) Color(0xFFFF2D55) else Color(0xFFFFF1B8), r, Offset(w * 0.5f + lane + r * 2f, y))
    }
    // palms
    fun palm(x: Float, height: Float, lean: Float, phase: Float) {
      val sway = sin(t * 0.8 + phase).toFloat() * 9f
      val base = Offset(x, h * 0.99f)
      val topP = Offset(x + lean + sway, base.y - height)
      drawPath(Path().apply { moveTo(base.x - 7f, base.y); lineTo(base.x + 7f, base.y); lineTo(topP.x + 3f, topP.y); lineTo(topP.x - 3f, topP.y); close() }, Color(0xFF05010F))
      for (a in listOf(-170f, -145f, -120f, -95f, -75f, -50f, -25f, -5f)) {
        val rad = a * PI / 180.0
        val len = height * 0.4f
        val end = Offset(topP.x + (cos(rad) * len).toFloat(), topP.y + (sin(rad) * len * 0.55f).toFloat() + len * 0.3f)
        val c = Offset((topP.x + end.x) / 2f, topP.y - len * 0.25f)
        drawPath(Path().apply { moveTo(topP.x, topP.y); quadraticBezierTo(c.x, c.y, end.x, end.y); quadraticBezierTo(c.x, c.y + 16f, topP.x, topP.y + 9f); close() }, Color(0xFF05010F))
      }
    }
    palm(w * 0.06f, h * 0.38f, 40f, 0f)
    palm(w * 0.95f, h * 0.34f, -36f, 2f)
    drawRect(Color.Black.copy(alpha = scrim))
  }
}
