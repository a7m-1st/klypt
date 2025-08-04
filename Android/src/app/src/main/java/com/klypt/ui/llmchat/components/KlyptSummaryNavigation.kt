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

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.Model
import com.klypt.data.models.ChatSummary
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.llmchat.*
import com.klypt.ui.modelmanager.ModelManagerViewModel

/**
 * Navigation manager for Klypt summary flows
 * This demonstrates how to integrate the summary review screen with the chat flow
 */
@Composable
fun KlyptSummaryNavigation(
    modelManagerViewModel: ModelManagerViewModel,
    navigateUp: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(KlyptScreen.Chat) }
    var currentSummary by remember { mutableStateOf<ChatSummary?>(null) }
    var currentModel by remember { mutableStateOf<Model?>(null) }
    var currentMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    when (currentScreen) {
        KlyptScreen.Chat -> {
            LlmChatScreen(
                modelManagerViewModel = modelManagerViewModel,
                navigateUp = navigateUp,
                onNavigateToSummaryReview = { summary, model, messages ->
                    currentSummary = summary
                    currentModel = model
                    currentMessages = messages
                    currentScreen = KlyptScreen.SummaryReview
                },
                viewModel = hiltViewModel()
            )
        }
        
        KlyptScreen.SummaryReview -> {
            val summary = currentSummary
            val model = currentModel
            val messages = currentMessages
            
            if (summary != null && model != null) {
                SummaryReviewScreen(
                    summary = summary,
                    model = model,
                    messages = messages,
                    onNavigateBack = {
                        currentScreen = KlyptScreen.Chat
                    },
                    onSaveComplete = {
                        // Navigate back to chat after successful save
                        currentScreen = KlyptScreen.Chat
                    }
                )
            }
        }
    }
}

enum class KlyptScreen {
    Chat,
    SummaryReview
}

/**
 * Alternative approach using a state holder for complex navigation
 */
@Composable
fun rememberKlyptSummaryNavigationState(): KlyptSummaryNavigationState {
    return remember { KlyptSummaryNavigationState() }
}

class KlyptSummaryNavigationState {
    var currentScreen by mutableStateOf(KlyptScreen.Chat)
        private set
    
    var currentSummary by mutableStateOf<ChatSummary?>(null)
        private set
    
    var currentModel by mutableStateOf<Model?>(null)
        private set
    
    var currentMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    fun navigateToSummaryReview(summary: ChatSummary, model: Model, messages: List<ChatMessage>) {
        currentSummary = summary
        currentModel = model
        currentMessages = messages
        currentScreen = KlyptScreen.SummaryReview
    }

    fun navigateToChat() {
        currentScreen = KlyptScreen.Chat
    }
}
