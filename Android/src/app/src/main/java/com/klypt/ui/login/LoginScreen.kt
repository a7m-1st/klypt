package com.klypt.ui.login

import android.graphics.drawable.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.klypt.ui.common.CountryCodeData
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.klypt.data.UserRole
import com.klypt.data.mainGreenColor

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //Top
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

        //Main body
        when (uiState.role) {
            UserRole.STUDENT -> {
                StudentLoginContent(
                    uiState = uiState,
                    onFirstNameChange = viewModel::updateFirstName,
                    onLastNameChange = viewModel::updateLastName,
                    onStudentLogin = { viewModel.login(onNavigateToHome) },
                    recoverAccount = {}
                )
            }
            UserRole.EDUCATOR -> {
                EducatorLoginContent(
                    uiState = uiState,
                    onPhoneNumberChange = viewModel::updatePhoneNumber,
                    onCountryCodeChange = viewModel::updateCountryCode,
                    onEducatorLogin = {
                        // First validate the educator exists in database, then navigate to OTP
                        viewModel.login(onNavigateToHome)
                    },
                    clickSignUp = onNavigateToSignup,
                    recoverAccount = {}
                )
            }
        }
    }

}

@Composable
private fun StudentLoginContent(
    uiState: LoginUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onStudentLogin: () -> Unit,
    recoverAccount: () -> Unit = {}
) {
    //firstName
    OutlinedTextField(
        value = uiState.firstName,
        onValueChange = onFirstNameChange,
        label = { Text("First Name") },
        leadingIcon = {
//                Icon(Icons.Default.TextFields)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text
        ),
        isError = uiState.firstNameError != null,
        supportingText = uiState.firstNameError?.let {it:String -> { Text(it) }},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    OutlinedTextField(
        value = uiState.lastName,
        onValueChange = onLastNameChange,
        label = { Text("Last Name") },
        leadingIcon = {
//                Icon(Icons.Default.TextFields)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text
        ),
        isError = uiState.lastNameError != null,
        supportingText = uiState.lastNameError?.let {{ Text(it) }},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    TextButton(
        onClick = { /* handle recover*/},
    ) {
        Text("Recover an account? Recover account",
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    //Login Button
    Button (
        onClick = onStudentLogin,
        enabled = !uiState.isLoading && uiState.isFormValid,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if(uiState.isLoading) {
            CircularProgressIndicator(color = Color(mainGreenColor))
        } else {
            Text("Log in as a Student", fontSize = 16.sp)
        }
    }

    uiState.errorMessage?.let{ error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducatorLoginContent(
    uiState: LoginUiState,
    onPhoneNumberChange: (String) -> Unit,
    onCountryCodeChange: (String, String) -> Unit,
    onEducatorLogin: () -> Unit,
    clickSignUp: () -> Unit,
    recoverAccount: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // Phone number with country code selector
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
                            onCountryCodeChange(country.dialCode, country.code)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Phone number field
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            isError = uiState.phoneNumberError != null,
            supportingText = { uiState.phoneNumberError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
    TextButton(
        onClick = { /* handle recover*/},
    ) {
        Text("Recover an account? Recover account",
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    //Login Button
    Button (
        onClick = onEducatorLogin,
        enabled = !uiState.isLoading && uiState.isFormValid,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if(uiState.isLoading) {
            CircularProgressIndicator(color = Color(mainGreenColor))
        } else {
            Text("Log in as a Educator", fontSize = 16.sp)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    //Signup Button
    Button (
        onClick = clickSignUp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if(uiState.isLoading) {
            CircularProgressIndicator(color = Color(mainGreenColor))
        } else {
            Text("Sign up as Educator", fontSize = 16.sp)
        }
    }
}