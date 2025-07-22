package com.klpyt.ui.login

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputValidator @Inject constructor() {
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Email is required")
            name.contains(" ") -> ValidationResult(false, "Exclude Spaces")
            else -> ValidationResult(true)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)