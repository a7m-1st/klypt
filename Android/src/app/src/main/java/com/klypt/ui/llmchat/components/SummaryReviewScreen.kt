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

package com.klypt.ui.llmchat.components

import android.widget.Button
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.Model
import com.klypt.data.models.ChatSummary
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.llmsingleturn.ClassSelectionDialog
import com.klypt.ui.llmsingleturn.KlypSaveViewModel
import com.klypt.ui.navigation.SummaryNavigationData

/**
 * Screen for reviewing and editing the generated Klypt summary before saving
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryReviewScreen(
    summary: ChatSummary,
    model: Model,
    messages: List<ChatMessage>,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    onNavigateToAddClass: (title: String, content: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: SummaryReviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var editedSummary by remember { mutableStateOf(summary.bulletPointSummary) }
    var editedTitle by remember { mutableStateOf(summary.sessionTitle) }
    
    // State for class selection dialog (always available now)
    var showClassSelectionDialog by remember { mutableStateOf(false) }
    val klypSaveViewModel: KlypSaveViewModel = hiltViewModel()
    val klypSaveUiState by klypSaveViewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Review Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateSummary(
                                summary = summary.copy(
                                    bulletPointSummary = editedSummary,
                                    sessionTitle = editedTitle
                                ),
                                onSuccess = onSaveComplete,
                                onError = { error ->
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to update summary: $error",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Title Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Session Title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter session title") },
                        singleLine = true
                    )
                }
            }

            // Summary Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI-Generated Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Info, contentDescription = "\"Model: ${model.name}\"")
                        }
                    }

                    
                    OutlinedTextField(
                        value = editedSummary,
                        onValueChange = { editedSummary = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp),
                        placeholder = { Text("Edit your summary here...") },
                        supportingText = { 
                            Text("You can edit this AI-generated summary before saving it to your records.")
                        }
                    )
                }
            }

            // Summary Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Summary Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${messages.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Column {
                            Text(
                                text = "${editedSummary.split('\n').filter { it.trim().startsWith("•") || it.trim().startsWith("-") }.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Key Points",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Column {
                            Text(
                                text = "${editedSummary.length}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        // Always show class selection dialog
                        showClassSelectionDialog = true
                        klypSaveViewModel.loadAvailableClasses()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && editedSummary.isNotBlank() && editedTitle.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Summary")
                }
            }
        }
    }
    
    // Class Selection Dialog - always available now
    if (showClassSelectionDialog) {
        ClassSelectionDialog(
            availableClasses = klypSaveUiState.availableClasses,
            isLoadingClasses = klypSaveUiState.isLoadingClasses,
            isLoading = klypSaveUiState.isLoading,
            onClassSelected = { selectedClass ->
                val title = editedTitle.ifBlank { "Chat Summary" }
                val content = editedSummary
                
                klypSaveViewModel.saveKlypToClass(
                    title = title,
                    content = content,
                    classDocument = selectedClass,
                    onSuccess = {
                        showClassSelectionDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "Summary saved successfully!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        onSaveComplete()
                    },
                    onError = { error ->
                        android.widget.Toast.makeText(
                            context,
                            error,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onAddClass = {
                showClassSelectionDialog = false
                val title = editedTitle.ifBlank { "Chat Summary" }
                val content = editedSummary
                // Store the pending summary data for later use
                SummaryNavigationData.storePendingSummaryData(title, content)
                onNavigateToAddClass(title, content)
            },
            onDismiss = {
                showClassSelectionDialog = false
                klypSaveViewModel.clearMessages()
            }
        )
    }
}
