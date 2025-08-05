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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.utils.ClassCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class NewClassViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NewClassUiState())
    val uiState = _uiState.asStateFlow()
    
    fun updateClassName(className: String) {
        _uiState.value = _uiState.value.copy(
            className = className,
            classNameError = validateClassName(className),
            isFormValid = validateClassName(className) == null && className.isNotBlank()
        )
    }
    
    fun updateClassCode(classCode: String) {
        _uiState.value = _uiState.value.copy(
            classCode = classCode.uppercase(),
            classCodeError = validateClassCode(classCode)
        )
    }
    
    fun showClassCodeInput() {
        _uiState.value = _uiState.value.copy(
            showClassCodeInput = true,
            showCreateNewForm = false,
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun showCreateNewForm() {
        _uiState.value = _uiState.value.copy(
            showCreateNewForm = true,
            showClassCodeInput = false,
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun resetToMainOptions() {
        _uiState.value = _uiState.value.copy(
            showClassCodeInput = false,
            showCreateNewForm = false,
            classCode = "",
            classCodeError = null,
            className = "",
            classNameError = null,
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    private fun validateClassName(className: String): String? {
        return when {
            className.isBlank() -> null // Allow empty class name, will use default
            className.length > 50 -> "Class name must be 50 characters or less"
            else -> null
        }
    }
    
    private fun validateClassCode(classCode: String): String? {
        return when {
            classCode.isBlank() -> "Class code is required"
            classCode.length < 6 -> "Class code must be at least 6 characters"
            !classCode.matches(Regex("^[A-Z0-9]+$")) -> "Class code must contain only letters and numbers"
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
    
    fun importClassByCode(
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val classCode = _uiState.value.classCode
        if (validateClassCode(classCode) != null) {
            onError("Please enter a valid class code")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // Search for class by classCode in database using optimized method
                val classData = classRepository.getClassByCode(classCode)
                
                if (classData != null) {
                    val className = classData["classTitle"] as? String ?: "Unknown Class"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Class found: $className"
                    )
                    onSuccess(classCode, className)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Class with code '$classCode' not found. Please check the code and try again."
                    )
                    onError("Class not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error searching for class: ${e.message}"
                )
                onError("Error searching for class")
            }
        }
    }
    
    /**
     * Import a class from JSON file
     * Expected JSON format:
     * {
     *     "classCode": "ABC123",
     *     "classTitle": "Mathematics 101",
     *     "educatorId": "educator_001",
     *     "studentIds": ["student1", "student2"],
     *     "updatedAt": "timestamp",
     *     "lastSyncedAt": "timestamp"
     * }
     */
    fun importClassFromJson(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                
                if (inputStream == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to read the selected file"
                    )
                    return@launch
                }
                
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.readText()
                reader.close()
                inputStream.close()
                
                // Parse JSON and validate structure
                val gson = Gson()
                val classData = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
                
                // Validate required fields
                if (!classData.containsKey("classCode") || !classData.containsKey("classTitle")) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid JSON format. Must contain 'classCode' and 'classTitle' fields."
                    )
                    return@launch
                }
                
                // Ensure all required fields are present with defaults
                val completeClassData = mutableMapOf<String, Any>().apply {
                    putAll(classData)
                    put("type", "class")
                    if (!containsKey("_id")) {
                        put("_id", classData["classCode"] as String)
                    }
                    if (!containsKey("updatedAt")) {
                        put("updatedAt", System.currentTimeMillis().toString())
                    }
                    if (!containsKey("lastSyncedAt")) {
                        put("lastSyncedAt", System.currentTimeMillis().toString())
                    }
                    if (!containsKey("educatorId")) {
                        put("educatorId", "imported_educator")
                    }
                    if (!containsKey("studentIds")) {
                        put("studentIds", emptyList<String>())
                    }
                }
                
                // Save to database
                val success = classRepository.save(completeClassData)
                
                if (success) {
                    val className = completeClassData["classTitle"] as String
                    val classCode = completeClassData["classCode"] as String
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Class '$className' imported successfully from JSON!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to save the imported class to database"
                    )
                }
                
            } catch (e: JsonSyntaxException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid JSON format. Please check your file and try again."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error importing class: ${e.message}"
                )
            }
        }
    }
}
