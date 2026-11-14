package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EffectTouchKey
import echo.music.iad1tya.constants.OverlayEffectKey
import echo.music.iad1tya.constants.OverlayEnabledKey
import echo.music.iad1tya.constants.OverlayIntensityKey
import echo.music.iad1tya.constants.SnowModeKey
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.ui.component.IconButton as AppIconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OverlaySettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
  val scroll = rememberScrollState()
  var enabled by rememberPreference(OverlayEnabledKey, true)
  var effect by rememberPreference(OverlayEffectKey, "snow")
  var mode by rememberPreference(SnowModeKey, "gentle")
  var intensity by rememberPreference(OverlayIntensityKey, 12)
  var touch by rememberPreference(EffectTouchKey, true)
  var lowPerf by rememberPreference(WinterLowPerfKey, false)
  Column(
    Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
      .verticalScroll(scroll)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
    Spacer(Modifier.height(16.dp))
    Material3SettingsGroup(
      scrollState = scroll,
      title = "Overlay effects",
      items =
        listOf(
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Overlay effect") },
            description = { Text("Draws over every screen. Never blocks touches. Pauses when the app is in the background") },
            trailingContent = { Switch(checked = enabled, onCheckedChange = { enabled = it }) },
            onClick = { enabled = !enabled },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Touch reacts") },
            description = { Text("Particles move away from your finger") },
            trailingContent = { Switch(checked = touch, onCheckedChange = { touch = it }) },
            onClick = { touch = !touch },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Low-performance mode") },
            description = { Text("Fewer particles at 30 fps") },
            trailingContent = { Switch(checked = lowPerf, onCheckedChange = { lowPerf = it }) },
            onClick = { lowPerf = !lowPerf },
          ),
        ),
    )
    Spacer(Modifier.height(16.dp))
    Text("Effect", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
      listOf("snow" to "Winter snow", "fire" to "Fire sparks", "leaves" to "Falling leaves", "spin" to "Spin", "water" to "Water drops").forEach { (k, l) ->
        FilterChip(selected = effect == k, onClick = { effect = k }, label = { Text(l) })
      }
    }
    if (effect == "snow") {
      Spacer(Modifier.height(16.dp))
      Text("Snow mode", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        FilterChip(selected = mode == "gentle", onClick = { mode = "gentle" }, label = { Text("Gentle snowfall") })
        FilterChip(selected = mode == "storm", onClick = { mode = "storm" }, label = { Text("Snowstorm") })
      }
    }
    Spacer(Modifier.height(16.dp))
    Text("Intensity: $intensity%" + if (intensity <= 15) "  (very low)" else "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Slider(value = intensity.toFloat(), onValueChange = { intensity = it.toInt().coerceAtLeast(1) }, valueRange = 1f..100f)
    Spacer(Modifier.height(24.dp))
  }
  TopAppBar(
    title = { Text("Overlay effects") },
    navigationIcon = {
      AppIconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
