package com.klypt.ui.otp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.ui.verification.PhoneVerificationViewModel
import kotlinx.coroutines.delay

// Utility function to check network connectivity
@Composable
fun isNetworkAvailable(): Boolean {
    val context = LocalContext.current
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

@Composable
fun OtpEntryScreen(
    phoneNumber: String,
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PhoneVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isConnected = isNetworkAvailable()
    var refreshTrigger by remember { mutableStateOf(0) }
    var otpCode by remember { mutableStateOf("") }
    val otpLength = 6

    // Auto-verify when OTP is complete
    LaunchedEffect(otpCode) {
        if (otpCode.length == otpLength && isConnected) {
            viewModel.verifyCode(otpCode)
        }
    }

    // Navigate to home when verification is successful
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            delay(1000) // Show success state briefly
            onNavigateToHome()
        }
    }

    // Send OTP when screen loads
    LaunchedEffect(phoneNumber) {
        if (phoneNumber.isNotEmpty() && isConnected) {
            viewModel.sendVerificationCode(phoneNumber)
        }
    }

    // Refresh network state
    LaunchedEffect(refreshTrigger) {
        if (isConnected && phoneNumber.isNotEmpty()) {
            viewModel.sendVerificationCode(phoneNumber)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Title
            Text(
                text = buildAnnotatedString {
                    append("Klypt")
                    withStyle(style = SpanStyle(color = Color(0xFF2FA96E))) {
                        append(".")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isConnected) {
                Text(
                    text = if (uiState.isVerified) "Verification Successful!"
                        else if (uiState.verificationSent) "Enter Verification Code"
                        else "Sending Code...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.isVerified) Color(0xFF2FA96E) else Color(0xFF343A40),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (uiState.isVerified) "Your phone number has been verified successfully"
                        else if (uiState.verificationSent) "We've sent a 6-digit code to $phoneNumber"
                        else "Please wait while we send the verification code",
                    fontSize = 14.sp,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (uiState.isVerified) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp),
                        tint = Color(0xFF2FA96E)
                    )
                } else {
                    BasicTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= otpLength) otpCode = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && uiState.verificationSent,
                        decorationBox = { innerTextField ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(otpLength) { index ->
                                    val char = otpCode.getOrNull(index)?.toString() ?: ""
                                    val isFocused = index == otpCode.length && !uiState.isLoading
                                    val hasError = uiState.error != null && otpCode.length == otpLength
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    hasError -> Color(0xFFFFEBEE)
                                                    char.isNotEmpty() -> Color(0xFFE3F2FD)
                                                    else -> Color(0xFFF5F5F5)
                                                }
                                            )
                                            .border(
                                                width = if (isFocused) 2.dp else 1.dp,
                                                color = when {
                                                    hasError -> Color(0xFFE57373)
                                                    char.isNotEmpty() -> Color(0xFF2196F3)
                                                    else -> Color(0xFFE0E0E0)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoading && char.isNotEmpty()) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF2FA96E)
                                            )
                                        } else {
                                            Text(
                                                text = char,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    hasError -> Color(0xFFE57373)
                                                    char.isNotEmpty() -> Color(0xFF1976D2)
                                                    else -> Color(0xFF9E9E9E)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Box(modifier = Modifier.size(0.dp)) { innerTextField() }
                        }
                    )

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = Color(0xFFE57373),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Back button: filled, blue background, white text
                        Button(
                            onClick = {
                                // Navigate back to Login page (phone entry)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = Color(0xFFE0E0E0),
                                disabledContentColor = Color(0xFF9E9E9E)
                            )
                        ) {
                            Text(
                                text = "Back",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Verify button: filled, green background, white text
                        Button(
                            onClick = {
                                if (otpCode.length == otpLength) viewModel.verifyCode(otpCode)
                            },
                            enabled = !uiState.isLoading && otpCode.length == otpLength && uiState.verificationSent,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2FA96E),
                                contentColor = Color(0xFF343A40),
                                disabledContainerColor = Color(0xFFE0E0E0),
                                disabledContentColor = Color(0xFF9E9E9E)
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                color = Color(0xFF343A40)
                                )
                            } else {
                                Text(
                                    text = "Verify",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "No Internet",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = Color(0xFFFF5722)
                )
                Text(
                    text = "No Internet Connection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343A40),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "CONNECT TO INTERNET TO RECEIVE OTP",
                    fontSize = 14.sp,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp),
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = {
                        refreshTrigger++
                        viewModel.clearError()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2FA96E), Color(0xFF4CAF50))
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = Color(0xFF2FA96E)
                    )
                    Text(
                        text = "Try Again",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2FA96E)
                    )
                }
            }
        }
    }
}
