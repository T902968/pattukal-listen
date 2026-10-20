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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight Snowland background: a quiet winter-night gradient with a small,
 * deterministic set of softly layered snowflakes. The fixed particle count keeps
 * animation cost predictable on lower-end devices.
 */
@Composable
fun SnowlandPlayerBackground(
  modifier: Modifier = Modifier,
  visible: Boolean = true,
) {
  val transition = rememberInfiniteTransition(label = "snowland")
  val fall by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "snowFall",
  )
  val drift by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI.toFloat()),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 14000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "snowDrift",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF071323), Color(0xFF142D49), Color(0xFF050B16))
        )
      )
  ) {
    Canvas(Modifier.fillMaxSize()) {
      if (!visible) return@Canvas
      val count = 32
      for (i in 0 until count) {
        val seed = (i * 37 % 101) / 101f
        val speed = 0.35f + (i % 5) * 0.13f
        val y = ((seed + fall * speed) % 1f) * size.height
        val baseX = ((i * 61 % 103) / 103f) * size.width
        val sway = sin(drift + i * 0.73f) * (size.width * 0.012f)
        val x = (baseX + sway).coerceIn(0f, size.width)
        val radius = 1.1f + (i % 4) * 0.55f
        val alpha = 0.28f + (i % 6) * 0.11f
        drawCircle(
          color = Color(0xFFEAF6FF).copy(alpha = alpha.coerceAtMost(0.9f)),
          radius = radius,
          center = Offset(x, y),
        )
        if (i % 7 == 0) {
          drawCircle(
            color = Color(0xFFB9DFFF).copy(alpha = 0.24f),
            radius = radius + 2.2f,
            center = Offset(x, y),
            style = Stroke(width = 0.8f),
          )
        }
      }
    }
  }
}
