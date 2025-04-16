package pl.podkal.domowniczeqqq.login

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthenticationManager,
    val savedStateHandle: SavedStateHandle,
    private val db: FirebaseFirestore // Inject Firestore instance
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResponse?>(null)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        authManager.loginWithEmail(email, password)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
                        val userMap = hashMapOf(
                            "email" to response.user.email,
                            "lastLogin" to FieldValue.serverTimestamp(),
                            "displayName" to response.user.displayName,
                            "photoUrl" to response.user.photoUrl?.toString(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users")
                            .document(response.user.uid)
                            .set(userMap, SetOptions.merge())
                            .addOnSuccessListener {
                                _authState.value = response
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthResponse.Error(e.message ?: "Failed to save user data")
                            }
                    }
                    else -> _authState.value = response
                }
            }
            .catch { e -> _authState.value = AuthResponse.Error(e.message ?: "Unexpected error") }
            .launchIn(viewModelScope)

        // Ustawienie destynacji na "home_screen" po sukcesie logowania
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthResponse.Success) {
                    savedStateHandle["navigation_destination"] = "home_screen"
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        authManager.signInWithGoogle(activity, launcher)
    }

    // The duplicate method was removed - keeping only the version that has Firestore integration

    fun handleGoogleSignInResult(data: Intent?) {
        authManager.handleGoogleSignInResult(data)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
                        val userMap = hashMapOf(
                            "email" to response.user.email,
                            "lastLogin" to FieldValue.serverTimestamp(),
                            "displayName" to response.user.displayName,
                            "photoUrl" to response.user.photoUrl?.toString(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users")
                            .document(response.user.uid)
                            .set(userMap, SetOptions.merge())
                            .addOnSuccessListener {
                                _authState.value = response
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthResponse.Error(e.message ?: "Failed to save user data")
                            }
                    }
                    else -> _authState.value = response
                }
            }
            .catch { e -> _authState.value = AuthResponse.Error(e.message ?: "Google sign-in failed") }
            .launchIn(viewModelScope)
    }

    fun loginSuccess() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthResponse.Success(currentUser)
        }
    }

    fun loginFailure(message: String) {
        _authState.value = AuthResponse.Error(message)
    }

    fun registerByEmail(email: String, password: String) {
        authManager.createAccountWithEmail(email, password)
            .onEach { response ->
                if (response is AuthResponse.Success) {
                    val user = response.user
                    val userMap = hashMapOf(
                        "email" to user.email,
                        "lastLogin" to FieldValue.serverTimestamp()
                    )
                    db.collection("users")
                        .document(user.uid)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {
                            _authState.value = AuthResponse.Success(user)
                        }
                        .addOnFailureListener { e ->
                            _authState.value = AuthResponse.Error(e.message ?: "Failed to save user data")
                        }
                } else {
                    _authState.value = response
                }
            }
            .catch { e ->
                _authState.value = AuthResponse.Error(e.message ?: "Registration failed")
            }
            .launchIn(viewModelScope)

        // Po rejestracji ustawiamy destynację na "login_screen" lub inną wybraną
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthResponse.Success) {
                    savedStateHandle["navigation_destination"] = "login_screen"
                }
            }
        }
    }

    fun checkIsUserLoggedIn() {
        viewModelScope.launch {
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                _authState.value = AuthResponse.Success(currentUser)
            } else {
                _authState.value = null
            }
        }
    }

    fun logout() {
        authManager.logout()
        _authState.value = null
    }
}
