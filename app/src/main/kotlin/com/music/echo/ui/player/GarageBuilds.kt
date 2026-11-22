package echo.music.iad1tya.ui.player

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A saved Player Garage design: a named snapshot of every garage-controlled preference. */
data class GarageBuild(val id: String, val name: String, val data: JSONObject)

object GarageBuilds {
  private val boolKeys = listOf(DiscThumbnailKey, RotatingThumbnailKey, HidePlayerSliderKey, GarageGlowKey, GarageGlowPulseKey)
  private val floatKeys = listOf(
    ThumbnailCornerRadiusKey, PlayerThumbScaleKey, PlayerThumbOffsetYKey, PlayerControlsOffsetYKey,
    GarageGlowStrengthKey, GarageArtShadowKey, GarageArtAlphaKey, GarageControlsScaleKey, GarageFontScaleKey,
  )
  private val intKeys = listOf(GarageAccentKey)
  private val strKeys = listOf(PlayerBackgroundStyleKey, PlayerButtonsStyleKey, SliderStyleKey)

  suspend fun snapshot(context: Context): JSONObject {
    val p = context.dataStore.data.first()
    val o = JSONObject()
    boolKeys.forEach { k -> p[k]?.let { o.put(k.name, it) } }
    floatKeys.forEach { k -> p[k]?.let { o.put(k.name, it.toDouble()) } }
    intKeys.forEach { k -> p[k]?.let { o.put(k.name, it) } }
    strKeys.forEach { k -> p[k]?.let { o.put(k.name, it) } }
    return o
  }

  /** Writes the snapshot back; keys missing from it return to their defaults. */
  suspend fun restore(context: Context, o: JSONObject) {
    context.dataStore.edit { p ->
      boolKeys.forEach { k -> if (o.has(k.name)) p[k] = o.getBoolean(k.name) else p.remove(k) }
      floatKeys.forEach { k -> if (o.has(k.name)) p[k] = o.getDouble(k.name).toFloat() else p.remove(k) }
      intKeys.forEach { k -> if (o.has(k.name)) p[k] = o.getInt(k.name) else p.remove(k) }
      strKeys.forEach { k -> if (o.has(k.name)) p[k] = o.getString(k.name) else p.remove(k) }
    }
  }

  suspend fun restoreDefaults(context: Context) = restore(context, JSONObject())

  suspend fun load(context: Context): List<GarageBuild> {
    val raw = context.dataStore.data.first()[GarageBuildsKey] ?: return emptyList()
    return runCatching {
      val a = JSONArray(raw)
      (0 until a.length()).map { val b = a.getJSONObject(it); GarageBuild(b.getString("id"), b.getString("name"), b.getJSONObject("data")) }
    }.getOrDefault(emptyList())
  }

  private suspend fun store(context: Context, list: List<GarageBuild>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("data", it.data)) }
    context.dataStore.edit { it[GarageBuildsKey] = a.toString() }
  }

  suspend fun save(context: Context, name: String, existingId: String? = null): String {
    val list = load(context).toMutableList()
    val snap = snapshot(context)
    val idx = list.indexOfFirst { it.id == existingId }
    val id = if (idx >= 0) list[idx].id else UUID.randomUUID().toString()
    val b = GarageBuild(id, name.ifBlank { "My build ${list.size + 1}" }, snap)
    if (idx >= 0) list[idx] = b else list.add(b)
    store(context, list)
    context.dataStore.edit { it[GarageActiveBuildKey] = id }
    return id
  }

  suspend fun rename(context: Context, id: String, name: String) =
    store(context, load(context).map { if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it })

  suspend fun duplicate(context: Context, id: String) {
    val l = load(context)
    val src = l.firstOrNull { it.id == id } ?: return
    store(context, l + GarageBuild(UUID.randomUUID().toString(), src.name + " copy", JSONObject(src.data.toString())))
  }

  suspend fun delete(context: Context, id: String) {
    store(context, load(context).filter { it.id != id })
    context.dataStore.edit { if (it[GarageActiveBuildKey] == id) it.remove(GarageActiveBuildKey) }
  }

  suspend fun apply(context: Context, b: GarageBuild) {
    restore(context, b.data)
    context.dataStore.edit { it[GarageActiveBuildKey] = b.id }
  }
}
