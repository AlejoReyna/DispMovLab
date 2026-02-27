package com.example.chat.presentation.auth

import android.app.Activity
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

/** Validation / logic errors – the UI maps these to localized strings. */
enum class AuthError {
    INVALID_CREDENTIALS,
    INVALID_REGISTER_DATA,
    PASSWORDS_DONT_MATCH,
    INVALID_PHONE,
    INVALID_OTP
}

/**
 * Steps of the phone registration flow with SMS verification.
 * ENTER_PHONE → ENTER_OTP → ENTER_PROFILE
 */
enum class PhoneRegStep { ENTER_PHONE, ENTER_OTP, ENTER_PROFILE }

data class AuthUiState(
    val user: UserProfile? = null,
    val loading: Boolean = false,
    // Logic errors (mapped to string resources by the UI)
    val authError: AuthError? = null,
    // Raw Firebase error messages
    val firebaseError: String? = null,
    // Login (true) vs. Register (false) tab
    val isLoginMode: Boolean = true,
    // Set to true after a verification email has been (re)sent
    val verificationEmailSent: Boolean = false,
    // Current step of phone registration
    val phoneRegStep: PhoneRegStep = PhoneRegStep.ENTER_PHONE,
    // Phone number from step 1 (carried into subsequent steps)
    val phoneNumberEntered: String = "",
    // verificationId from Firebase after SMS is sent
    val verificationId: String = ""
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

    // ── Tab / mode ───────────────────────────────────────────────────────────

    fun setLoginMode(isLogin: Boolean) {
        _uiState.update {
            it.copy(
                isLoginMode = isLogin,
                authError = null,
                firebaseError = null,
                phoneRegStep = PhoneRegStep.ENTER_PHONE,
                phoneNumberEntered = "",
                verificationId = ""
            )
        }
    }

    // ── Phone + Password Sign-In ─────────────────────────────────────────────

    fun signIn(phoneNumber: String, password: String) {
        val cleaned = phoneNumber.trim()
        if (!cleaned.startsWith("+") || cleaned.length < 9 || !InputValidators.isValidPassword(password)) {
            _uiState.update { it.copy(authError = AuthError.INVALID_CREDENTIALS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, authError = null, firebaseError = null) }
            val result = authRepository.signInWithPhone(cleaned, password)
            _uiState.update {
                it.copy(loading = false, firebaseError = result.exceptionOrNull()?.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun resendEmailVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, verificationEmailSent = false) }
            authRepository.resendEmailVerification()
            _uiState.update { it.copy(loading = false, verificationEmailSent = true) }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            authRepository.reloadUser()
            _uiState.update { it.copy(loading = false) }
        }
    }

    // ── Phone Registration (SMS verification) ────────────────────────────────

    /**
     * Step 1 – validates the phone number and sends the SMS verification code.
     * On success, advances to ENTER_OTP. If the device auto-verifies, skips to ENTER_PROFILE.
     */
    fun sendPhoneVerification(phoneNumber: String, activity: Activity) {
        val cleaned = phoneNumber.trim()
        if (!cleaned.startsWith("+") || cleaned.length < 9) {
            _uiState.update { it.copy(authError = AuthError.INVALID_PHONE) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, authError = null, firebaseError = null) }
            val result = authRepository.sendPhoneVerification(cleaned, activity)
            result.fold(
                onSuccess = { verificationId ->
                    if (verificationId == null) {
                        // Auto-verified by Android — skip OTP step
                        _uiState.update {
                            it.copy(
                                loading = false,
                                phoneNumberEntered = cleaned,
                                phoneRegStep = PhoneRegStep.ENTER_PROFILE
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                phoneNumberEntered = cleaned,
                                verificationId = verificationId,
                                phoneRegStep = PhoneRegStep.ENTER_OTP
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, firebaseError = e.message) }
                }
            )
        }
    }

    /**
     * Step 2 – verifies the SMS code entered by the user.
     * On success, advances to ENTER_PROFILE.
     */
    fun verifyPhoneCode(smsCode: String) {
        val code = smsCode.trim()
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _uiState.update { it.copy(authError = AuthError.INVALID_OTP) }
            return
        }
        val verificationId = _uiState.value.verificationId
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, authError = null, firebaseError = null) }
            val result = authRepository.verifyPhoneCode(verificationId, code)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(loading = false, phoneRegStep = PhoneRegStep.ENTER_PROFILE)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, firebaseError = e.message) }
                }
            )
        }
    }

    /**
     * Step 3 – links email/password to the phone-authenticated account.
     * Email verification is sent automatically.
     */
    fun completePhoneRegistration(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (displayName.isBlank() || !InputValidators.isValidEmail(email) || !InputValidators.isValidPassword(password)) {
            _uiState.update { it.copy(authError = AuthError.INVALID_REGISTER_DATA) }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(authError = AuthError.PASSWORDS_DONT_MATCH) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, authError = null, firebaseError = null) }
            val result = authRepository.linkEmailPassword(
                phoneNumber = _uiState.value.phoneNumberEntered,
                displayName = displayName.trim(),
                email = email.trim(),
                password = password
            )
            result.fold(
                onSuccess = {
                    // Auth state listener picks up the updated user automatically.
                    // ChatNavHost will navigate to EmailVerificationScreen.
                    _uiState.update { it.copy(loading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, firebaseError = e.message) }
                }
            )
        }
    }

    /** Go back to phone-entry step (user wants to change their number). */
    fun resetPhoneStep() {
        _uiState.update {
            it.copy(
                phoneRegStep = PhoneRegStep.ENTER_PHONE,
                authError = null,
                firebaseError = null,
                phoneNumberEntered = "",
                verificationId = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(authError = null, firebaseError = null) }
    }
}
