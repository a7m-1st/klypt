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

package com.klypt.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.GalleryTopAppBar
import com.klypt.R
import com.klypt.data.AppBarAction
import com.klypt.data.AppBarActionType
import com.klypt.data.Task
import com.klypt.data.TaskType
import com.klypt.data.UserRole
import com.klypt.data.DummyDataGenerator
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Klyp
import com.klypt.ui.common.TaskIcon
import com.klypt.ui.common.tos.TosDialog
import com.klypt.ui.common.tos.TosViewModel
import com.klypt.ui.modelmanager.ModelManagerViewModel
import com.klypt.ui.navigation.SummaryNavigationData
import com.klypt.ui.theme.customColors
import kotlinx.coroutines.delay

private const val TASK_COUNT_ANIMATION_DURATION = 250

/**
 * Enhanced Home Screen that displays educational content based on user role
 * Now fetches actual data from the database using UserContextProvider and EducationalContentRepository
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHomeScreen(
    modelManagerViewModel: ModelManagerViewModel,
    tosViewModel: TosViewModel,
    navigateToTaskScreen: (Task) -> Unit,
    onNavigateToNewClass: () -> Unit,
    modifier: Modifier = Modifier,
    homeContentViewModel: HomeContentViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
    val homeUiState by homeContentViewModel.uiState.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Check for refresh flag from navigation and force refresh if needed
    LaunchedEffect(Unit) {
        if (SummaryNavigationData.getShouldRefreshHome()) {
            // Clear the flag and force a complete refresh
            SummaryNavigationData.setShouldRefreshHome(false)
            homeContentViewModel.refresh()
        }
    }

    // Refresh data when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Refresh data when coming back to home screen
                homeContentViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show error message if any
    LaunchedEffect(homeUiState.errorMessage) {
        homeUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            homeContentViewModel.clearError()
        }
    }

    AnimatedVisibility(visible = !showTosDialog, enter = fadeIn()) {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                GalleryTopAppBar(
                    title = stringResource(R.string.app_name),
                    rightAction = AppBarAction(
                        actionType = AppBarActionType.APP_SETTING,
                        actionFn = { showSettingsDialog = true },
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                // Navigate to LLM Chat for quick AI assistance
                ExtendedFloatingActionButton(
                    onClick = { 
                        // Navigate to LLM Chat with the first available model
                        val llmChatTask = modelManagerUiState.tasks.find { it.type == TaskType.LLM_CHAT }
                        if (llmChatTask != null && llmChatTask.models.isNotEmpty()) {
                            navigateToTaskScreen(llmChatTask)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = "AI Chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI")
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(innerPadding)
            ) {
                if (homeUiState.isLoading) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading your content...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    // Main content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // User welcome section
                        item {
                            UserWelcomeSection(
                                currentUser = homeUiState.currentUser,
                                userRole = homeUiState.userRole
                            )
                        }

                        // Quick stats
                        item {
                            QuickStatsSection(
                                stats = when (homeUiState.userRole) {
                                    UserRole.STUDENT -> mapOf(
                                        "totalClasses" to homeUiState.myClasses.size,
                                        "totalKlyps" to homeUiState.recentKlyps.size,
                                        "upcomingAssignments" to homeUiState.upcomingAssignments.size
                                    )
                                    UserRole.EDUCATOR -> homeUiState.classStatistics
                                },
                                userRole = homeUiState.userRole
                            )
                        }

                        // My Classes section
                        item {
                            MyClassesSection(
                                classes = homeUiState.myClasses,
                                onClassClick = { classDoc ->
                                    // Navigate to class details
                                },
                                onAddNewClassClick = onNavigateToNewClass
                            )
                        }

                        // Recent Klyps/Content section
                        item {
                            RecentKlypsSection(
                                klyps = homeUiState.recentKlyps,
                                onKlypClick = { klyp ->
                                    // Navigate to Klyp details
                                }
                            )
                        }

                        // Assignments section (for students)
                        if (homeUiState.userRole == UserRole.STUDENT) {
                            item {
                                UpcomingAssignmentsSection(
                                    assignments = homeUiState.upcomingAssignments
                                )
                            }
                        }

                        // AI Features section (existing task list)
                        item {
                            AIFeaturesSection(
                                tasks = modelManagerUiState.tasks,
                                navigateToTaskScreen = navigateToTaskScreen,
                                loadingModelAllowlist = modelManagerUiState.loadingModelAllowlist
                            )
                        }
                    }
                }

                // Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    // TOS Dialog
    if (showTosDialog) {
        TosDialog(
            onTosAccepted = {
                showTosDialog = false
                tosViewModel.acceptTos()
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            curThemeOverride = modelManagerViewModel.readThemeOverride(),
            modelManagerViewModel = modelManagerViewModel,
            onDismissed = { showSettingsDialog = false },
        )
    }
}

/**
 * AI Features section that wraps the existing task list
 */
@Composable
private fun AIFeaturesSection(
    tasks: List<Task>,
    navigateToTaskScreen: (Task) -> Unit,
    loadingModelAllowlist: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI-Powered Features",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            )
        }
        
        TaskList(
            tasks = tasks,
            navigateToTaskScreen = navigateToTaskScreen,
            loadingModelAllowlist = loadingModelAllowlist
        )
    }
}

/**
 * Task list component that displays available AI tasks
 */
@Composable
internal fun TaskList(
  tasks: List<Task>,
  navigateToTaskScreen: (Task) -> Unit,
  loadingModelAllowlist: Boolean,
) {
  // Label to show when in the process of loading model allowlist.
  if (loadingModelAllowlist) {
    Row(
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    ) {
      CircularProgressIndicator(
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 3.dp,
        modifier = Modifier.padding(end = 8.dp).size(20.dp),
      )
      Text(stringResource(R.string.loading_model_list), style = MaterialTheme.typography.bodyMedium)
    }
  }
  // Model list.
  else {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      for (task in tasks) {
        // Skip audio task for now.
        if (task.type != TaskType.LLM_ASK_AUDIO) {
          TaskCard(
            task = task,
            onClick = { navigateToTaskScreen(task) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

/**
 * Task card component that displays individual AI task information
 */
@Composable
private fun TaskCard(task: Task, onClick: () -> Unit, modifier: Modifier = Modifier) {
  // Observes the model count and updates the model count label with a fade-in/fade-out animation
  // whenever the count changes.
  val modelCount by remember {
    derivedStateOf {
      val trigger = task.updateTrigger.value
      if (trigger >= 0) {
        task.models.size
      } else {
        0
      }
    }
  }
  val modelCountLabel by remember {
    derivedStateOf {
      when (modelCount) {
        1 -> "1 Model"
        else -> "%d Models".format(modelCount)
      }
    }
  }
  var curModelCountLabel by remember { mutableStateOf("") }
  var modelCountLabelVisible by remember { mutableStateOf(true) }

  LaunchedEffect(modelCountLabel) {
    if (curModelCountLabel.isEmpty()) {
      curModelCountLabel = modelCountLabel
    } else {
      modelCountLabelVisible = false
      delay(TASK_COUNT_ANIMATION_DURATION.toLong())
      curModelCountLabel = modelCountLabel
      modelCountLabelVisible = true
    }
  }

  Card(
    modifier = modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.customColors.taskCardBgColor),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Title and model count
      Column {
        Text(
          task.type.label,
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          curModelCountLabel,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // Icon.
      TaskIcon(task = task, width = 40.dp)
    }
  }
}
