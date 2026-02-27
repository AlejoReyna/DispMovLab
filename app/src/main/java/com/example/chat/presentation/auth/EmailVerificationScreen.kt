package com.example.chat.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chat.R

@Composable
fun EmailVerificationScreen(
    email: String,
    loading: Boolean,
    verificationEmailSent: Boolean,
    onResendEmail: () -> Unit,
    onCheckVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.email_verification_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.email_verification_message, email),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        if (verificationEmailSent) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.email_verification_sent),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary action: check if the user has already verified
        Button(
            onClick = onCheckVerification,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_check_verification))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary: resend verification email
        OutlinedButton(
            onClick = onResendEmail,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_resend_email))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign out
        OutlinedButton(
            onClick = onSignOut,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_sign_out))
        }
    }
}
