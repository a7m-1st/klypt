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

package com.klypt.ui.classes

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.KlypRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ExportUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val exportedFileUri: Uri? = null,
    val showShareDialog: Boolean = false
)

@HiltViewModel
class ClassExportViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState = _uiState.asStateFlow()
    
    /**
     * Export a class with all its details and related klyps to JSON file
     */
    fun exportClassToJson(context: Context, classCode: String, className: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        
        viewModelScope.launch {
            try {
                // Get class data
                val classData = classRepository.getClassByCode(classCode)
                if (classData == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Class not found in database"
                    )
                    return@launch
                }

                // Get all klyps for this class
                val klypDataList = klypRepository.getKlypsByClassCode(classCode)
                
                // Convert klyps to export format
                val klypsForExport = klypDataList.map { klypData ->
                    mapOf(
                        "_id" to (klypData["_id"]?.toString() ?: ""),
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
                        "_id" to (classData["_id"]?.toString() ?: ""),
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
                val jsonString = gson.toJson(exportData)
                
                // Save to Downloads folder
                val fileName = "${classCode}_${className.replace(" ", "_")}_export.json"
                val fileUri = saveJsonToFile(context, fileName, jsonString)
                
                if (fileUri != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Class exported successfully to Downloads/$fileName",
                        exportedFileUri = fileUri,
                        showShareDialog = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to save export file"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ClassExportViewModel", "Error exporting class to JSON", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error exporting class: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Save JSON content to a file in Downloads folder
     */
    private suspend fun saveJsonToFile(context: Context, fileName: String, jsonContent: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray())
                    outputStream.flush()
                }
                fileUri
            }
        } catch (e: IOException) {
            android.util.Log.e("ClassExportViewModel", "Failed to save file", e)
            null
        }
    }
    
    /**
     * Create and launch a share intent for the exported JSON file
     */
    fun shareExportedFile(context: Context) {
        val fileUri = _uiState.value.exportedFileUri
        if (fileUri != null) {
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Class Export - JSON File")
                    putExtra(Intent.EXTRA_TEXT, "Sharing exported class data in JSON format.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooserIntent = Intent.createChooser(shareIntent, "Share Class Export")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
                
                // Dismiss share dialog after launching
                dismissShareDialog()
                
            } catch (e: Exception) {
                android.util.Log.e("ClassExportViewModel", "Error sharing file", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error sharing file: ${e.message}",
                    showShareDialog = false
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No file available to share",
                showShareDialog = false
            )
        }
    }
    
    /**
     * Dismiss the share dialog
     */
    fun dismissShareDialog() {
        _uiState.value = _uiState.value.copy(
            showShareDialog = false,
            exportedFileUri = null
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            showShareDialog = false,
            exportedFileUri = null
        )
    }
}
