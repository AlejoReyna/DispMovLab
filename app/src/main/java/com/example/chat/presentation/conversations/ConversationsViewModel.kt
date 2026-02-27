package com.example.chat.presentation.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.domain.model.Conversation
import com.example.chat.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val items: List<Conversation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class UserPickerUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val searchResult: UserProfile? = null,
    val notFound: Boolean = false
)

class ConversationsViewModel : ViewModel() {
    private val chatRepository = AppContainer.chatRepository
    private val authRepository = AppContainer.authRepository

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    private val _pickerState = MutableStateFlow(UserPickerUiState())
    val pickerState: StateFlow<UserPickerUiState> = _pickerState.asStateFlow()

    private var currentUserUid: String = ""

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { profile ->
                currentUserUid = profile?.uid.orEmpty()
            }
        }
        viewModelScope.launch {
            chatRepository.observeConversations().collect { conversations ->
                _uiState.update { it.copy(items = conversations) }
            }
        }
    }

    fun searchByPhone(phoneNumber: String) {
        val cleaned = phoneNumber.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            _pickerState.update { it.copy(loading = true, error = null, searchResult = null, notFound = false) }
            val result = chatRepository.searchUserByPhone(cleaned)
            result.fold(
                onSuccess = { user ->
                    if (user == null || user.uid == currentUserUid) {
                        _pickerState.update { it.copy(loading = false, notFound = true) }
                    } else {
                        _pickerState.update { it.copy(loading = false, searchResult = user) }
                    }
                },
                onFailure = { e ->
                    _pickerState.update { it.copy(loading = false, error = e.message) }
                }
            )
        }
    }

    fun clearSearch() {
        _pickerState.update { it.copy(searchResult = null, notFound = false, error = null) }
    }

    fun loadUsers() {
        // Kept for compatibility
    }

    suspend fun createOrOpenConversation(otherUid: String): String? {
        val result = chatRepository.createDirectConversation(otherUid)
        return result.getOrNull()
    }
}
