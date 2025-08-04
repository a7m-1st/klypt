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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.firebaseAnalytics
import com.klypt.ui.llmchat.components.ChatSummaryViewModel
import com.klypt.ui.llmchat.components.SummaryLoadingScreen
import com.klypt.ui.common.chat.ChatMessageAudioClip
import com.klypt.ui.common.chat.ChatMessageImage
import com.klypt.ui.common.chat.ChatMessageText
import com.klypt.ui.common.chat.ChatView
import com.klypt.ui.llmchat.components.ChatSummaryUiState
import com.klypt.ui.modelmanager.ModelManagerViewModel
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

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmChatViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskImageViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskAudioViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  chatSummaryViewModel: ChatSummaryViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val summaryUiState by chatSummaryViewModel.uiState.collectAsState()
  
  // Show loading screen when generating summary
  if (summaryUiState.isLoading) {
    SummaryLoadingScreen(modifier = modifier)
    return
  }

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
        onSuccess = {
          android.widget.Toast.makeText(
            context,
            "Klypt summary saved successfully!",
            android.widget.Toast.LENGTH_SHORT
          ).show()
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
    modifier = modifier,
  )
}
