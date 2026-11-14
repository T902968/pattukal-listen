package echo.music.iad1tya.ui.component

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps the generated Home rows for the life of the app process. LazyColumn disposes rows that scroll
 * away, which used to re-run the search + shuffle and change the songs every time. Rows are now built
 * once per (language, mood, refresh count) and reused until the user refreshes.
 */
object HomeCache {
  private val map = ConcurrentHashMap<String, Any>()

  @Suppress("UNCHECKED_CAST") fun <T : Any> get(key: String): T? = map[key] as? T

  fun put(key: String, value: Any) {
    if (map.size > 60) map.clear()
    map[key] = value
  }
}
