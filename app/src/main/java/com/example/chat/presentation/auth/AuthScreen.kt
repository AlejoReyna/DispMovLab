package com.example.chat.presentation.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.chat.R

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onTabSelected: (isLogin: Boolean) -> Unit,
    // Phone registration callbacks (3-step SMS verification)
    onSendPhoneVerification: (phoneNumber: String, activity: Activity) -> Unit,
    onVerifyPhoneCode: (smsCode: String) -> Unit,
    onCompletePhoneRegistration: (
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) -> Unit,
    onResetPhoneStep: () -> Unit
) {
    val activity = LocalContext.current as Activity

    // ── Login fields ─────────────────────────────────────────────────────────
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var loginPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // ── Phone registration fields ─────────────────────────────────────────────
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var smsCode by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regConfirmPassword by rememberSaveable { mutableStateOf("") }
    var regPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var regConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val errorText = when (uiState.authError) {
        AuthError.INVALID_CREDENTIALS -> stringResource(R.string.error_invalid_credentials)
        AuthError.INVALID_REGISTER_DATA -> stringResource(R.string.error_invalid_register_data)
        AuthError.PASSWORDS_DONT_MATCH -> stringResource(R.string.error_passwords_dont_match)
        AuthError.INVALID_PHONE -> stringResource(R.string.error_invalid_phone)
        AuthError.INVALID_OTP -> stringResource(R.string.error_invalid_otp)
        null -> uiState.firebaseError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Tabs ──────────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = if (uiState.isLoginMode) 0 else 1) {
            Tab(
                selected = uiState.isLoginMode,
                onClick = { onTabSelected(true) },
                text = { Text(stringResource(R.string.auth_tab_login)) }
            )
            Tab(
                selected = !uiState.isLoginMode,
                onClick = { onTabSelected(false) },
                text = { Text(stringResource(R.string.auth_tab_register)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  LOGIN TAB – phone + password
        // ═════════════════════════════════════════════════════════════════════
        if (uiState.isLoginMode) {
            Text(
                text = stringResource(R.string.auth_tab_login),
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = loginEmail,
                onValueChange = { loginEmail = it },
                label = { Text(stringResource(R.string.label_phone)) },
                placeholder = { Text(stringResource(R.string.label_phone_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = loginPassword,
                onValueChange = { loginPassword = it },
                label = { Text(stringResource(R.string.label_password)) },
                singleLine = true,
                visualTransformation = if (loginPasswordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    val icon = if (loginPasswordVisible) Icons.Filled.VisibilityOff
                               else Icons.Filled.Visibility
                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = if (loginPasswordVisible)
                                stringResource(R.string.hide_password)
                            else
                                stringResource(R.string.show_password)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSignIn(loginEmail.trim(), loginPassword) },
                enabled = !uiState.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(stringResource(R.string.btn_sign_in))
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  REGISTER TAB – phone + SMS verification (3 steps)
        // ═════════════════════════════════════════════════════════════════════
        else {
            when (uiState.phoneRegStep) {

                // ── Step 1: Enter phone number ────────────────────────────────
                PhoneRegStep.ENTER_PHONE -> {
                    Text(
                        text = stringResource(R.string.phone_step_title_phone),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(stringResource(R.string.label_phone)) },
                        placeholder = { Text(stringResource(R.string.label_phone_hint)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.label_phone_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onSendPhoneVerification(phoneNumber.trim(), activity) },
                        enabled = !uiState.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Text(stringResource(R.string.btn_send_code))
                        }
                    }
                }

                // ── Step 2: Enter SMS verification code ───────────────────────
                PhoneRegStep.ENTER_OTP -> {
                    Text(
                        text = stringResource(R.string.phone_step_title_otp),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = stringResource(R.string.phone_step_desc_otp, uiState.phoneNumberEntered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = smsCode,
                        onValueChange = { if (it.length <= 6) smsCode = it },
                        label = { Text(stringResource(R.string.label_sms_code)) },
                        placeholder = { Text(stringResource(R.string.label_sms_code_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onVerifyPhoneCode(smsCode.trim()) },
                        enabled = !uiState.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Text(stringResource(R.string.btn_verify_code))
                        }
                    }

                    TextButton(
                        onClick = onResetPhoneStep,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                        Text(
                            text = "  " + stringResource(R.string.btn_back_to_phone),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // ── Step 3: Enter profile (name, email, password) ─────────────
                PhoneRegStep.ENTER_PROFILE -> {
                    Text(
                        text = stringResource(R.string.phone_step_title_profile),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = stringResource(R.string.phone_step_desc_profile),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.label_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text(stringResource(R.string.label_email)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text(stringResource(R.string.label_password)) },
                        singleLine = true,
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            val icon = if (regPasswordVisible) Icons.Filled.VisibilityOff
                                       else Icons.Filled.Visibility
                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (regPasswordVisible)
                                        stringResource(R.string.hide_password)
                                    else
                                        stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it },
                        label = { Text(stringResource(R.string.label_confirm_password)) },
                        singleLine = true,
                        visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            val icon = if (regConfirmPasswordVisible) Icons.Filled.VisibilityOff
                                       else Icons.Filled.Visibility
                            IconButton(onClick = {
                                regConfirmPasswordVisible = !regConfirmPasswordVisible
                            }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (regConfirmPasswordVisible)
                                        stringResource(R.string.hide_password)
                                    else
                                        stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onCompletePhoneRegistration(
                                displayName.trim(),
                                regEmail.trim(),
                                regPassword,
                                regConfirmPassword
                            )
                        },
                        enabled = !uiState.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Text(stringResource(R.string.btn_complete_registration))
                        }
                    }

                    TextButton(
                        onClick = onResetPhoneStep,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                        Text(
                            text = "  " + stringResource(R.string.btn_back_to_phone),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── Error message ─────────────────────────────────────────────────────
        errorText?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
