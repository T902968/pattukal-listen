package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.component.WinterVariant
import echo.music.iad1tya.ui.component.glassSurface
import echo.music.iad1tya.ui.component.rememberWinterVariant
import echo.music.iad1tya.ui.utils.resize
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Pattukal Wrapped: your year in music, built only from your real listening history on this device. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreen(navController: NavController) {
  val database = LocalDatabase.current
  val variant = rememberWinterVariant()
  val year = remember { LocalDate.now().year }
  val from = remember { LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
  val to = remember { LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() }
  val songs by remember { database.mostPlayedSongs(from, 5, 0, to) }.collectAsState(initial = emptyList())
  val artists by remember { database.mostPlayedArtists(from, 5, 0, to) }.collectAsState(initial = emptyList())
  val totalMs by remember { database.getTotalPlayTimeInRange(from, to) }.collectAsState(initial = 0L)
  val minutes = ((totalMs ?: 0L) / 60000L)
  val pages = 4
  val pager = rememberPagerState { pages }

  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(variant.sky))) {
    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
      Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp, 72.dp, 24.dp, 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        when (page) {
          0 -> {
            Text("PATTUKAL WRAPPED $year", fontSize = 12.sp, letterSpacing = 2.sp, color = variant.bands[0], fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text(if (minutes == 0L) "No listening yet" else "$minutes", fontSize = 76.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(if (minutes == 0L) "Play some songs and come back" else "minutes of music this year", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
            if (minutes >= 60) Text("That is about ${minutes / 60} hours", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
            Text("Swipe  →", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 40.dp))
          }
          1 -> {
            val s = songs.firstOrNull()
            Text("YOUR TOP SONG", fontSize = 12.sp, letterSpacing = 2.sp, color = variant.bands[0], fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            if (s == null) Text("Nothing yet", color = Color.White, fontSize = 22.sp)
            else {
              AsyncImage(model = s.song.thumbnailUrl?.resize(544, 544), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(240.dp).clip(RoundedCornerShape(28.dp)))
              Spacer(Modifier.height(18.dp))
              Text(s.song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
              Text(s.artists.joinToString { it.name }, fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          2 -> {
            Text("TOP SONGS", fontSize = 12.sp, letterSpacing = 2.sp, color = variant.bands[0], fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (songs.isEmpty()) Text("Nothing yet", color = Color.White, fontSize = 22.sp)
            songs.forEachIndexed { i, s ->
              Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).glassSurface(RoundedCornerShape(18.dp), Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = variant.bands[0], modifier = Modifier.padding(horizontal = 8.dp))
                AsyncImage(model = s.song.thumbnailUrl?.resize(180, 180), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.padding(start = 12.dp)) {
                  Text(s.song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(s.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
          else -> {
            Text("TOP ARTISTS", fontSize = 12.sp, letterSpacing = 2.sp, color = variant.bands[0], fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (artists.isEmpty()) Text("Nothing yet", color = Color.White, fontSize = 22.sp)
            artists.forEachIndexed { i, a ->
              Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).glassSurface(RoundedCornerShape(18.dp), Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = variant.bands[0], modifier = Modifier.padding(horizontal = 8.dp))
                AsyncImage(model = a.artist.thumbnailUrl?.resize(180, 180), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape))
                Text(a.artist.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
              }
            }
          }
        }
      }
    }
    Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      repeat(pages) { i -> Box(Modifier.size(width = if (i == pager.currentPage) 22.dp else 8.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 0.95f else 0.35f))) }
    }
    IconButton(onClick = { navController.navigateUp() }, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 18.dp, start = 4.dp)) {
      Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
    }
  }
}
