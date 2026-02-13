package com.example.chat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chat.domain.model.UserRole
import com.example.chat.presentation.admin.AdminScreen
import com.example.chat.presentation.admin.AdminViewModel
import com.example.chat.presentation.auth.AuthScreen
import com.example.chat.presentation.auth.AuthViewModel
import com.example.chat.presentation.chatroom.ChatRoomScreen
import com.example.chat.presentation.chatroom.ChatRoomViewModel
import com.example.chat.presentation.chatroom.ChatRoomViewModelFactory
import com.example.chat.presentation.conversations.ConversationsScreen
import com.example.chat.presentation.conversations.ConversationsViewModel

@Composable
fun ChatNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateCompat()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route

    LaunchedEffect(authState.user?.uid, route) {
        val user = authState.user
        if (user == null && route != Routes.Auth) {
            navController.navigate(Routes.Auth) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        } else if (user != null && route == Routes.Auth) {
            navController.navigate(Routes.Conversations) {
                popUpTo(Routes.Auth) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.user == null) Routes.Auth else Routes.Conversations
    ) {
        composable(Routes.Auth) {
            AuthScreen(
                uiState = authState,
                onSignIn = authViewModel::signIn,
                onSignUp = { email, password, confirmPassword, displayName ->
                    authViewModel.signUp(email, password, confirmPassword, displayName)
                }
            )
        }
        composable(Routes.Conversations) {
            val vm: ConversationsViewModel = viewModel()
            val state by vm.uiState.collectAsStateCompat()
            val isAdmin = authState.user?.role == UserRole.ADMIN
            ConversationsScreen(
                state = state,
                canOpenAdmin = isAdmin,
                onConversationSelected = { conv ->
                    navController.navigate(Routes.chatRoom(conv.id))
                },
                onOpenAdmin = { navController.navigate(Routes.Admin) },
                onSignOut = authViewModel::signOut
            )
        }
        composable(
            route = "${Routes.ChatRoom}/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStack ->
            val conversationId = backStack.arguments?.getString("conversationId").orEmpty()
            val vm: ChatRoomViewModel = viewModel(factory = ChatRoomViewModelFactory(conversationId))
            val state by vm.uiState.collectAsStateCompat()
            ChatRoomScreen(
                state = state,
                onSendMessage = vm::sendMessage,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Admin) {
            val user = authState.user
            if (user?.role != UserRole.ADMIN) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                val vm: AdminViewModel = viewModel()
                val state by vm.uiState.collectAsStateCompat()
                AdminScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onToggleUserActive = vm::setUserActive,
                    onSetUserRole = { uid, admin ->
                        vm.setUserRole(uid, if (admin) UserRole.ADMIN else UserRole.USER)
                    }
                )
            }
        }
    }
}
