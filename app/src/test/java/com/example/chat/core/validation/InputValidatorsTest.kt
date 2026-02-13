package com.example.chat.core.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorsTest {
    @Test
    fun `email validator accepts basic valid email`() {
        assertTrue(InputValidators.isValidEmail("user@example.com"))
    }

    @Test
    fun `email validator rejects invalid email`() {
        assertFalse(InputValidators.isValidEmail("invalid-email"))
    }

    @Test
    fun `password validator requires minimum length`() {
        assertTrue(InputValidators.isValidPassword("12345678"))
        assertFalse(InputValidators.isValidPassword("1234"))
    }

    @Test
    fun `message validator enforces limits`() {
        assertTrue(InputValidators.isValidMessage("hola"))
        assertFalse(InputValidators.isValidMessage(""))
        assertFalse(InputValidators.isValidMessage(" ".repeat(5001)))
    }
}
