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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val TAG = "QuizViewModel"

data class QuizUiState(
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: List<Char?> = emptyList(),
    val score: Double = 0.0,
    val correctAnswers: Int = 0,
    val quizAttempt: QuizAttempt? = null,
    val errorMessage: String? = null
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
        
        currentKlyp = klyp
        studentId = userId
        startTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        if (klyp.questions.isEmpty()) {
            Log.e(TAG, "No questions available for klyp: ${klyp.title}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No questions available for this quiz"
            )
            return
        }

        Log.d(TAG, "Quiz initialized with ${klyp.questions.size} questions")
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            questions = klyp.questions,
            selectedAnswers = List(klyp.questions.size) { null },
            currentQuestionIndex = 0,
            errorMessage = null
        )
        
        // Create initial quiz attempt
        createQuizAttempt()
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
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to initialize quiz: ${e.message}"
                )
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
}
