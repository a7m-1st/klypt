package com.klypt.ui.otp

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import com.klypt.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val tokenManager: TokenManager,
): ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState : StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun changeOtp(otp: String) {
        if (otp.length <= _uiState.value.otpLength && otp.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(otpValue = otp)
        }
    }
}