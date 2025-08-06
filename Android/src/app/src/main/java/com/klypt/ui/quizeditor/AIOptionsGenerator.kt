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
import com.klypt.data.Model
import com.klypt.ui.llmchat.LlmChatModelHelper
import com.klypt.ui.llmchat.LlmModelInstance
import kotlinx.coroutines.delay

private const val TAG = "AIOptionsGenerator"

/**
 * Utility object for generating quiz options using AI
 */
object AIOptionsGenerator {
    
    /**
     * Generate 4 multiple choice options for a given question using AI
     */
    suspend fun generateOptionsForQuestion(
        model: Model,
        questionText: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Wait for model to be initialized
            var attempts = 0
            while (model.instance == null && attempts < 50) { // Max 5 seconds wait
                delay(100)
                attempts++
            }
            
            if (model.instance == null) {
                onError("AI model is not initialized")
                return
            }
            
            val prompt = buildPrompt(questionText)
            val instance = model.instance as LlmModelInstance
            var result = ""
            
            Log.d(TAG, "Generating options for question: $questionText")
            
            LlmChatModelHelper.runInference(
                model = model,
                input = prompt,
                images = emptyList(),
                audioClips = emptyList(),
                resultListener = { partialResult, done ->
                    result = partialResult
                    if (done) {
                        val options = parseOptionsFromResponse(result)
                        if (options.size == 4) {
                            Log.d(TAG, "Successfully generated options: $options")
                            onResult(options)
                        } else {
                            Log.w(TAG, "AI returned ${options.size} options instead of 4, using fallback")
                            onResult(generateFallbackOptions(questionText))
                        }
                    }
                },
                cleanUpListener = {
                    // Clean up any resources if needed
                    Log.d(TAG, "AI inference cleanup completed")
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating options", e)
            onError("Failed to generate options: ${e.message}")
        }
    }
    
    /**
     * Build the AI prompt for generating options
     */
    private fun buildPrompt(questionText: String): String {
        return """
            Generate exactly 4 multiple choice options for this question:
            "$questionText"
            
            Requirements:
            - Provide exactly 4 options
            - One should be clearly correct
            - Three should be plausible but incorrect  
            - Make options educational and appropriate
            - Format as a simple JSON array of strings
            - Example: ["Correct answer", "Incorrect option 1", "Incorrect option 2", "Incorrect option 3"]
            
            Only respond with the JSON array:
        """.trimIndent()
    }
    
    /**
     * Parse options from AI response
     */
    private fun parseOptionsFromResponse(response: String): List<String> {
        try {
            // Clean the response
            val cleanedResponse = response.trim()
            
            // Try to find JSON array pattern
            val jsonMatch = Regex("""\[.*?\]""", RegexOption.DOT_MATCHES_ALL).find(cleanedResponse)
            
            if (jsonMatch != null) {
                val jsonString = jsonMatch.value
                // Simple JSON parsing - extract strings between quotes
                val optionMatches = Regex(""""([^"]+)"""").findAll(jsonString)
                val options = optionMatches.map { it.groupValues[1] }.toList()
                
                if (options.size >= 4) {
                    return options.take(4) // Take first 4 if more than 4
                }
            }
            
            // Fallback: try to parse line by line
            val lines = cleanedResponse.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { !it.startsWith("[") && !it.startsWith("]") && !it.startsWith("{") && !it.startsWith("}") }
                .map { it.removePrefix("-").removePrefix("•").removePrefix("*").trim() }
                .map { it.removeSurrounding("\"") }
                .filter { it.isNotBlank() }
            
            if (lines.size >= 4) {
                return lines.take(4)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response", e)
        }
        
        return emptyList()
    }
    
    /**
     * Generate fallback options when AI fails
     */
    private fun generateFallbackOptions(questionText: String): List<String> {
        Log.d(TAG, "Generating fallback options for: $questionText")
        
        // Generate contextual fallback options based on question
        return when {
            questionText.contains("what", ignoreCase = true) ||
            questionText.contains("which", ignoreCase = true) -> {
                listOf(
                    "Option A - Please edit this option",
                    "Option B - Please edit this option", 
                    "Option C - Please edit this option",
                    "Option D - Please edit this option"
                )
            }
            questionText.contains("when", ignoreCase = true) ||
            questionText.contains("year", ignoreCase = true) ||
            questionText.contains("time", ignoreCase = true) -> {
                listOf(
                    "In the early period",
                    "In the middle period",
                    "In the late period", 
                    "Never happened"
                )
            }
            questionText.contains("where", ignoreCase = true) ||
            questionText.contains("location", ignoreCase = true) -> {
                listOf(
                    "Location A",
                    "Location B", 
                    "Location C",
                    "Location D"
                )
            }
            questionText.contains("how many", ignoreCase = true) ||
            questionText.contains("number", ignoreCase = true) -> {
                listOf(
                    "1-2",
                    "3-5",
                    "6-10",
                    "More than 10"
                )
            }
            else -> {
                listOf(
                    "True",
                    "False",
                    "Partially true",
                    "Not applicable"
                )
            }
        }
    }
}
