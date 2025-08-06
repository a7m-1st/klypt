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

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.Model
import com.klypt.data.models.Klyp

private const val TAG = "QuizEditorScreen"

/** Navigation destination data */
object QuizEditorDestination {
    val route = "QuizEditorRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizEditorScreen(
    klyp: Klyp,
    model: Model,
    onNavigateBack: () -> Unit,
    onSaveCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize the view model with klyp and model
    LaunchedEffect(klyp, model) {
        Log.d(TAG, "Initializing QuizEditorScreen with klyp: ${klyp.title} and model: ${model.name}")
        viewModel.initializeWithKlyp(klyp, model)
        viewModel.initializeAIWithContext(context, model)
    }
    
    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Log.d(TAG, "Questions saved successfully, navigating back")
            onSaveCompleted()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Edit Quiz: ${klyp.title}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${uiState.questions.size} questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigate back clicked from QuizEditorScreen")
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save button
                    IconButton(
                        onClick = {
                            Log.d(TAG, "Save questions clicked")
                            viewModel.saveQuestions()
                        },
                        enabled = !uiState.isSaving && uiState.questions.isNotEmpty()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save Questions")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Add question button
            if (!uiState.isInitializing && uiState.isInitialized) {
                FloatingActionButton(
                    onClick = {
                        Log.d(TAG, "Add question clicked")
                        viewModel.addQuestion()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Question")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isInitializing -> {
                    QuizEditorInitializationScreen(
                        modelName = model.name,
                        showStopButton = uiState.showStopButton,
                        elapsedTimeMs = uiState.loadingElapsedTime,
                        onStopClicked = if (uiState.showStopButton) {
                            {
                                Log.d(TAG, "Stop initialization clicked")
                                viewModel.stopInitialization()
                            }
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.initializationError != null -> {
                    QuizEditorErrorScreen(
                        error = uiState.initializationError!!,
                        onRetryClicked = {
                            Log.d(TAG, "Retry initialization clicked")
                            viewModel.retryInitialization()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.isInitialized -> {
                    QuizEditorMainContent(
                        questions = uiState.questions,
                        onQuestionTextChanged = { questionId, text ->
                            viewModel.updateQuestionText(questionId, text)
                        },
                        onOptionTextChanged = { questionId, optionIndex, text ->
                            viewModel.updateQuestionOption(questionId, optionIndex, text)
                        },
                        onCorrectAnswerChanged = { questionId, correctIndex ->
                            viewModel.updateCorrectAnswer(questionId, correctIndex)
                        },
                        onGenerateOptionsClicked = { questionId ->
                            Log.d(TAG, "Generate options clicked for question: $questionId")
                            viewModel.generateOptionsForQuestion(questionId)
                        },
                        onDeleteQuestionClicked = { questionId ->
                            Log.d(TAG, "Delete question clicked for question: $questionId")
                            viewModel.deleteQuestion(questionId)
                        }
                    )
                }
                
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Error/Success messages at the bottom
            AnimatedVisibility(visible = uiState.saveError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.saveError ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            AnimatedVisibility(visible = uiState.saveSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "Questions saved successfully!",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Clear save status after showing success message
    LaunchedEffect(uiState.saveError, uiState.saveSuccess) {
        if (uiState.saveError != null || uiState.saveSuccess) {
            kotlinx.coroutines.delay(3000) // Show for 3 seconds
            viewModel.clearSaveStatus()
        }
    }
}

@Composable
private fun QuizEditorInitializationScreen(
    modelName: String,
    showStopButton: Boolean,
    elapsedTimeMs: Long,
    onStopClicked: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Initializing AI Model",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Setting up $modelName for question generation...",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (elapsedTimeMs > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Elapsed: ${elapsedTimeMs / 1000}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (showStopButton && onStopClicked != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = onStopClicked
            ) {
                Text("Stop Initialization")
            }
        }
    }
}

@Composable
private fun QuizEditorErrorScreen(
    error: String,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Initialization Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetryClicked
        ) {
            Text("Retry Initialization")
        }
    }
}

@Composable
private fun QuizEditorMainContent(
    questions: List<EditableQuestion>,
    onQuestionTextChanged: (String, String) -> Unit,
    onOptionTextChanged: (String, Int, String) -> Unit,
    onCorrectAnswerChanged: (String, Int) -> Unit,
    onGenerateOptionsClicked: (String) -> Unit,
    onDeleteQuestionClicked: (String) -> Unit
) {
    if (questions.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No Questions Yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Tap the + button to add your first question",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(questions, key = { it.id }) { question ->
                QuestionEditorCard(
                    question = question,
                    onQuestionTextChanged = { text ->
                        onQuestionTextChanged(question.id, text)
                    },
                    onOptionTextChanged = { optionIndex, text ->
                        onOptionTextChanged(question.id, optionIndex, text)
                    },
                    onCorrectAnswerChanged = { correctIndex ->
                        onCorrectAnswerChanged(question.id, correctIndex)
                    },
                    onGenerateOptionsClicked = {
                        onGenerateOptionsClicked(question.id)
                    },
                    onDeleteClicked = {
                        onDeleteQuestionClicked(question.id)
                    },
                    canDelete = questions.size > 1
                )
            }
        }
    }
}
