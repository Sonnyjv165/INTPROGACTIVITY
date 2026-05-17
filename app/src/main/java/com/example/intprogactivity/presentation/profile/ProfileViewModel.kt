package com.example.intprogactivity.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.repository.UserRepository
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.usecase.auth.SignOutUseCase
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _signOutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val signOutState: StateFlow<UiState<Unit>> = _signOutState.asStateFlow()

    private val _updateProfileState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateProfileState: StateFlow<UiState<Unit>> = _updateProfileState.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _signOutState.value = UiState.Success(Unit)
        }
    }

    fun updateProfile(displayName: String, phone: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser.first() ?: return@launch
            _updateProfileState.value = UiState.Loading
            val updated = user.copy(displayName = displayName, phone = phone)
            when (userRepository.updateUserProfile(updated)) {
                is com.example.intprogactivity.util.Result.Success -> {
                    _updateProfileState.value = UiState.Success(Unit)
                }
                is com.example.intprogactivity.util.Result.Error -> {
                    _updateProfileState.value = UiState.Error("Failed to update profile")
                }
                else -> Unit
            }
        }
    }

    fun resetUpdateState() {
        _updateProfileState.value = UiState.Idle
    }
}
