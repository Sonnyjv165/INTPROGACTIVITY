package com.example.intprogactivity.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "open",    // open | closed — matches web
    val createdAt: Long = 0L,
    val replies: List<TicketReply> = emptyList()
)

data class TicketReply(
    val message: String = "",
    val isStaff: Boolean = false,
    val createdAt: Long = 0L
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _ticketsState = MutableStateFlow<UiState<List<SupportTicket>>>(UiState.Idle)
    val ticketsState: StateFlow<UiState<List<SupportTicket>>> = _ticketsState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val submitState: StateFlow<UiState<Unit>> = _submitState.asStateFlow()

    fun loadTickets() {
        viewModelScope.launch {
            val user = authRepository.currentUser.first() ?: return@launch
            _ticketsState.value = UiState.Loading
            try {
                val snapshot = firestore.collection(Constants.FIRESTORE_SUPPORT_TICKETS)
                    .whereEqualTo("userId", user.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await()

                val tickets = snapshot.documents.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val rawReplies = doc.get("replies") as? List<Map<String, Any>> ?: emptyList()
                    SupportTicket(
                        id        = doc.id,
                        userId    = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        subject   = doc.getString("subject") ?: "",
                        message   = doc.getString("message") ?: "",
                        status    = doc.getString("status") ?: "open",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        replies   = rawReplies.map { r ->
                            TicketReply(
                                message   = r["message"] as? String ?: "",
                                isStaff   = r["isStaff"] as? Boolean ?: false,
                                createdAt = (r["createdAt"] as? Long) ?: 0L
                            )
                        }
                    )
                }
                _ticketsState.value = UiState.Success(tickets)
            } catch (e: Exception) {
                _ticketsState.value = UiState.Error(e.message ?: "Failed to load tickets")
            }
        }
    }

    fun submitTicket(subject: String, message: String) {
        if (subject.isBlank() || message.isBlank()) {
            _submitState.value = UiState.Error("Subject and message are required")
            return
        }
        viewModelScope.launch {
            val user = authRepository.currentUser.first() ?: return@launch
            _submitState.value = UiState.Loading
            try {
                val ticketId = UUID.randomUUID().toString()
                val data = mapOf(
                    "userId"    to user.uid,
                    "userEmail" to user.email,
                    "subject"   to subject,
                    "message"   to message,
                    "status"    to "open",
                    "createdAt" to System.currentTimeMillis(),
                    "replies"   to emptyList<Any>()
                )
                firestore.collection(Constants.FIRESTORE_SUPPORT_TICKETS)
                    .document(ticketId)
                    .set(data).await()
                _submitState.value = UiState.Success(Unit)
                loadTickets()   // refresh list
            } catch (e: Exception) {
                _submitState.value = UiState.Error(e.message ?: "Failed to submit ticket")
            }
        }
    }

    fun resetSubmitState() { _submitState.value = UiState.Idle }
}
