package echo.music.iad1tya.devmode

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

data class SyncStatus(val text: String = "Not synced yet", val lastSyncMillis: Long = 0L, val ok: Boolean = false)

/** Fetches the published home-config.json from GitHub Pages, caches it, and exposes it to the Home screen. */
object HomeConfigRepository {
  const val PAGES_URL = "https://t902968.github.io/pattukal-listen/home-config.json"
  private const val PREFS = "home_remote_config"

  val config = MutableStateFlow<HomeConfig?>(null)
  val status = MutableStateFlow(SyncStatus())

  fun init(context: Context) {
    if (config.value != null) return
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    p.getString("json", null)?.let { config.value = HomeConfig.parse(it) }
    val last = p.getLong("last", 0L)
    if (last > 0) status.value = SyncStatus("Showing cached content", last, config.value != null)
  }

  suspend fun refresh(context: Context): SyncStatus {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val s = withContext(Dispatchers.IO) {
      try {
        val c = URL("$PAGES_URL?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("Cache-Control", "no-cache")
        when (c.responseCode) {
          200 -> {
            val text = c.inputStream.bufferedReader().readText()
            val parsed = HomeConfig.parse(text)
            if (parsed == null) SyncStatus("Remote file is not valid, keeping previous content", System.currentTimeMillis(), false)
            else {
              prefs.edit().putString("json", text).putLong("last", System.currentTimeMillis()).apply()
              config.value = parsed
              SyncStatus("Synced with GitHub Pages", System.currentTimeMillis(), true)
            }
          }
          404 -> SyncStatus("Nothing published yet (home-config.json not found)", System.currentTimeMillis(), false)
          else -> SyncStatus("Server answered ${c.responseCode}", System.currentTimeMillis(), false)
        }
      } catch (e: Exception) {
        SyncStatus("Offline or unreachable: ${e.message?.take(60)}", System.currentTimeMillis(), false)
      }
    }
    status.value = s
    return s
  }

  fun formatTime(ms: Long): String = if (ms <= 0) "never" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
}
