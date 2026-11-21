package echo.music.iad1tya.devmode

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the developer's GitHub token encrypted with a hardware-backed Android Keystore key. The token is typed in at runtime, never shipped in the APK. */
object SecureToken {
  private const val ALIAS = "pattukal_dev_token"
  private const val PREFS = "dev_secure"

  private fun key(): SecretKey {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
    val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
    return g.generateKey()
  }

  fun save(context: Context, token: String) {
    val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
    val enc = c.doFinal(token.toByteArray())
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString("t", Base64.encodeToString(enc, Base64.NO_WRAP)).putString("iv", Base64.encodeToString(c.iv, Base64.NO_WRAP)).apply()
  }

  fun load(context: Context): String? = runCatching {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val t = p.getString("t", null) ?: return null
    val iv = p.getString("iv", null) ?: return null
    val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))) }
    String(c.doFinal(Base64.decode(t, Base64.NO_WRAP)))
  }.getOrNull()

  fun clear(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply() }
}
