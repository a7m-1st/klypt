package com.klypt.ui.signup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SignupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun updateFullName(name: String) {
        _uiState.value = _uiState.value.copy(fullName = name)
    }
    fun updateAge(age: String) {
        _uiState.value = _uiState.value.copy(age = age)
    }
    fun updateCurrentJob(job: String) {
        _uiState.value = _uiState.value.copy(currentJob = job)
    }
    fun updateInstituteName(name: String) {
        _uiState.value = _uiState.value.copy(instituteName = name)
    }
    fun updatePhoneNumber(number: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = number)
    }
}
