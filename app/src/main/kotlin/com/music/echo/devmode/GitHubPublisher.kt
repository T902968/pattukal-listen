package echo.music.iad1tya.devmode

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ConfigCommit(val sha: String, val message: String, val date: String)

/**
 * Publishes home-config.json through the GitHub Contents API using a token the developer pastes in at runtime.
 * Authorisation is GitHub's: only an account with push access to the repo can publish. The access code in the app
 * only hides the screen and is not a security boundary on its own.
 */
object GitHubPublisher {
  private const val OWNER = "T902968"
  private const val REPO = "pattukal-listen"
  private const val PATH = "home-config.json"
  private const val BRANCH = "main"
  private const val API = "https://api.github.com/repos/$OWNER/$REPO"

  private class Res(val code: Int, val body: String)

  private fun call(method: String, url: String, token: String, body: String? = null): Res {
    val c = URL(url).openConnection() as HttpURLConnection
    c.requestMethod = method
    c.connectTimeout = 12000; c.readTimeout = 20000
    c.setRequestProperty("Authorization", "Bearer $token")
    c.setRequestProperty("Accept", "application/vnd.github+json")
    c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
    c.setRequestProperty("User-Agent", "Pattukal-DevMode")
    if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(body.toByteArray()) } }
    val code = c.responseCode
    val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
    return Res(code, text)
  }

  private fun err(r: Res) = "GitHub ${r.code}: " + (runCatching { JSONObject(r.body).optString("message") }.getOrNull() ?: r.body.take(120))

  suspend fun verify(token: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val r = call("GET", API, token)
      if (r.code != 200) error(err(r))
      val push = JSONObject(r.body).optJSONObject("permissions")?.optBoolean("push") ?: false
      if (!push) error("This token cannot write to $OWNER/$REPO")
      "Token OK, write access to $OWNER/$REPO"
    }
  }

  private fun currentSha(token: String): String? {
    val r = call("GET", "$API/contents/$PATH?ref=$BRANCH", token)
    return if (r.code == 200) JSONObject(r.body).optString("sha") else if (r.code == 404) null else error(err(r))
  }

  private fun put(token: String, content: String, message: String): String {
    val body = JSONObject().put("message", message).put("branch", BRANCH)
      .put("content", Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP))
    currentSha(token)?.let { body.put("sha", it) }
    val r = call("PUT", "$API/contents/$PATH", token, body.toString())
    if (r.code !in 200..201) error(err(r))
    return JSONObject(r.body).optJSONObject("commit")?.optString("sha").orEmpty().take(7)
  }

  suspend fun publish(token: String, config: HomeConfig, message: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val stamped = config.copy(updatedAt = java.time.Instant.now().toString())
      "Published as commit " + put(token, stamped.toJson(), message.ifBlank { "Update home content" }) + ". GitHub Pages can take a minute to update."
    }
  }

  suspend fun history(token: String): Result<List<ConfigCommit>> = withContext(Dispatchers.IO) {
    runCatching {
      val r = call("GET", "$API/commits?path=$PATH&sha=$BRANCH&per_page=20", token)
      if (r.code != 200) error(err(r))
      val a = JSONArray(r.body)
      (0 until a.length()).map {
        val o = a.getJSONObject(it)
        val c = o.getJSONObject("commit")
        ConfigCommit(o.getString("sha"), c.getString("message").lineSequence().first(), c.getJSONObject("committer").optString("date"))
      }
    }
  }

  /** Rollback = publish the old file contents as a new commit, so history stays intact. */
  suspend fun rollback(token: String, sha: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val r = call("GET", "$API/contents/$PATH?ref=$sha", token)
      if (r.code != 200) error(err(r))
      val b64 = JSONObject(r.body).getString("content").replace("\n", "")
      val old = String(Base64.decode(b64, Base64.DEFAULT))
      "Rolled back as commit " + put(token, old, "Rollback home content to ${sha.take(7)}")
    }
  }

  suspend fun fetchCommitConfig(token: String, sha: String): HomeConfig? = withContext(Dispatchers.IO) {
    runCatching {
      val r = call("GET", "$API/contents/$PATH?ref=$sha", token)
      HomeConfig.parse(String(Base64.decode(JSONObject(r.body).getString("content").replace("\n", ""), Base64.DEFAULT)))
    }.getOrNull()
  }
}
