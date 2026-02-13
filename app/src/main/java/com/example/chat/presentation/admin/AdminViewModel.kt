package com.example.chat.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.domain.model.ModerationReport
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val reports: List<ModerationReport> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val chatRepository = AppContainer.chatRepository
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        refreshUsers()
        viewModelScope.launch {
            chatRepository.observeOpenReports().collect { reports ->
                _uiState.update { it.copy(reports = reports, loading = false) }
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            val usersResult = chatRepository.getUsers()
            _uiState.update {
                it.copy(
                    users = usersResult.getOrDefault(emptyList()),
                    loading = false,
                    error = usersResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun setUserActive(uid: String, active: Boolean) {
        viewModelScope.launch {
            val result = chatRepository.updateUserActiveState(uid, active)
            _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            refreshUsers()
        }
    }

    fun setUserRole(uid: String, role: UserRole) {
        viewModelScope.launch {
            val result = chatRepository.updateUserRole(uid, role)
            _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            refreshUsers()
        }
    }
}
