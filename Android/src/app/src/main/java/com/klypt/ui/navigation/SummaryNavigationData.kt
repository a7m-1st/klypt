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

package com.klypt.ui.navigation

import com.klypt.data.Model
import com.klypt.data.models.ChatSummary
import com.klypt.ui.common.chat.ChatMessage

/**
 * Temporary data holder for passing complex objects between navigation destinations
 * This is a simple solution for the navigation limitation with complex objects
 */
object SummaryNavigationData {
    
    private var currentSummary: ChatSummary? = null
    private var currentModel: Model? = null
    private var currentMessages: List<ChatMessage> = emptyList()
    
    fun storeSummaryData(summary: ChatSummary, model: Model, messages: List<ChatMessage>) {
        currentSummary = summary
        currentModel = model
        currentMessages = messages
    }
    
    fun getSummaryData(): Triple<ChatSummary?, Model?, List<ChatMessage>> {
        return Triple(currentSummary, currentModel, currentMessages)
    }
    
    fun clearSummaryData() {
        currentSummary = null
        currentModel = null 
        currentMessages = emptyList()
    }
}
