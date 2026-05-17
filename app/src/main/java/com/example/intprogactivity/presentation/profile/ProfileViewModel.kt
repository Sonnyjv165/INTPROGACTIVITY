package com.example.intprogactivity.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.auth.SignOutUseCase
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _signOutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val signOutState: StateFlow<UiState<Unit>> = _signOutState.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _signOutState.value = UiState.Success(Unit)
        }
    }
}
