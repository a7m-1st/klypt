package com.klypt.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.database.UserData
import com.klypt.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouchDBDemoViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CouchDBDemoUiState())
    val uiState: StateFlow<CouchDBDemoUiState> = _uiState.asStateFlow()
    
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = userRepository.getAllUsers()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = result.getOrNull() ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to load users"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun clearAllUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val users = _uiState.value.users
                var deletedCount = 0
                
                users.forEach { userData ->
                    val deleteResult = userRepository.deleteUser(userData.firstName, userData.lastName)
                    if (deleteResult.isSuccess && deleteResult.getOrNull() == true) {
                        deletedCount++
                    }
                }
                
                // Reload users after deletion
                loadUsers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to clear users"
                )
            }
        }
    }
}

data class CouchDBDemoUiState(
    val isLoading: Boolean = false,
    val users: List<UserData> = emptyList(),
    val errorMessage: String? = null
)
