package com.klypt.ui.signup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.ui.common.CountryCodeData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNext: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // Optional for spacing
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
            
            // Phone number with country code selector
            var expanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Country code dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .width(120.dp)
                        .padding(end = 8.dp)
                ) {
                    OutlinedTextField(
                        value = "${uiState.selectedCountry} ${uiState.countryCode}",
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(),
                        label = { Text("Code") }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CountryCodeData.countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.code} ${country.dialCode}") },
                                onClick = {
                                    viewModel.updateCountryCode(country.dialCode, country.code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Phone number field
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = { viewModel.updatePhoneNumber(it) },
                    label = { Text("Phone Number*") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            // Next button right after the fields
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
}
