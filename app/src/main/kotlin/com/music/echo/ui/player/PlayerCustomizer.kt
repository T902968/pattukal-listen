package echo.music.iad1tya.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Cyan = Color(0xFF22D3EE)
private val Violet = Color(0xFF8B5CF6)
private val Blue = Color(0xFF3B82F6)
private val Ink = Color(0xFF05060F)
private val Glass = Color(0xFFFFFFFF)

private fun pretty(name: String) = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

enum class GarageTab(val label: String, val hint: String) {
  BODY("BODY", "Layout, artwork frame and position"),
  PAINT("PAINT", "Background and accent colour"),
  LIGHTING("LIGHTING", "Ambient album-art glow"),
  WHEELS("WHEELS", "Control buttons and icons"),
  INTERIOR("INTERIOR", "Slider, typography and sizing"),
  PERFORMANCE("PERFORMANCE", "Animations and effects"),
}

private val Accents = listOf(0, 0xFF22D3EE.toInt(), 0xFF8B5CF6.toInt(), 0xFF3B82F6.toInt(), 0xFFFF4FA3.toInt(), 0xFFFF9A3C.toInt(), 0xFF34D399.toInt(), 0xFFF43F5E.toInt(), 0xFFFACC15.toInt())

/**
 * Pattukal Player Garage: the Now Playing edit mode upgraded to a full customization studio with a live
 * preview, six categories and saved builds. Every control writes straight to the real preferences, so the
 * actual player behind it is always showing the current build; Cancel restores what you had on entry.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerEditPanel(tab: GarageTab, onTabChange: (GarageTab) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

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
  var glow by rememberPreference(GarageGlowKey, false)
  var pulse by rememberPreference(GarageGlowPulseKey, true)
  var glowStrength by rememberPreference(GarageGlowStrengthKey, 0.6f)
  var accent by rememberPreference(GarageAccentKey, 0)
  var shadow by rememberPreference(GarageArtShadowKey, 0f)
  var artAlpha by rememberPreference(GarageArtAlphaKey, 1f)
  var ctrlScale by rememberPreference(GarageControlsScaleKey, 1f)
  var fontScale by rememberPreference(GarageFontScaleKey, 1f)
  val activeId by rememberPreference(GarageActiveBuildKey, "")

  var entry by remember { mutableStateOf<JSONObject?>(null) }
  var builds by remember { mutableStateOf(listOf<GarageBuild>()) }
  var nameDialog by remember { mutableStateOf<Pair<String?, String>?>(null) } // (buildId or null for new, text)
  var menuFor by remember { mutableStateOf<GarageBuild?>(null) }

  LaunchedEffect(Unit) { entry = GarageBuilds.snapshot(context); builds = GarageBuilds.load(context) }
  fun reloadBuilds() = scope.launch { builds = GarageBuilds.load(context) }

  val meta by (LocalPlayerConnection.current?.mediaMetadata ?: remember { MutableStateFlow(null) }).collectAsState()
  val accentColor = if (accent == 0) Cyan else Color(accent)

  Column(
    modifier.fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
      .background(Brush.verticalGradient(listOf(Color(0xF00A0B26), Color(0xF405060F))))
      .border(1.dp, Brush.horizontalGradient(listOf(Cyan, Violet, Blue)), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
      .padding(top = 10.dp),
  ) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
        items(GarageTab.entries.toList()) { t ->
          val sel = t == tab
          Text(
            t.label, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold,
            color = if (sel) Ink else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.clip(RoundedCornerShape(50))
              .background(if (sel) Brush.horizontalGradient(listOf(Cyan, Violet)) else Brush.horizontalGradient(listOf(Glass.copy(alpha = 0.08f), Glass.copy(alpha = 0.08f))))
              .clickable { onTabChange(t) }.padding(horizontal = 12.dp, vertical = 7.dp),
          )
        }
      }
      TextButton(onClick = { scope.launch { GarageBuilds.restoreDefaults(context) } }) { Text("Reset all", color = Violet, fontSize = 12.sp) }
    }
        // Panel
        Column(
          Modifier.heightIn(max = 230.dp).fillMaxWidth().padding(top = 4.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
          AnimatedContent(targetState = tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "garage-tab") { t ->
            Column {
              Text(t.hint, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 4.dp))
              when (t) {
                GarageTab.BODY -> {
                  Label("Artwork frame")
                  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Square" to 0f, "Rounded" to 16f, "Soft" to 36f, "Circle" to 300f).forEach { (l, v) ->
                      GChip(l, !disc && corner == v) { disc = false; corner = v }
                    }
                    GChip("Vinyl disc", disc) { disc = true }
                  }
                  Label("Corner radius")
                  GSlider(corner.coerceAtMost(60f), { corner = it; disc = false }, 0f..60f)
                  Label("Artwork size")
                  GSlider(scale, { scale = it }, 0.6f..1.15f)
                  Label("Artwork position (or drag it in edit mode)")
                  GSlider(thumbY, { thumbY = it }, -120f..160f)
                  Label("Controls position (or drag them)")
                  GSlider(ctrlY, { ctrlY = it }, -160f..120f)
                  Label("Artwork transparency")
                  GSlider(artAlpha, { artAlpha = it }, 0.3f..1f)
                  Label("Artwork shadow")
                  GSlider(shadow, { shadow = it }, 0f..32f)
                  Reset { corner = 3f; disc = false; scale = 1f; thumbY = 0f; ctrlY = 0f; artAlpha = 1f; shadow = 0f }
                }
                GarageTab.PAINT -> {
                  Label("Player background")
                  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectablePlayerBackgroundStyles.forEach { s -> GChip(pretty(s.name), background == s) { background = s } }
                  }
                  Label("Accent colour")
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScrollCompat()) {
                    Accents.forEach { c ->
                      Box(
                        Modifier.size(38.dp).clip(CircleShape)
                          .background(if (c == 0) Brush.sweepGradient(listOf(Cyan, Violet, Blue, Cyan)) else Brush.linearGradient(listOf(Color(c), Color(c))))
                          .border(if (accent == c) 3.dp else 1.dp, if (accent == c) Color.White else Glass.copy(alpha = 0.3f), CircleShape)
                          .clickable { accent = c },
                      )
                    }
                  }
                  Text(if (accent == 0) "Following your theme" else "Custom accent on buttons and sliders", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 6.dp))
                  Reset { background = PlayerBackgroundStyle.DEFAULT; accent = 0 }
                }
                GarageTab.LIGHTING -> {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Ambient album-art glow", color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = glow, onCheckedChange = { glow = it })
                  }
                  Label("Glow strength")
                  GSlider(glowStrength, { glowStrength = it; glow = true }, 0.1f..1f)
                  Label("Glow uses your accent colour when one is set, otherwise cyan.")
                  Reset { glow = false; glowStrength = 0.6f }
                }
                GarageTab.WHEELS -> {
                  Label("Button style")
                  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerButtonsStyle.entries.forEach { s -> GChip(pretty(s.name), buttons == s) { buttons = s } }
                  }
                  Label("Control size")
                  GSlider(ctrlScale, { ctrlScale = it }, 0.8f..1.25f)
                  Reset { buttons = PlayerButtonsStyle.DEFAULT; ctrlScale = 1f }
                }
                GarageTab.INTERIOR -> {
                  Label("Progress bar")
                  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderStyle.entries.forEach { s -> GChip(pretty(s.name), slider == s) { slider = s } }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Hide progress bar", color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = hideSlider, onCheckedChange = { hideSlider = it })
                  }
                  Label("Text size")
                  GSlider(fontScale, { fontScale = it }, 0.85f..1.3f)
                  Reset { slider = SliderStyle.WAVY; hideSlider = false; fontScale = 1f }
                }
                GarageTab.PERFORMANCE -> {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Spin artwork while playing", color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = rotating, onCheckedChange = { rotating = it })
                  }
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Glow breathing animation", color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = pulse, onCheckedChange = { pulse = it })
                  }
                  Label("Turn animations off to save battery. Everything else in the player keeps working.")
                  Reset { rotating = false; pulse = true }
                }
              }
            }
          }

          // Builds
          Label("BUILDS")
          if (builds.isEmpty()) Text("No saved builds yet. Tune your player and press Save build.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(builds) { b ->
              val sel = b.id == activeId
              Column(
                Modifier.clip(RoundedCornerShape(16.dp)).background(Glass.copy(alpha = 0.07f))
                  .border(BorderStroke(if (sel) 2.dp else 1.dp, if (sel) Cyan else Glass.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                  .clickable { scope.launch { GarageBuilds.apply(context, b) } }.padding(horizontal = 14.dp, vertical = 10.dp),
              ) {
                Text(b.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Tap to switch", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                Text("Manage", color = Violet, fontSize = 11.sp, modifier = Modifier.clickable { menuFor = b }.padding(top = 4.dp))
              }
            }
          }
          Spacer(Modifier.height(20.dp))
        }


    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      GButton("Undo", false, Modifier.weight(1f)) { scope.launch { entry?.let { GarageBuilds.restore(context, it) } } }
      GButton("Save build", false, Modifier.weight(1f)) { nameDialog = (activeId.ifBlank { null }?.takeIf { id -> builds.any { it.id == id } }) to (builds.firstOrNull { it.id == activeId }?.name ?: "My build ${builds.size + 1}") }
      GButton("Done", true, Modifier.weight(1f)) { onDismiss() }
    }
  }

  nameDialog?.let { (id, text) ->
    var t by remember { mutableStateOf(text) }
    AlertDialog(
      onDismissRequest = { nameDialog = null },
      title = { Text(if (id == null) "Save new build" else "Save build") },
      text = { OutlinedTextField(value = t, onValueChange = { t = it }, singleLine = true, label = { Text("Build name") }) },
      confirmButton = { TextButton(onClick = { scope.launch { GarageBuilds.save(context, t, id); reloadBuilds().join(); nameDialog = null } }) { Text(if (id == null) "Save" else "Update") } },
      dismissButton = {
        Row {
          if (id != null) TextButton(onClick = { scope.launch { GarageBuilds.save(context, t, null); reloadBuilds().join(); nameDialog = null } }) { Text("Save as new") }
          TextButton(onClick = { nameDialog = null }) { Text("Cancel") }
        }
      },
    )
  }

  menuFor?.let { b ->
    var rename by remember { mutableStateOf(b.name) }
    AlertDialog(
      onDismissRequest = { menuFor = null },
      title = { Text("Manage build") },
      text = { OutlinedTextField(value = rename, onValueChange = { rename = it }, singleLine = true, label = { Text("Name") }) },
      confirmButton = { TextButton(onClick = { scope.launch { GarageBuilds.rename(context, b.id, rename); reloadBuilds().join(); menuFor = null } }) { Text("Rename") } },
      dismissButton = {
        Row {
          TextButton(onClick = { scope.launch { GarageBuilds.duplicate(context, b.id); reloadBuilds().join(); menuFor = null } }) { Text("Duplicate") }
          TextButton(onClick = { scope.launch { GarageBuilds.delete(context, b.id); reloadBuilds().join(); menuFor = null } }) { Text("Delete") }
        }
      },
    )
  }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun GaragePreview(
  thumb: String?, title: String, artist: String, corner: Float, disc: Boolean, rotating: Boolean, scale: Float, artAlpha: Float,
  shadow: Float, glow: Boolean, pulse: Boolean, glowStrength: Float, accent: Color, buttons: PlayerButtonsStyle,
  slider: SliderStyle, hideSlider: Boolean, ctrlScale: Float, fontScale: Float,
) {
  val inf = rememberInfiniteTransition(label = "garage-preview")
  val breathe by inf.animateFloat(0.75f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "breathe")
  val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin")
  val shape = if (disc) CircleShape else RoundedCornerShape(corner.coerceIn(0f, 60f).dp)
  val btnColor = when (buttons) { PlayerButtonsStyle.PRIMARY -> accent; PlayerButtonsStyle.TERTIARY -> Violet; else -> Color.White }
  Box(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(250.dp).clip(RoundedCornerShape(28.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFF0E1030), Color(0xFF07081A))))
      .border(1.5.dp, Brush.linearGradient(listOf(Cyan, Violet, Blue)), RoundedCornerShape(28.dp)),
  ) {
    if (glow) Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp).size(220.dp).alpha((glowStrength * (if (pulse) breathe else 1f)).coerceIn(0f, 1f))
      .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.75f), Color.Transparent)), CircleShape))
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.height(124.dp), contentAlignment = Alignment.Center) {
        AsyncImage(
          model = thumb, contentDescription = null, contentScale = ContentScale.Crop,
          modifier = Modifier.size((108 * scale).dp).shadow(shadow.dp, shape).clip(shape).alpha(artAlpha)
            .then(if (rotating) Modifier.androidxRotate(spin) else Modifier),
        )
      }
      Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (15 * fontScale).sp, maxLines = 1)
      Text(artist, color = Color.White.copy(alpha = 0.6f), fontSize = (12 * fontScale).sp, maxLines = 1)
      if (!hideSlider) {
        Slider(value = 0.38f, onValueChange = {}, enabled = true, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
          modifier = Modifier.padding(horizontal = 12.dp).height(if (slider == SliderStyle.SLIM) 18.dp else 28.dp))
      }
      Row(horizontalArrangement = Arrangement.spacedBy((16 * ctrlScale).dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("⏮" to 30, "▶" to 44, "⏭" to 30).forEach { (g, s) ->
          Box(Modifier.size((s * ctrlScale).dp).clip(CircleShape).background(btnColor.copy(alpha = if (g == "▶") 0.95f else 0.18f)), contentAlignment = Alignment.Center) {
            Text(g, color = if (g == "▶") Ink else btnColor, fontSize = (14 * ctrlScale).sp)
          }
        }
      }
    }
  }
}

private fun Modifier.androidxRotate(deg: Float): Modifier = this.then(Modifier.graphicsLayerRotate(deg))
private fun Modifier.graphicsLayerRotate(deg: Float): Modifier = this.graphicsLayer { rotationZ = deg }

@Composable
private fun Label(text: String) {
  Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cyan.copy(alpha = 0.9f), modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun GSlider(value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
  Slider(value = value, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Violet, inactiveTrackColor = Color.White.copy(alpha = 0.18f)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GChip(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(
    selected = selected, onClick = onClick, label = { Text(label) },
    colors = FilterChipDefaults.filterChipColors(
      containerColor = Glass.copy(alpha = 0.07f), labelColor = Color.White.copy(alpha = 0.85f),
      selectedContainerColor = Violet.copy(alpha = 0.55f), selectedLabelColor = Color.White,
    ),
  )
}

@Composable
private fun Reset(onClick: () -> Unit) {
  TextButton(onClick = onClick, modifier = Modifier.padding(top = 10.dp)) { Text("Reset this category", color = Violet) }
}

@Composable
private fun GButton(label: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier.clip(RoundedCornerShape(50))
      .background(if (primary) Brush.horizontalGradient(listOf(Cyan, Violet)) else Brush.horizontalGradient(listOf(Glass.copy(alpha = 0.1f), Glass.copy(alpha = 0.1f))))
      .clickable(onClick = onClick).padding(vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) { Text(label, color = if (primary) Ink else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}
