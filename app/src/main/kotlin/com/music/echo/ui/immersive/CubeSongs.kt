package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import echo.music.iad1tya.ui.component.HomeCache
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.constants.MusicCubeSongsKey
import echo.music.iad1tya.constants.MusicLanguagesKey
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.ui.component.parseMusicLanguages
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val SEP = '\u001F'
const val MAX_CUBE_SONGS = 6

class CubePick(val id: String, val title: String, val thumb: String)

fun decodeCubePicks(raw: String): List<CubePick> =
  raw.lines().mapNotNull { line ->
    val p = line.split(SEP)
    if (p.size >= 3 && p[0].isNotBlank()) CubePick(p[0], p[1], p[2]) else null
  }.take(MAX_CUBE_SONGS)

fun encodeCubePicks(picks: List<CubePick>): String = picks.joinToString("\n") { "${it.id}$SEP${it.title}$SEP${it.thumb}" }

private fun CubePick.toSong() = Song(song = SongEntity(id = id, title = title, thumbnailUrl = thumb), artists = emptyList())

private fun SongItem.toPick() = CubePick(id, title, thumbnail)

/**
 * The Music Cube shown at the top of Home. Uses the six songs the user picked; otherwise songs in their
 * chosen music language; otherwise Quick Picks. Works for a brand-new user with an empty library.
 */
@Composable
fun HomeMusicCube(navController: NavController, fallback: List<Song>, mood: String? = null) {
  val (rawPicks, _) = rememberPreference(MusicCubeSongsKey, defaultValue = "")
  val (rawLang, _) = rememberPreference(MusicLanguagesKey, defaultValue = "")
  val picks = remember(rawPicks) { decodeCubePicks(rawPicks) }
  val language = remember(rawLang) { parseMusicLanguages(rawLang).firstOrNull() }
  val cubeKey = "cube|${language?.code}|$mood"
  val langSongs by
    produceState<List<CubePick>>(HomeCache.get(cubeKey) ?: emptyList(), language?.code, picks.isEmpty(), mood) {
      HomeCache.get<List<CubePick>>(cubeKey)?.let { value = it; return@produceState }
      value =
        if (language == null || picks.isNotEmpty()) emptyList()
        else
          withContext(Dispatchers.IO) {
            YouTube.search("${language.query} ${if (mood.isNullOrBlank()) "hits" else "$mood songs"}", YouTube.SearchFilter.FILTER_SONG)
              .getOrNull()?.items.orEmpty().filterIsInstance<SongItem>().take(MAX_CUBE_SONGS).map { it.toPick() }
          }
      if (value.isNotEmpty()) HomeCache.put(cubeKey, value)
    }
  val songs =
    when {
      picks.isNotEmpty() -> picks.map { it.toSong() }
      langSongs.isNotEmpty() -> langSongs.map { it.toSong() }
      else -> fallback.distinctBy { it.id }.take(MAX_CUBE_SONGS)
    }
  var showPicker by remember { mutableStateOf(false) }
  if (songs.isNotEmpty()) MusicCubeHomeCard(navController, songs, onCustomize = { showPicker = true })
  if (showPicker) CubeSongPickerDialog(onDismiss = { showPicker = false })
}

/** Lets the user choose which six songs appear on the cube. */
@Composable
fun CubeSongPickerDialog(onDismiss: () -> Unit) {
  val (raw, setRaw) = rememberPreference(MusicCubeSongsKey, defaultValue = "")
  val picks = remember(raw) { decodeCubePicks(raw) }
  var query by remember { mutableStateOf("") }
  val results by
    produceState<List<SongItem>>(emptyList(), query) {
      if (query.trim().length < 2) {
        value = emptyList()
        return@produceState
      }
      delay(450)
      value =
        withContext(Dispatchers.IO) {
          YouTube.search(query.trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty()
            .filterIsInstance<SongItem>().take(12)
        }
    }

  val database = echo.music.iad1tya.LocalDatabase.current
  val liked by database.likedSongsByCreateDateAsc().collectAsState(initial = emptyList())
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(28.dp),
    title = { Text("Your cube songs (${picks.size}/$MAX_CUBE_SONGS)") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        picks.forEach { p ->
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { setRaw(encodeCubePicks(picks.filter { it.id != p.id })) }) { Text("Remove") }
          }
        }
        if (picks.isEmpty()) {
          Text(
            "Nothing chosen: the cube shows songs from your music language.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          singleLine = true,
          label = { Text("Search a song to add") },
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        if (liked.isNotEmpty() && query.trim().length < 2) {
          Text("Your liked songs", style = MaterialTheme.typography.labelLarge)
          LazyColumn(Modifier.heightIn(max = 200.dp)) {
            items(liked.reversed().take(40), key = { "liked_${it.id}" }) { ls ->
              Row(
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(enabled = picks.size < MAX_CUBE_SONGS && picks.none { it.id == ls.id }) {
                    setRaw(encodeCubePicks(picks + CubePick(ls.id, ls.song.title, ls.song.thumbnailUrl.orEmpty())))
                  }
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                AsyncImage(
                  model = ls.song.thumbnailUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Column(Modifier.weight(1f)) {
                  Text(ls.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(ls.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
        LazyColumn(Modifier.heightIn(max = 240.dp)) {
          items(results, key = { it.id }) { s ->
            Row(
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = picks.size < MAX_CUBE_SONGS && picks.none { it.id == s.id }) {
                  setRaw(encodeCubePicks(picks + s.toPick()))
                }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              AsyncImage(
                model = s.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
              )
              Column(Modifier.weight(1f)) {
                Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  s.artists.joinToString { it.name },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    dismissButton = { TextButton(enabled = picks.isNotEmpty(), onClick = { setRaw("") }) { Text("Reset") } },
  )
}
