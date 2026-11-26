package com.music.echo.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class AddonEndpoint(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class AddonSearchResponse(
    val tracks: List<AddonTrack> = emptyList()
)

@Serializable
data class AddonTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int? = null,
    val artworkURL: String? = null,
    val albumArtworkURL: String? = null
)

@Serializable
data class AddonStreamResponse(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val streamQuality: String = "",
    val audioQuality: String = "",
    val codec: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val mimeType: String? = null,
    val container: String? = null,
    val atmos: Boolean? = null,
    val error: String? = null
) {
    val isLossless get() = audioQuality.contains("LOSSLESS", true) || format.contains("flac", true) ||
        format.contains("alac", true) || codec?.contains("flac", true) == true
    val isAtmos get() = atmos == true || format.contains("atmos", true) || codec?.contains("eac3", true) == true
    /** Honest one-line description built only from what the server reported. */
    fun summary(): String = buildList {
        add((codec ?: format).uppercase().ifBlank { "AUDIO" })
        bitrate?.takeIf { it > 0 }?.let { add("${if (it > 5000) it / 1000 else it} kbps") }
        if (sampleRate != null && bitDepth != null) add("${bitDepth}-bit/${sampleRate / 1000.0} kHz")
        if (isLossless) add("Lossless")
        if (isAtmos) add("Dolby Atmos")
    }.joinToString(" · ")
}

/** The last stream an addon actually served, for the Stats view. */
object LastAddonStream { @Volatile var info: String? = null }

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient()
    private val ADDONS_KEY = stringPreferencesKey("extensions_addons_list")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAddons(): List<AddonEndpoint> {
        val jsonStr = context.dataStore.data.map { it[ADDONS_KEY] }.first() ?: "[]"
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addAddon(endpoint: AddonEndpoint) {
        val current = getAddons().toMutableList()
        current.add(endpoint)
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }
    
    suspend fun toggleAddon(id: String, enabled: Boolean) {
        val current = getAddons().map { if (it.id == id) it.copy(enabled = enabled) else it }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun removeAddon(id: String) {
        val current = getAddons().filter { it.id != id }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun search(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val addons = getAddons().filter { it.enabled }
        val results = mutableListOf<SongItem>()
        
        for (addon in addons) {
            try {
                val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${query.replace(" ", "%20")}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    results.addAll(addonResults.map { 
                        SongItem(
                            id = "addon_${addon.id}_${it.id}",
                            title = it.title,
                            artists = listOf(Artist(name = it.artist, id = null)),
                            thumbnail = it.artworkURL ?: it.albumArtworkURL ?: "",
                            duration = it.duration
                        )
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results
    }

    suspend fun getStreamUrl(fullId: String): String? = withContext(Dispatchers.IO) {
        // fullId format: addon_{addonId}_{trackId}
        if (!fullId.startsWith("addon_")) return@withContext null
        val parts = fullId.split("_", limit = 3)
        if (parts.size < 3) return@withContext null
        val addonId = parts[1]
        val trackId = parts[2]
        
        val addon = getAddons().find { it.id == addonId } ?: return@withContext null
        
        try {
            val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
            val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream?id=${trackId}"
            val request = Request.Builder().url(streamUrlReq).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val streamRes: AddonStreamResponse = json.decodeFromString(body)
                return@withContext streamRes.url
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun addonQ(t: echo.music.iad1tya.constants.AudioQuality) = when (t) {
        echo.music.iad1tya.constants.AudioQuality.HIGH -> "HIGH"
        else -> "LOSSLESS"
    }

    /** LOW/Medium never ask addons: they are the YouTube tiers. High and above try addons first. */
    suspend fun resolveStream(title: String?, artist: String?, duration: Long?, videoId: String, tier: echo.music.iad1tya.constants.AudioQuality = echo.music.iad1tya.constants.AudioQuality.LOSSLESS): AddonStreamResponse? = withContext(Dispatchers.IO) {
        if (!videoId.startsWith("addon_") &&
            (tier == echo.music.iad1tya.constants.AudioQuality.LOW || tier == echo.music.iad1tya.constants.AudioQuality.OPUS)) return@withContext null
        if (videoId.startsWith("addon_")) {
            val parts = videoId.split("_", limit = 3)
            if (parts.size == 3) {
                val addonId = parts[1]
                val trackId = parts[2]
                val addons = getAddons().filter { it.enabled }
                val targetAddon = addons.find { it.id == addonId }
                if (targetAddon != null) {
                    val baseUrl = if (targetAddon.url.startsWith("http")) targetAddon.url else "https://" + targetAddon.url
                    val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/$trackId?quality=${addonQ(tier)}"
                    val streamReq = Request.Builder().url(streamUrlReq).build()
                    val streamRes = httpClient.newCall(streamReq).execute()
                    val streamBody = streamRes.body?.string()
                    if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                        return@withContext json.decodeFromString<AddonStreamResponse>(streamBody).also { LastAddonStream.info = it.summary() }
                    }
                }
            }
            return@withContext null
        }
        if (title == null || artist == null) return@withContext null
        val addons = getAddons().filter { it.enabled }
        if (addons.isEmpty()) return@withContext null
        
        val query = "$title $artist"
        for (addon in addons) {
            try {
                android.util.Log.e("ExtensionManager", "Searching addon ${addon.url} for $title $artist")
                val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${query.replace(" ", "%20")}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    // Very basic Matcher:
                    val match = bestMatch(addonResults, title, artist, duration)
                    
                    if (match != null) {
                        android.util.Log.e("ExtensionManager", "Matched track: ${match.title} by ${match.artist}. Fetching stream...")
                        val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                        val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/${match.id}?quality=${addonQ(tier)}"
                        val streamReq = Request.Builder().url(streamUrlReq).build()
                        val streamRes = httpClient.newCall(streamReq).execute()
                        val streamBody = streamRes.body?.string()
                        if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                            android.util.Log.e("ExtensionManager", "Stream response: $streamBody")
                            val streamData: AddonStreamResponse = json.decodeFromString(streamBody)
                            if (streamData.url.isBlank()) continue
                            LastAddonStream.info = streamData.summary()
                            return@withContext streamData
                        } else {
                            android.util.Log.e("ExtensionManager", "Stream request failed: ${streamRes.code} ${streamRes.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("\\(.*?\\)|\\[.*?]"), "").replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim().replace(Regex("\\s+"), " ")

    /** Title must match; artist and duration (within 5 s) break ties. Returns null instead of guessing wrong. */
    private fun bestMatch(c: List<AddonTrack>, title: String, artist: String, durationMs: Long?): AddonTrack? {
        val t = norm(title)
        val a = norm(artist.split(",", "&").first())
        return c.map { tr ->
            var score = 0
            val tt = norm(tr.title)
            if (tt == t) score += 4 else if (tt.contains(t) || t.contains(tt)) score += 2 else return@map tr to -1
            val ta = norm(tr.artist)
            if (a.isNotEmpty() && (ta.contains(a) || a.contains(ta))) score += 3
            if (durationMs != null && tr.duration != null && kotlin.math.abs(tr.duration - durationMs / 1000) <= 5) score += 2
            tr to score
        }.filter { it.second >= 5 }.maxByOrNull { it.second }?.first
    }
}
