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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.Model
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
import com.klypt.data.repositories.KlypRepository
import com.klypt.ui.llmchat.LlmChatModelHelper
import com.klypt.ui.llmchat.LlmModelInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "QuizEditorViewModel"

data class EditableQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctAnswerIndex: Int = 0,
    val isGeneratingOptions: Boolean = false
)

data class QuizEditorUiState(
    val isInitializing: Boolean = false,
    val isInitialized: Boolean = false,
    val initializationError: String? = null,
    val showStopButton: Boolean = false,
    val loadingElapsedTime: Long = 0L,
    val questions: List<EditableQuestion> = listOf(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val currentKlyp: Klyp? = null
)

@HiltViewModel
class QuizEditorViewModel @Inject constructor(
    private val klypRepository: KlypRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizEditorUiState())
    val uiState: StateFlow<QuizEditorUiState> = _uiState.asStateFlow()
    
    private var currentModel: Model? = null
    
    fun initializeWithKlyp(klyp: Klyp, model: Model) {
        Log.d(TAG, "Initializing QuizEditor with klyp: ${klyp.title} and model: ${model.name}")
        
        currentModel = model
        
        // Convert existing questions to editable format
        val editableQuestions = if (klyp.questions.isNotEmpty()) {
            klyp.questions.map { question ->
                EditableQuestion(
                    questionText = question.questionText,
                    options = question.options.takeIf { it.size >= 4 } ?: listOf("", "", "", ""),
                    correctAnswerIndex = when (question.correctAnswer) {
                        'A' -> 0
                        'B' -> 1
                        'C' -> 2
                        'D' -> 3
                        else -> 0
                    }
                )
            }
        } else {
            // Start with one empty question if no questions exist
            listOf(EditableQuestion())
        }
        
        _uiState.value = _uiState.value.copy(
            currentKlyp = klyp,
            questions = editableQuestions
        )
        
        // Initialize AI model
        initializeAI(model)
    }
    
    private fun initializeAI(model: Model) {
        if (_uiState.value.isInitializing || _uiState.value.isInitialized) {
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isInitializing = true,
            initializationError = null,
            loadingElapsedTime = 0L
        )
        
        // Start elapsed time tracking
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (_uiState.value.isInitializing) {
                delay(100)
                _uiState.value = _uiState.value.copy(
                    loadingElapsedTime = System.currentTimeMillis() - startTime,
                    showStopButton = _uiState.value.loadingElapsedTime > 5000L // Show after 5 seconds
                )
            }
        }
        
        // Initialize the model
        viewModelScope.launch(Dispatchers.Main) {
            // Note: This requires a Context, which should be passed from the UI
            // For now, we'll set the state to indicate initialization is needed
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                isInitialized = true // Assume initialization succeeds for now
            )
        }
    }
    
    fun initializeAIWithContext(context: android.content.Context, model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                LlmChatModelHelper.initialize(context, model) { error ->
                    viewModelScope.launch {
                        if (error.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isInitializing = false,
                                initializationError = error
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isInitializing = false,
                                isInitialized = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AI model", e)
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    initializationError = "Failed to initialize AI: ${e.message}"
                )
            }
        }
    }
    
    fun stopInitialization() {
        Log.d(TAG, "Stopping AI initialization")
        _uiState.value = _uiState.value.copy(
            isInitializing = false,
            initializationError = "Initialization cancelled by user"
        )
    }
    
    fun retryInitialization() {
        Log.d(TAG, "Retrying AI initialization")
        currentModel?.let { model ->
            _uiState.value = _uiState.value.copy(
                initializationError = null
            )
            initializeAI(model)
        }
    }
    
    fun addQuestion() {
        Log.d(TAG, "Adding new question")
        val currentQuestions = _uiState.value.questions
        val newQuestion = EditableQuestion()
        
        _uiState.value = _uiState.value.copy(
            questions = currentQuestions + newQuestion
        )
    }
    
    fun deleteQuestion(questionId: String) {
        Log.d(TAG, "Deleting question with ID: $questionId")
        val currentQuestions = _uiState.value.questions
        val updatedQuestions = currentQuestions.filter { it.id != questionId }
        
        _uiState.value = _uiState.value.copy(
            questions = updatedQuestions
        )
    }
    
    fun updateQuestionText(questionId: String, text: String) {
        val currentQuestions = _uiState.value.questions
        val updatedQuestions = currentQuestions.map { question ->
            if (question.id == questionId) {
                question.copy(questionText = text)
            } else {
                question
            }
        }
        
        _uiState.value = _uiState.value.copy(
            questions = updatedQuestions
        )
    }
    
    fun updateQuestionOption(questionId: String, optionIndex: Int, text: String) {
        val currentQuestions = _uiState.value.questions
        val updatedQuestions = currentQuestions.map { question ->
            if (question.id == questionId) {
                val updatedOptions = question.options.toMutableList()
                updatedOptions[optionIndex] = text
                question.copy(options = updatedOptions)
            } else {
                question
            }
        }
        
        _uiState.value = _uiState.value.copy(
            questions = updatedQuestions
        )
    }
    
    fun updateCorrectAnswer(questionId: String, correctIndex: Int) {
        val currentQuestions = _uiState.value.questions
        val updatedQuestions = currentQuestions.map { question ->
            if (question.id == questionId) {
                question.copy(correctAnswerIndex = correctIndex)
            } else {
                question
            }
        }
        
        _uiState.value = _uiState.value.copy(
            questions = updatedQuestions
        )
    }
    
    fun generateOptionsForQuestion(questionId: String) {
        if (!_uiState.value.isInitialized || currentModel == null) {
            Log.w(TAG, "Cannot generate options: AI model not initialized")
            return
        }
        
        val question = _uiState.value.questions.find { it.id == questionId }
        if (question == null || question.questionText.isBlank()) {
            Log.w(TAG, "Cannot generate options: Question text is empty")
            return
        }
        
        Log.d(TAG, "Generating options for question: ${question.questionText}")
        
        // Mark question as generating
        val updatedQuestions = _uiState.value.questions.map { q ->
            if (q.id == questionId) {
                q.copy(isGeneratingOptions = true)
            } else {
                q
            }
        }
        
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                AIOptionsGenerator.generateOptionsForQuestion(
                    model = currentModel!!,
                    questionText = question.questionText,
                    onResult = { options ->
                        // Update the question with generated options
                        val updatedQuestions = _uiState.value.questions.map { q ->
                            if (q.id == questionId) {
                                q.copy(
                                    options = options,
                                    isGeneratingOptions = false
                                )
                            } else {
                                q
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
                    },
                    onError = { error ->
                        Log.e(TAG, "Failed to generate options: $error")
                        
                        // Mark question as not generating and show fallback
                        val fallbackOptions = listOf(
                            "Option A - Edit this",
                            "Option B - Edit this", 
                            "Option C - Edit this",
                            "Option D - Edit this"
                        )
                        
                        val updatedQuestions = _uiState.value.questions.map { q ->
                            if (q.id == questionId) {
                                q.copy(
                                    options = fallbackOptions,
                                    isGeneratingOptions = false
                                )
                            } else {
                                q
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate options", e)
                
                // Mark question as not generating and show error
                val updatedQuestions = _uiState.value.questions.map { q ->
                    if (q.id == questionId) {
                        q.copy(isGeneratingOptions = false)
                    } else {
                        q
                    }
                }
                
                _uiState.value = _uiState.value.copy(questions = updatedQuestions)
            }
        }
    }
    
    fun saveQuestions() {
        val currentKlyp = _uiState.value.currentKlyp
        if (currentKlyp == null) {
            Log.e(TAG, "Cannot save: No klyp loaded")
            return
        }
        
        Log.d(TAG, "Saving questions to klyp: ${currentKlyp.title}")
        
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            saveError = null,
            saveSuccess = false
        )
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert editable questions to Question objects
                val questions = _uiState.value.questions.mapNotNull { editableQuestion ->
                    // Only save questions with non-empty text and at least one non-empty option
                    if (editableQuestion.questionText.isNotBlank() && 
                        editableQuestion.options.any { it.isNotBlank() }) {
                        
                        // Ensure all 4 options exist, fill empty ones with placeholders
                        val completeOptions = (0..3).map { index ->
                            editableQuestion.options.getOrNull(index)?.takeIf { it.isNotBlank() }
                                ?: "Option ${('A' + index)}"
                        }
                        
                        Question(
                            questionText = editableQuestion.questionText,
                            options = completeOptions,
                            correctAnswer = when (editableQuestion.correctAnswerIndex) {
                                0 -> 'A'
                                1 -> 'B' 
                                2 -> 'C'
                                3 -> 'D'
                                else -> 'A'
                            }
                        )
                    } else null
                }
                
                // Update the klyp with new questions
                val updatedKlyp = currentKlyp.copy(questions = questions)
                
                // Convert to Map for repository
                val klypData = mapOf(
                    "_id" to updatedKlyp._id,
                    "type" to updatedKlyp.type,
                    "classCode" to updatedKlyp.classCode,
                    "title" to updatedKlyp.title,
                    "mainBody" to updatedKlyp.mainBody,
                    "questions" to questions.map { question ->
                        mapOf(
                            "questionText" to question.questionText,
                            "options" to question.options,
                            "correctAnswer" to question.correctAnswer.toString()
                        )
                    },
                    "createdAt" to updatedKlyp.createdAt
                )
                
                val success = klypRepository.save(klypData)
                
                if (success) {
                    Log.d(TAG, "Successfully saved ${questions.size} questions")
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        currentKlyp = updatedKlyp
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = "Failed to save questions to database"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save questions", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "Error saving questions: ${e.message}"
                )
            }
        }
    }
    
    fun clearSaveStatus() {
        _uiState.value = _uiState.value.copy(
            saveError = null,
            saveSuccess = false
        )
    }
}
