package com.klypt.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.UserRole
import com.klypt.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: InputValidator,
    private val syncService: com.klypt.data.sync.SyncService
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState : StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(
            firstName = firstName,
            firstNameError = null
        )

        validateForm()
        checkLocalDataAvailability()
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(
            lastName = lastName,
            lastNameError = null
        )
        
        validateForm()
        checkLocalDataAvailability()
    }

    fun updateUserRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(
            role = role
        )
    }

    fun login(onSuccess: () -> Unit) {
        if(!validateInputs()) return;

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // First, check if user exists locally for faster response
                val localUserExists = syncService.checkUserLocally(
                    _uiState.value.firstName,
                    _uiState.value.lastName
                )
                
                // Attempt network login
                val result = authRepository.login(
                    firstName = _uiState.value.firstName,
                    lastName = _uiState.value.lastName
                )

                if(result.isSuccess) {
                    onSuccess()
                } else {
                    // If network login fails, try offline login
                    if (localUserExists.isSuccess && localUserExists.getOrNull() == true) {
                        val offlineResult = syncService.offlineLogin(
                            _uiState.value.firstName,
                            _uiState.value.lastName
                        )
                        
                        if (offlineResult.isSuccess && offlineResult.getOrNull() != null) {
                            // Use offline data (you might want to set a flag to indicate offline mode)
                            onSuccess()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Login failed. No network connection and no offline data available."
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Login Failed"
                        )
                    }
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
    
    private fun checkLocalDataAvailability() {
        val firstName = _uiState.value.firstName
        val lastName = _uiState.value.lastName
        
        if (firstName.isNotBlank() && lastName.isNotBlank()) {
            viewModelScope.launch {
                val localUserExists = syncService.checkUserLocally(firstName, lastName)
                _uiState.value = _uiState.value.copy(
                    localDataAvailable = localUserExists.isSuccess && localUserExists.getOrNull() == true
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(localDataAvailable = false)
        }
    }
}