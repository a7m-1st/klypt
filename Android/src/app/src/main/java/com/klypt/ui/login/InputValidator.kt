package com.klypt.ui.login

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputValidator @Inject constructor() {
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name is required")
            name.contains(" ") -> ValidationResult(false, "Exclude Spaces")
            else -> ValidationResult(true)
        }
    }
    
    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        return when {
            phoneNumber.isBlank() -> ValidationResult(false, "Phone number is required")
            phoneNumber.length < 9 -> ValidationResult(false, "Phone number must be at least 9 digits")
            !phoneNumber.all { it.isDigit() } -> ValidationResult(false, "Phone number must contain only digits")
            else -> ValidationResult(true)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)