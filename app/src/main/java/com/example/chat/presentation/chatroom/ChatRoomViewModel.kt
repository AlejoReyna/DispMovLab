package com.example.chat.presentation.chatroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.core.validation.InputValidators
import com.example.chat.domain.model.Message
import com.example.chat.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatRoomUiState(
    val messages: List<Message> = emptyList(),
    /** Map of uid → displayName for quick lookup in the UI */
    val senderNames: Map<String, String> = emptyMap(),
    /** UID of the currently authenticated user */
    val currentUserUid: String = "",
    /** True if the current user is an admin (can see moderation actions) */
    val isAdmin: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null
)

class ChatRoomViewModel(private val conversationId: String) : ViewModel() {
    private val chatRepository = AppContainer.chatRepository
    private val authRepository = AppContainer.authRepository

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        // Observe current user for UID and role
        viewModelScope.launch {
            authRepository.currentUser.collect { profile ->
                _uiState.update {
                    it.copy(
                        currentUserUid = profile?.uid.orEmpty(),
                        isAdmin = profile?.role == UserRole.ADMIN
                    )
                }
            }
        }
        // Observe messages in real-time
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                resolveNewSenders(messages)
            }
        }
    }

    /** For any sender not yet in senderNames, fetch the display name from Firestore. */
    private fun resolveNewSenders(messages: List<Message>) {
        val knownUids = _uiState.value.senderNames.keys
        val unknownUids = messages.map { it.senderId }.toSet() - knownUids
        if (unknownUids.isEmpty()) return

        viewModelScope.launch {
            val result = chatRepository.getUsers(100)
            result.getOrNull()?.let { users ->
                val newNames = users.associate { it.uid to it.displayName.ifBlank { it.email } }
                _uiState.update { state ->
                    state.copy(senderNames = state.senderNames + newNames)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (!InputValidators.isValidMessage(text)) return
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            val result = chatRepository.sendMessage(conversationId, text)
            _uiState.update {
                it.copy(
                    sending = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}

class ChatRoomViewModelFactory(private val conversationId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatRoomViewModel(conversationId) as T
    }
}
