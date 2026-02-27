package com.example.chat

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.chat.core.LanguagePrefs
import com.example.chat.presentation.navigation.ChatNavHost
import com.example.chat.presentation.theme.DispMovTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguagePrefs.getLanguage(newBase)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DispMovTheme {
                ChatNavHost()
            }
        }
    }
}
