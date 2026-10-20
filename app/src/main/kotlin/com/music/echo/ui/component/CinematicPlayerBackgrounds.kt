package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class CinematicScene { TOKYO, FIREWORKS, CAR_DRIFT }

@Composable
private fun CinematicPlayerBackground(scene: CinematicScene, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "cinematicBackground")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = when (scene) {
          CinematicScene.TOKYO -> 18000
          CinematicScene.FIREWORKS -> 5200
          CinematicScene.CAR_DRIFT -> 4200
        },
        easing = LinearEasing,
      ),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sceneMotion",
  )
  val sky = when (scene) {
    CinematicScene.TOKYO -> listOf(Color(0xFF05091D), Color(0xFF15113C), Color(0xFF070B19))
    CinematicScene.FIREWORKS -> listOf(Color(0xFF020611), Color(0xFF09162B), Color(0xFF010309))
    CinematicScene.CAR_DRIFT -> listOf(Color(0xFF070B12), Color(0xFF202A36), Color(0xFF030507))
  }
  Box(
    modifier = modifier.fillMaxSize().background(Brush.verticalGradient(sky))
  ) {
    Canvas(Modifier.fillMaxSize()) {
      when (scene) {
        CinematicScene.TOKYO -> {
          // Neon skyline with deterministic lit windows and slowly sweeping signs.
          val horizon = size.height * 0.68f
          val widths = listOf(0.11f, 0.16f, 0.09f, 0.14f, 0.12f, 0.18f, 0.10f, 0.16f)
          var left = -size.width * 0.02f
          widths.forEachIndexed { index, fraction ->
            val w = size.width * fraction
            val h = size.height * (0.18f + (index % 4) * 0.095f)
            val top = horizon - h
            val tint = if (index % 2 == 0) Color(0xFF13D9F2) else Color(0xFFFF4FB8)
            drawRect(Color(0xFF080D20), Offset(left, top), Size(w, h))
            drawRect(tint.copy(alpha = 0.28f), Offset(left, top), Size(w, 2.2f))
            val cols = maxOf(2, (w / 12f).toInt())
            val rows = maxOf(2, (h / 16f).toInt())
            for (row in 0 until rows) for (col in 0 until cols) {
              if ((row * 7 + col * 3 + index) % 4 != 0) {
                val wx = left + 4f + col * ((w - 8f) / cols)
                val wy = top + 7f + row * ((h - 12f) / rows)
                drawRect(
                  color = tint.copy(alpha = 0.38f + ((row + col) % 3) * 0.16f),
                  topLeft = Offset(wx, wy),
                  size = Size(2.4f, 4.5f),
                )
              }
            }
            left += w + size.width * 0.015f
          }
          drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC02040B))),
            Offset(0f, horizon),
            Size(size.width, size.height - horizon),
          )
          val sweepX = (phase * 1.4f - 0.2f) * size.width
          drawLine(Color(0xFF25DDF7).copy(alpha = 0.5f), Offset(sweepX, horizon), Offset(sweepX + size.width * 0.18f, size.height), 3f)
          drawLine(Color(0xFFFF4FB8).copy(alpha = 0.22f), Offset(sweepX + 14f, horizon), Offset(sweepX + size.width * 0.2f, size.height), 1.5f)
        }
        CinematicScene.FIREWORKS -> {
          val colors = listOf(Color(0xFF45D9FF), Color(0xFFFF5FC8), Color(0xFFFFD36B), Color(0xFFB89CFF))
          val centers = listOf(
            Offset(size.width * 0.23f, size.height * 0.28f),
            Offset(size.width * 0.72f, size.height * 0.22f),
            Offset(size.width * 0.53f, size.height * 0.42f),
          )
          centers.forEachIndexed { idx, center ->
            val local = ((phase + idx * 0.31f) % 1f)
            val radius = 8f + local * size.minDimension * 0.20f
            val alpha = (1f - local).coerceIn(0f, 1f)
            val color = colors[idx % colors.size]
            drawCircle(color.copy(alpha = alpha * 0.5f), radius * 0.62f, center, style = Stroke(width = 1.2f))
            drawCircle(color.copy(alpha = alpha * 0.25f), radius, center, style = Stroke(width = 0.8f))
            for (ray in 0 until 28) {
              val angle = (ray * 2.0 * PI / 28.0).toFloat()
              val inner = radius * 0.34f
              val outer = radius * (0.78f + (ray % 4) * 0.07f)
              val start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner)
              val end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer)
              drawLine(color.copy(alpha = alpha * 0.9f), start, end, strokeWidth = if (ray % 3 == 0) 2f else 1f)
              drawCircle(color.copy(alpha = alpha), 1.5f + (ray % 3), end)
            }
          }
          for (i in 0 until 45) {
            val x = (i * 67 % 101) / 101f * size.width
            val y = (i * 43 % 97) / 97f * size.height * 0.65f
            drawCircle(Color.White.copy(alpha = 0.15f + (i % 4) * 0.08f), 0.8f + (i % 2), Offset(x, y))
          }
        }
        CinematicScene.CAR_DRIFT -> {
          val roadTop = size.height * 0.58f
          drawRect(
            Brush.verticalGradient(listOf(Color(0xFF27313C), Color(0xFF050608))),
            Offset(0f, roadTop),
            Size(size.width, size.height - roadTop),
          )
          // Fast road streaks and a stylized classic coupe silhouette, all vector-drawn.
          for (i in 0 until 16) {
            val y = roadTop + ((i * 31f + phase * size.height * 0.65f) % (size.height - roadTop))
            val length = size.width * (0.06f + (i % 4) * 0.035f)
            val x = ((i * 73f + phase * size.width * 0.55f) % size.width)
            drawLine(Color(0xFF48CFFF).copy(alpha = 0.12f + (i % 3) * 0.07f), Offset(x, y), Offset((x + length).coerceAtMost(size.width), y - 2f), 1.5f)
          }
          val cx = size.width * (0.5f + sin(phase * 2f * PI.toFloat()) * 0.055f)
          val cy = size.height * 0.68f
          rotate(degrees = sin(phase * 2f * PI.toFloat()) * 5f, pivot = Offset(cx, cy)) {
            drawRoundRect(Color(0xFF10151D), Offset(cx - size.width * 0.24f, cy - size.height * 0.055f), Size(size.width * 0.48f, size.height * 0.105f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f))
            drawRoundRect(Color(0xFF35495D), Offset(cx - size.width * 0.13f, cy - size.height * 0.12f), Size(size.width * 0.26f, size.height * 0.09f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f))
            drawRoundRect(Color(0xFF8AB9D4).copy(alpha = 0.8f), Offset(cx - size.width * 0.095f, cy - size.height * 0.105f), Size(size.width * 0.19f, size.height * 0.055f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
            drawRect(Color(0xFFFF5B50), Offset(cx - size.width * 0.215f, cy - size.height * 0.012f), Size(size.width * 0.055f, size.height * 0.018f))
            drawRect(Color(0xFFFF5B50), Offset(cx + size.width * 0.16f, cy - size.height * 0.012f), Size(size.width * 0.055f, size.height * 0.018f))
            drawCircle(Color(0xFF030405), size.width * 0.035f, Offset(cx - size.width * 0.15f, cy + size.height * 0.04f))
            drawCircle(Color(0xFF030405), size.width * 0.035f, Offset(cx + size.width * 0.15f, cy + size.height * 0.04f))
          }
          drawLine(Color(0xFF32CFFF).copy(alpha = 0.55f), Offset(size.width * 0.15f, roadTop + 10f), Offset(size.width * 0.04f, size.height), 2f)
          drawLine(Color(0xFFFF4A8D).copy(alpha = 0.42f), Offset(size.width * 0.83f, roadTop + 6f), Offset(size.width * 0.98f, size.height), 2f)
        }
      }
    }
  }
}

@Composable
fun TokyoPlayerBackground(modifier: Modifier = Modifier) =
  CinematicPlayerBackground(CinematicScene.TOKYO, modifier)

@Composable
fun FireworksPlayerBackground(modifier: Modifier = Modifier) =
  CinematicPlayerBackground(CinematicScene.FIREWORKS, modifier)

@Composable
fun CarDriftPlayerBackground(modifier: Modifier = Modifier) =
  CinematicPlayerBackground(CinematicScene.CAR_DRIFT, modifier)
