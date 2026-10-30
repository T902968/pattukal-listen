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
import echo.music.iad1tya.constants.HalloweenAmbientKey
import echo.music.iad1tya.constants.HalloweenChoiceKey
import echo.music.iad1tya.constants.HalloweenPortalKey
import echo.music.iad1tya.constants.HalloweenReducedMotionKey
import echo.music.iad1tya.ui.component.IconButton as AppIconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalloweenSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
  val scroll = rememberScrollState()
  var choice by rememberPreference(HalloweenChoiceKey, "")
  val enabled = choice == "halloween"
  Column(
    Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
      .verticalScroll(scroll)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
    Spacer(Modifier.height(16.dp))
    Material3SettingsGroup(
      scrollState = scroll,
      title = "Seasonal Halloween Test (Experimental)",
      items =
        listOf(
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Halloween effects") },
            description = { Text("Haunted Home, cursed cube, portals. Turn off to return to the original Pattukal theme. Your settings and playback are not changed.") },
            trailingContent = { Switch(checked = enabled, onCheckedChange = { choice = if (it) "halloween" else "original" }) },
            onClick = { choice = if (enabled) "original" else "halloween" },
          ),
          boolItem("Reduced motion", "Shorter, simpler animations for Halloween effects", HalloweenReducedMotionKey, false),
          boolItem("Song-opening portal", "Reality Tear transition when a new song starts", HalloweenPortalKey, true),
          boolItem("Ambient wind sound", "Quiet generated wind on Home. Off by default", HalloweenAmbientKey, false),
        ),
    )
    Spacer(Modifier.height(24.dp))
  }
  TopAppBar(
    title = { Text("Halloween (Experimental)") },
    navigationIcon = {
      AppIconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
