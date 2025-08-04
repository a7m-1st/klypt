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

package com.klypt.ui.classcodedisplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.ClassDocument
import com.klypt.data.utils.ClassCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassCodeDisplayViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ClassCodeDisplayUiState())
    val uiState = _uiState.asStateFlow()
    
    fun initializeWithClassData(classCode: String, className: String) {
        _uiState.value = _uiState.value.copy(
            classCode = classCode,
            className = className,
            isSuccess = true
        )
    }
    
    fun updateClassName(newClassName: String) {
        _uiState.value = _uiState.value.copy(className = newClassName)
    }
    
    fun saveClass(
        educatorId: String,
        onSuccess: (ClassDocument) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                
                val classDocument = ClassDocument(
                    _id = _uiState.value.classCode,
                    classCode = _uiState.value.classCode,
                    classTitle = _uiState.value.className.ifBlank { "Untitled Class" },
                    updatedAt = currentTime,
                    lastSyncedAt = currentTime,
                    educatorId = educatorId,
                    studentIds = emptyList()
                )
                
                // Here you would normally save to database
                // For now, we'll just simulate success
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
                
                onSuccess(classDocument)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to save class"
                )
                onError(_uiState.value.errorMessage ?: "Unknown error")
            }
        }
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
