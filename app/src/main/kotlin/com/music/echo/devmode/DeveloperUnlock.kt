package echo.music.iad1tya.devmode

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import java.security.MessageDigest

/** Hidden access gate. Only hides the dashboard; real authority to publish is the GitHub token. */
private const val CODE_SHA256 = "95b7d6f3a5d7578bb7c6975787d8713eb144b461d58bf7107011d0057999d6a6"
private var failedAttempts = 0

private fun sha256(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

@Composable
fun DeveloperUnlockDialog(onDismiss: () -> Unit, onUnlocked: () -> Unit) {
  var code by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Developer access") },
    text = {
      androidx.compose.foundation.layout.Column {
        OutlinedTextField(
          value = code, onValueChange = { code = it; error = null }, singleLine = true, label = { Text("Access code") },
          visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        when {
          failedAttempts >= 5 -> error = "Too many attempts. Restart the app to try again."
          sha256(code.trim()) == CODE_SHA256 -> { failedAttempts = 0; onUnlocked() }
          else -> { failedAttempts++; error = "Wrong code" }
        }
      }) { Text("Unlock") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
