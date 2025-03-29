@file:Suppress("DEPRECATION")

package pl.podkal.domowniczeqqq.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.podkal.domowniczeq.R
import javax.inject.Inject


@Suppress("DEPRECATION")
class AuthenticationManager @Inject constructor(
    @ApplicationContext private val context: Context) {
    private val auth = Firebase.auth

    fun createAccountWithEmail(email: String, password: String): Flow<AuthResponse> = callbackFlow {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthResponse.Success)
                } else {
                    trySend(AuthResponse.Error(task.exception?.message ?: "Account creation failed"))
                }
                close()
            }
        awaitClose()
    }

    fun loginWithEmail(email: String, password: String): Flow<AuthResponse> = callbackFlow {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthResponse.Success)
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                } else {
                    trySend(AuthResponse.Error(task.exception?.message ?: "Login failed"))
                    Toast.makeText(context, "Login unsuccessful", Toast.LENGTH_SHORT).show()
                }
                close()
            }
        awaitClose()
    }

    fun signInWithGoogle(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val googleSignInClient = GoogleSignIn.getClient(
            activity, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }
    fun handleGoogleSignInResult(data: Intent?, onResult: (Boolean) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    onResult(authTask.isSuccessful)
                    Toast.makeText(context, "Signing in with Google completed", Toast.LENGTH_SHORT).show()
                }
        } else {
            onResult(false)
            Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return Firebase.auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

}


sealed class AuthResponse {
    object Success : AuthResponse()
    data class Error(val message: String) : AuthResponse()
}
