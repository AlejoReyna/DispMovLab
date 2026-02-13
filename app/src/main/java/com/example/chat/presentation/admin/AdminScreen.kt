package com.example.chat.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chat.domain.model.UserRole

@Composable
fun AdminScreen(
    state: AdminUiState,
    onBack: () -> Unit,
    onToggleUserActive: (uid: String, active: Boolean) -> Unit,
    onSetUserRole: (uid: String, admin: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = onBack) { Text("Volver") }
        Text("Administracion", style = MaterialTheme.typography.headlineSmall)
        Text("Usuarios")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.users) { user ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${user.displayName} (${if (user.role == UserRole.ADMIN) "admin" else "user"})"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onToggleUserActive(user.uid, !user.isActive) }) {
                            Text(if (user.isActive) "Desactivar" else "Activar")
                        }
                        Button(
                            onClick = {
                                onSetUserRole(user.uid, user.role != UserRole.ADMIN)
                            }
                        ) {
                            Text(if (user.role == UserRole.ADMIN) "Quitar admin" else "Hacer admin")
                        }
                    }
                }
            }
        }
        Text("Reportes abiertos: ${state.reports.size}")
        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
