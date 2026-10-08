@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: (() -> Unit)? = null,
  highlightKey: String? = null
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("About Paattukal", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(
            onClick = { onBack?.invoke() ?: navController.navigateUp() },
            onLongClick = navController::backToMain,
          ) {
            Icon(
              painter = androidx.compose.ui.res.painterResource(R.drawable.arrow_back),
              contentDescription = null
            )
          }
        },
        windowInsets = TopAppBarDefaults.windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
        )
        .padding(
          start = 18.dp,
          end = 18.dp,
          top = innerPadding.calculateTopPadding() + 18.dp,
          bottom = 28.dp
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(250.dp)
          .background(
            brush = Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .padding(28.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            tonalElevation = 10.dp
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_nobg),
                contentDescription = null,
                modifier = Modifier.padding(18.dp),
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(Modifier.height(18.dp))

          Text(
            "Paattukal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
          )

          Text(
            "AKSHAYS UI",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
        tonalElevation = 2.dp
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            "Built for listening.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            "A premium, lightweight music experience focused on your library, albums and playlists.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            "Inspired by Echo Music",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
          )
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Version", fontWeight = FontWeight.SemiBold)
          Text(
            BuildConfig.VERSION_NAME,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
