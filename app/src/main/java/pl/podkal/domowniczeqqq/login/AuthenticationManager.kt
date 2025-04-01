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
import com.google.firebase.firestore.FirebaseFirestore
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
    private val db = FirebaseFirestore.getInstance()

    fun createAccountWithEmail(email: String, password: String): Flow<AuthResponse> = callbackFlow {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.user != null) {
                    trySend(AuthResponse.Success(task.result?.user!!))
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
                if (task.isSuccessful && task.result?.user != null) {
                    trySend(AuthResponse.Success(task.result?.user!!))
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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        googleSignInClient.signOut() // This will force account selection on next sign-in
        launcher.launch(googleSignInClient.signInIntent)
    }
    fun handleGoogleSignInResult(data: Intent?): Flow<AuthResponse> = callbackFlow {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful && authTask.result?.user != null) {
                        trySend(AuthResponse.Success(authTask.result?.user!!))
                        Toast.makeText(context, "Signing in with Google completed", Toast.LENGTH_SHORT).show()
                    } else {
                        trySend(AuthResponse.Error("Google sign-in failed"))
                        Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                    close()
                }
        } catch (e: Exception) {
            trySend(AuthResponse.Error(e.message ?: "Google sign-in failed"))
            close()
        }
        awaitClose()
    }

    fun getCurrentUser(): FirebaseUser? {
        return Firebase.auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

}


sealed class AuthResponse {
    data class Success(val user: FirebaseUser) : AuthResponse()
    data class Error(val message: String) : AuthResponse()
    object Loading : AuthResponse()
}