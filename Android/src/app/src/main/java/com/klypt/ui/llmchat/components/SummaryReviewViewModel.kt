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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryReviewUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SummaryReviewViewModel @Inject constructor(
    private val databaseManager: DatabaseManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SummaryReviewUiState())
    val uiState: StateFlow<SummaryReviewUiState> = _uiState.asStateFlow()
    
    /**
     * Updates an existing chat summary in the database
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
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
                if (success) {
                    Log.d("SummaryReviewViewModel", "Successfully updated summary")
                    // Add small delay to ensure database write is complete before navigation
                    kotlinx.coroutines.delay(100)
                    onSuccess()
                } else {
                    val errorMsg = "Failed to update summary in database"
                    Log.e("SummaryReviewViewModel", errorMsg)
                    _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
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
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
