package echo.music.iad1tya.ui.halloween

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Haunted sky for Home: AMOLED black, moon, tree and castle silhouettes, drifting fog, embers and a few ghost wisps.
 * Dark and low-contrast so text stays readable. Decorative loops stop when the app is hidden; reduced motion draws a still scene.
 */
@Composable
fun HauntedBackground(modifier: Modifier = Modifier, scroll: () -> Float = { 0f }) {
  val fx = rememberHwFx()
  val visible = rememberAppVisible()
  val animate = visible && !fx.reduced
  val frame = rememberFrameTime(active = animate, gapNanos = if (fx.lowEnd) 66_000_000L else 33_000_000L)
  val embers = (if (fx.lowEnd) 8 else 22).let { if (fx.reduced) 0 else it }
  val seeds = androidx.compose.runtime.remember { List(24) { Random(it * 31 + 7) } .map { floatArrayOf(it.nextFloat(), 0.04f + it.nextFloat() * 0.08f, it.nextFloat(), 1.5f + it.nextFloat() * 2.5f) } }

  Canvas(
    modifier
      .fillMaxSize()
      .drawWithCache {
        val w = size.width
        val h = size.height
        val sky = Brush.verticalGradient(listOf(Color.Black, Color(0xFF0A0614), Color(0xFF120A1C)), 0f, h)
        val moonC = Offset(w * 0.72f, h * 0.15f)
        val moonR = w * 0.15f
        val glow = Brush.radialGradient(listOf(HW.Silver.copy(alpha = 0.22f), HW.Violet.copy(alpha = 0.08f), Color.Transparent), moonC, moonR * 3.2f)
        val stars = List(46) { Random(it + 99).let { r -> Triple(r.nextFloat() * w, r.nextFloat() * h * 0.55f, 0.6f + r.nextFloat() * 1.3f) } }
        val castle = Path().apply {
          val bx = w * 0.56f; val by = h
          val bw = w * 0.44f; val top = h * 0.80f
          moveTo(bx, by)
          lineTo(bx, top + 30f)
          // left tower
          lineTo(bx + bw * 0.04f, top + 30f); lineTo(bx + bw * 0.04f, top - 50f); lineTo(bx + bw * 0.12f, top - 90f); lineTo(bx + bw * 0.20f, top - 50f); lineTo(bx + bw * 0.20f, top + 10f)
          // wall with battlements
          var x = bx + bw * 0.20f
          while (x < bx + bw * 0.62f) { lineTo(x, top); lineTo(x, top - 18f); lineTo(x + 14f, top - 18f); lineTo(x + 14f, top); x += 28f }
          // tall tower
          lineTo(bx + bw * 0.62f, top - 20f); lineTo(bx + bw * 0.62f, top - 120f); lineTo(bx + bw * 0.72f, top - 175f); lineTo(bx + bw * 0.82f, top - 120f); lineTo(bx + bw * 0.82f, top - 10f)
          lineTo(bx + bw, top + 20f); lineTo(bx + bw, by); close()
        }
        val trees = Path().apply {
          fun fir(cx: Float, baseY: Float, th: Float) {
            val tw = th * 0.38f
            moveTo(cx - tw, baseY)
            for (k in 0 until 4) {
              val y = baseY - th * (k + 1) / 4f
              val ww = tw * (1f - k * 0.2f)
              lineTo(cx - ww * 0.55f, baseY - th * k / 4f - th * 0.04f); lineTo(cx - ww * 0.3f, y + th * 0.05f)
            }
            lineTo(cx, baseY - th)
            for (k in 3 downTo 0) {
              val y = baseY - th * (k + 1) / 4f
              val ww = tw * (1f - k * 0.2f)
              lineTo(cx + ww * 0.3f, y + th * 0.05f); lineTo(cx + ww * 0.55f, baseY - th * k / 4f - th * 0.04f)
            }
            lineTo(cx + tw, baseY); close()
          }
          fir(w * 0.06f, h, h * 0.20f); fir(w * 0.17f, h, h * 0.15f); fir(w * 0.30f, h, h * 0.18f); fir(w * 0.40f, h, h * 0.11f)
        }
        onDrawBehind {
          drawRect(sky)
          stars.forEach { (x, y, r) -> drawCircle(Color.White.copy(alpha = 0.35f), r, Offset(x, y)) }
          val off = (scroll() * 0.04f).coerceIn(0f, 60f)
          translate(top = -off) {
            drawCircle(glow, moonR * 3.2f, moonC)
            drawCircle(HW.Silver.copy(alpha = 0.62f), moonR, moonC)
            drawCircle(Color(0xFF8A90A6).copy(alpha = 0.22f), moonR * 0.2f, moonC + Offset(-moonR * 0.3f, -moonR * 0.1f))
            drawCircle(Color(0xFF8A90A6).copy(alpha = 0.18f), moonR * 0.12f, moonC + Offset(moonR * 0.35f, moonR * 0.3f))
          }
          translate(top = off * 0.3f) {
            drawPath(castle, HW.Ink)
            drawPath(trees, HW.Ink)
            // a lit castle window
            drawCircle(HW.Orange.copy(alpha = 0.55f), 3f, Offset(w * 0.56f + w * 0.44f * 0.72f, h * 0.80f - 120f))
          }
        }
      }
  ) {
    val t = frame.value / 1_000_000_000f
    val w = size.width
    val h = size.height
    // drifting fog at the bottom
    val fogShift = if (animate) sin(t * 0.12f) * w * 0.12f else 0f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, HW.Violet.copy(alpha = 0.10f), Color(0xFF1B1030).copy(alpha = 0.32f)), h * 0.62f, h))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB9A7E8).copy(alpha = 0.10f), Color.Transparent), Offset(w * 0.3f + fogShift, h * 0.93f), w * 0.7f), w * 0.7f, Offset(w * 0.3f + fogShift, h * 0.93f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB9A7E8).copy(alpha = 0.08f), Color.Transparent), Offset(w * 0.8f - fogShift, h * 0.88f), w * 0.6f), w * 0.6f, Offset(w * 0.8f - fogShift, h * 0.88f))
    // embers rising
    for (i in 0 until embers) {
      val s = seeds[i]
      val p = (t * s[1] + s[2]) % 1f
      val x = s[0] * w + sin(t * 0.8f + i) * 14f
      val y = h * (1f - p)
      val a = (sin(p * PI.toFloat())).coerceIn(0f, 1f) * 0.65f
      drawCircle(HW.Orange.copy(alpha = a), s[3], Offset(x, y))
    }
    // ghost wisps
    if (animate && !fx.lowEnd) {
      for (i in 0 until 3) {
        val p = (t * 0.02f + i * 0.33f) % 1f
        val x = w * (1.2f * p - 0.1f)
        val y = h * (0.30f + 0.12f * i) + sin(t * 0.5f + i * 2f) * 18f
        drawCircle(Brush.radialGradient(listOf(HW.Violet.copy(alpha = 0.16f), Color.Transparent), Offset(x, y), 46f), 46f, Offset(x, y))
      }
    }
  }
}
