package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** GTA VI inspired sunset: hot pink to orange to violet sky, a low sun and palm silhouettes. All drawn, no copyrighted art. */
@Composable
fun GtaViBackground(modifier: Modifier = Modifier) {
  Canvas(modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B0640), Color(0xFF7B1FA2), Color(0xFFFF4FA3), Color(0xFFFF9A3C), Color(0xFFFFD36B)), 0f, h * 0.78f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1B8), Color(0xFFFF9A3C).copy(alpha = 0.0f)), Offset(w * 0.5f, h * 0.7f), w * 0.55f), w * 0.55f, Offset(w * 0.5f, h * 0.7f))
    drawCircle(Color(0xFFFFE08A), w * 0.17f, Offset(w * 0.5f, h * 0.7f))
    // ground
    drawRect(Color(0xFF12041F), Offset(0f, h * 0.82f), androidx.compose.ui.geometry.Size(w, h * 0.18f))
    // palms
    fun palm(x: Float, base: Float, height: Float, lean: Float) {
      val top = Offset(x + lean, base - height)
      val trunk = Path().apply {
        moveTo(x - 6f, base); lineTo(x + 6f, base)
        lineTo(top.x + 3f, top.y); lineTo(top.x - 3f, top.y); close()
      }
      drawPath(trunk, Color(0xFF12041F))
      for (a in listOf(-150f, -120f, -90f, -60f, -30f, -170f, -10f)) {
        val rad = Math.toRadians(a.toDouble())
        val len = height * 0.42f
        val end = Offset(top.x + (Math.cos(rad) * len).toFloat(), top.y + (Math.sin(rad) * len * 0.6f).toFloat() + len * 0.28f)
        val mid = Offset((top.x + end.x) / 2f, top.y - len * 0.22f)
        val leaf = Path().apply { moveTo(top.x, top.y); quadraticBezierTo(mid.x, mid.y, end.x, end.y); quadraticBezierTo(mid.x, mid.y + 14f, top.x, top.y + 8f); close() }
        drawPath(leaf, Color(0xFF12041F))
      }
    }
    val base = h * 0.84f
    palm(w * 0.12f, base, h * 0.42f, 40f)
    palm(w * 0.88f, base, h * 0.36f, -34f)
    palm(w * 0.03f, base, h * 0.28f, 22f)
  }
}
