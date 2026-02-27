package com.example.chat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.chat.presentation.auth.EmailVerificationScreen
import com.example.chat.presentation.chatroom.ChatRoomScreen
import com.example.chat.presentation.chatroom.ChatRoomViewModel
import com.example.chat.presentation.chatroom.ChatRoomViewModelFactory
import com.example.chat.presentation.conversations.ConversationsScreen
import com.example.chat.presentation.conversations.ConversationsViewModel
import com.example.chat.presentation.conversations.UserPickerScreen
import com.example.chat.presentation.videocall.VideoCallScreen
import com.example.chat.presentation.videocall.VideoCallViewModel
import com.example.chat.presentation.videocall.VideoCallViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ChatNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateCompat()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route

    // ── Auth guard ────────────────────────────────────────────────────────────
    LaunchedEffect(authState.user?.uid, authState.user?.isEmailVerified, route) {
        val user = authState.user
        when {
            user == null && route != Routes.Auth -> {
                navController.navigate(Routes.Auth) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            // Only redirect to email verification when the user has an email linked.
            // A phone-only user (email is empty) is still completing registration on AuthScreen.
            user != null && user.email.isNotEmpty() && !user.isEmailVerified && route != Routes.EmailVerification -> {
                navController.navigate(Routes.EmailVerification) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            user != null && user.email.isNotEmpty() && user.isEmailVerified &&
                    (route == Routes.Auth || route == Routes.EmailVerification) -> {
                navController.navigate(Routes.Conversations) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    val startDestination = when {
        authState.user == null                                               -> Routes.Auth
        authState.user!!.email.isEmpty()                                    -> Routes.Auth
        !authState.user!!.isEmailVerified                                   -> Routes.EmailVerification
        else                                                                -> Routes.Conversations
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Routes.Auth) {
            AuthScreen(
                uiState = authState,
                onSignIn = authViewModel::signIn,
                onTabSelected = authViewModel::setLoginMode,
                // Phone registration (3-step SMS verification)
                onSendPhoneVerification = { phone, activity ->
                    authViewModel.sendPhoneVerification(phone, activity)
                },
                onVerifyPhoneCode = authViewModel::verifyPhoneCode,
                onCompletePhoneRegistration = { displayName, email, password, confirmPassword ->
                    authViewModel.completePhoneRegistration(displayName, email, password, confirmPassword)
                },
                onResetPhoneStep = authViewModel::resetPhoneStep
            )
        }

        // ── Email verification ────────────────────────────────────────────────
        composable(Routes.EmailVerification) {
            EmailVerificationScreen(
                email = authState.user?.email.orEmpty(),
                loading = authState.loading,
                verificationEmailSent = authState.verificationEmailSent,
                onResendEmail = authViewModel::resendEmailVerification,
                onCheckVerification = authViewModel::checkEmailVerification,
                onSignOut = authViewModel::signOut
            )
        }

        // ── Conversations list ────────────────────────────────────────────────
        composable(Routes.Conversations) {
            val vm: ConversationsViewModel = viewModel()
            val state by vm.uiState.collectAsStateCompat()
            ConversationsScreen(
                state = state,
                canOpenAdmin = authState.user?.role == UserRole.ADMIN,
                onConversationSelected = { conv ->
                    navController.navigate(Routes.chatRoom(conv.id))
                },
                onOpenAdmin = { navController.navigate(Routes.Admin) },
                onSignOut = authViewModel::signOut,
                onNewChat = { navController.navigate(Routes.UserPicker) }
            )
        }

        // ── User picker (start new conversation) ─────────────────────────────
        composable(Routes.UserPicker) {
            val vm: ConversationsViewModel = viewModel(
                viewModelStoreOwner = navController
                    .getBackStackEntry(Routes.Conversations)
            )
            val pickerState by vm.pickerState.collectAsStateCompat()
            val scope = rememberCoroutineScope()
            UserPickerScreen(
                state = pickerState,
                onLoadUsers = vm::loadUsers,
                onSearchByPhone = vm::searchByPhone,
                onUserSelected = { user ->
                    scope.launch {
                        val convId = vm.createOrOpenConversation(user.uid)
                        if (convId != null) {
                            navController.navigate(Routes.chatRoom(convId)) {
                                popUpTo(Routes.Conversations)
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Chat room ─────────────────────────────────────────────────────────
        composable(
            route = "${Routes.ChatRoom}/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStack ->
            val conversationId = backStack.arguments?.getString("conversationId").orEmpty()
            val vm: ChatRoomViewModel =
                viewModel(factory = ChatRoomViewModelFactory(conversationId))
            val state by vm.uiState.collectAsStateCompat()
            ChatRoomScreen(
                state = state,
                onSendMessage = vm::sendMessage,
                onBack = { navController.popBackStack() },
                onStartVideoCall = {
                    navController.navigate(Routes.videoCall(conversationId))
                }
            )
        }

        // ── Video call ────────────────────────────────────────────────────────
        composable(
            route = "${Routes.VideoCall}/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStack ->
            val conversationId = backStack.arguments?.getString("conversationId").orEmpty()
            val vm: VideoCallViewModel =
                viewModel(factory = VideoCallViewModelFactory(conversationId))
            val state by vm.uiState.collectAsStateCompat()
            VideoCallScreen(
                channelId = conversationId,
                uiState = state,
                onInitAndJoin = { vm.initAndJoin(navController.context) },
                onSetupLocalVideo = vm::setupLocalVideo,
                onSetupRemoteVideo = vm::setupRemoteVideo,
                onToggleAudio = vm::toggleAudio,
                onToggleVideo = vm::toggleVideo,
                onLeaveCall = vm::leaveCall,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Admin ─────────────────────────────────────────────────────────────
        composable(Routes.Admin) {
            if (authState.user?.role != UserRole.ADMIN) {
                LaunchedEffect(Unit) { navController.popBackStack() }
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
