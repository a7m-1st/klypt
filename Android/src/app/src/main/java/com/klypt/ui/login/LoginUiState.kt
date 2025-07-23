package com.klypt.ui.login

data class LoginUiState (
    val firstName: String = "",
    val lastName: String = "",
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val errorMessage: String? = null,
    val isFormValid: Boolean = false
)
