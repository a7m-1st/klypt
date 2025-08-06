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

package com.klypt.ui.quizeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuestionEditorCard(
    question: EditableQuestion,
    onQuestionTextChanged: (String) -> Unit,
    onOptionTextChanged: (Int, String) -> Unit,
    onCorrectAnswerChanged: (Int) -> Unit,
    onGenerateOptionsClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with question number and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (canDelete) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete Question",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Question text input
            OutlinedTextField(
                value = question.questionText,
                onValueChange = onQuestionTextChanged,
                label = { Text("Enter your question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("What would you like to ask about this topic?") }
            )
            
            // Generate options button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Answer Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = onGenerateOptionsClicked,
                    enabled = question.questionText.isNotBlank() && !question.isGeneratingOptions,
                    modifier = Modifier.height(36.dp)
                ) {
                    if (question.isGeneratingOptions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Icon(
                            Icons.Outlined.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🧠 Generate Options")
                    }
                }
            }
            
            // Options
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                question.options.forEachIndexed { index, option ->
                    val optionLabel = ('A' + index).toString()
                    val isCorrect = question.correctAnswerIndex == index
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isCorrect,
                                onClick = { onCorrectAnswerChanged(index) },
                                role = Role.RadioButton
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = if (isCorrect) {
                            CardDefaults.outlinedCardBorder().copy(
                                width = 2.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Radio button and option label
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCorrect,
                                    onClick = null // handled by card click
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "$optionLabel.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Option text input
                            OutlinedTextField(
                                value = option,
                                onValueChange = { text -> onOptionTextChanged(index, text) },
                                placeholder = { Text("Enter option $optionLabel") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
            
            // Correct answer indicator
            if (question.options.any { it.isNotBlank() }) {
                Text(
                    text = "✅ Correct answer: ${('A' + question.correctAnswerIndex)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Question") },
            text = { Text("Are you sure you want to delete this question? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClicked()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
