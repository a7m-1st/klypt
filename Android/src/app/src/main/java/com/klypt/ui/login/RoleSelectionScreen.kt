package com.klypt.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.UserRole

@Composable
fun RoleSelectionScreen(
    onNavigateToLogin: () -> Unit,
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

        // Role selection text
        Text(
            text = "Choose your account type",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Radio button group
        Column(
            modifier = Modifier
                .selectableGroup()
                .fillMaxWidth()
        ) {
            // Student option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (uiState.role == UserRole.STUDENT),
                        onClick = { viewModel.updateUserRole(UserRole.STUDENT) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (uiState.role == UserRole.STUDENT),
                    onClick = null // null recommended for accessibility with screenreaders
                )
                Text(
                    text = "Student",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Educator option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (uiState.role == UserRole.EDUCATOR),
                        onClick = { viewModel.updateUserRole(UserRole.EDUCATOR) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (uiState.role == UserRole.EDUCATOR),
                    onClick = null // null recommended for accessibility with screenreaders
                )
                Text(
                    text = "Educator",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Continue button
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", fontSize = 16.sp)
        }
    }
}
