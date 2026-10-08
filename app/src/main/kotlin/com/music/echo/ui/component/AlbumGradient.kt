package echo.music.iad1tya.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import echo.music.iad1tya.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumGradient(thumbnailUrl: String?, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val surfaceColor = MaterialTheme.colorScheme.surface
  val fallbackColorInt = MaterialTheme.colorScheme.primaryContainer.toArgb()
  var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

  LaunchedEffect(thumbnailUrl) {
    if (thumbnailUrl == null) return@LaunchedEffect
    withContext(Dispatchers.IO) {
      try {
        val request = ImageRequest.Builder(context).data(thumbnailUrl).size(96, 96).build()
        val bitmap = context.imageLoader.execute(request).image?.toBitmap()
        if (bitmap != null) {
          val palette = withContext(Dispatchers.Default) {
            Palette.from(bitmap).maximumColorCount(6).resizeBitmapArea(96 * 96).generate()
          }
          extractedColors = PlayerColorExtractor.extractGradientColors(palette, fallbackColorInt)
        }
      } catch (_: Exception) {}
    }
  }

  val color1 by animateColorAsState(
    extractedColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary,
    tween(700),
    label = "ambientColor1"
  )
  val color2 by animateColorAsState(
    extractedColors.getOrNull(1) ?: MaterialTheme.colorScheme.secondary,
    tween(700),
    label = "ambientColor2"
  )

  Box(
    modifier = modifier.background(
      Brush.radialGradient(
        colors = listOf(
          color1.copy(alpha = 0.34f),
          color2.copy(alpha = 0.18f),
          surfaceColor.copy(alpha = 0.94f),
          surfaceColor
        )
      )
    )
  )
}
