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

package com.klypt.ui.klypdetails

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.TASK_LLM_CHAT
import com.klypt.data.models.Klyp
import com.klypt.data.services.UserContextProvider

private const val TAG = "KlypDetailsScreen"

/** Navigation destination data */
object KlypDetailsDestination {
    val route = "KlypDetailsRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlypDetailsScreen(
    klyp: Klyp,
    onNavigateBack: () -> Unit,
    onNavigateToLLMChat: (String, String, String) -> Unit, // classCode, title, content
    onNavigateToQuiz: (Klyp) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KlypDetailsViewModel = hiltViewModel(),
    userContextProvider: UserContextProvider
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize the view model with the klyp data
    LaunchedEffect(klyp) {
        Log.d(TAG, "Initializing KlypDetailsScreen with klyp: ${klyp.title} (ID: ${klyp._id})")
        viewModel.initializeWithKlyp(klyp)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = klyp.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigate back clicked from KlypDetailsScreen")
                        onNavigateBack()
                    }) {
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
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Main content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Klyp Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Class Code: ${klyp.classCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (klyp.questions.isNotEmpty()) {
                                Text(
                                    text = "Questions: ${klyp.questions.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "Created: ${klyp.createdAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Content Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Content",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Content is read-only
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = klyp.mainBody,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }

                    // Questions Preview Card (if questions exist)
                    if (klyp.questions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Questions Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "This Klyp contains ${klyp.questions.size} question(s). You can play the quiz to test your knowledge or transfer the content to chat for further discussion.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action buttons at the bottom
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Transfer to Chat button
                            Button(
                                onClick = {
                                    Log.d(TAG, "Transfer to Chat clicked for klyp: ${klyp.title}")
                                    Log.d(TAG, "Class code: ${klyp.classCode}, Title: ${klyp.title}")
                                    Log.d(TAG, "Content length: ${klyp.mainBody.length}")
                                    onNavigateToLLMChat(klyp.classCode, klyp.title, klyp.mainBody)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = "Transfer to Chat",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Discuss in Chat")
                            }
                            
                            // Quiz button - Generate if no questions, Play if questions exist
                            if (klyp.questions.isEmpty()) {
                                // Generate Quiz button when no questions exist
                                Button(
                                    onClick = {
                                        Log.d(TAG, "Generate Quiz clicked for klyp: ${klyp.title}")
                                        if (uiState.isGeneratingQuiz) {
                                            Log.w(TAG, "Quiz generation already in progress, ignoring click")
                                            return@Button
                                        }
                                        
                                        // Start quiz generation and navigation
                                        viewModel.generateQuizQuestions(
                                            klyp = klyp,
                                            context = context,
                                            onSuccess = { updatedKlyp ->
                                                Log.d(TAG, "Quiz generation successful, navigating to quiz")
                                                onNavigateToQuiz(updatedKlyp)
                                            },
                                            onError = { error ->
                                                Log.e(TAG, "Quiz generation failed: $error")
                                                // Error handling is done in ViewModel
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isGeneratingQuiz,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (uiState.isGeneratingQuiz) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Generate Quiz",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (uiState.isGeneratingQuiz) "Generating..." else "Generate Quiz")
                                }
                            } else {
                                // Play Quiz button when questions already exist
                                Button(
                                    onClick = {
                                        Log.d(TAG, "Play Quiz clicked for klyp: ${klyp.title}")
                                        Log.d(TAG, "Number of questions: ${klyp.questions.size}")
                                        // Directly navigate to quiz since questions already exist
                                        onNavigateToQuiz(klyp)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play Quiz",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play Quiz")
                                }
                            }
                        }
                        
                        // Status messages
                        uiState.errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        if (uiState.isGeneratingQuiz) {
                            Text(
                                text = "Generating quiz questions using AI...",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
