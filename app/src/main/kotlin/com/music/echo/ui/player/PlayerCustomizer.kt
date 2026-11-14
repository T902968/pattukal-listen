package echo.music.iad1tya.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.constants.DiscThumbnailKey
import echo.music.iad1tya.constants.HidePlayerSliderKey
import echo.music.iad1tya.constants.PlayerBackgroundStyle
import echo.music.iad1tya.constants.PlayerBackgroundStyleKey
import echo.music.iad1tya.constants.PlayerButtonsStyle
import echo.music.iad1tya.constants.PlayerButtonsStyleKey
import echo.music.iad1tya.constants.PlayerControlsOffsetYKey
import echo.music.iad1tya.constants.PlayerThumbOffsetYKey
import echo.music.iad1tya.constants.PlayerThumbScaleKey
import echo.music.iad1tya.constants.RotatingThumbnailKey
import echo.music.iad1tya.constants.SelectablePlayerBackgroundStyles
import echo.music.iad1tya.constants.SliderStyle
import echo.music.iad1tya.constants.SliderStyleKey
import echo.music.iad1tya.constants.ThumbnailCornerRadiusKey
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference

private fun pretty(name: String) = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Edit mode for the Now Playing screen: shape, size, position, background, buttons, slider and disc. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerCustomizerSheet(onDismiss: () -> Unit) {
  var corner by rememberPreference(ThumbnailCornerRadiusKey, 3f)
  var disc by rememberPreference(DiscThumbnailKey, false)
  var rotating by rememberPreference(RotatingThumbnailKey, false)
  var scale by rememberPreference(PlayerThumbScaleKey, 1f)
  var thumbY by rememberPreference(PlayerThumbOffsetYKey, 0f)
  var ctrlY by rememberPreference(PlayerControlsOffsetYKey, 0f)
  var background by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
  var buttons by rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
  var slider by rememberEnumPreference(SliderStyleKey, SliderStyle.WAVY)
  var hideSlider by rememberPreference(HidePlayerSliderKey, false)

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(bottom = 8.dp)) {
        Text("Customize player", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = {
          corner = 3f; disc = false; rotating = false; scale = 1f; thumbY = 0f; ctrlY = 0f
          background = PlayerBackgroundStyle.DEFAULT; buttons = PlayerButtonsStyle.DEFAULT; slider = SliderStyle.WAVY; hideSlider = false
        }) { Text("Reset") }
      }
      Section("Thumbnail shape")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Square" to 0f, "Rounded" to 16f, "Soft" to 36f, "Circle" to 300f).forEach { (l, v) ->
          FilterChip(selected = !disc && corner == v, onClick = { disc = false; corner = v }, label = { Text(l) })
        }
        FilterChip(selected = disc, onClick = { disc = true }, label = { Text("Vinyl disc") })
      }
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text("Spin while playing", modifier = Modifier.weight(1f)); Switch(checked = rotating, onCheckedChange = { rotating = it })
      }
      Section("Thumbnail size")
      Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.6f..1.15f)
      Section("Thumbnail position")
      Slider(value = thumbY, onValueChange = { thumbY = it }, valueRange = -60f..160f)
      Section("Controls position")
      Slider(value = ctrlY, onValueChange = { ctrlY = it }, valueRange = -90f..90f)
      Section("Background")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectablePlayerBackgroundStyles.forEach { s -> FilterChip(selected = background == s, onClick = { background = s }, label = { Text(pretty(s.name)) }) }
      }
      Section("Buttons")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayerButtonsStyle.entries.forEach { s -> FilterChip(selected = buttons == s, onClick = { buttons = s }, label = { Text(pretty(s.name)) }) }
      }
      Section("Slider")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SliderStyle.entries.forEach { s -> FilterChip(selected = slider == s, onClick = { slider = s }, label = { Text(pretty(s.name)) }) }
      }
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text("Hide slider", modifier = Modifier.weight(1f)); Switch(checked = hideSlider, onCheckedChange = { hideSlider = it })
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun Section(text: String) {
  Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}
