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
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.utils.ClassCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewClassViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository
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
    
    fun dismissDuplicateDialog() {
        _uiState.value = _uiState.value.copy(
            showDuplicateDialog = false,
            duplicateClassInfo = null
        )
    }
    
    /**
     * Handle user's decision when duplicate class is found
     */
    fun handleDuplicateClass(overwrite: Boolean) {
        val duplicateInfo = _uiState.value.duplicateClassInfo
        if (duplicateInfo == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No duplicate class information found"
            )
            return
        }
        
        if (overwrite) {
            // User chose to overwrite, proceed with import
            viewModelScope.launch {
                performImport(duplicateInfo.importData, duplicateInfo.klypsData, isOverwrite = true)
            }
        }
        
        // Dismiss dialog regardless of choice
        dismissDuplicateDialog()
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
    
    private var cachedClassForSummary: Pair<String, String>? = null
    
    fun createClassForSummary(): Pair<String, String> {
        // Cache the generated class code to prevent duplicate creation on multiple calls
        if (cachedClassForSummary == null) {
            val classCode = generateClassCode()
            val className = _uiState.value.className.ifBlank { "New Class" }
            cachedClassForSummary = Pair(classCode, className)
        }
        return cachedClassForSummary!!
    }
    
    fun clearCachedClassForSummary() {
        cachedClassForSummary = null
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
     * Export a class with all its details and related klyps to JSON format
     */
    suspend fun exportClassToJson(classCode: String): String? {
        return try {
            // Get class data
            val classData = classRepository.getClassByCode(classCode)
            if (classData == null) {
                return null
            }

            // Get all klyps for this class
            val klypDataList = klypRepository.getKlypsByClassCode(classCode)
            
            // Convert klyps to export format
            val klypsForExport = klypDataList.map { klypData ->
                mapOf(
                    "_id" to (klypData["_id"] as? String ?: ""),
                    "type" to (klypData["type"] as? String ?: "klyp"),
                    "title" to (klypData["title"] as? String ?: ""),
                    "mainBody" to (klypData["mainBody"] as? String ?: ""),
                    "questions" to (klypData["questions"] as? List<*> ?: emptyList<Any>()),
                    "createdAt" to (klypData["createdAt"] as? String ?: "")
                )
            }

            // Create the complete export data structure
            val exportData = mapOf(
                "exportVersion" to "1.0",
                "exportTimestamp" to System.currentTimeMillis().toString(),
                "classDetails" to mapOf(
                    "_id" to (classData["_id"] as? String ?: ""),
                    "type" to (classData["type"] as? String ?: "class"),
                    "classCode" to (classData["classCode"] as? String ?: ""),
                    "classTitle" to (classData["classTitle"] as? String ?: ""),
                    "educatorId" to (classData["educatorId"] as? String ?: ""),
                    "studentIds" to (classData["studentIds"] as? List<*> ?: emptyList<String>()),
                    "updatedAt" to (classData["updatedAt"] as? String ?: ""),
                    "lastSyncedAt" to (classData["lastSyncedAt"] as? String ?: "")
                ),
                "klyps" to klypsForExport,
                "klypCount" to klypsForExport.size
            )

            // Convert to JSON string
            val gson = Gson()
            gson.toJson(exportData)
        } catch (e: Exception) {
            android.util.Log.e("NewClassViewModel", "Error exporting class to JSON", e)
            null
        }
    }

    /**
     * Import a class from JSON file
     * Expected JSON format:
     * {
     *     "exportVersion": "1.0", (optional)
     *     "classDetails": {
     *         "classCode": "ABC123",
     *         "classTitle": "Mathematics 101",
     *         "educatorId": "educator_001",
     *         "studentIds": ["student1", "student2"],
     *         "updatedAt": "timestamp",
     *         "lastSyncedAt": "timestamp"
     *     },
     *     "klyps": [
     *         {
     *             "title": "Klyp Title",
     *             "mainBody": "Klyp content",
     *             "questions": [...],
     *             "createdAt": "timestamp"
     *         }
     *     ]
     * }
     * 
     * Also supports legacy format:
     * {
     *     "classCode": "ABC123",
     *     "classTitle": "Mathematics 101",
     *     ...
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
                val jsonData = gson.fromJson(jsonContent, Map::class.java) as Map<String, Any>
                
                // Check if it's the new format (with exportVersion or classDetails)
                val isNewFormat = jsonData.containsKey("exportVersion") || jsonData.containsKey("classDetails")
                
                val classData: Map<String, Any>
                val klypsData: List<Map<String, Any>>
                
                if (isNewFormat) {
                    // New format with class details and klyps
                    val classDetails = jsonData["classDetails"] as? Map<String, Any>
                    if (classDetails == null || !classDetails.containsKey("classCode") || !classDetails.containsKey("classTitle")) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Invalid JSON format. Must contain 'classDetails' with 'classCode' and 'classTitle' fields."
                        )
                        return@launch
                    }
                    
                    classData = classDetails
                    klypsData = (jsonData["klyps"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                } else {
                    // Legacy format - just class data
                    if (!jsonData.containsKey("classCode") || !jsonData.containsKey("classTitle")) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Invalid JSON format. Must contain 'classCode' and 'classTitle' fields."
                        )
                        return@launch
                    }
                    
                    classData = jsonData
                    klypsData = emptyList()
                }
                
                // Check if class with same code already exists
                val classCodeToImport = classData["classCode"] as String
                val existingClass = classRepository.getClassByCode(classCodeToImport)
                
                if (existingClass != null) {
                    val existingClassName = existingClass["classTitle"] as? String ?: "Unknown Class"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDuplicateDialog = true,
                        duplicateClassInfo = DuplicateClassInfo(
                            existingClassName = existingClassName,
                            existingClassCode = classCodeToImport,
                            importData = classData,
                            klypsData = klypsData
                        ),
                        errorMessage = null
                    )
                    return@launch
                }
                
                // If no duplicate found, proceed with import
                performImport(classData, klypsData)
                
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
    
    /**
     * Internal function to perform the actual import
     */
    suspend fun performImport(
        classData: Map<String, Any>, 
        klypsData: List<Map<String, Any>>,
        isOverwrite: Boolean = false
    ) {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // If overwriting, delete existing klyps first
            if (isOverwrite) {
                val classCodeToImport = classData["classCode"] as String
                val existingKlyps = klypRepository.getKlypsByClassCode(classCodeToImport)
                existingKlyps.forEach { klypData ->
                    val klypId = klypData["_id"] as? String
                    if (klypId != null) {
                        klypRepository.delete(klypId)
                    }
                }
            }
            
            // Ensure all required fields are present with defaults for class
            val completeClassData = mutableMapOf<String, Any>().apply {
                putAll(classData)
                put("type", "class")
                if (!containsKey("_id") || isOverwrite) {
                    // Generate a unique class ID or use existing for overwrite
                    if (isOverwrite) {
                        val existingClass = classRepository.getClassByCode(classData["classCode"] as String)
                        put("_id", existingClass?.get("_id") as? String ?: "class_${java.util.UUID.randomUUID().toString().replace("-", "").take(8)}")
                    } else {
                        put("_id", "class_${java.util.UUID.randomUUID().toString().replace("-", "").take(8)}")
                    }
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
            
            // Save class to database
            val classSuccess = classRepository.save(completeClassData)
            
            if (!classSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save the imported class to database"
                )
                return
            }
            
            // Import klyps if present
            var klypSuccessCount = 0
            val classCodeForKlyps = completeClassData["classCode"] as String
            
            for (klypData in klypsData) {
                try {
                    val completeKlypData = mutableMapOf<String, Any>().apply {
                        putAll(klypData)
                        put("type", "klyp")
                        put("classCode", classCodeForKlyps) // Ensure klyp belongs to this class
                        if (!containsKey("_id")) {
                            put("_id", "klyp_${UUID.randomUUID()}")
                        }
                        if (!containsKey("createdAt")) {
                            put("createdAt", System.currentTimeMillis().toString())
                        }
                        if (!containsKey("title")) {
                            put("title", "Imported Klyp")
                        }
                        if (!containsKey("mainBody")) {
                            put("mainBody", "")
                        }
                        if (!containsKey("questions")) {
                            put("questions", emptyList<Map<String, Any>>())
                        }
                    }
                    
                    if (klypRepository.save(completeKlypData)) {
                        klypSuccessCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NewClassViewModel", "Failed to import klyp: ${e.message}")
                }
            }
            
            val className = completeClassData["classTitle"] as String
            val action = if (isOverwrite) "updated" else "imported"
            val successMessage = if (klypsData.isNotEmpty()) {
                "Class '$className' $action successfully with $klypSuccessCount/${klypsData.size} klyps!"
            } else {
                "Class '$className' $action successfully from JSON!"
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = successMessage
            )
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error importing class: ${e.message}"
            )
        }
    }
}
