package com.example.chat.presentation.chatroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.core.validation.InputValidators
import com.example.chat.domain.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatRoomUiState(
    val messages: List<Message> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null
)

class ChatRoomViewModel(
    private val conversationId: String
) : ViewModel() {
    private val chatRepository = AppContainer.chatRepository
    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (!InputValidators.isValidMessage(text)) {
            _uiState.update { it.copy(error = "Mensaje no valido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            val result = chatRepository.sendMessage(conversationId, text.trim())
            _uiState.update {
                it.copy(
                    sending = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}

class ChatRoomViewModelFactory(
    private val conversationId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatRoomViewModel(conversationId) as T
    }
}
