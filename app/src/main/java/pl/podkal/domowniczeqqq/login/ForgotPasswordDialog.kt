package pl.podkal.domowniczeqqq.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onResetPassword: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset hasła") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adres e-mail") },
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isBlank()) {
                    showError = true
                    errorMessage = "Proszę podać adres e-mail."
                } else {
                    onResetPassword(email)
                    onDismiss()
                }
            }) {
                Text("Wyślij")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}