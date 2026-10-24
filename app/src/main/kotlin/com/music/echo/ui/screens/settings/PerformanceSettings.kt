package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import echo.music.iad1tya.constants.BatterySaverKey
import echo.music.iad1tya.constants.LiquidGlassGlobalEnabledKey
import echo.music.iad1tya.constants.LowEndModeKey
import echo.music.iad1tya.constants.ReduceAnimationsKey
import echo.music.iad1tya.ui.component.IconButton as AppIconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

@Composable
private fun perfItem(
  title: String,
  desc: String,
  key: Preferences.Key<Boolean>,
  default: Boolean,
): Material3SettingsItem {
  var on by rememberPreference(key, default)
  return Material3SettingsItem(
    icon = painterResource(R.drawable.sparks),
    title = { Text(title) },
    description = { Text(desc) },
    trailingContent = { Switch(checked = on, onCheckedChange = { on = it }) },
    onClick = { on = !on },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
  val scroll = rememberScrollState()
  Column(
    Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
      .verticalScroll(scroll)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
    Spacer(Modifier.height(16.dp))
    Material3SettingsGroup(
      scrollState = scroll,
      title = "Performance",
      items =
        listOf(
          perfItem("Low-end device mode", "Turns off liquid glass and animated backgrounds for smooth use on older phones", LowEndModeKey, false),
          perfItem("Battery saver", "Same as low-end mode, so the screen does less work and uses less battery", BatterySaverKey, false),
          perfItem("Reduce animations", "Stops moving backgrounds and decorative animations", ReduceAnimationsKey, false),
          perfItem("Liquid glass effect", "Turn off to remove blur and lag on weaker phones", LiquidGlassGlobalEnabledKey, true),
        ),
    )
    Spacer(Modifier.height(24.dp))
  }
  TopAppBar(
    title = { Text("Performance") },
    navigationIcon = {
      AppIconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
