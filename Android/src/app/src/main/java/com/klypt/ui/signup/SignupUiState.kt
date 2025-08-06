package com.klypt.ui.signup

data class SignupUiState(
    val fullName: String = "",
    val age: String = "",
    val currentJob: String = "",
    val instituteName: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "+1", // Default to US
    val selectedCountry: String = "US" // Default country
)
