@file:Suppress("DEPRECATION")

package pl.podkal.domowniczeqqq.login

import android.content.Context
import android.content.Intent
import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.podkal.domowniczeq.R
import android.widget.Toast

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
                    val user = task.result?.user!!
                    // Ensure user data is not null
                    val safeUser = user.apply {
                        if (displayName == null) {
                            updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName("").build())
                        }
                        if (email == null) {
                            // This shouldn't happen but just in case
                            trySend(AuthResponse.Error("Email is null"))
                            Toast.makeText(context, "Login unsuccessful: missing email", Toast.LENGTH_SHORT).show()
                            close()
                            return@addOnCompleteListener
                        }
                    }
                    trySend(AuthResponse.Success(safeUser))
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
        try {
            // Check Google Play Services availability first
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                // Google Play Services is not available
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    try {
                        // Show a dialog that allows the user to update/install Google Play Services
                        googleApiAvailability.getErrorDialog(activity, resultCode, 1001)?.show()
                    } catch (e: Exception) {
                        // If showing dialog fails, fallback to toast message
                        Toast.makeText(
                            context,
                            "Google Play Services update required. Please use email login instead.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "This device doesn't support Google Play Services. Please use email login.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            googleSignInClient.signOut() // This will force account selection on next sign-in
            launcher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Google Play Services not available: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    fun handleGoogleSignInResult(data: Intent?): Flow<AuthResponse> = callbackFlow {
        try {
            if (data == null) {
                trySend(AuthResponse.Error("No sign-in data received"))
                Toast.makeText(context, "No sign-in data received", Toast.LENGTH_SHORT).show()
                close()
                return@callbackFlow
            }

            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    trySend(AuthResponse.Success(authTask.result?.user!!))
                                    Toast.makeText(context, "Signing in with Google completed", Toast.LENGTH_SHORT).show()
                                } else {
                                    trySend(AuthResponse.Error("Google sign-in failed: ${authTask.exception?.message ?: "Unknown error"}"))
                                    Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                                }
                                close()
                            }
                    } catch (e: ApiException) {
                        trySend(AuthResponse.Error("Google sign-in failed: ${e.message}"))
                        Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        close()
                    }
                } else {
                    trySend(AuthResponse.Error("Google sign-in unsuccessful: ${task.exception?.message ?: "Unknown error"}"))
                    Toast.makeText(context, "Google sign-in unsuccessful", Toast.LENGTH_SHORT).show()
                    close()
                    return@callbackFlow
                }

            } catch (e: Exception) {
                trySend(AuthResponse.Error("Google sign-in process error: ${e.message}"))
                Toast.makeText(context, "Google sign-in process error: ${e.message}", Toast.LENGTH_SHORT).show()
                close()
            }
        } catch (e: Exception) {
            trySend(AuthResponse.Error(e.message ?: "Google sign-in failed"))
            Toast.makeText(context, "Google sign-in failed with exception", Toast.LENGTH_SHORT).show()
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