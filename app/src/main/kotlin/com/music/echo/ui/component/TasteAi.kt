package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.constants.TasteAiDailyMixKey
import echo.music.iad1tya.constants.TasteAiDiscoverKey
import echo.music.iad1tya.constants.TasteAiEnabledKey
import echo.music.iad1tya.constants.TasteAiMoodKey
import echo.music.iad1tya.utils.rememberPreference
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random

private suspend fun tasteSearch(q: String): List<SongItem> =
  withContext(Dispatchers.IO) {
    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
  }

private fun moodNow(): Pair<String, String> =
  when (LocalTime.now().hour) {
    in 5..10 -> "Morning energy" to "feel good morning songs"
    in 11..16 -> "Focus & flow" to "focus chill songs"
    in 17..21 -> "Evening vibes" to "evening relaxing songs"
    else -> "Late night calm" to "late night slow songs"
  }

/** Pattukal Taste AI rows for Home: daily mix, mood, discover. Built from favourites and listening history. */
@Composable
fun TasteAiHomeRows(refreshKey: Any?, mood: String? = null) {
  val (enabled, _) = rememberPreference(TasteAiEnabledKey, true)
  if (!enabled) return
  val (dailyOn, _) = rememberPreference(TasteAiDailyMixKey, true)
  val (moodOn, _) = rememberPreference(TasteAiMoodKey, true)
  val (discoverOn, _) = rememberPreference(TasteAiDiscoverKey, true)
  val database = LocalDatabase.current

  val rows by
    produceState<List<Triple<String, String, List<SongItem>>>>(emptyList(), enabled, dailyOn, moodOn, discoverOn, refreshKey, mood) {
      val artists =
        withContext(Dispatchers.IO) {
          val liked = database.likedSongsByCreateDateAsc().first().takeLast(40)
          val played = database.mostPlayedSongs(0L, limit = 20).first()
          (liked + played).flatMap { it.artists }.map { it.name }.filter { it.isNotBlank() }
        }
      val top = artists.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
      val rnd = Random(LocalDate.now().toEpochDay() + ((refreshKey as? Int) ?: 0))
      value =
        coroutineScope {
          buildList {
            if (dailyOn && top.isNotEmpty()) {
              val seeds = top.shuffled(rnd).take(3)
              add(async { Triple("Daily Mix", "Made from your favourites", seeds.flatMap { tasteSearch(if (mood.isNullOrBlank()) "$it songs" else "$it $mood").take(5) }.distinctBy { it.id }.shuffled(rnd)) })
            }
            if (moodOn || !mood.isNullOrBlank()) {
              val (label, q) = if (mood.isNullOrBlank()) moodNow() else mood to "$mood songs"
              add(async { Triple(label, "Picked for this time of day", tasteSearch(q).take(14)) })
            }
            if (discoverOn && top.size > 1) {
              val seed = top.drop(1).shuffled(rnd).first()
              add(async { Triple("Discover", "Similar to $seed", tasteSearch("songs like $seed").take(14)) })
            }
          }.awaitAll().filter { it.third.isNotEmpty() }
        }
    }

  Column { rows.forEach { (t, sub, songs) -> SongCardRow(title = t, songs = songs, subtitle = sub) } }
}
