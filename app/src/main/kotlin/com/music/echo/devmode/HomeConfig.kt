package echo.music.iad1tya.devmode

import org.json.JSONArray
import org.json.JSONObject

/** Remote Songs-home content, published as home-config.json in the GitHub repository (served by GitHub Pages). */
data class HomeItem(val id: String, val kind: String, val title: String, val subtitle: String, val thumb: String) // kind: song | playlist
data class HomeSection(val id: String, val type: String, val title: String, val enabled: Boolean, val items: List<HomeItem>) // type: featured | trending | playlists | recommended
data class HomeBanner(val enabled: Boolean, val title: String, val subtitle: String, val imageUrl: String, val songId: String)
data class Announcement(val id: String, val text: String, val enabled: Boolean)
data class HomeConfig(
  val version: Int,
  val updatedAt: String,
  val banner: HomeBanner,
  val announcements: List<Announcement>,
  val sections: List<HomeSection>,
) {
  fun toJson(): String {
    val o = JSONObject()
    o.put("version", version)
    o.put("updatedAt", updatedAt)
    o.put("banner", JSONObject().put("enabled", banner.enabled).put("title", banner.title).put("subtitle", banner.subtitle).put("imageUrl", banner.imageUrl).put("songId", banner.songId))
    o.put("announcements", JSONArray().also { a -> announcements.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("enabled", it.enabled)) } })
    o.put("sections", JSONArray().also { a ->
      sections.forEach { s ->
        a.put(JSONObject().put("id", s.id).put("type", s.type).put("title", s.title).put("enabled", s.enabled).put("items", JSONArray().also { ia ->
          s.items.forEach { ia.put(JSONObject().put("id", it.id).put("kind", it.kind).put("title", it.title).put("subtitle", it.subtitle).put("thumb", it.thumb)) }
        }))
      }
    })
    return o.toString(2)
  }

  companion object {
    fun empty() = HomeConfig(
      1, "", HomeBanner(false, "", "", "", ""), emptyList(),
      listOf(
        HomeSection("featured", "featured", "Featured", true, emptyList()),
        HomeSection("trending", "trending", "Trending now", true, emptyList()),
        HomeSection("playlists", "playlists", "Playlists for you", true, emptyList()),
        HomeSection("recommended", "recommended", "Recommended", true, emptyList()),
      ),
    )

    fun parse(text: String): HomeConfig? = runCatching {
      val o = JSONObject(text)
      val b = o.optJSONObject("banner")
      fun arr(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getJSONObject(it) }
      HomeConfig(
        o.optInt("version", 1), o.optString("updatedAt"),
        HomeBanner(b?.optBoolean("enabled") ?: false, b?.optString("title").orEmpty(), b?.optString("subtitle").orEmpty(), b?.optString("imageUrl").orEmpty(), b?.optString("songId").orEmpty()),
        arr(o.optJSONArray("announcements")).map { Announcement(it.optString("id"), it.optString("text"), it.optBoolean("enabled", true)) },
        arr(o.optJSONArray("sections")).map { s ->
          HomeSection(s.optString("id"), s.optString("type"), s.optString("title"), s.optBoolean("enabled", true),
            arr(s.optJSONArray("items")).map { HomeItem(it.optString("id"), it.optString("kind", "song"), it.optString("title"), it.optString("subtitle"), it.optString("thumb")) })
        },
      )
    }.getOrNull()
  }
}
