package com.example.chat.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chat.R
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    state: AdminUiState,
    onBack: () -> Unit,
    onToggleUserActive: (uid: String, active: Boolean) -> Unit,
    onSetUserRole: (uid: String, isAdmin: Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_admin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            state.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Text(
                text = "Usuarios (${state.users.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn {
                items(state.users, key = { it.uid }) { user ->
                    AdminUserItem(
                        user = user,
                        onToggleActive = { onToggleUserActive(user.uid, !user.isActive) },
                        onToggleRole = { onSetUserRole(user.uid, user.role != UserRole.ADMIN) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AdminUserItem(
    user: UserProfile,
    onToggleActive: () -> Unit,
    onToggleRole: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Name + email
        Text(
            text = user.displayName.ifBlank { user.email },
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Chips row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            // Role chip
            AssistChip(
                onClick = onToggleRole,
                label = {
                    Text(
                        if (user.role == UserRole.ADMIN)
                            stringResource(R.string.role_admin)
                        else
                            stringResource(R.string.role_user)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (user.role == UserRole.ADMIN)
                            Icons.Default.AdminPanelSettings
                        else
                            Icons.Default.Person,
                        contentDescription = null
                    )
                },
                colors = if (user.role == UserRole.ADMIN)
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                else AssistChipDefaults.assistChipColors()
            )

            // Active/Inactive chip
            FilterChip(
                selected = user.isActive,
                onClick = onToggleActive,
                label = {
                    Text(
                        if (user.isActive) stringResource(R.string.label_active)
                        else stringResource(R.string.label_inactive)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (user.isActive) Icons.Default.ToggleOn
                                      else Icons.Default.ToggleOff,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
