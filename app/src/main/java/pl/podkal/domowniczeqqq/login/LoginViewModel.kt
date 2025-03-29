package pl.podkal.domowniczeqqq.login

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthenticationManager,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResponse?>(null)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        authManager.loginWithEmail(email, password)
            .onEach { response -> _authState.value = response }
            .catch { e -> _authState.value = AuthResponse.Error(e.message ?: "Unexpected error") }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            authState.collect{ stan ->
                if( stan is AuthResponse.Success) {
                    savedStateHandle["navigation_destination"] = "home_screen"
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        authManager.signInWithGoogle(activity, launcher)
    }
    fun handleGoogleSignInResult(data: Intent?) {
        authManager.handleGoogleSignInResult(data) { success ->
            if (success) {
                _authState.value = AuthResponse.Success
            } else {
                _authState.value = AuthResponse.Error("Logowanie Google nie powiodło się")
            }
        }
    }



    fun loginSuccess() {
        _authState.value = AuthResponse.Success
    }

    fun loginFailure(message: String) {
        _authState.value = AuthResponse.Error(message)
    }

    fun registerbyEmail(email: String, password: String) {
        authManager.createAccountWithEmail(
            email, password
        )
            .onEach { response ->
                _authState.value = response
            }
            .catch { e ->
                _authState.value = AuthResponse.Error(e.message ?: "Registration failed")
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            authState.collect { stan ->
                if (stan is AuthResponse.Success) {
                    savedStateHandle["navigation_destination"] = "login_screen"
                }
            }
        }
    }

    fun checkIsUserLoggedIn() {
        viewModelScope.launch {
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                _authState.value = AuthResponse.Success
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


