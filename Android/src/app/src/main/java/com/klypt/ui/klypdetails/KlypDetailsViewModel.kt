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

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.TASK_LLM_CHAT
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.services.ChatSummaryService
import com.klypt.ui.llmchat.LlmChatModelHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume

private const val TAG = "KlypDetailsViewModel"

data class KlypDetailsUiState(
    val isLoading: Boolean = false,
    val isGeneratingQuiz: Boolean = false,
    val currentKlyp: Klyp? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class KlypDetailsViewModel @Inject constructor(
    private val klypRepository: KlypRepository,
    private val chatSummaryService: ChatSummaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(KlypDetailsUiState())
    val uiState: StateFlow<KlypDetailsUiState> = _uiState.asStateFlow()

    fun initializeWithKlyp(klyp: Klyp) {
        Log.d(TAG, "Initializing with klyp: ${klyp.title} (ID: ${klyp._id})")
        _uiState.value = _uiState.value.copy(
            currentKlyp = klyp,
            isLoading = false,
            errorMessage = null
        )
    }

    /**
     * Generates quiz questions using LLM and validates the JSON format
     */
    fun generateQuizQuestions(
        klyp: Klyp,
        context: Context,
        onSuccess: (Klyp) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting quiz generation for klyp: ${klyp.title}")
                _uiState.value = _uiState.value.copy(
                    isGeneratingQuiz = true,
                    errorMessage = null
                )

                // Get the first available LLM model
                val availableModels = TASK_LLM_CHAT.models
                if (availableModels.isEmpty()) {
                    Log.e(TAG, "No LLM models available for quiz generation")
                    _uiState.value = _uiState.value.copy(
                        isGeneratingQuiz = false,
                        errorMessage = "No AI models available for quiz generation"
                    )
                    onError("No AI models available for quiz generation")
                    return@launch
                }

                val model = availableModels.first()
                Log.d(TAG, "Using model: ${model.name} for quiz generation")

                // Create a detailed prompt for question generation
                val prompt = createQuestionGenerationPrompt(klyp)
                Log.d(TAG, "Generated prompt length: ${prompt.length}")

                // Initialize model if needed
                if (model.instance == null) {
                    Log.d(TAG, "Initializing model: ${model.name}")
                    // Note: In a real implementation, you'd need to initialize the model properly
                    // For now, we'll simulate the generation
                }

                // Generate questions using LLM
                val generatedQuestionsJson = generateQuestionsWithLLM(model, prompt)
                Log.d(TAG, "Generated questions JSON: $generatedQuestionsJson")

                // Parse and validate the JSON
                val questions = parseAndValidateQuestions(generatedQuestionsJson)
                Log.d(TAG, "Parsed ${questions.size} valid questions")

                if (questions.isEmpty()) {
                    throw Exception("No valid questions were generated")
                }

                // Update the klyp with new questions
                val updatedKlyp = klyp.copy(questions = questions)
                Log.d(TAG, "Created updated klyp with ${updatedKlyp.questions.size} questions")

                // Save the updated klyp to database
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

                val saveSuccess = klypRepository.save(klypData)
                if (!saveSuccess) {
                    Log.e(TAG, "Failed to save updated klyp to database")
                    throw Exception("Failed to save quiz questions to database")
                }

                Log.d(TAG, "Successfully saved updated klyp to database")

                _uiState.value = _uiState.value.copy(
                    isGeneratingQuiz = false,
                    currentKlyp = updatedKlyp,
                    errorMessage = null
                )

                onSuccess(updatedKlyp)

            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                _uiState.value = _uiState.value.copy(
                    isGeneratingQuiz = false,
                    errorMessage = "Failed to generate quiz: ${e.message}"
                )
                onError("Failed to generate quiz: ${e.message}")
            }
        }
    }

    private fun createQuestionGenerationPrompt(klyp: Klyp): String {
        return """
            Based on the following educational content, generate 5 multiple-choice questions to test understanding.
            
            Content Title: ${klyp.title}
            Content:
            ${klyp.mainBody}
            
            Please generate exactly 5 multiple-choice questions in the following JSON format:
            {
                "questions": [
                    {
                        "questionText": "Question text here?",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctAnswer": "A"
                    }
                ]
            }
            
            Requirements:
            1. Questions should test comprehension of the key concepts
            2. Each question must have exactly 4 options (A, B, C, D)
            3. The correctAnswer must be one of: A, B, C, or D
            4. Questions should be clear and unambiguous
            5. Options should be plausible but only one should be correct
            6. Return ONLY the JSON, no additional text
            
            Generate the questions now:
        """.trimIndent()
    }

    private suspend fun generateQuestionsWithLLM(model: com.klypt.data.Model, prompt: String): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                Log.d(TAG, "Starting LLM inference for question generation")
                
                // For now, we'll simulate the LLM response with a realistic example
                // In a real implementation, you would use the actual LLM inference
                val simulatedResponse = """
                {
                    "questions": [
                        {
                            "questionText": "What is the main purpose of the content discussed?",
                            "options": ["Option A - Basic concept", "Option B - Advanced theory", "Option C - Main topic focus", "Option D - Secondary information"],
                            "correctAnswer": "C"
                        },
                        {
                            "questionText": "Which of the following is a key component mentioned?",
                            "options": ["Component X", "Component Y", "Component Z", "All of the above"],
                            "correctAnswer": "D"
                        },
                        {
                            "questionText": "What should you remember about this topic?",
                            "options": ["It's optional", "It's fundamental", "It's complex", "It's outdated"],
                            "correctAnswer": "B"
                        },
                        {
                            "questionText": "How does this concept apply in practice?",
                            "options": ["Theoretical only", "Practical application", "No application", "Limited use"],
                            "correctAnswer": "B"
                        },
                        {
                            "questionText": "What is the expected outcome of understanding this?",
                            "options": ["Confusion", "Better comprehension", "No change", "More questions"],
                            "correctAnswer": "B"
                        }
                    ]
                }
                """.trimIndent()
                
                Log.d(TAG, "LLM simulation completed, returning response")
                continuation.resume(simulatedResponse)
                
                // TODO: Replace with actual LLM inference
                /*
                LlmChatModelHelper.runInference(
                    model = model,
                    input = prompt,
                    resultListener = { partialResult, done ->
                        if (done) {
                            Log.d(TAG, "LLM inference completed")
                            continuation.resume(partialResult)
                        }
                    },
                    cleanUpListener = {
                        Log.d(TAG, "LLM inference cleanup completed")
                    }
                )
                */
                
            } catch (e: Exception) {
                Log.e(TAG, "LLM inference failed", e)
                continuation.resume("""{"questions": []}""")
            }
        }
    }

    private fun parseAndValidateQuestions(jsonString: String): List<Question> {
        return try {
            Log.d(TAG, "Parsing questions JSON: $jsonString")
            
            val jsonObject = JSONObject(jsonString)
            val questionsArray = jsonObject.getJSONArray("questions")
            val questions = mutableListOf<Question>()

            for (i in 0 until questionsArray.length()) {
                try {
                    val questionObj = questionsArray.getJSONObject(i)
                    
                    val questionText = questionObj.getString("questionText")
                    val optionsArray = questionObj.getJSONArray("options")
                    val correctAnswer = questionObj.getString("correctAnswer")

                    // Validate options
                    if (optionsArray.length() != 4) {
                        Log.w(TAG, "Question $i has ${optionsArray.length()} options, expected 4. Skipping.")
                        continue
                    }

                    val options = mutableListOf<String>()
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(j))
                    }

                    // Validate correct answer
                    val correctAnswerChar = when (correctAnswer.uppercase()) {
                        "A" -> 'A'
                        "B" -> 'B'
                        "C" -> 'C'
                        "D" -> 'D'
                        else -> {
                            Log.w(TAG, "Invalid correct answer '$correctAnswer' for question $i. Skipping.")
                            continue
                        }
                    }

                    // Validate question text
                    if (questionText.isBlank()) {
                        Log.w(TAG, "Question $i has blank text. Skipping.")
                        continue
                    }

                    questions.add(
                        Question(
                            questionText = questionText,
                            options = options,
                            correctAnswer = correctAnswerChar
                        )
                    )
                    
                    Log.d(TAG, "Successfully parsed question $i: $questionText")
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse question $i", e)
                    continue
                }
            }

            Log.d(TAG, "Successfully parsed ${questions.size} questions out of ${questionsArray.length()}")
            questions
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions JSON", e)
            emptyList()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
