package com.example.chat.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.core.validation.InputValidators
import com.example.chat.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: UserProfile? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { profile ->
                _uiState.update { it.copy(user = profile) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (!InputValidators.isValidEmail(email) || !InputValidators.isValidPassword(password)) {
            _uiState.update { it.copy(error = "Credenciales no validas") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.signIn(email, password)
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String, displayName: String) {
        if (displayName.isBlank() || !InputValidators.isValidEmail(email) || !InputValidators.isValidPassword(password)) {
            _uiState.update { it.copy(error = "Datos de registro no validos") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Las contrasenas no coinciden") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.signUp(email, password, displayName)
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
