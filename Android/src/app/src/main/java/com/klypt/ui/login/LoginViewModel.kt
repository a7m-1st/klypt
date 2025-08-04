package com.klypt.ui.login

import android.util.Log
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

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phoneNumber,
            phoneNumberError = null
        )
        
        validateForm()
    }

    fun updateCountryCode(countryCode: String, country: String) {
        _uiState.value = _uiState.value.copy(
            countryCode = countryCode,
            selectedCountry = country
        )
    }

    fun updateUserRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(
            role = role
        )
    }

    fun login(onSuccess: () -> Unit) {
        if(!validateInputs()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // First, check if user exists locally for faster response
                val localUserResult = authRepository.getUserFromCouchDB(
                    _uiState.value.firstName,
                    _uiState.value.lastName,
                    _uiState.value.role
                )

                Log.d(Variables.TAG, "Couch DB: $localUserResult")
                
                // Attempt network login
                val result = authRepository.login(
                    firstName = _uiState.value.firstName,
                    lastName = _uiState.value.lastName,
                    userRole = _uiState.value.role
                )

                if(result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOfflineMode = false
                    )
                    onSuccess()
                } else {
                    // If network login fails, try offline login with local CouchDB data
                    if (localUserResult.isSuccess && localUserResult.getOrNull() != null) {
                        // User exists in local CouchDB, proceed with offline mode
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOfflineMode = true,
                            errorMessage = null
                        )
                        onSuccess()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Login failed. No network connection and no offline data available."
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

    /**
     * Search for users in CouchDB (useful for auto-complete or user discovery)
     */
    fun searchUsers(query: String, onResult: (List<String>) -> Unit) {
        if (query.isBlank()) {
            onResult(emptyList())
            return
        }
        
        viewModelScope.launch {
            try {
                val searchResult = authRepository.searchUsersInCouchDB(query, _uiState.value.role)
                if (searchResult.isSuccess) {
                    val users = searchResult.getOrNull() ?: emptyList()
                    val userNames = when (_uiState.value.role) {
                        UserRole.STUDENT -> {
                            users.map { user ->
                                if (user is com.klypt.data.models.Student) {
                                    "${user.firstName} ${user.lastName}"
                                } else ""
                            }
                        }
                        UserRole.EDUCATOR -> {
                            users.map { user ->
                                if (user is com.klypt.data.models.Educator) {
                                    user.fullName
                                } else ""
                            }
                        }
                    }
                    onResult(userNames.filter { it.isNotEmpty() })
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Get offline user statistics for debugging/info purposes
     */
    fun getOfflineUserStats(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val allUsersResult = authRepository.getAllUsersFromCouchDB(_uiState.value.role)
                if (allUsersResult.isSuccess) {
                    val users = allUsersResult.getOrNull() ?: emptyList()
                    val activeUsers = users.count { it.isActive }
                    val totalUsers = users.size
                    val roleText = when (_uiState.value.role) {
                        UserRole.STUDENT -> "Students"
                        UserRole.EDUCATOR -> "Educators"
                    }
                    val stats = "Offline $roleText: $activeUsers active, $totalUsers total"
                    onResult(stats)
                } else {
                    onResult("No offline data available")
                }
            } catch (e: Exception) {
                onResult("Error retrieving offline data")
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
        val isValid = when (_uiState.value.role) {
            UserRole.STUDENT -> _uiState.value.firstName.isNotBlank() &&
                    _uiState.value.lastName.isNotBlank()
            UserRole.EDUCATOR -> _uiState.value.phoneNumber.isNotBlank()
        }

        _uiState.value = _uiState.value.copy(isFormValid = isValid)
    }
    
    private fun checkLocalDataAvailability() {
        val firstName = _uiState.value.firstName
        val lastName = _uiState.value.lastName
        
        if (firstName.isNotBlank() && lastName.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val localUserResult = authRepository.getUserFromCouchDB(firstName, lastName, _uiState.value.role)
                    _uiState.value = _uiState.value.copy(
                        localDataAvailable = localUserResult.isSuccess && localUserResult.getOrNull() != null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(localDataAvailable = false)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(localDataAvailable = false)
        }
    }
}

object Variables {
    const val TAG:String = "Login"
}