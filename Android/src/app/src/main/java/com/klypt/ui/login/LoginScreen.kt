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
                        // Navigate to OTP screen with phone number
                        onNavigateToHome()
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
    
    // Common country codes (sorted alphabetically by country code)
    val countries = listOf(
        "AD" to "+376",
        "AE" to "+971",
        "AF" to "+93",
        "AG" to "+1-268",
        "AI" to "+1-264",
        "AL" to "+355",
        "AM" to "+374",
        "AO" to "+244",
        "AR" to "+54",
        "AS" to "+1-684",
        "AT" to "+43",
        "AU" to "+61",
        "AW" to "+297",
        "AZ" to "+994",
        "BA" to "+387",
        "BB" to "+1-246",
        "BD" to "+880",
        "BE" to "+32",
        "BF" to "+226",
        "BG" to "+359",
        "BH" to "+973",
        "BI" to "+257",
        "BJ" to "+229",
        "BM" to "+1-441",
        "BN" to "+673",
        "BO" to "+591",
        "BR" to "+55",
        "BS" to "+1-242",
        "BT" to "+975",
        "BW" to "+267",
        "BY" to "+375",
        "BZ" to "+501",
        "CA" to "+1",
        "CD" to "+243",
        "CF" to "+236",
        "CG" to "+242",
        "CH" to "+41",
        "CI" to "+225",
        "CL" to "+56",
        "CM" to "+237",
        "CN" to "+86",
        "CO" to "+57",
        "CR" to "+506",
        "CU" to "+53",
        "CV" to "+238",
        "CY" to "+357",
        "CZ" to "+420",
        "DE" to "+49",
        "DJ" to "+253",
        "DK" to "+45",
        "DM" to "+1-767",
        "DO" to "+1-809",
        "DZ" to "+213",
        "EC" to "+593",
        "EE" to "+372",
        "EG" to "+20",
        "ER" to "+291",
        "ES" to "+34",
        "ET" to "+251",
        "FI" to "+358",
        "FJ" to "+679",
        "FM" to "+691",
        "FR" to "+33",
        "GA" to "+241",
        "GB" to "+44",
        "GE" to "+995",
        "GH" to "+233",
        "GM" to "+220",
        "GN" to "+224",
        "GQ" to "+240",
        "GR" to "+30",
        "GT" to "+502",
        "GW" to "+245",
        "GY" to "+592",
        "HK" to "+852",
        "HN" to "+504",
        "HR" to "+385",
        "HT" to "+509",
        "HU" to "+36",
        "ID" to "+62",
        "IE" to "+353",
        "IL" to "+972",
        "IN" to "+91",
        "IQ" to "+964",
        "IR" to "+98",
        "IS" to "+354",
        "IT" to "+39",
        "JM" to "+1-876",
        "JO" to "+962",
        "JP" to "+81",
        "KE" to "+254",
        "KG" to "+996",
        "KH" to "+855",
        "KI" to "+686",
        "KM" to "+269",
        "KN" to "+1-869",
        "KP" to "+850",
        "KR" to "+82",
        "KW" to "+965",
        "KY" to "+1-345",
        "KZ" to "+7",
        "LA" to "+856",
        "LB" to "+961",
        "LC" to "+1-758",
        "LI" to "+423",
        "LK" to "+94",
        "LR" to "+231",
        "LS" to "+266",
        "LT" to "+370",
        "LU" to "+352",
        "LV" to "+371",
        "LY" to "+218",
        "MA" to "+212",
        "MC" to "+377",
        "MD" to "+373",
        "ME" to "+382",
        "MG" to "+261",
        "MH" to "+692",
        "MK" to "+389",
        "ML" to "+223",
        "MM" to "+95",
        "MN" to "+976",
        "MR" to "+222",
        "MT" to "+356",
        "MU" to "+230",
        "MV" to "+960",
        "MW" to "+265",
        "MX" to "+52",
        "MY" to "+60",
        "MZ" to "+258",
        "NA" to "+264",
        "NE" to "+227",
        "NG" to "+234",
        "NI" to "+505",
        "NL" to "+31",
        "NO" to "+47",
        "NP" to "+977",
        "NR" to "+674",
        "NZ" to "+64",
        "OM" to "+968",
        "PA" to "+507",
        "PE" to "+51",
        "PG" to "+675",
        "PH" to "+63",
        "PK" to "+92",
        "PL" to "+48",
        "PT" to "+351",
        "PW" to "+680",
        "PY" to "+595",
        "QA" to "+974",
        "RO" to "+40",
        "RS" to "+381",
        "RU" to "+7",
        "RW" to "+250",
        "SA" to "+966",
        "SB" to "+677",
        "SC" to "+248",
        "SD" to "+249",
        "SE" to "+46",
        "SG" to "+65",
        "SI" to "+386",
        "SK" to "+421",
        "SL" to "+232",
        "SM" to "+378",
        "SN" to "+221",
        "SO" to "+252",
        "SR" to "+597",
        "ST" to "+239",
        "SV" to "+503",
        "SY" to "+963",
        "SZ" to "+268",
        "TD" to "+235",
        "TG" to "+228",
        "TH" to "+66",
        "TJ" to "+992",
        "TM" to "+993",
        "TN" to "+216",
        "TO" to "+676",
        "TR" to "+90",
        "TT" to "+1-868",
        "TV" to "+688",
        "TW" to "+886",
        "TZ" to "+255",
        "UA" to "+380",
        "UG" to "+256",
        "US" to "+1",
        "UY" to "+598",
        "UZ" to "+998",
        "VA" to "+379",
        "VC" to "+1-784",
        "VE" to "+58",
        "VN" to "+84",
        "VU" to "+678",
        "WS" to "+685",
        "YE" to "+967",
        "ZA" to "+27",
        "ZM" to "+260",
        "ZW" to "+263"
        "CV" to "+238",
        "KY" to "+1-345",
        "CF" to "+236",
        "TD" to "+235",
        "CL" to "+56",
        "CN" to "+86",
        "CO" to "+57",
        "KM" to "+269",
        "CD" to "+243",
        "CG" to "+242",
        "CR" to "+506",
        "CI" to "+225",
        "HR" to "+385",
        "CU" to "+53",
        "CY" to "+357",
        "CZ" to "+420",
        "DK" to "+45",
        "DJ" to "+253",
        "DM" to "+1-767",
        "DO" to "+1-809",
        "EC" to "+593",
        "EG" to "+20",
        "SV" to "+503",
        "GQ" to "+240",
        "ER" to "+291",
        "EE" to "+372",
        "ET" to "+251",
        "FJ" to "+679",
        "FI" to "+358",
        "FR" to "+33",
        "GA" to "+241",
        "GM" to "+220",
        "GE" to "+995",
        "DE" to "+49",
        "GH" to "+233",
        "GR" to "+30",
        "GD" to "+1-473",
        "GT" to "+502",
        "GN" to "+224",
        "GW" to "+245",
        "GY" to "+592",
        "HT" to "+509",
        "HN" to "+504",
        "HU" to "+36",
        "IS" to "+354",
        "IN" to "+91",
        "ID" to "+62",
        "IR" to "+98",
        "IQ" to "+964",
        "IE" to "+353",
        "IL" to "+972",
        "IT" to "+39",
        "JM" to "+1-876",
        "JP" to "+81",
        "JO" to "+962",
        "KZ" to "+7",
        "KE" to "+254",
        "KI" to "+686",
        "KP" to "+850",
        "KR" to "+82",
        "KW" to "+965",
        "KG" to "+996",
        "LA" to "+856",
        "LV" to "+371",
        "LB" to "+961",
        "LS" to "+266",
        "LR" to "+231",
        "LY" to "+218",
        "LI" to "+423",
        "LT" to "+370",
        "LU" to "+352",
        "MG" to "+261",
        "MW" to "+265",
        "MY" to "+60",
        "MV" to "+960",
        "ML" to "+223",
        "MT" to "+356",
        "MH" to "+692",
        "MR" to "+222",
        "MU" to "+230",
        "MX" to "+52",
        "FM" to "+691",
        "MD" to "+373",
        "MC" to "+377",
        "MN" to "+976",
        "ME" to "+382",
        "MA" to "+212",
        "MZ" to "+258",
        "MM" to "+95",
        "NA" to "+264",
        "NR" to "+674",
        "NP" to "+977",
        "NL" to "+31",
        "NZ" to "+64",
        "NI" to "+505",
        "NE" to "+227",
        "NG" to "+234",
        "NO" to "+47",
        "OM" to "+968",
        "PK" to "+92",
        "PW" to "+680",
        "PA" to "+507",
        "PG" to "+675",
        "PY" to "+595",
        "PE" to "+51",
        "PH" to "+63",
        "PL" to "+48",
        "PT" to "+351",
        "QA" to "+974",
        "RO" to "+40",
        "RU" to "+7",
        "RW" to "+250",
        "KN" to "+1-869",
        "LC" to "+1-758",
        "VC" to "+1-784",
        "WS" to "+685",
        "SM" to "+378",
        "ST" to "+239",
        "SA" to "+966",
        "SN" to "+221",
        "RS" to "+381",
        "SC" to "+248",
        "SL" to "+232",
        "SG" to "+65",
        "SK" to "+421",
        "SI" to "+386",
        "SB" to "+677",
        "SO" to "+252",
        "ZA" to "+27",
        "ES" to "+34",
        "LK" to "+94",
        "SD" to "+249",
        "SR" to "+597",
        "SZ" to "+268",
        "SE" to "+46",
        "CH" to "+41",
        "SY" to "+963",
        "TW" to "+886",
        "TJ" to "+992",
        "TZ" to "+255",
        "TH" to "+66",
        "TG" to "+228",
        "TO" to "+676",
        "TT" to "+1-868",
        "TN" to "+216",
        "TR" to "+90",
        "TM" to "+993",
        "TV" to "+688",
        "UG" to "+256",
        "UA" to "+380",
        "AE" to "+971",
        "GB" to "+44",
        "US" to "+1",
        "UY" to "+598",
        "UZ" to "+998",
        "VU" to "+678",
        "VA" to "+379",
        "VE" to "+58",
        "VN" to "+84",
        "YE" to "+967",
        "ZM" to "+260",
        "ZW" to "+263"
    )


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
                countries.forEach { (country, code) ->
                    DropdownMenuItem(
                        text = { Text("$country $code") },
                        onClick = {
                            onCountryCodeChange(code, country)
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