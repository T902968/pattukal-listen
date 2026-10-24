package echo.music.iad1tya.ui.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import coil3.compose.AsyncImage
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.constants.SkippedForeverKey
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.launch

private const val SKIP_SEP = '\u001F'

/** id -> title for every song the user chose to "Skip This Forever". */
fun parseSkippedForever(raw: String): Map<String, String> =
  raw.lines()
    .mapNotNull { line ->
      val i = line.indexOf(SKIP_SEP)
      when {
        i > 0 -> line.substring(0, i) to line.substring(i + 1)
        line.isNotBlank() -> line to ""
        else -> null
      }
    }
    .toMap()

suspend fun skipSongForever(context: Context, id: String, title: String) {
  context.dataStore.edit { prefs ->
    val current = parseSkippedForever(prefs[SkippedForeverKey].orEmpty()).toMutableMap()
    current[id] = title.replace('\n', ' ')
    prefs[SkippedForeverKey] = current.entries.joinToString("\n") { "${it.key}$SKIP_SEP${it.value}" }
  }
}

suspend fun unskipSong(context: Context, id: String) {
  context.dataStore.edit { prefs ->
    val current = parseSkippedForever(prefs[SkippedForeverKey].orEmpty()).toMutableMap()
    current.remove(id)
    prefs[SkippedForeverKey] = current.entries.joinToString("\n") { "${it.key}$SKIP_SEP${it.value}" }
  }
}

@Composable
fun rememberSkippedIds(): Set<String> {
  val (raw, _) = rememberPreference(SkippedForeverKey, defaultValue = "")
  return remember(raw) { parseSkippedForever(raw).keys }
}

/**
 * A titled row of song cards. Tap plays; hold opens: Add to play next, Add to queue, Skip this forever.
 * Songs the user skipped forever are never shown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCardRow(title: String, songs: List<SongItem>, subtitle: String? = null) {
  val playerConnection = LocalPlayerConnection.current ?: return
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val skipped = rememberSkippedIds()
  val visible = songs.filter { it.id !in skipped }
  if (visible.isEmpty()) return

  Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
    Text(
      title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
    if (subtitle != null) {
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
      )
    }
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(visible, key = { "card_${title}_${it.id}" }) { song ->
        var menu by remember { mutableStateOf(false) }
        Box {
          Column(
            Modifier.width(140.dp)
              .clip(RoundedCornerShape(16.dp))
              .combinedClickable(
                onClick = {
                  playerConnection.playQueue(
                    YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                  )
                },
                onLongClick = {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  menu = true
                },
              )
          ) {
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(16.dp))) {
              AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp),
              )
            }
            Text(
              song.title,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(top = 6.dp),
            )
            Text(
              song.artists.joinToString { it.name },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
              text = { Text("Add to play next") },
              onClick = {
                playerConnection.playNext(song.toMediaItem())
                menu = false
                Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
              },
            )
            DropdownMenuItem(
              text = { Text("Add to queue") },
              onClick = {
                playerConnection.addToQueue(song.toMediaItem())
                menu = false
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
              },
            )
            DropdownMenuItem(
              text = { Text("Skip this forever") },
              onClick = {
                menu = false
                scope.launch { skipSongForever(context, song.id, song.title) }
                Toast.makeText(context, "Won't be recommended again", Toast.LENGTH_SHORT).show()
              },
            )
          }
        }
      }
    }
  }
}
