package com.example.chat.presentation.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chat.domain.model.Conversation

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    canOpenAdmin: Boolean,
    onConversationSelected: (Conversation) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSignOut) { Text("Salir") }
            if (canOpenAdmin) {
                Button(onClick = onOpenAdmin) { Text("Admin") }
            }
        }
        Text("Conversaciones", style = MaterialTheme.typography.headlineSmall)
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

@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = conversation.title.ifBlank { "Sin titulo" })
        Text(
            text = conversation.lastMessagePreview.orEmpty(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
