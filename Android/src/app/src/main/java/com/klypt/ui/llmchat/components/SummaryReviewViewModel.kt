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

package com.klypt.ui.llmchat.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.DatabaseManager
import com.klypt.data.models.ChatSummary
import com.klypt.data.repositories.KlypRepository
import com.klypt.ui.navigation.SummaryNavigationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SummaryReviewUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SummaryReviewViewModel @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val klypRepository: KlypRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SummaryReviewUiState())
    val uiState: StateFlow<SummaryReviewUiState> = _uiState.asStateFlow()
    
    /**
     * Updates an existing chat summary in the database and creates a corresponding klyp
     */
    fun updateSummary(
        summary: ChatSummary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                Log.d("SummaryReviewViewModel", "Updating summary with ID: ${summary._id}")
                
                // Update the summary in the database
                val success = databaseManager.updateChatSummary(summary)
                
                if (success) {
                    Log.d("SummaryReviewViewModel", "Successfully updated summary")
                    
                    // Check if we're in a class creation context
                    val classContext = SummaryNavigationData.getClassCreationContext()
                    Log.d("SummaryReviewViewModel", "Class creation context: $classContext")
                    if (classContext != null) {
                        Log.d("SummaryReviewViewModel", "Class creation context found, creating klyp for class: '${classContext.classCode}'")
                        
                        // Create a klyp from the summary data
                        val klypSuccess = createKlypFromSummary(summary, classContext.classCode)
                        
                        if (klypSuccess) {
                            Log.d("SummaryReviewViewModel", "Successfully created klyp from summary")
                        } else {
                            Log.e("SummaryReviewViewModel", "Failed to create klyp from summary")
                            // Don't fail the whole operation if klyp creation fails
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Add small delay to ensure database write is complete before navigation
                    kotlinx.coroutines.delay(100)
                    onSuccess()
                } else {
                    val errorMsg = "Failed to update summary in database"
                    Log.e("SummaryReviewViewModel", errorMsg)
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
                    onError(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e("SummaryReviewViewModel", "Exception updating summary", e)
                val errorMsg = "Error updating summary: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
                onError(errorMsg)
            }
        }
    }
    
    /**
     * Creates a klyp from the chat summary data
     */
    private suspend fun createKlypFromSummary(summary: ChatSummary, classCode: String): Boolean {
        return try {
            Log.d("SummaryReviewViewModel", "Creating klyp from summary for class: '$classCode' (length: ${classCode.length})")
            Log.d("SummaryReviewViewModel", "Summary title: '${summary.sessionTitle}'")
            Log.d("SummaryReviewViewModel", "Summary content length: ${summary.bulletPointSummary.length}")
            
            if (classCode.isEmpty()) {
                Log.e("SummaryReviewViewModel", "ERROR: classCode is empty when creating klyp!")
                return false
            }
            
            val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
            val klypId = "klyp_${UUID.randomUUID()}"
            
            Log.d("SummaryReviewViewModel", "Generated klyp ID: $klypId")
            
            val klypData = mapOf(
                "_id" to klypId,
                "type" to "klyp",
                "classCode" to classCode,
                "title" to summary.sessionTitle,
                "mainBody" to summary.bulletPointSummary,
                "questions" to emptyList<Map<String, Any>>(), // Empty questions as requested
                "createdAt" to currentTime
            )
            
            Log.d("SummaryReviewViewModel", "Klyp data created: $klypData")
            
            val success = klypRepository.save(klypData)
            
            if (success) {
                Log.d("SummaryReviewViewModel", "Successfully created klyp with ID: $klypId for class: $classCode")
            } else {
                Log.e("SummaryReviewViewModel", "Failed to save klyp to repository")
            }
            
            success
        } catch (e: Exception) {
            Log.e("SummaryReviewViewModel", "Exception creating klyp from summary", e)
            false
        }
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
