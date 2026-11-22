package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R

/**
 * Home entry for Spotify: a small icon that opens Spotify import and playlist connection, and, once playlists have been
 * connected or imported, a row showing them right on the home screen.
 */
@Composable
fun SpotifyHomeRow(navController: NavController) {
  val database = LocalDatabase.current
  val all by database.playlistsByUpdatedDateAsc().collectAsState(initial = emptyList())
  val spotify = all.filter { it.playlist.id.startsWith("SPOTIFY_") }.asReversed()
  Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Row(
      Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(50)).clickable { navController.navigate("settings/spotify_import") }
        .background(Color(0xFF1DB954).copy(alpha = 0.16f)).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1DB954)), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_spotify), contentDescription = "Spotify", tint = Color.Black, modifier = Modifier.size(18.dp))
      }
      Text(if (spotify.isEmpty()) "Connect Spotify · import playlists" else "Spotify · import or connect more", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    }
    if (spotify.isNotEmpty()) {
      Text("Your Spotify playlists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
      LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(spotify, key = { "sp_" + it.playlist.id }) { p ->
          Column(Modifier.width(130.dp).clickable { navController.navigate("local_playlist/${p.playlist.id}") }) {
            AsyncImage(
              model = p.playlist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
              modifier = Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text(if (p.playlist.id == "SPOTIFY_LIKED_SONGS") "Liked songs" else p.playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
          }
        }
      }
    }
  }
}
