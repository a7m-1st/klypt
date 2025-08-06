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

package com.klypt.ui.llmchat

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.firebaseAnalytics
import com.klypt.data.Model
import com.klypt.data.models.ChatSummary
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.common.chat.ChatMessageType
import com.klypt.ui.llmchat.components.ChatSummaryViewModel
import com.klypt.ui.llmchat.components.SummaryLoadingScreen
import com.klypt.ui.common.chat.ChatMessageAudioClip
import com.klypt.ui.common.chat.ChatMessageImage
import com.klypt.ui.common.chat.ChatMessageText
import com.klypt.ui.common.chat.ChatSide
import com.klypt.ui.common.chat.ChatView
import com.klypt.ui.llmchat.components.ChatSummaryUiState
import com.klypt.ui.modelmanager.ModelManagerViewModel
import com.klypt.ui.navigation.SummaryNavigationData
import kotlinx.coroutines.launch

/** Navigation destination data */
object LlmChatDestination {
  val route = "LlmChatRoute"
}

object LlmAskImageDestination {
  val route = "LlmAskImageRoute"
}

object LlmAskAudioDestination {
  val route = "LlmAskAudioRoute"
}

object SummaryReviewDestination {
  val route = "SummaryReviewRoute"
}

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onNavigateToSummaryReview: (ChatSummary, Model, List<ChatMessage>) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
  viewModel: LlmChatViewModel,
  initialContent: String = "",
  classCode: String = "",
  className: String = ""
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    onNavigateToSummaryReview = onNavigateToSummaryReview,
    modifier = modifier,
    initialContent = initialContent,
    classCode = classCode,
    className = className
  )
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onNavigateToSummaryReview: (ChatSummary, Model, List<ChatMessage>) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
  viewModel: LlmAskImageViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    onNavigateToSummaryReview = onNavigateToSummaryReview,
    modifier = modifier,
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onNavigateToSummaryReview: (ChatSummary, Model, List<ChatMessage>) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
  viewModel: LlmAskAudioViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    onNavigateToSummaryReview = onNavigateToSummaryReview,
    modifier = modifier,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onNavigateToSummaryReview: (ChatSummary, Model, List<ChatMessage>) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
  chatSummaryViewModel: ChatSummaryViewModel = hiltViewModel(),
  initialContent: String = "",
  classCode: String = "", // Add class context parameters
  className: String = ""
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val summaryUiState by chatSummaryViewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  
  // Keep track of which models have received the initial content
  val modelsWithInitialContent = remember { mutableSetOf<String>() }
  
  // Set class context if provided
  LaunchedEffect(classCode, className) {
    if (classCode.isNotEmpty()) {
      // Determine if this is for creating a new class or adding to existing class
      // If className is "New Klyp", it means we're adding content to an existing class
      val isNewClassCreation = className != "New Klyp"
      val classContext = SummaryNavigationData.ClassCreationContext(classCode, className, isNewClassCreation)
      android.util.Log.e("DEBUG_NAV", "Setting class context: classCode=$classCode, className=$className, isNewClassCreation=$isNewClassCreation")
      SummaryNavigationData.setClassCreationContext(classContext)
    }
  }
  
  // Add initial content as a user message if provided, and apply to newly selected models
  LaunchedEffect(initialContent, modelManagerUiState.selectedModel.name) {
    if (initialContent.isNotEmpty()) {
      val selectedModel = modelManagerUiState.selectedModel
      
      // Only add initial content if this model hasn't received it yet
      if (!modelsWithInitialContent.contains(selectedModel.name)) {
        // Check if this model already has messages (excluding prompt template messages)
        val existingMessages = viewModel.uiState.value.messagesByModel[selectedModel.name] ?: emptyList()
        val hasNonTemplateMessages = existingMessages.any { it.type != ChatMessageType.PROMPT_TEMPLATES }
        
        if (!hasNonTemplateMessages) {
          // Add the initial content as a user message
          viewModel.addMessage(
            model = selectedModel,
            message = ChatMessageText(content = initialContent, side = ChatSide.USER)
          )
          
          // Show a toast to indicate content was transferred
          Toast.makeText(context, "Content transferred to chat. Ready for discussion!", Toast.LENGTH_SHORT).show()
          
          // Automatically generate a response to acknowledge the context
          scope.launch {
            // Small delay to ensure the message is added properly
            kotlinx.coroutines.delay(100)
            
            // Generate a response to acknowledge the transferred content
            viewModel.generateResponse(
              model = selectedModel,
              input = initialContent,
              onError = {
                // Handle error silently or show a message
                Toast.makeText(context, "Failed to process initial context", Toast.LENGTH_SHORT).show()
              }
            )
          }
          
          modelsWithInitialContent.add(selectedModel.name)
        }
      }
    }
  }
  
  // Show loading screen when generating summary
  if (summaryUiState.isLoading) {
    SummaryLoadingScreen(modifier = modifier)
    return
  }

  Column(modifier = modifier) {
    // Class Context Indicator
    val classContext = SummaryNavigationData.getClassCreationContext()
    //TODO(): Figure out a clean way to show this
//    if (classContext != null) {
//      ClassContextIndicator(
//        classCode = classContext.classCode,
//        className = classContext.className,
//        modifier = Modifier.fillMaxWidth()
//      )
//    }

    ChatView(
      task = viewModel.task,
      viewModel = viewModel,
      modelManagerViewModel = modelManagerViewModel,
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        if (message is ChatMessageText) {
          chatMessageText = message
          text = message.content
        } else if (message is ChatMessageImage) {
          images.add(message.bitmap)
        } else if (message is ChatMessageAudioClip) {
          audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        modelManagerViewModel.addTextInputHistory(text)
        viewModel.generateResponse(
          model = model,
          input = text,
          images = images,
          audioMessages = audioMessages,
          onError = {
            viewModel.handleError(
              context = context,
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              triggeredMessage = chatMessageText,
            )
          },
        )

        firebaseAnalytics?.logEvent(
          "generate_action",
          bundleOf("capability_name" to viewModel.task.type.toString(), "model_id" to model.name),
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = {
            viewModel.handleError(
              context = context,
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              triggeredMessage = message,
            )
          },
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model -> viewModel.resetSession(model = model) },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },

    onKlyptSummaryClicked = { model, messages ->
      chatSummaryViewModel.createKlyptSummary(
        model = model,
        messages = messages,
        context = context,
        onSuccess = { chatSummary ->
          // Navigate to summary review screen
          onNavigateToSummaryReview(chatSummary, model, messages)
        },
        onError = { errorMessage ->
          android.widget.Toast.makeText(
            context,
            errorMessage,
            android.widget.Toast.LENGTH_LONG
          ).show()
        }
      )
    },
    navigateUp = navigateUp,
    modifier = Modifier.weight(1f),
  )
  }
}

@Composable
private fun ClassContextIndicator(
  classCode: String,
  className: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        Icons.Default.Class,
        contentDescription = "Class",
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(20.dp)
      )
      
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Working in Class: $classCode",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
          ),
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (className != "New Klyp") {
          Text(
            text = className,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
        } else {
          Text(
            text = "Klypt will be saved here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          )
        }
      }
    }
  }
}
