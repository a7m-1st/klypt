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

package com.klypt.ui.klypdetails

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.TASK_LLM_CHAT
import com.klypt.data.Model
import com.klypt.data.models.Klyp
import com.klypt.ui.common.EnhancedMarkdownText
import com.klypt.ui.modelmanager.ModelManagerViewModel
import com.klypt.ui.common.humanReadableSize
import com.klypt.ui.common.modelitem.StatusIcon

private const val TAG = "KlypDetailsScreen"

/** Navigation destination data */
object KlypDetailsDestination {
    val route = "KlypDetailsRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlypDetailsScreen(
    klyp: Klyp,
    onNavigateBack: () -> Unit,
    onNavigateToLLMChat: (String, String, String, String) -> Unit, // classCode, title, content, modelName
    onNavigateToQuiz: (Klyp) -> Unit,
    onNavigateToQuizEditor: (Klyp, Model) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KlypDetailsViewModel = hiltViewModel(),
    modelManagerViewModel: ModelManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showQuizGenerationModelDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) } // Track if we're in edit mode or auto-generation mode
    var showActionsBottomSheet by remember { mutableStateOf(false) }
    
    // Initialize the view model with the klyp data
    LaunchedEffect(klyp) {
        Log.d(TAG, "Initializing KlypDetailsScreen with klyp: ${klyp.title} (ID: ${klyp._id})")
        viewModel.initializeWithKlyp(klyp)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = klyp.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigate back clicked from KlypDetailsScreen")
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discuss in Chat FAB
                FloatingActionButton(
                    onClick = {
                        Log.d(TAG, "Transfer to Chat clicked for klyp: ${klyp.title}")
                        val contextContent = "Please use the following content as context for our discussion:\n\n**Title:** ${klyp.title}\n\n**Content:**\n${klyp.mainBody}\n\n---\n\nI'd like to discuss this content with you. Please let me know that you've received this context and are ready to discuss it."
                        onNavigateToLLMChat(klyp.classCode, klyp.title, contextContent, "")
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Discuss in Chat")
                }
                
                // Actions FAB
                FloatingActionButton(
                    onClick = { showActionsBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Actions")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isInitializingModel) {
                QuizLoadingScreen(
                    isInitializingModel = true,
                    showStopButton = uiState.showStopButton,
                    elapsedTimeMs = uiState.modelInitElapsedTime,
                    onStopClicked = if (uiState.showStopButton) {
                        {
                            Log.d(TAG, "Stop model initialization clicked")
                            viewModel.stopModelInitialization()
                        }
                    } else null
                )
            } else {
                // Main content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Klyp Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Class Code: ${klyp.classCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (klyp.questions.isNotEmpty()) {
                                Text(
                                    text = "Questions: ${klyp.questions.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "Created: ${klyp.createdAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Content Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Content",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Content is read-only
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                EnhancedMarkdownText(
                                    markdown = klyp.mainBody,
                                    modifier = Modifier.padding(16.dp),
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16f
                                )
                            }
                        }
                    }

                    // Questions Preview Card (if questions exist)
                    if (klyp.questions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Questions Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "This Klyp contains ${klyp.questions.size} question(s). You can play the quiz to test your knowledge or transfer the content to chat for further discussion.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Status messages
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                if (uiState.isGeneratingQuiz) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (klyp.questions.isEmpty()) {
                                    "Generating quiz questions using AI..."
                                } else {
                                    "Regenerating quiz questions with new AI content..."
                                },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Actions Bottom Sheet
    if (showActionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionsBottomSheet = false }
        ) {
            ActionsBottomSheetContent(
                klyp = klyp,
                uiState = uiState,
                context = context,
                onNavigateToLLMChat = onNavigateToLLMChat,
                onNavigateToQuiz = onNavigateToQuiz,
                onShowQuizGenerationDialog = { editMode ->
                    isEditMode = editMode
                    showQuizGenerationModelDialog = true
                    showActionsBottomSheet = false
                },
                viewModel = viewModel,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
    
    // Quiz Generation Model Selection Dialog
    if (showQuizGenerationModelDialog) {
        QuizGenerationModelSelectionDialog(
            modelManagerViewModel = modelManagerViewModel,
            klypTitle = klyp.title,
            isEditMode = isEditMode,
            onModelSelected = { selectedModel ->
                Log.d(TAG, "Model selected for quiz ${if (isEditMode) "editing" else "generation"}: ${selectedModel.name}")
                showQuizGenerationModelDialog = false
                
                if (isEditMode) {
                    // Navigate to quiz editor
                    onNavigateToQuizEditor(klyp, selectedModel)
                } else {
                    // Start quiz generation with selected model
                    viewModel.generateQuizQuestions(
                        klyp = klyp,
                        context = context,
                        onSuccess = { updatedKlyp ->
                            Log.d(TAG, "Quiz generation successful, navigating to quiz")
                            onNavigateToQuiz(updatedKlyp)
                        },
                        onError = { error ->
                            Log.e(TAG, "Quiz generation failed: $error")
                            // Error handling is done in ViewModel
                        }
                    )
                }
            },
            onDismiss = {
                Log.d(TAG, "Quiz generation model selection dismissed")
                showQuizGenerationModelDialog = false
            }
        )
    }
}

@Composable
private fun ActionsBottomSheetContent(
    klyp: Klyp,
    uiState: KlypDetailsUiState,
    context: android.content.Context,
    onNavigateToLLMChat: (String, String, String, String) -> Unit,
    onNavigateToQuiz: (Klyp) -> Unit,
    onShowQuizGenerationDialog: (Boolean) -> Unit,
    viewModel: KlypDetailsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Actions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Add New Klyp button
        ElevatedCard(
            onClick = {
                Log.d(TAG, "Add Klyp clicked from KlypDetailsScreen for class: ${klyp.classCode}")
                // Navigate to LLM Chat for klyp creation with class context
                onNavigateToLLMChat(klyp.classCode, "New Klyp", "", "")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Klyp",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Add Klyp",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Create new educational content for this class",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quiz Actions
        if (!klyp.questions.isEmpty()) {
            // Play Quiz
            ElevatedCard(
                onClick = {
                    Log.d(TAG, "Play Quiz clicked for klyp: ${klyp.title}")
                    onNavigateToQuiz(klyp)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play Quiz",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Play Quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${klyp.questions.size} questions available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Regenerate Quiz
            ElevatedCard(
                onClick = {
                    Log.d(TAG, "Regenerate Quiz clicked for klyp: ${klyp.title}")
                    if (uiState.isGeneratingQuiz) {
                        Log.w(TAG, "Quiz regeneration already in progress, ignoring click")
                        return@ElevatedCard
                    }

                    viewModel.regenerateQuizQuestions(
                        klyp = klyp,
                        context = context,
                        onSuccess = { updatedKlyp ->
                            Log.d(TAG, "Quiz regeneration successful")
                        },
                        onError = { error ->
                            Log.e(TAG, "Quiz regeneration failed: $error")
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isGeneratingQuiz) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Regenerate Quiz",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = if (uiState.isGeneratingQuiz) "Regenerating..." else "Regenerate Quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Generate new questions with AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit Quiz Questions
            ElevatedCard(
                onClick = {
                    Log.d(TAG, "Edit Quiz clicked for klyp: ${klyp.title}")
                    onShowQuizGenerationDialog(true)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Edit Quiz",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "New Quiz Questions",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Modify existing questions manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Auto-Generate Quiz
            ElevatedCard(
                onClick = {
                    Log.d(TAG, "Generate Quiz clicked for klyp: ${klyp.title}")
                    if (uiState.isGeneratingQuiz) {
                        Log.w(TAG, "Quiz generation already in progress, ignoring click")
                        return@ElevatedCard
                    }
                    onShowQuizGenerationDialog(false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Generate Quiz",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Auto-Generate Quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "AI will create quiz questions automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Create/Edit Quiz
            ElevatedCard(
                onClick = {
                    Log.d(TAG, "Edit Quiz clicked for klyp: ${klyp.title}")
                    onShowQuizGenerationDialog(true)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Create Quiz",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Create/Edit Quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Create questions manually with AI assistance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizGenerationModelSelectionDialog(
    modelManagerViewModel: ModelManagerViewModel,
    klypTitle: String,
    isEditMode: Boolean,
    onModelSelected: (Model) -> Unit,
    onDismiss: () -> Unit
) {
    val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
    val availableModels = com.klypt.data.TASK_LLM_CHAT.models
    var selectedModel by remember { mutableStateOf<Model?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose AI Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEditMode) 
                                "for editing \"$klypTitle\" quiz" 
                            else 
                                "for generating \"$klypTitle\" quiz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Select which AI model to use for generating quiz questions from the content. More powerful models may create better questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // Model List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableModels) { model ->
                        QuizGenerationModelSelectionItem(
                            model = model,
                            isSelected = selectedModel == model,
                            downloadStatus = modelManagerUiState.modelDownloadStatus[model.name],
                            onSelect = { selectedModel = model }
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            selectedModel?.let { model ->
                                onModelSelected(model)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedModel != null
                    ) {
                        Text("Generate Quiz")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizGenerationModelSelectionItem(
    model: Model,
    isSelected: Boolean,
    downloadStatus: com.klypt.data.ModelDownloadStatus?,
    onSelect: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
            
            // Model info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIcon(downloadStatus = downloadStatus)
                    
                    Text(
                        text = model.sizeInBytes.humanReadableSize(),
                        style = com.klypt.ui.theme.labelSmallNarrow.copy(
                            color = contentColor.copy(alpha = 0.7f),
                            lineHeight = 12.sp
                        )
                    )
                }
                
                if (model.info.isNotEmpty()) {
                    Text(
                        text = model.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
