package com.example.intprogactivity.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.rewards.GetCoinHistoryUseCase
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
class RewardsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val getCoinHistoryUseCase: GetCoinHistoryUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _coinHistoryState = MutableStateFlow<UiState<List<TripCoinTransaction>>>(UiState.Loading)
    val coinHistoryState: StateFlow<UiState<List<TripCoinTransaction>>> = _coinHistoryState.asStateFlow()

    private val authRepo = authRepository

    init {
        viewModelScope.launch {
            val uid = authRepo.currentUser.first()?.uid ?: return@launch
            loadHistory(uid)
        }
    }

    private suspend fun loadHistory(uid: String) {
        getCoinHistoryUseCase(uid).fold(
            onSuccess = { _coinHistoryState.value = UiState.Success(it) },
            onFailure = { _coinHistoryState.value = UiState.Error(it.message ?: "Failed to load history") }
        )
    }
}
