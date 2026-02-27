package com.example.chat.presentation.conversations

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chat.R
import com.example.chat.core.LanguagePrefs
import com.example.chat.domain.model.Conversation

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    canOpenAdmin: Boolean,
    onConversationSelected: (Conversation) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onNewChat: () -> Unit
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_new_chat))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onSignOut) {
                    Text(stringResource(R.string.btn_sign_out))
                }
                OutlinedButton(onClick = { showLanguageDialog = true }) {
                    Text(stringResource(R.string.btn_language))
                }
                if (canOpenAdmin) {
                    Button(onClick = onOpenAdmin) {
                        Text(stringResource(R.string.btn_admin))
                    }
                }
            }

            Text(
                text = stringResource(R.string.screen_conversations),
                style = MaterialTheme.typography.headlineSmall
            )

            LazyColumn {
                items(state.items) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationSelected(conversation) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Language dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.dialog_select_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showLanguageDialog = false
                            LanguagePrefs.setLanguage(context, LanguagePrefs.LANG_ES)
                            (context as? Activity)?.recreate()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.lang_es)) }
                    TextButton(
                        onClick = {
                            showLanguageDialog = false
                            LanguagePrefs.setLanguage(context, LanguagePrefs.LANG_EN)
                            (context as? Activity)?.recreate()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.lang_en)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = conversation.title.ifBlank { stringResource(R.string.no_title) })
        Text(
            text = conversation.lastMessagePreview ?: stringResource(R.string.no_last_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
