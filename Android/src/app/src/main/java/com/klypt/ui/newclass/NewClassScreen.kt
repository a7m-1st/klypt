/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klypt.ui.newclass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/** Navigation destination data */
object NewClassDestination {
    val route = "NewClassRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClassScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLLMChat: (String, String) -> Unit, // classCode, className
    modifier: Modifier = Modifier,
    viewModel: NewClassViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create New Class") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top section with logo
            Text(
                text = buildAnnotatedString {
                    append("Klypt")
                    withStyle(style = SpanStyle(color = Color(0xFF2FA96E))) {
                        append(".")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Class icon
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "New Class",
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Title and description
            Text(
                text = "Create a New Class",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Give your class a name and start creating engaging learning content",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Class name input
            OutlinedTextField(
                value = uiState.className,
                onValueChange = viewModel::updateClassName,
                label = { Text("Class Name") },
                placeholder = { Text("e.g., Mathematics 101, Biology Basics") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = uiState.classNameError != null,
                supportingText = uiState.classNameError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Helper text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "What happens next?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "You'll create your first Klyp (learning content) and get a unique class code to share with students.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Button(
                onClick = {
                    val (classCode, className) = viewModel.proceedToLLMChat()
                    // URL encode the class name to handle special characters
                    val encodedClassName = java.net.URLEncoder.encode(className, "UTF-8")
                    onNavigateToLLMChat(classCode, encodedClassName)
                },
                enabled = true, // Always enabled as class name is optional
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue to Create Content", fontSize = 16.sp)
            }
            
            // Skip class name option
            TextButton(
                onClick = {
                    val (classCode, className) = viewModel.proceedToLLMChat()
                    val encodedClassName = java.net.URLEncoder.encode(className, "UTF-8")
                    onNavigateToLLMChat(classCode, encodedClassName)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Skip - I'll name it later")
            }
        }
    }
}
