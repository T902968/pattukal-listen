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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.GestureMusicEnabledKey
import echo.music.iad1tya.constants.GestureMusicGlitterKey
import echo.music.iad1tya.constants.GestureMusicManualSeenKey
import echo.music.iad1tya.constants.GestureMusicWriteKey
import echo.music.iad1tya.constants.GestureMusicWriteLangKey
import echo.music.iad1tya.ui.component.GestureMusicManual
import echo.music.iad1tya.ui.component.IconButton as AppIconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

private val writeLanguages = listOf("en" to "English", "ml" to "Malayalam", "hi" to "Hindi", "ta" to "Tamil", "te" to "Telugu", "kn" to "Kannada")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMusicSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
  val scroll = rememberScrollState()
  var enabled by rememberPreference(GestureMusicEnabledKey, true)
  var write by rememberPreference(GestureMusicWriteKey, true)
  var glitter by rememberPreference(GestureMusicGlitterKey, true)
  var lang by rememberPreference(GestureMusicWriteLangKey, "en")
  var manualSeen by rememberPreference(GestureMusicManualSeenKey, false)
  var showManual by remember { mutableStateOf(false) }
  if (showManual) GestureMusicManual { showManual = false; manualSeen = true }
  val langLabel = writeLanguages.firstOrNull { it.first == lang }?.second ?: "English"

  Column(
    Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
      .verticalScroll(scroll)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
    Spacer(Modifier.height(16.dp))
    Material3SettingsGroup(
      scrollState = scroll,
      title = "Gesture Music",
      items =
        listOf(
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Gesture Music") },
            description = { Text("Double tap and hold anywhere, then draw. Circle = Favorites, wave = Recently played, big diagonal = shuffle, heart = like the current song. Ordinary taps and scrolling are never affected") },
            trailingContent = { Switch(checked = enabled, onCheckedChange = { enabled = it }) },
            onClick = { enabled = !enabled },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Write a song name") },
            description = { Text("Write letters with your finger after the double tap and hold. Pause, and that song plays. First use downloads a small handwriting model") },
            trailingContent = { Switch(checked = write, onCheckedChange = { write = it }) },
            onClick = { write = !write },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Writing language") },
            description = { Text("$langLabel (tap to change)") },
            onClick = {
              val i = writeLanguages.indexOfFirst { it.first == lang }
              lang = writeLanguages[(i + 1) % writeLanguages.size].first
            },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("Glitter glitch trail") },
            description = { Text("Sparkles and RGB glitch while you draw") },
            trailingContent = { Switch(checked = glitter, onCheckedChange = { glitter = it }) },
            onClick = { glitter = !glitter },
          ),
          Material3SettingsItem(
            icon = painterResource(R.drawable.sparks),
            title = { Text("How to use") },
            description = { Text("Show the Gesture Music guide again") },
            onClick = { showManual = true },
          ),
        ),
    )
    Spacer(Modifier.height(24.dp))
  }
  TopAppBar(
    title = { Text("Gesture Music") },
    navigationIcon = {
      AppIconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
