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
import com.klypt.ui.llmchat.LlmModelInstance
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
    val isInitializingModel: Boolean = false,
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
        generateQuizQuestionsInternal(klyp, context, onSuccess, onError, forceRegenerate = false)
    }
    
    /**
     * Regenerates quiz questions even if they already exist
     */
    fun regenerateQuizQuestions(
        klyp: Klyp,
        context: Context,
        onSuccess: (Klyp) -> Unit,
        onError: (String) -> Unit
    ) {
        generateQuizQuestionsInternal(klyp, context, onSuccess, onError, forceRegenerate = true)
    }
    
    /**
     * Internal function that handles both generation and regeneration of quiz questions
     */
    private fun generateQuizQuestionsInternal(
        klyp: Klyp,
        context: Context,
        onSuccess: (Klyp) -> Unit,
        onError: (String) -> Unit,
        forceRegenerate: Boolean
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting quiz ${if (forceRegenerate) "regeneration" else "generation"} for klyp: ${klyp.title}")
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

                // Check if model needs initialization and wait for it
                if (model.instance == null) {
                    Log.d(TAG, "Model ${model.name} not initialized, showing loading screen...")
                    _uiState.value = _uiState.value.copy(isInitializingModel = true)
                    
                    // Wait for model initialization to complete
                    var initializationAttempts = 0
                    val maxAttempts = 120 // Wait up to 120 seconds
                    
                    while (model.instance == null && initializationAttempts < maxAttempts) {
                        kotlinx.coroutines.delay(1000) // Wait 1 second
                        initializationAttempts++
                        Log.d(TAG, "Waiting for model initialization... attempt $initializationAttempts/$maxAttempts")
                    }
                    
                    _uiState.value = _uiState.value.copy(isInitializingModel = false)
                    
                    if (model.instance == null) {
                        Log.e(TAG, "Model initialization failed after $maxAttempts attempts")
                        _uiState.value = _uiState.value.copy(
                            isGeneratingQuiz = false,
                            errorMessage = "AI model initialization timed out. Please ensure the model is loaded and try again."
                        )
                        onError("AI model initialization timed out. Please ensure the model is loaded and try again.")
                        return@launch
                    }
                    
                    Log.d(TAG, "Model ${model.name} successfully initialized")
                }

                // Create a detailed prompt for question generation
                val prompt = createQuestionGenerationPrompt(klyp)
                Log.d(TAG, "Generated prompt length: ${prompt.length}")

                // Generate questions using LLM
                val generatedQuestionsJson = generateQuestionsWithLLM(model, prompt)
                Log.d(TAG, "Generated questions JSON: $generatedQuestionsJson")

                // Parse and validate the JSON
                val questions = parseAndValidateQuestions(generatedQuestionsJson)
                Log.d(TAG, "Parsed ${questions.size} valid questions")

                if (questions.isEmpty()) {
                    throw Exception("No valid questions were generated by the AI model")
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
                Log.e(TAG, "Quiz ${if (forceRegenerate) "regeneration" else "generation"} failed", e)
                val errorMsg = when {
                    e.message?.contains("Model instance is null") == true -> 
                        "AI model failed to initialize. Please try again."
                    e.message?.contains("No valid questions") == true -> 
                        "AI model couldn't generate valid questions. Please try again or check the content."
                    e.message?.contains("save") == true -> 
                        "Failed to save generated questions. Please try again."
                    e.message?.contains("initialize") == true ->
                        "Failed to initialize AI model. Please check your internet connection and try again."
                    else -> "Failed to generate quiz: ${e.message}"
                }
                
                _uiState.value = _uiState.value.copy(
                    isGeneratingQuiz = false,
                    isInitializingModel = false,
                    errorMessage = errorMsg
                )
                onError(errorMsg)
            }
        }
    }

    private fun createQuestionGenerationPrompt(klyp: Klyp): String {
        return """
            Based on the following educational content, generate exactly 5 multiple-choice questions to test understanding and comprehension.
            
            Title: ${klyp.title}
            
            Content:
            ${klyp.mainBody}
            
            Please generate exactly 5 multiple-choice questions in the following JSON format:
            {
                "questions": [
                    {
                        "questionText": "Your question here?",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctAnswer": "A"
                    }
                ]
            }
            
            IMPORTANT REQUIREMENTS:
            1. Generate exactly 5 questions - no more, no less
            2. Each question should test comprehension of key concepts from the content
            3. Each question must have exactly 4 options labeled as shown above
            4. The correctAnswer must be exactly one of: "A", "B", "C", or "D"
            5. Questions should be clear, unambiguous, and directly related to the content
            6. Options should be plausible but only one should be correct
            7. Avoid trick questions or overly complex wording
            8. Focus on understanding rather than memorization
            9. Return ONLY the JSON format shown above - no additional text or formatting
            10. Ensure the JSON is valid and properly formatted
            
            Generate the 5 questions now:
        """.trimIndent()
    }

    private suspend fun generateQuestionsWithLLM(model: com.klypt.data.Model, prompt: String): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                Log.d(TAG, "Starting actual LLM inference for question generation")
                
                // Check if model instance is available
                if (model.instance == null) {
                    Log.e(TAG, "Model instance is null")
                    continuation.resume("""{"questions": []}""")
                    return@suspendCancellableCoroutine
                }
                
                val instance = model.instance as LlmModelInstance
                val fullResponse = StringBuilder()
                
                LlmChatModelHelper.runInference(
                    model = model,
                    input = prompt,
                    resultListener = { partialResult, done ->
                        fullResponse.append(partialResult)
                        if (done) {
                            Log.d(TAG, "LLM inference completed")
                            continuation.resume(fullResponse.toString())
                        }
                    },
                    cleanUpListener = {
                        Log.d(TAG, "LLM inference cleanup completed")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "LLM inference failed", e)
                continuation.resume("""{"questions": []}""")
            }
        }
    }

    private fun parseAndValidateQuestions(jsonString: String): List<Question> {
        return try {
            Log.d(TAG, "Parsing questions JSON (first 500 chars): ${jsonString.take(500)}")
            
            // Clean up the JSON string - remove any extra text before/after JSON
            val cleanedJson = extractJsonFromResponse(jsonString)
            Log.d(TAG, "Cleaned JSON: $cleanedJson")
            
            val jsonObject = JSONObject(cleanedJson)
            val questionsArray = jsonObject.getJSONArray("questions")
            val questions = mutableListOf<Question>()

            if (questionsArray.length() == 0) {
                Log.w(TAG, "No questions found in the response")
                return emptyList()
            }

            for (i in 0 until questionsArray.length()) {
                try {
                    val questionObj = questionsArray.getJSONObject(i)
                    
                    val questionText = questionObj.getString("questionText").trim()
                    val optionsArray = questionObj.getJSONArray("options")
                    val correctAnswer = questionObj.getString("correctAnswer").trim().uppercase()

                    // Validate question text
                    if (questionText.isEmpty()) {
                        Log.w(TAG, "Question $i has empty text. Skipping.")
                        continue
                    }

                    // Validate options
                    if (optionsArray.length() != 4) {
                        Log.w(TAG, "Question $i has ${optionsArray.length()} options, expected 4. Skipping.")
                        continue
                    }

                    val options = mutableListOf<String>()
                    for (j in 0 until optionsArray.length()) {
                        val option = optionsArray.getString(j).trim()
                        if (option.isEmpty()) {
                            Log.w(TAG, "Question $i has empty option at index $j. Skipping question.")
                            break
                        }
                        options.add(option)
                    }

                    if (options.size != 4) {
                        continue
                    }

                    // Validate correct answer
                    val correctAnswerChar = when (correctAnswer) {
                        "A" -> 'A'
                        "B" -> 'B'
                        "C" -> 'C'
                        "D" -> 'D'
                        else -> {
                            Log.w(TAG, "Invalid correct answer '$correctAnswer' for question $i. Skipping.")
                            continue
                        }
                    }

                    questions.add(
                        Question(
                            questionText = questionText,
                            options = options,
                            correctAnswer = correctAnswerChar
                        )
                    )
                    
                    Log.d(TAG, "Successfully parsed question $i: ${questionText.take(50)}...")
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse question $i", e)
                    continue
                }
            }

            Log.d(TAG, "Successfully parsed ${questions.size} questions out of ${questionsArray.length()}")
            
            if (questions.size < 3) {
                Log.w(TAG, "Only ${questions.size} valid questions parsed, which is less than minimum expected")
            }
            
            questions
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions JSON", e)
            emptyList()
        }
    }
    
    /**
     * Extracts JSON content from LLM response that might contain extra text
     */
    private fun extractJsonFromResponse(response: String): String {
        try {
            val startIndex = response.indexOf("{")
            val lastIndex = response.lastIndexOf("}")
            
            if (startIndex != -1 && lastIndex != -1 && lastIndex > startIndex) {
                return response.substring(startIndex, lastIndex + 1)
            }
            
            // If no braces found, return original response
            return response
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting JSON from response", e)
            return response
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
