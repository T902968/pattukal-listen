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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxHeight
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.playback.queues.ListQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private class WTile(val id: String, val title: String, val artist: String, val thumb: String?, val meta: MediaMetadata)

/**
 * Pattukal Wrapped: square tiles of your top 11 songs from your real history. New listeners (little history)
 * get a recommended set instead. Tap tiles to select them, then Play / Play next / Add to queue.
 */
@Composable
fun WrappedScreen(navController: NavController) {
  val database = LocalDatabase.current
  val variant = rememberWinterVariant()
  val connection = LocalPlayerConnection.current
  val context = LocalContext.current
  val year = remember { LocalDate.now().year }
  val from = remember { LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
  val to = remember { LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() }
  val history by remember { database.mostPlayedSongs(from, 11, 0, to) }.collectAsState(initial = null)
  val totalMs by remember { database.getTotalPlayTimeInRange(from, to) }.collectAsState(initial = 0L)
  val minutes = ((totalMs ?: 0L) / 60000L)
  val topArtists by remember { database.mostPlayedArtists(from, 3, 0, to) }.collectAsState(initial = emptyList())
  val drift = rememberInfiniteTransition(label = "wrapped-bg")
  val phase by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "phase")
  val isNew = history != null && history!!.size < 3

  val recs by produceState<List<WTile>>(emptyList(), isNew) {
    if (!isNew) return@produceState
    value = withContext(Dispatchers.IO) {
      YouTube.search("top trending songs $year", YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty()
        .filterIsInstance<SongItem>().take(11)
        .map { WTile(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnail, it.toMediaMetadata()) }
    }
  }
  val tiles: List<WTile> = if (isNew) recs else history.orEmpty().map { WTile(it.id, it.song.title, it.artists.joinToString { a -> a.name }, it.song.thumbnailUrl, it.toMediaMetadata()) }
  val selected = remember { mutableStateListOf<String>() }
  val chosen = tiles.filter { it.id in selected }

  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(variant.sky))) {
    Canvas(Modifier.fillMaxSize()) {
      val cols = variant.bands
      for (i in 0 until 3) {
        val c = cols[i % cols.size]
        val cx = size.width * (0.2f + 0.3f * i + 0.15f * (if (i % 2 == 0) phase else 1f - phase))
        val cy = size.height * (0.15f + 0.28f * i + 0.08f * phase)
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.32f), Color.Transparent), Offset(cx, cy), size.width * 0.7f), size.width * 0.7f, Offset(cx, cy))
      }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 120.dp)) {
      Text(if (isNew) "WELCOME TO PATTUKAL" else "PATTUKAL WRAPPED $year", fontSize = 12.sp, letterSpacing = 3.sp, color = variant.bands[0], fontWeight = FontWeight.Bold)
      Text(
        if (isNew) "Songs to start your story" else if (minutes <= 0L) "Your year in music" else "$minutes minutes",
        fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 42.sp, modifier = Modifier.padding(top = 6.dp),
      )
      Text(
        if (isNew) "You are new here, so these are picks for you. Listen a little and your own Wrapped will fill in."
        else "of music this year · your top ${tiles.size} songs",
        fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
      )
      if (!isNew && tiles.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WStat("TOP ARTIST", topArtists.firstOrNull()?.artist?.name ?: "-", Modifier.weight(1.4f))
          WStat("HOURS", "${minutes / 60}", Modifier.weight(0.8f))
          WStat("#1 SONG", tiles.first().title, Modifier.weight(1.4f))
        }
      }
      if (tiles.isEmpty()) {
        Text(if (history == null || isNew) "Loading…" else "Nothing yet", color = Color.White.copy(alpha = 0.7f))
      }
      val rows = tiles.chunked(3)
      rows.forEachIndexed { r, row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          row.forEachIndexed { c, t ->
            val rank = r * 3 + c + 1
            val on = t.id in selected
            val enter = remember { Animatable(0f) }
            LaunchedEffect(t.id) { kotlinx.coroutines.delay((rank * 70).toLong()); enter.animateTo(1f, tween(450)) }
            Box(
              Modifier.weight(1f).aspectRatio(1f).alpha(enter.value).scale(0.85f + 0.15f * enter.value).clip(RoundedCornerShape(18.dp))
                .border(if (on) 3.dp else 1.dp, if (on) variant.bands[0] else Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .clickable { if (on) selected.remove(t.id) else selected.add(t.id) },
            ) {
              AsyncImage(model = t.thumb?.resize(400, 400), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
              Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (on) 0.75f else 0.7f)), startY = 120f)))
              Text("$rank", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
              if (on) Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Black, color = variant.bands[0], modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
              Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(t.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
          repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
      }
    }
    IconButton(onClick = { navController.navigateUp() }, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 8.dp, start = 4.dp)) {
      Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
    }
    Row(
      Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
        .glassSurface(RoundedCornerShape(26.dp), Color(0xFF0B1E3D)).padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
      val all = tiles.isNotEmpty()
      val targets = if (chosen.isNotEmpty()) chosen else tiles
      Text(if (chosen.isEmpty()) "Select songs" else "${chosen.size} selected", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 8.dp).clickable { if (all) { if (chosen.size == tiles.size) selected.clear() else { selected.clear(); selected.addAll(tiles.map { it.id }) } } })
      WBtn("Play", true) {
        if (targets.isNotEmpty()) connection?.playQueue(ListQueue(title = "Wrapped", items = targets.map { it.meta.toMediaItem() }))
      }
      WBtn("Next", false) {
        if (targets.isNotEmpty()) { connection?.playNext(targets.map { it.meta.toMediaItem() }); Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show() }
      }
      WBtn("+Queue", false) {
        if (targets.isNotEmpty()) { connection?.addToQueue(targets.map { it.meta.toMediaItem() }); Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show() }
      }
    }
  }
}

@Composable
private fun WStat(label: String, value: String, modifier: Modifier) {
  Column(modifier.glassSurface(RoundedCornerShape(16.dp), Color.White).padding(horizontal = 12.dp, vertical = 10.dp)) {
    Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
    Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun WBtn(label: String, primary: Boolean, onClick: () -> Unit) {
  Text(
    label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
    color = if (primary) Color(0xFF0B1E3D) else Color.White,
    modifier = Modifier.clip(CircleShape).background(if (primary) Color.White else Color.White.copy(alpha = 0.16f)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 9.dp),
  )
}
