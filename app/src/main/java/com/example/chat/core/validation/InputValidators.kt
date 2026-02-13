package com.example.chat.core.validation

object InputValidators {
    fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun isValidMessage(text: String): Boolean {
        val size = text.trim().length
        return size in 1..4000
    }
}
