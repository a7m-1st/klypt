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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
import com.klypt.data.models.QuizAttempt
import com.klypt.data.models.StudentAnswer
import com.klypt.data.repositories.QuizAttemptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val TAG = "QuizViewModel"

data class QuizUiState(
    val isLoading: Boolean = true,
    val isInitializing: Boolean = false,
    val isCompleted: Boolean = false,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: List<Char?> = emptyList(),
    val score: Double = 0.0,
    val correctAnswers: Int = 0,
    val quizAttempt: QuizAttempt? = null,
    val errorMessage: String? = null,
    val showStopButton: Boolean = false,
    val loadingElapsedTime: Long = 0L
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizAttemptRepository: QuizAttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private lateinit var currentKlyp: Klyp
    private lateinit var studentId: String
    private var startTime: String = ""

    fun initializeQuiz(klyp: Klyp, userId: String) {
        Log.d(TAG, "Initializing quiz for klyp: ${klyp.title} with user: $userId")
        
        // Validate input parameters first
        if (userId.isBlank()) {
            Log.e(TAG, "Invalid user ID provided")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isInitializing = false,
                showStopButton = false,
                errorMessage = "Invalid user session. Please log in again."
            )
            return
        }
        
        // Set initializing state
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isInitializing = true,
            errorMessage = null,
            showStopButton = false,
            loadingElapsedTime = 0L
        )
        
        currentKlyp = klyp
        studentId = userId
        startTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        viewModelScope.launch {
            try {
                val startLoadingTime = System.currentTimeMillis()
                
                // Start a timer to track elapsed time and show stop button
                val timerJob = launch {
                    while (_uiState.value.isInitializing) {
                        val elapsed = System.currentTimeMillis() - startLoadingTime
                        _uiState.value = _uiState.value.copy(
                            loadingElapsedTime = elapsed,
                            showStopButton = elapsed > 3000L // Show stop button after 3 seconds
                        )
                        kotlinx.coroutines.delay(1000L) // Update every second
                    }
                }
                
                // The actual initialization process
                val initializationJob = launch {
                    // Validate klyp data
                    if (klyp._id.isBlank()) {
                        Log.e(TAG, "Invalid klyp ID")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isInitializing = false,
                            showStopButton = false,
                            errorMessage = "Invalid quiz data. Please try again."
                        )
                        return@launch
                    }
                    
                    // Check if questions are available
                    if (klyp.questions.isEmpty()) {
                        Log.e(TAG, "No questions available for klyp: ${klyp.title}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isInitializing = false,
                            showStopButton = false,
                            errorMessage = "This quiz doesn't have any questions yet. Please contact your instructor or try again later."
                        )
                        return@launch
                    }
                    
                    // Validate question data integrity
                    val invalidQuestions = klyp.questions.filter { question ->
                        question.questionText.isBlank() || 
                        question.options.isEmpty() || 
                        question.options.size < 2 ||
                        question.correctAnswer !in 'A'..'Z' ||
                        question.correctAnswer >= ('A' + question.options.size)
                    }
                    
                    if (invalidQuestions.isNotEmpty()) {
                        Log.e(TAG, "Found ${invalidQuestions.size} invalid questions in klyp: ${klyp.title}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isInitializing = false,
                            showStopButton = false,
                            errorMessage = "Some quiz questions are incomplete. Please contact your instructor."
                        )
                        return@launch
                    }

                    // Brief delay for better UX (shorter than before)
                    kotlinx.coroutines.delay(300)
                    
                    Log.d(TAG, "Quiz initialized successfully with ${klyp.questions.size} valid questions")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isInitializing = false,
                        showStopButton = false,
                        questions = klyp.questions,
                        selectedAnswers = List(klyp.questions.size) { null },
                        currentQuestionIndex = 0,
                        errorMessage = null
                    )
                    
                    // Create initial quiz attempt
                    createQuizAttempt()
                }
                
                initializationJob.join()
                timerJob.cancel()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize quiz", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitializing = false,
                    showStopButton = false,
                    errorMessage = "Failed to initialize quiz: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun createQuizAttempt() {
        viewModelScope.launch {
            try {
                val attemptId = "attempt_${UUID.randomUUID()}"
                Log.d(TAG, "Creating quiz attempt with ID: $attemptId")
                
                val quizAttempt = QuizAttempt(
                    _id = attemptId,
                    studentId = studentId,
                    klypId = currentKlyp._id,
                    classCode = currentKlyp.classCode,
                    answers = emptyList(),
                    percentageComplete = 0.0,
                    score = null,
                    startedAt = startTime,
                    completedAt = null,
                    isSubmitted = false
                )

                _uiState.value = _uiState.value.copy(quizAttempt = quizAttempt)
                Log.d(TAG, "Quiz attempt created successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create quiz attempt", e)
                // Don't fail the entire quiz initialization if quiz attempt creation fails
                // User can still take the quiz, it just won't be tracked
                Log.w(TAG, "Continuing with quiz initialization despite quiz attempt creation failure")
            }
        }
    }

    fun selectAnswer(questionIndex: Int, answer: Char) {
        Log.d(TAG, "Selecting answer '$answer' for question $questionIndex")
        
        val currentAnswers = _uiState.value.selectedAnswers.toMutableList()
        currentAnswers[questionIndex] = answer
        
        _uiState.value = _uiState.value.copy(selectedAnswers = currentAnswers)
        
        // Update quiz attempt progress
        updateQuizProgress()
    }

    fun nextQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        val totalQuestions = _uiState.value.questions.size
        
        if (currentIndex < totalQuestions - 1) {
            val newIndex = currentIndex + 1
            Log.d(TAG, "Moving to next question: $newIndex")
            
            _uiState.value = _uiState.value.copy(currentQuestionIndex = newIndex)
            updateQuizProgress()
        }
    }

    fun previousQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        
        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            Log.d(TAG, "Moving to previous question: $newIndex")
            
            _uiState.value = _uiState.value.copy(currentQuestionIndex = newIndex)
        }
    }

    fun submitQuiz() {
        Log.d(TAG, "Submitting quiz")
        
        viewModelScope.launch {
            try {
                val questions = _uiState.value.questions
                val selectedAnswers = _uiState.value.selectedAnswers
                
                // Calculate score
                var correctCount = 0
                val studentAnswers = mutableListOf<StudentAnswer>()
                
                for (i in questions.indices) {
                    val question = questions[i]
                    val selectedAnswer = selectedAnswers[i]
                    val isCorrect = selectedAnswer == question.correctAnswer
                    
                    if (isCorrect) {
                        correctCount++
                    }
                    
                    studentAnswers.add(
                        StudentAnswer(
                            questionIndex = i,
                            selectedAnswer = selectedAnswer,
                            isCorrect = isCorrect
                        )
                    )
                    
                    Log.d(TAG, "Question $i: Selected=$selectedAnswer, Correct=${question.correctAnswer}, IsCorrect=$isCorrect")
                }
                
                val score = correctCount.toDouble() / questions.size.toDouble()
                val completedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                
                Log.d(TAG, "Quiz completed: $correctCount/$questions.size correct, Score: ${(score * 100).toInt()}%")
                
                // Update quiz attempt with final results
                val currentAttempt = _uiState.value.quizAttempt
                if (currentAttempt != null) {
                    val completedAttempt = currentAttempt.copy(
                        answers = studentAnswers,
                        percentageComplete = 100.0,
                        score = score,
                        completedAt = completedAt,
                        isSubmitted = true
                    )
                    
                    // Save to database
                    val attemptData = mapOf(
                        "_id" to completedAttempt._id,
                        "type" to completedAttempt.type,
                        "studentId" to completedAttempt.studentId,
                        "klypId" to completedAttempt.klypId,
                        "classCode" to completedAttempt.classCode,
                        "answers" to studentAnswers.map { answer ->
                            mapOf(
                                "questionIndex" to answer.questionIndex,
                                "selectedAnswer" to answer.selectedAnswer?.toString(),
                                "isCorrect" to answer.isCorrect
                            )
                        },
                        "percentageComplete" to completedAttempt.percentageComplete,
                        "score" to completedAttempt.score,
                        "startedAt" to completedAttempt.startedAt,
                        "completedAt" to completedAttempt.completedAt,
                        "isSubmitted" to completedAttempt.isSubmitted
                    )
                    
                    val saveSuccess = quizAttemptRepository.save(attemptData as Map<String, Any>)
                    if (saveSuccess) {
                        Log.d(TAG, "Quiz attempt saved successfully")
                    } else {
                        Log.e(TAG, "Failed to save quiz attempt")
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isCompleted = true,
                    score = score,
                    correctAnswers = correctCount,
                    errorMessage = null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit quiz", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to submit quiz: ${e.message}"
                )
            }
        }
    }

    private fun updateQuizProgress() {
        val selectedAnswers = _uiState.value.selectedAnswers
        val totalQuestions = _uiState.value.questions.size
        val answeredQuestions = selectedAnswers.count { it != null }
        val percentageComplete = (answeredQuestions.toDouble() / totalQuestions.toDouble()) * 100.0
        
        Log.d(TAG, "Quiz progress: $answeredQuestions/$totalQuestions answered (${percentageComplete.toInt()}%)")
        
        viewModelScope.launch {
            try {
                val currentAttempt = _uiState.value.quizAttempt
                if (currentAttempt != null) {
                    val updatedAttempt = currentAttempt.copy(
                        percentageComplete = percentageComplete,
                        answers = selectedAnswers.mapIndexed { index, answer ->
                            StudentAnswer(
                                questionIndex = index,
                                selectedAnswer = answer,
                                isCorrect = null // Will be calculated on submission
                            )
                        }.filter { it.selectedAnswer != null }
                    )
                    
                    _uiState.value = _uiState.value.copy(quizAttempt = updatedAttempt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quiz progress", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Stop the quiz initialization process
     */
    fun stopInitialization() {
        Log.d(TAG, "Stopping quiz initialization by user request")
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitializing = false,
            showStopButton = false,
            errorMessage = "Quiz initialization was cancelled."
        )
    }
    
    /**
     * Retry quiz initialization in case of failure
     */
    fun retryInitialization() {
        if (::currentKlyp.isInitialized) {
            Log.d(TAG, "Retrying quiz initialization for klyp: ${currentKlyp.title}")
            initializeQuiz(currentKlyp, studentId)
        } else {
            Log.e(TAG, "Cannot retry initialization - no klyp data available")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Unable to retry. Please navigate back and try again."
            )
        }
    }
    
    /**
     * Force skip the loading screen (for debugging purposes)
     */
    fun skipLoading() {
        if (_uiState.value.isInitializing && ::currentKlyp.isInitialized) {
            Log.d(TAG, "Force skipping loading screen")
            if (currentKlyp.questions.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitializing = false,
                    questions = currentKlyp.questions,
                    selectedAnswers = List(currentKlyp.questions.size) { null },
                    currentQuestionIndex = 0,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitializing = false,
                    errorMessage = "No questions available for this quiz"
                )
            }
        }
    }
}
