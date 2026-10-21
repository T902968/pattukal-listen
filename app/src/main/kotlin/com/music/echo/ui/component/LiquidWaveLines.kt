package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.PI
import kotlin.math.sin

/**
 * Flowing, glowing wave lines for the default liquid-glass player background. Several layers of
 * sine ribbons drift at different speeds with a soft glow, tinted by the song colour.
 */
@Composable
fun LiquidWaveLines(tint: Color, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "liquidWaveLines")
  val phase by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = (2 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
      label = "wavePhase"
    )
  Canvas(modifier) {
    val w = size.width
    val h = size.height
    val lines = 9
    for (i in 0 until lines) {
      val f = i / (lines - 1f)
      val baseY = h * (0.30f + 0.55f * f)
      val amp = h * (0.018f + 0.020f * sin(f * PI.toFloat()))
      val freq = 1.4f + f * 1.3f
      val dir = if (i % 2 == 0) 1f else -1f
      val path = Path()
      var x = 0f
      val step = 10f
      var first = true
      while (x <= w + step) {
        val t = x / w
        val y =
          baseY +
            amp * sin(t * freq * 2f * PI.toFloat() + phase * dir + i) +
            amp * 0.5f * sin(t * (freq + 1.7f) * 2f * PI.toFloat() - phase * 1.6f * dir)
        if (first) {
          path.moveTo(x, y)
          first = false
        } else path.lineTo(x, y)
        x += step
      }
      val a = 0.10f + 0.16f * (1f - kotlin.math.abs(f - 0.5f) * 1.6f).coerceIn(0f, 1f)
      val brush =
        Brush.horizontalGradient(
          listOf(Color.Transparent, tint.copy(alpha = a), Color.White.copy(alpha = a * 0.8f), tint.copy(alpha = a), Color.Transparent)
        )
      // soft glow underlay, then the crisp line
      drawPath(path, brush, style = Stroke(width = 7f, cap = StrokeCap.Round), alpha = 0.45f)
      drawPath(path, brush, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
    }
  }
}
