package com.klypt.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.repository.TwilioRepository
import com.klypt.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val twilioRepository: TwilioRepository,
    private val tokenManager: TokenManager
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to send verification code"
                    )
                }
        }
    }

    fun verifyCode(code: String) {
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

//            twilioRepository.verifyCode(phoneNumber, code)
//                .onSuccess { response ->
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        isVerified = response.valid,
//                        error = if (!response.valid) "Invalid verification code" else null
//                    )
//                }
//                .onFailure { exception ->
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        error = exception.message ?: "Failed to verify code"
//                    )
//                }

            //TODO() For now just pass verificataion
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isVerified = true
            )
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
