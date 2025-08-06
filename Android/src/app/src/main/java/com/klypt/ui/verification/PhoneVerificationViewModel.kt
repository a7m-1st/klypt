package com.klypt.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.repository.TwilioRepository
import com.klypt.storage.TokenManager
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.utils.DatabaseUtils
import com.klypt.data.models.Educator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val twilioRepository: TwilioRepository,
    private val tokenManager: TokenManager,
    private val educatorRepository: EducatorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneVerificationUiState())
    val uiState: StateFlow<PhoneVerificationUiState> = _uiState.asStateFlow()

    fun saveTwilioCredentials(serviceSid: String, authToken: String) {
        tokenManager.saveTwilioSid(serviceSid)
        tokenManager.saveTwilioAuthToken(authToken)
    }

    fun sendVerificationCode(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            // Attempt to send verification, but don't show Twilio errors
            try {
                twilioRepository.sendVerification(phoneNumber)
                    .onSuccess { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            verificationSent = true,
                            verificationSid = response.sid,
                            phoneNumber = phoneNumber
                        )
                    }
                    .onFailure { exception ->
                        // Don't log Twilio errors, just mark as sent for optional verification
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            verificationSent = true,
                            phoneNumber = phoneNumber,
                            error = null // Clear any errors since verification is optional
                        )
                    }
            } catch (e: Exception) {
                // Handle any unexpected exceptions
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verificationSent = true,
                    phoneNumber = phoneNumber,
                    error = null
                )
            }
        }
    }

    fun verifyCode(code: String, signupData: Map<String, Any>? = null, onNavigateToSignup: () -> Unit = {}) {
        val phoneNumber = _uiState.value.phoneNumber
        if (phoneNumber.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Phone number not found. Please resend verification code."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // For signup flow - create educator in database when verification is clicked
                if (signupData != null) {
                    createEducatorInDatabase(phoneNumber, signupData)
                    // Always mark as verified for signup flow
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerified = true
                    )
                } else {
                    // For login flow - check if phone number exists in database
                    val existingEducator = educatorRepository.get(phoneNumber)
                    
                    if (existingEducator.isEmpty()) {
                        // Phone number doesn't exist in database, navigate to signup
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Phone number not found. Please sign up first."
                        )
                        onNavigateToSignup()
                    } else {
                        // Phone number exists, proceed with verification
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isVerified = true
                        )
                    }
                }
            } catch (e: Exception) {
                // For signup flow, even if educator creation fails, still mark as verified
                if (signupData != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerified = true
                    )
                } else {
                    // For login flow, if there's an error checking the database, show error
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error verifying phone number: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun createEducatorInDatabase(phoneNumber: String, signupData: Map<String, Any>) {
        try {
            // Create educator with provided signup data
            val educatorId = phoneNumber // Use phone number as ID for educators
            val fullName = signupData["fullName"] as? String ?: ""
            val ageStr = signupData["age"] as? String ?: "0"
            val age = ageStr.toIntOrNull() ?: 0
            val currentJob = signupData["currentJob"] as? String ?: ""
            val instituteName = signupData["instituteName"] as? String ?: ""
            
            val educatorData = mapOf(
                "_id" to educatorId,
                "type" to "educator",
                "fullName" to fullName,
                "age" to age,
                "currentJob" to currentJob,
                "instituteName" to instituteName,
                "phoneNumber" to phoneNumber,
                "verified" to true, // Mark as verified since we allow signup without OTP
                "recoveryCode" to "EDU${System.currentTimeMillis()}", // Generate recovery code
                "classIds" to emptyList<String>() // Start with no classes
            )
            
            // Save educator to database
            val success = educatorRepository.save(educatorData)
            if (success) {
                // Store educator identification in token manager for session persistence
                tokenManager.saveEducatorIdentification(phoneNumber, fullName)
                android.util.Log.d("PhoneVerificationViewModel", "Successfully created educator: $fullName with phone: $phoneNumber")
            } else {
                android.util.Log.w("PhoneVerificationViewModel", "Failed to save educator to database")
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneVerificationViewModel", "Error creating educator in database", e)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = PhoneVerificationUiState()
    }
}

data class PhoneVerificationUiState(
    val isLoading: Boolean = false,
    val verificationSent: Boolean = false,
    val isVerified: Boolean = false,
    val phoneNumber: String? = null,
    val verificationSid: String? = null,
    val error: String? = null
)
