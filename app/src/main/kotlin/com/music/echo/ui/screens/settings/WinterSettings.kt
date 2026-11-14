package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
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
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EasterEggsFoundKey
import echo.music.iad1tya.constants.EffectTouchKey
import echo.music.iad1tya.constants.FrostEdgesKey
import echo.music.iad1tya.constants.WaterFillKey
import echo.music.iad1tya.constants.WinterEnabledKey
import echo.music.iad1tya.constants.WinterLowPerfKey
import echo.music.iad1tya.constants.WinterVariantKey
import echo.music.iad1tya.ui.component.IconButton as AppIconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.component.WinterVariant
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

@Composable
private fun boolItem(title: String, desc: String, key: Preferences.Key<Boolean>, default: Boolean): Material3SettingsItem {
  var on by rememberPreference(key, default)
  return Material3SettingsItem(
    icon = painterResource(R.drawable.sparks),
    title = { Text(title) },
    description = { Text(desc) },
    trailingContent = { Switch(checked = on, onCheckedChange = { on = it }) },
    onClick = { on = !on },
  )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WinterSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
  val scroll = rememberScrollState()
  var variant by rememberPreference(WinterVariantKey, "frozen")
  val (eggs, _) = rememberPreference(EasterEggsFoundKey, 0)
  Column(
    Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
      .verticalScroll(scroll)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
    Spacer(Modifier.height(16.dp))
    Material3SettingsGroup(
      scrollState = scroll,
      title = "Winter",
      items =
        listOf(
          boolItem("Winter mode", "Icy navy theme, aurora Home and winter effects. Turn off to return to your normal theme", WinterEnabledKey, true),
          boolItem("Frost edges", "Frosted glow around the screen edges", FrostEdgesKey, false),
          boolItem("Touch reacts", "Flakes and particles move away from your finger", EffectTouchKey, true),
          boolItem("Water fill", "Pull the cord at the top of Home to fill the screen with water. Albums float on it", WaterFillKey, false),
          boolItem("Low-performance mode", "Fewer particles, 30 fps and a still aurora. Effects also pause when the app is in the background", WinterLowPerfKey, false),
        ),
    )
    Spacer(Modifier.height(16.dp))
    Text("Theme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
      WinterVariant.entries.forEach { v -> FilterChip(selected = variant == v.key, onClick = { variant = v.key }, label = { Text(v.label) }) }
    }
    Spacer(Modifier.height(12.dp))
    Text("Falling snow, fire sparks, leaves, spin and water drops are now in Settings > Overlay effects.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Text("Hidden snowflakes found: $eggs  (golden flakes drift through the snow, tap them)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
  }
  TopAppBar(
    title = { Text("Winter") },
    navigationIcon = {
      AppIconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
