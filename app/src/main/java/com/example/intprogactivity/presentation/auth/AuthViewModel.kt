package com.example.intprogactivity.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.auth.*
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val registerUseCase: RegisterUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val signOutUseCase: SignOutUseCase,
    authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val authState: StateFlow<UiState<User>> = _authState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val forgotPasswordState: StateFlow<UiState<Unit>> = _forgotPasswordState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = signInUseCase(email, password)
            _authState.value = when (result) {
                is com.example.intprogactivity.util.Result.Success -> UiState.Success(result.data)
                is com.example.intprogactivity.util.Result.Error -> UiState.Error(result.exception.message ?: "Sign-in failed")
                is com.example.intprogactivity.util.Result.Loading -> UiState.Loading
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = googleSignInUseCase(idToken)
            _authState.value = when (result) {
                is com.example.intprogactivity.util.Result.Success -> UiState.Success(result.data)
                is com.example.intprogactivity.util.Result.Error -> UiState.Error(result.exception.message ?: "Google sign-in failed")
                is com.example.intprogactivity.util.Result.Loading -> UiState.Loading
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = registerUseCase(email, password, confirmPassword, firstName, lastName)
            _authState.value = when (result) {
                is com.example.intprogactivity.util.Result.Success -> UiState.Success(result.data)
                is com.example.intprogactivity.util.Result.Error -> UiState.Error(result.exception.message ?: "Registration failed")
                is com.example.intprogactivity.util.Result.Loading -> UiState.Loading
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = UiState.Loading
            val result = forgotPasswordUseCase(email)
            _forgotPasswordState.value = when (result) {
                is com.example.intprogactivity.util.Result.Success -> UiState.Success(Unit)
                is com.example.intprogactivity.util.Result.Error -> UiState.Error(result.exception.message ?: "Failed to send reset email")
                is com.example.intprogactivity.util.Result.Loading -> UiState.Loading
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _authState.value = UiState.Idle
        }
    }

    fun setError(message: String) {
        _authState.value = UiState.Error(message)
    }

    fun resetState() {
        _authState.value = UiState.Idle
        _forgotPasswordState.value = UiState.Idle
    }
}
