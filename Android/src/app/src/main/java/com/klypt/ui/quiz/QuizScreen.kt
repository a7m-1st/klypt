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

package com.klypt.ui.quiz

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
import com.klypt.data.services.UserContextProvider

private const val TAG = "QuizScreen"

/** Navigation destination data */
object QuizDestination {
    val route = "QuizRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    klyp: Klyp,
    onNavigateBack: () -> Unit,
    onQuizCompleted: (score: Double, totalQuestions: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel(),
    userContextProvider: UserContextProvider
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize the view model with the klyp data
    LaunchedEffect(klyp) {
        Log.d(TAG, "Initializing QuizScreen with klyp: ${klyp.title} (${klyp.questions.size} questions)")
        viewModel.initializeQuiz(klyp, userContextProvider.getCurrentUserId() ?: "unknown_user")
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Quiz: ${klyp.title}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.currentQuestionIndex >= 0 && uiState.questions.isNotEmpty()) {
                            Text(
                                text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.questions.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigate back clicked from QuizScreen")
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
            when {
                uiState.isLoading || uiState.isInitializing -> {
                    QuizInitializationScreen(
                        klypTitle = klyp.title,
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
                
                uiState.isCompleted -> {
                    QuizCompletedContent(
                        score = uiState.score,
                        totalQuestions = uiState.questions.size,
                        correctAnswers = uiState.correctAnswers,
                        onNavigateBack = onNavigateBack,
                        onQuizCompleted = onQuizCompleted
                    )
                }
                
                uiState.questions.isNotEmpty() && uiState.currentQuestionIndex < uiState.questions.size -> {
                    QuizQuestionContent(
                        question = uiState.questions[uiState.currentQuestionIndex],
                        questionIndex = uiState.currentQuestionIndex,
                        totalQuestions = uiState.questions.size,
                        selectedAnswer = uiState.selectedAnswers.getOrNull(uiState.currentQuestionIndex),
                        onAnswerSelected = { answer ->
                            Log.d(TAG, "Answer selected: $answer for question ${uiState.currentQuestionIndex}")
                            viewModel.selectAnswer(uiState.currentQuestionIndex, answer)
                        },
                        onNextQuestion = {
                            Log.d(TAG, "Next question clicked")
                            viewModel.nextQuestion()
                        },
                        onPreviousQuestion = if (uiState.currentQuestionIndex > 0) {
                            {
                                Log.d(TAG, "Previous question clicked")
                                viewModel.previousQuestion()
                            }
                        } else null,
                        onSubmitQuiz = {
                            Log.d(TAG, "Submit quiz clicked")
                            viewModel.submitQuiz()
                        },
                        isLastQuestion = uiState.currentQuestionIndex == uiState.questions.size - 1
                    )
                }
                
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "No questions available for this quiz",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            
                            // Retry button if there's an error
                            uiState.errorMessage?.let {
                                Button(
                                    onClick = {
                                        Log.d(TAG, "Retry button clicked")
                                        viewModel.retryInitialization()
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
    question: Question,
    questionIndex: Int,
    totalQuestions: Int,
    selectedAnswer: Char?,
    onAnswerSelected: (Char) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: (() -> Unit)?,
    onSubmitQuiz: () -> Unit,
    isLastQuestion: Boolean
) {
    val progress by animateFloatAsState(
        targetValue = (questionIndex + 1).toFloat() / totalQuestions.toFloat(),
        label = "quiz_progress"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Progress indicator
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Progress: ${questionIndex + 1}/$totalQuestions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Question content
        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Question text
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Answer options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    question.options.forEachIndexed { index, option ->
                        val optionChar = ('A' + index)
                        val isSelected = selectedAnswer == optionChar
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onAnswerSelected(optionChar) },
                                    role = Role.RadioButton
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null // handled by card click
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = "$optionChar. $option",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Previous button
            if (onPreviousQuestion != null) {
                OutlinedButton(
                    onClick = onPreviousQuestion,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
            }
            
            // Next/Submit button
            Button(
                onClick = if (isLastQuestion) onSubmitQuiz else onNextQuestion,
                enabled = selectedAnswer != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isLastQuestion) "Submit Quiz" else "Next")
            }
        }
    }
}

@Composable
private fun QuizCompletedContent(
    score: Double,
    totalQuestions: Int,
    correctAnswers: Int,
    onNavigateBack: () -> Unit,
    onQuizCompleted: (Double, Int) -> Unit
) {
    // Remove the LaunchedEffect that was causing immediate navigation
    // The onQuizCompleted callback will be called when user clicks "Back to Klyp Details"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success/Result icon
        val (icon, iconColor) = when {
            score >= 0.8 -> Icons.Default.Check to Color(0xFF4CAF50)
            score >= 0.6 -> Icons.Default.Check to Color(0xFFFF9800)
            else -> Icons.Default.Close to Color(0xFFF44336)
        }
        
        Card(
            modifier = Modifier.size(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = iconColor.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = iconColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Results
        Text(
            text = "Quiz Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "$correctAnswers out of $totalQuestions correct",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Performance message
                val performanceMessage = when {
                    score >= 0.9 -> "Excellent work! 🎉"
                    score >= 0.8 -> "Great job! 👏"
                    score >= 0.7 -> "Good effort! 👍"
                    score >= 0.6 -> "Keep practicing! 📚"
                    else -> "Don't give up! Review and try again. 💪"
                }
                
                Text(
                    text = performanceMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    Log.d(TAG, "Quiz completed with score: $score, total questions: $totalQuestions")
                    onQuizCompleted(score, totalQuestions)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Klyp Details")
            }
        }
    }
}
