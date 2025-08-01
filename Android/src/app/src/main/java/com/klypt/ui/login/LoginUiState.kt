package com.klypt.ui.login

import com.klypt.data.UserRole

data class LoginUiState (
    val firstName: String = "",
    val lastName: String = "",
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val isFormValid: Boolean = false,

    //General
    val role: UserRole = UserRole.STUDENT,
    val errorMessage: String? = null,
    val isOfflineMode: Boolean = false,
    val localDataAvailable: Boolean = false,

    //educator login
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
)
