package com.klypt.ui.signup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SignupScreen(
    onNext: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top - Same title as login page
        Text(
            text = buildAnnotatedString {
                append("Klypt")
                withStyle(style = SpanStyle(color = Color(0xFF2FA96E))) {
                    append(".")
                }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Signup form fields
        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = { viewModel.updateFullName(it) },
            label = { Text("Full Name*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = uiState.age,
            onValueChange = { viewModel.updateAge(it) },
            label = { Text("Age*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = uiState.currentJob,
            onValueChange = { viewModel.updateCurrentJob(it) },
            label = { Text("Current Job*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = uiState.instituteName,
            onValueChange = { viewModel.updateInstituteName(it) },
            label = { Text("Institute Name*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = { viewModel.updatePhoneNumber(it) },
            label = { Text("Phone Number*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Next button at the very bottom
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Next", fontSize = 16.sp)
        }
    }
}
