package com.klypt.ui.otp

data class OtpUiState(
    val otpValue: String = "",
    val otpLength: Int = 6,
    val otpError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
