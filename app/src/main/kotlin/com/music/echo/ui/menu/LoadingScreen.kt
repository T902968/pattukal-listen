package echo.music.iad1tya.ui.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import echo.music.iad1tya.R

@Composable
fun LoadingScreen(
  isVisible: Boolean,
  value: Int,
) {
  if (!isVisible) return

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary

  Dialog(onDismissRequest = {}) {
    val transition = rememberInfiniteTransition(label = "pattukal_loader")
    val rotation by
      transition.animateFloat(
        initialValue = -10f,
        targetValue = 350f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "loader_rotation",
      )
    val pulse by
      transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "loader_pulse",
      )

    Box(
      modifier =
        Modifier
          .size(210.dp)
          .background(Color.Transparent)
          .drawBehind {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      primaryColor.copy(alpha = 0.34f),
                      secondaryColor.copy(alpha = 0.14f),
                      Color.Transparent,
                    ),
                  center = center,
                  radius = size.minDimension * 0.48f,
                ),
              radius = size.minDimension * 0.48f,
            )
          },
      contentAlignment = Alignment.Center,
    ) {
      // A lightweight Compose version of the supplied 3D loader idea:
      // layered cubes, perspective rotation and a breathing ambient halo.
      Box(
        modifier =
          Modifier
            .size(86.dp)
            .graphicsLayer {
              rotationX = -18f
              rotationY = rotation * 0.35f
              rotationZ = rotation
              scaleX = pulse
              scaleY = pulse
            }
            .background(
              brush =
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary,
                  )
                ),
              shape = RoundedCornerShape(22.dp),
            )
      ) {
        Box(
          modifier =
            Modifier
              .align(Alignment.Center)
              .size(58.dp)
              .graphicsLayer {
                rotationZ = -rotation * 0.55f
                rotationY = rotation * 0.2f
              }
              .background(
                Color.Black.copy(alpha = 0.22f),
                RoundedCornerShape(17.dp),
              )
        )
        Icon(
          painter = painterResource(R.drawable.music_note),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.align(Alignment.Center).size(34.dp),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(R.string.progress_percent, value.toString()),
          color = Color.White,
          fontSize = 18.sp,
          style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(4.dp))
        Text(
          text = "•",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 18.sp,
        )
      }
    }
  }
}
