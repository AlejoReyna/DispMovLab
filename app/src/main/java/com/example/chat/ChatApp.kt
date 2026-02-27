package com.example.chat

import android.app.Application
import com.google.firebase.auth.FirebaseAuth

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Disable app verification entirely for emulator testing.
        // This skips both Play Integrity and reCAPTCHA.
        // IMPORTANT: Only works with test phone numbers configured in Firebase Console.
        // Remove this before releasing to production.
        FirebaseAuth.getInstance().firebaseAuthSettings
            .setAppVerificationDisabledForTesting(true)
    }

    private fun isEmulator(): Boolean =
        android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.BRAND.startsWith("generic")
            || android.os.Build.DEVICE.startsWith("generic")
}
