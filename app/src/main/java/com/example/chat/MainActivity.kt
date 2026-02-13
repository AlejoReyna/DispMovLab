package com.example.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.chat.presentation.navigation.ChatNavHost
import com.example.chat.presentation.theme.DispMovTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DispMovTheme {
                ChatNavHost()
            }
        }
    }
}
