package echo.music.iad1tya.devmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue

private fun HomeItem.toMeta() = MediaMetadata(id = id, title = title, artists = listOf(MediaMetadata.Artist(null, subtitle)), duration = -1, thumbnailUrl = thumb.ifBlank { null })

/** Renders the developer-managed Songs home content: banner, announcements and sections. Used on Home and in the dashboard preview. */
@Composable
fun RemoteHomeSections(config: HomeConfig, navController: NavController?, modifier: Modifier = Modifier) {
  val connection = LocalPlayerConnection.current
  fun play(i: HomeItem) {
    if (i.kind == "playlist") navController?.navigate("online_playlist/${i.id}")
    else connection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = i.id), i.toMeta()))
  }
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    if (config.banner.enabled && (config.banner.title.isNotBlank() || config.banner.imageUrl.isNotBlank())) {
      Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))
          .background(Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFF8B5CF6))))
          .clickable(enabled = config.banner.songId.isNotBlank()) {
            play(HomeItem(config.banner.songId, "song", config.banner.title, config.banner.subtitle, config.banner.imageUrl))
          },
      ) {
        if (config.banner.imageUrl.isNotBlank()) AsyncImage(model = config.banner.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp))
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
          Text(config.banner.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
          if (config.banner.subtitle.isNotBlank()) Text(config.banner.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
      }
    }
    config.announcements.filter { it.enabled && it.text.isNotBlank() }.forEach { a ->
      Text(
        a.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(14.dp),
      )
    }
    config.sections.filter { it.enabled && it.items.isNotEmpty() }.forEach { s ->
      Column {
        Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(s.items) { i ->
            Column(Modifier.width(140.dp).clickable { play(i) }) {
              AsyncImage(
                model = i.thumb.ifBlank { null }, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
              )
              Text(i.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
              Text(i.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}
