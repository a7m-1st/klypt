package com.klpyt.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klpyt.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: InputValidator
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState : StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(
            firstName = firstName,
            firstNameError = null
        )

        validateForm()
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(
            lastName = lastName,
            lastNameError = null
        )
        validateForm()
    }

    fun login(onSuccess: () -> Unit) {
        if(!validateInputs()) return;

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val result = authRepository.login(
                    firstName = _uiState.value.firstName,
                    lastName = _uiState.value.lastName
                )

                if(result.isSuccess) {onSuccess()}
                else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Login Failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Network Error"
                )
            }

        }
    }

    //IsValid, errorMessage takes String
    private fun validateInputs(): Boolean {
        val firstNameValidation = validator.validateName(_uiState.value.firstName)
        val lastNameValidation = validator.validateName(_uiState.value.lastName)

        _uiState.value = _uiState.value.copy(
            firstNameError = firstNameValidation.errorMessage,
            lastNameError = lastNameValidation.errorMessage
        )

        return firstNameValidation.isValid && lastNameValidation.isValid
    }

    private fun validateForm() {
        val isValid = _uiState.value.firstName.isNotBlank() &&
                _uiState.value.lastName.isNotBlank();

        _uiState.value = _uiState.value.copy(isFormValid = isValid)
    }
}