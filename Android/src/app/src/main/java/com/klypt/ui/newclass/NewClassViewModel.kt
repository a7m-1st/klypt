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

package com.klypt.ui.newclass

import androidx.lifecycle.ViewModel
import com.klypt.data.utils.ClassCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NewClassViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(NewClassUiState())
    val uiState = _uiState.asStateFlow()
    
    fun updateClassName(className: String) {
        _uiState.value = _uiState.value.copy(
            className = className,
            classNameError = validateClassName(className),
            isFormValid = validateClassName(className) == null && className.isNotBlank()
        )
    }
    
    private fun validateClassName(className: String): String? {
        return when {
            className.isBlank() -> null // Allow empty class name, will use default
            className.length > 50 -> "Class name must be 50 characters or less"
            else -> null
        }
    }
    
    fun generateClassCode(): String {
        return ClassCodeGenerator.generateClassCode()
    }
    
    fun proceedToLLMChat(): Pair<String, String> {
        val classCode = generateClassCode()
        val className = _uiState.value.className.ifBlank { "Untitled Class" }
        return Pair(classCode, className)
    }
}
