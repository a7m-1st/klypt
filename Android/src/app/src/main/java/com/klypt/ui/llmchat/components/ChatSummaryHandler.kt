package com.klypt.ui.llmchat.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.Model
import com.klypt.data.models.ChatSummary
import com.klypt.data.services.ChatSummaryService
import com.klypt.data.services.UserContextProvider
import com.klypt.firebaseAnalytics
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.navigation.SummaryNavigationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handler for Klypt summary functionality
 */
class ChatSummaryHandler(
    private val chatSummaryService: ChatSummaryService,
    private val userContextProvider: UserContextProvider,
    private val context: android.content.Context
) {
    suspend fun handleKlyptSummary(model: Model, messages: List<ChatMessage>) {
        try {
            Log.d("ChatSummaryHandler", "Starting Klypt summary process")
            
            val userId = userContextProvider.getCurrentUserId()
            val userRole = userContextProvider.getCurrentUserRole()
            val classCode = userContextProvider.getCurrentClassCode()
            
            Log.d("ChatSummaryHandler", "User context - ID: $userId, Role: $userRole, Class: $classCode")
            Log.d("ChatSummaryHandler", "Model: ${model.name}, Messages count: ${messages.size}")
            
            // Validate required data
            if (userId.isEmpty()) {
                Log.e("ChatSummaryHandler", "User ID is empty")
                throw IllegalStateException("User ID is required")
            }
            
            if (classCode.isEmpty()) {
                Log.e("ChatSummaryHandler", "Class code is empty")
                throw IllegalStateException("Class code is required")
            }
            
            if (messages.isEmpty()) {
                Log.e("ChatSummaryHandler", "No messages to save")
                throw IllegalStateException("Messages are required")
            }
            
            Log.d("ChatSummaryHandler", "Calling chatSummaryService.saveChatSummary...")
            val result = chatSummaryService.saveChatSummary(
                messages = messages,
                userId = userId,
                userRole = userRole,
                classCode = classCode,
                model = model,
                sessionTitle = "LLM Chat Session"
            )

            Log.d("ChatSummaryHandler", "Save result - Success: ${result.isSuccess}")
            Log.d("chatsummary", messages.toString())
            
            if (result.isSuccess) {
                Log.d("ChatSummaryHandler", "Successfully saved Klypt summary")
                android.widget.Toast.makeText(
                    context, 
                    "Klypt summary saved successfully!", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                firebaseAnalytics?.logEvent(
                    "klypt_summary_created",
                    bundleOf(
                        "model_name" to model.name,
                        "message_count" to messages.size,
                        "user_role" to userRole.name,
                        "class_code" to classCode
                    )
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("ChatSummaryHandler", "Failed to save Klypt summary: $errorMessage")
                result.exceptionOrNull()?.let { exception ->
                    Log.e("ChatSummaryHandler", "Exception details:", exception)
                }
                
                android.widget.Toast.makeText(
                    context, 
                    "Failed to save Klypt summary: $errorMessage", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("ChatSummaryHandler", "Exception in handleKlyptSummary: ${e.message}", e)
            android.widget.Toast.makeText(
                context, 
                "Error creating Klypt summary: ${e.message}", 
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * ViewModel for managing chat summary operations
 */
@HiltViewModel
class ChatSummaryViewModel @Inject constructor(
    private val chatSummaryService: ChatSummaryService,
    private val userContextProvider: UserContextProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatSummaryUiState())
    val uiState: StateFlow<ChatSummaryUiState> = _uiState.asStateFlow()
    
    fun createKlyptSummary(
        model: Model, 
        messages: List<ChatMessage>,
        context: android.content.Context,
        onSuccess: (ChatSummary) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                Log.d("ChatSummaryViewModel", "Starting Klypt summary creation via ViewModel")
                
                val userId = userContextProvider.getCurrentUserId()
                val userRole = userContextProvider.getCurrentUserRole()
                
                // Check if we have class context from navigation, otherwise use default
                val classContext = SummaryNavigationData.getClassCreationContext()
                val classCode = classContext?.classCode ?: userContextProvider.getCurrentClassCode()
                
                Log.d("ChatSummaryViewModel", "User context - ID: $userId, Role: $userRole, Class: $classCode")
                Log.d("ChatSummaryViewModel", "Class context from navigation: ${classContext != null}")
                Log.d("ChatSummaryViewModel", "Model: ${model.name}, Messages count: ${messages.size}")
                
                // Validate required data
                if (userId.isEmpty()) {
                    Log.e("ChatSummaryViewModel", "User ID is empty")
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "User ID is required")
                    onError("User ID is required")
                    return@launch
                }
                
                if (classCode.isEmpty()) {
                    Log.e("ChatSummaryViewModel", "Class code is empty")
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Class code is required")
                    onError("Class code is required")
                    return@launch
                }
                
                if (messages.isEmpty()) {
                    Log.e("ChatSummaryViewModel", "No messages to save")
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Messages are required")
                    onError("Messages are required")
                    return@launch
                }
                
                Log.d("ChatSummaryViewModel", "Calling chatSummaryService.saveChatSummary...")
                val result = chatSummaryService.saveChatSummary(
                    messages = messages,
                    userId = userId,
                    userRole = userRole,
                    classCode = classCode,
                    model = model,
                    sessionTitle = "LLM Chat Session"
                )
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
                Log.d("ChatSummaryViewModel", "Save result - Success: ${result.isSuccess}")
                
                if (result.isSuccess) {
                    Log.d("ChatSummaryViewModel", "Successfully saved Klypt summary")
                    val chatSummary = result.getOrNull()
                    if (chatSummary != null) {
                        onSuccess(chatSummary)
                    } else {
                        onError("Failed to retrieve saved summary")
                    }
                    firebaseAnalytics?.logEvent(
                        "klypt_summary_created",
                        bundleOf(
                            "model_name" to model.name,
                            "message_count" to messages.size,
                            "user_role" to userRole.name,
                            "class_code" to classCode
                        )
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("ChatSummaryViewModel", "Failed to save Klypt summary: $errorMessage")
                    result.exceptionOrNull()?.let { exception ->
                        Log.e("ChatSummaryViewModel", "Exception details:", exception)
                    }
                    _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
                    onError("Failed to save Klypt summary: $errorMessage")
                }
            } catch (e: Exception) {
                Log.e("ChatSummaryViewModel", "Exception in createKlyptSummary: ${e.message}", e)
                val errorMsg = "Error creating Klypt summary: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
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

data class ChatSummaryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
