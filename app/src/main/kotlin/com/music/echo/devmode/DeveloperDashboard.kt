package echo.music.iad1tya.devmode

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Cyan = Color(0xFF22D3EE)
private val Violet = Color(0xFF8B5CF6)
private val Ink = Color(0xFF05060F)
private val Glass = Color.White

private enum class DevTab(val label: String) { CONTENT("Content"), PREVIEW("Preview"), PUBLISH("Publish"), HISTORY("History") }

private fun move(list: List<HomeSection>, i: Int, d: Int): List<HomeSection> {
  val j = i + d
  if (j !in list.indices) return list
  return list.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
}

/** Hidden developer dashboard: edit the Songs home content, preview it, publish through GitHub, roll back. */
@Composable
fun DeveloperDashboardScreen(navController: NavController) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val remote by HomeConfigRepository.config.collectAsState()
  val status by HomeConfigRepository.status.collectAsState()
  var draft by remember { mutableStateOf(remote ?: HomeConfig.empty()) }
  var tab by remember { mutableStateOf(DevTab.CONTENT) }
  var token by remember { mutableStateOf(SecureToken.load(context).orEmpty()) }
  var message by remember { mutableStateOf("") }
  var commitMsg by remember { mutableStateOf("Update home content") }
  var busy by remember { mutableStateOf(false) }
  var history by remember { mutableStateOf(listOf<ConfigCommit>()) }
  var searchFor by remember { mutableStateOf<String?>(null) } // section id
  var playlistFor by remember { mutableStateOf<String?>(null) }
  var confirmRollback by remember { mutableStateOf<ConfigCommit?>(null) }
  var addSection by remember { mutableStateOf(false) }

  fun updSection(id: String, f: (HomeSection) -> HomeSection) { draft = draft.copy(sections = draft.sections.map { if (it.id == id) f(it) else it }) }

  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink, Color(0xFF0A0B26), Color(0xFF10072A))))) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { navController.navigateUp() }) { Text("Back", color = Cyan) }
        Column(Modifier.weight(1f)) {
          Text("DEVELOPER MODE", fontSize = 10.sp, letterSpacing = 3.sp, color = Cyan, fontWeight = FontWeight.Bold)
          Text("Home Studio", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }
      // sync status
      Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass.copy(alpha = 0.07f))
          .border(1.dp, Glass.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).padding(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(10.dp).clip(CircleShape).background(if (status.ok) Color(0xFF34D399) else Color(0xFFFACC15)))
          Text("  ${status.text}", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
          TextButton(onClick = { scope.launch { HomeConfigRepository.refresh(context) } }) { Text("Refresh", color = Violet) }
        }
        Text("Last sync: ${HomeConfigRepository.formatTime(status.lastSyncMillis)}  ·  Published: ${remote?.updatedAt?.ifBlank { "unknown" } ?: "none"}", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
      }
      LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DevTab.entries.toList()) { t ->
          val sel = t == tab
          Text(
            t.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) Ink else Color.White,
            modifier = Modifier.clip(RoundedCornerShape(50))
              .background(if (sel) Brush.horizontalGradient(listOf(Cyan, Violet)) else Brush.horizontalGradient(listOf(Glass.copy(alpha = 0.08f), Glass.copy(alpha = 0.08f))))
              .clickable {
                tab = t
                if (t == DevTab.HISTORY && token.isNotBlank()) scope.launch {
                  busy = true; GitHubPublisher.history(token).onSuccess { history = it; message = "" }.onFailure { message = it.message.orEmpty() }; busy = false
                }
              }.padding(horizontal = 18.dp, vertical = 9.dp),
          )
        }
      }

      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        when (tab) {
          DevTab.CONTENT -> {
            Head("Banner")
            Switchy("Show banner", draft.banner.enabled) { draft = draft.copy(banner = draft.banner.copy(enabled = it)) }
            Field("Title", draft.banner.title) { draft = draft.copy(banner = draft.banner.copy(title = it)) }
            Field("Subtitle", draft.banner.subtitle) { draft = draft.copy(banner = draft.banner.copy(subtitle = it)) }
            Field("Artwork image URL", draft.banner.imageUrl) { draft = draft.copy(banner = draft.banner.copy(imageUrl = it)) }
            Field("Song id to play on tap (optional)", draft.banner.songId) { draft = draft.copy(banner = draft.banner.copy(songId = it)) }

            Head("Announcements and recommendations")
            draft.announcements.forEach { a ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = a.enabled, onCheckedChange = { on -> draft = draft.copy(announcements = draft.announcements.map { if (it.id == a.id) it.copy(enabled = on) else it }) })
                Box(Modifier.weight(1f).padding(start = 8.dp)) { Field("Text", a.text) { t -> draft = draft.copy(announcements = draft.announcements.map { if (it.id == a.id) it.copy(text = t) else it }) } }
                TextButton(onClick = { draft = draft.copy(announcements = draft.announcements.filter { it.id != a.id }) }) { Text("✕", color = Color(0xFFF43F5E)) }
              }
            }
            TextButton(onClick = { draft = draft.copy(announcements = draft.announcements + Announcement(UUID.randomUUID().toString(), "", true)) }) { Text("+ Add announcement", color = Cyan) }

            Head("Sections (order = top to bottom)")
            draft.sections.forEachIndexed { idx, s ->
              Column(Modifier.padding(vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Glass.copy(alpha = 0.06f)).border(1.dp, Glass.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Switch(checked = s.enabled, onCheckedChange = { on -> updSection(s.id) { it.copy(enabled = on) } })
                  Text("  ${s.type}", color = Cyan, fontSize = 11.sp, modifier = Modifier.weight(1f))
                  TextButton(onClick = { draft = draft.copy(sections = move(draft.sections, idx, -1)) }, enabled = idx > 0) { Text("↑") }
                  TextButton(onClick = { draft = draft.copy(sections = move(draft.sections, idx, 1)) }, enabled = idx < draft.sections.lastIndex) { Text("↓") }
                  TextButton(onClick = { draft = draft.copy(sections = draft.sections.filter { it.id != s.id }) }) { Text("✕", color = Color(0xFFF43F5E)) }
                }
                Field("Section title", s.title) { t -> updSection(s.id) { it.copy(title = t) } }
                s.items.forEach { i ->
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    AsyncImage(model = i.thumb.ifBlank { null }, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Glass.copy(alpha = 0.1f)))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                      Text(i.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                      Text("${i.kind} · ${i.subtitle}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1)
                    }
                    TextButton(onClick = { updSection(s.id) { sec -> sec.copy(items = sec.items.filter { it.id != i.id }) } }) { Text("Remove", color = Color(0xFFF43F5E), fontSize = 12.sp) }
                  }
                }
                Row {
                  TextButton(onClick = { searchFor = s.id }) { Text("+ Song", color = Cyan) }
                  TextButton(onClick = { playlistFor = s.id }) { Text("+ Playlist", color = Cyan) }
                }
              }
            }
            TextButton(onClick = { addSection = true }) { Text("+ Add section", color = Cyan) }
            Spacer(Modifier.height(24.dp))
          }
          DevTab.PREVIEW -> {
            Text("This is how the Songs home will look with your draft. Nothing is public until you publish.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
            Box(Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 14.dp)) { RemoteHomeSections(draft, null) }
            Spacer(Modifier.height(24.dp))
          }
          DevTab.PUBLISH -> {
            Head("GitHub access")
            Text("Paste a fine-grained token with Contents: read and write on T902968/pattukal-listen. It is stored encrypted on this device only; nothing is built into the APK.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("GitHub token") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Row {
              TextButton(onClick = { if (token.isNotBlank()) { SecureToken.save(context, token.trim()); Toast.makeText(context, "Token saved securely", Toast.LENGTH_SHORT).show() } }) { Text("Save token", color = Cyan) }
              TextButton(onClick = { scope.launch { busy = true; GitHubPublisher.verify(token.trim()).onSuccess { message = it }.onFailure { message = it.message.orEmpty() }; busy = false } }) { Text("Verify", color = Cyan) }
              TextButton(onClick = { SecureToken.clear(context); token = "" }) { Text("Forget", color = Color(0xFFF43F5E)) }
            }
            Head("Publish")
            OutlinedTextField(value = commitMsg, onValueChange = { commitMsg = it }, label = { Text("Change note") }, modifier = Modifier.fillMaxWidth())
            Button(
              enabled = !busy && token.isNotBlank(),
              onClick = { scope.launch { busy = true; GitHubPublisher.publish(token.trim(), draft, commitMsg).onSuccess { message = it }.onFailure { message = "Failed: " + it.message }; busy = false } },
              modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
            ) { Text(if (busy) "Working…" else "Publish to GitHub") }
            if (message.isNotBlank()) Text(message, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Text("Published content appears in the app after GitHub Pages rebuilds (usually about a minute) and the app refreshes.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
          }
          DevTab.HISTORY -> {
            Head("Configuration history")
            if (token.isBlank()) Text("Add your token in Publish first.", color = Color.White.copy(alpha = 0.6f))
            if (message.isNotBlank()) Text(message, color = Color(0xFFFACC15), fontSize = 12.sp)
            history.forEach { c ->
              Row(Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Glass.copy(alpha = 0.06f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(c.message, color = Color.White, fontSize = 13.sp, maxLines = 2)
                  Text("${c.sha.take(7)} · ${c.date}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                TextButton(onClick = { scope.launch { GitHubPublisher.fetchCommitConfig(token, c.sha)?.let { draft = it; tab = DevTab.CONTENT; Toast.makeText(context, "Loaded into editor", Toast.LENGTH_SHORT).show() } } }) { Text("Load", color = Cyan) }
                TextButton(onClick = { confirmRollback = c }) { Text("Rollback", color = Violet) }
              }
            }
            if (history.isEmpty() && token.isNotBlank() && !busy) Text("No history yet.", color = Color.White.copy(alpha = 0.5f))
          }
        }
      }
    }
  }

  searchFor?.let { sid ->
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<SongItem>()) }
    AlertDialog(
      onDismissRequest = { searchFor = null },
      title = { Text("Add song") },
      text = {
        Column {
          OutlinedTextField(value = q, onValueChange = { q = it }, singleLine = true, label = { Text("Search songs") })
          TextButton(onClick = { scope.launch { results = withContext(Dispatchers.IO) { YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(8) } } }) { Text("Search") }
          Column(Modifier.height(260.dp).verticalScroll(rememberScrollState())) {
            results.forEach { r ->
              Row(Modifier.fillMaxWidth().clickable {
                updSection(sid) { it.copy(items = it.items + HomeItem(r.id, "song", r.title, r.artists.joinToString { a -> a.name }, r.thumbnail)) }
                searchFor = null
              }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = r.thumbnail, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                Column(Modifier.padding(start = 10.dp)) { Text(r.title, maxLines = 1); Text(r.artists.joinToString { it.name }, fontSize = 12.sp, maxLines = 1) }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { searchFor = null }) { Text("Close") } },
    )
  }

  playlistFor?.let { sid ->
    var pid by remember { mutableStateOf("") }
    var ptitle by remember { mutableStateOf("") }
    var pthumb by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { playlistFor = null },
      title = { Text("Add playlist") },
      text = {
        Column {
          OutlinedTextField(value = pid, onValueChange = { pid = it }, singleLine = true, label = { Text("Playlist id or link") })
          OutlinedTextField(value = ptitle, onValueChange = { ptitle = it }, singleLine = true, label = { Text("Title") })
          OutlinedTextField(value = pthumb, onValueChange = { pthumb = it }, singleLine = true, label = { Text("Artwork URL") })
        }
      },
      confirmButton = {
        TextButton(onClick = {
          val id = pid.substringAfter("list=").substringBefore("&").trim()
          if (id.isNotBlank()) updSection(sid) { it.copy(items = it.items + HomeItem(id, "playlist", ptitle.ifBlank { "Playlist" }, "Playlist", pthumb)) }
          playlistFor = null
        }) { Text("Add") }
      },
      dismissButton = { TextButton(onClick = { playlistFor = null }) { Text("Cancel") } },
    )
  }

  if (addSection) {
    AlertDialog(
      onDismissRequest = { addSection = false },
      title = { Text("New section type") },
      text = {
        Column {
          listOf("featured" to "Featured songs", "trending" to "Trending songs", "playlists" to "Playlists", "recommended" to "Recommended content").forEach { (type, title) ->
            TextButton(onClick = { draft = draft.copy(sections = draft.sections + HomeSection(UUID.randomUUID().toString(), type, title, true, emptyList())); addSection = false }) { Text(title) }
          }
        }
      },
      confirmButton = { TextButton(onClick = { addSection = false }) { Text("Cancel") } },
    )
  }

  confirmRollback?.let { c ->
    AlertDialog(
      onDismissRequest = { confirmRollback = null },
      title = { Text("Roll back?") },
      text = { Text("Publish the content from ${c.sha.take(7)} again as a new commit. Your history is kept.") },
      confirmButton = { TextButton(onClick = { scope.launch { busy = true; GitHubPublisher.rollback(token, c.sha).onSuccess { message = it }.onFailure { message = "Failed: " + it.message }; busy = false; confirmRollback = null } }) { Text("Roll back") } },
      dismissButton = { TextButton(onClick = { confirmRollback = null }) { Text("Cancel") } },
    )
  }
}

@Composable private fun Head(t: String) { Text(t, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) }

@Composable
private fun Switchy(label: String, v: Boolean, on: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) { Text(label, color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = v, onCheckedChange = on) }
}

@Composable
private fun Field(label: String, v: String, on: (String) -> Unit) {
  OutlinedTextField(value = v, onValueChange = on, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
}
