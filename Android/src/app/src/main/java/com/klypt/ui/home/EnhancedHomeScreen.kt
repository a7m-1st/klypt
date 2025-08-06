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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
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
import com.klypt.ui.classes.ClassExportViewModel
import com.klypt.ui.common.TaskIcon
import com.klypt.ui.common.tos.TosDialog
import com.klypt.ui.common.tos.TosViewModel
import com.klypt.ui.debug.DebugMenuButton
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
    onNavigateToViewAllClasses: () -> Unit = {},
    onNavigateToClassDetails: (ClassDocument) -> Unit = {},
    onNavigateToKlypDetails: (Klyp) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeContentViewModel: HomeContentViewModel = hiltViewModel(),
    exportViewModel: ClassExportViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
    val homeUiState by homeContentViewModel.uiState.collectAsState()
    val exportUiState by exportViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Show export messages
    LaunchedEffect(exportUiState.successMessage) {
        exportUiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            exportViewModel.clearMessages()
        }
    }
    
    LaunchedEffect(exportUiState.errorMessage) {
        exportUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar("Error: $message")
            exportViewModel.clearMessages()
        }
    }

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
                // Column with two floating action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Camera button for LLM with Image
                    FloatingActionButton(
                        onClick = { 
                            // Navigate to LLM ASK IMAGE task
                            val llmImageTask = modelManagerUiState.tasks.find { it.type == TaskType.LLM_ASK_IMAGE }
                            if (llmImageTask != null && llmImageTask.models.isNotEmpty()) {
                                navigateToTaskScreen(llmImageTask)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "AI Camera")
                    }
                    
                    // Chat button for LLM Chat (existing functionality)
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
                        Text("Klypt AI")
                    }
                }
            },
            bottomBar = {
                // Only show bottom navigation for students
                if (homeUiState.userRole == UserRole.STUDENT) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "Stats") },
                            label = { Text("Stats") },
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements") },
                            label = { Text("Achievements") },
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 }
                        )
                    }
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
                    // Main content - show different content based on selected tab
                    when {
                        homeUiState.userRole == UserRole.STUDENT -> {
                            when (selectedTabIndex) {
                                0 -> HomeTabContent(
                                    homeUiState = homeUiState,
                                    onNavigateToClassDetails = onNavigateToClassDetails,
                                    onNavigateToNewClass = onNavigateToNewClass,
                                    onNavigateToViewAllClasses = onNavigateToViewAllClasses,
                                    onNavigateToKlypDetails = onNavigateToKlypDetails,
                                    onExportClass = { classDocument ->
                                        exportViewModel.exportClassToJson(
                                            context = context,
                                            classCode = classDocument.classCode,
                                            className = classDocument.classTitle
                                        )
                                    }
                                )
                                1 -> StatsTabContent(
                                    homeUiState = homeUiState
                                )
                                2 -> AchievementsTabContent(
                                    homeUiState = homeUiState
                                )
                            }
                        }
                        else -> {
                            // Educator content
                            EducatorHomeContent(
                                homeUiState = homeUiState,
                                onNavigateToClassDetails = onNavigateToClassDetails,
                                onNavigateToNewClass = onNavigateToNewClass,
                                onNavigateToViewAllClasses = onNavigateToViewAllClasses,
                                onNavigateToKlypDetails = onNavigateToKlypDetails,
                                onExportClass = { classDocument ->
                                    exportViewModel.exportClassToJson(
                                        context = context,
                                        classCode = classDocument.classCode,
                                        className = classDocument.classTitle
                                    )
                                }
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
            onLogout = {
                // Call the logout function from HomeContentViewModel
                homeContentViewModel.logout()
                // Navigate to login
                onLogout()
            }
        )
    }

    // Share dialog after successful export
    if (exportUiState.showShareDialog) {
        AlertDialog(
            onDismissRequest = { exportViewModel.dismissShareDialog() },
            title = { Text("Export Successful") },
            text = { Text("Your class has been exported to JSON format. Would you like to share the file?") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        exportViewModel.shareExportedFile(context)
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportViewModel.dismissShareDialog() }) {
                    Text("Done")
                }
            }
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

/**
 * Home tab content - shows overview and recent activity
 */
@Composable
private fun HomeTabContent(
    homeUiState: HomeUiState,
    onNavigateToClassDetails: (ClassDocument) -> Unit,
    onNavigateToNewClass: () -> Unit,
    onNavigateToViewAllClasses: () -> Unit,
    onNavigateToKlypDetails: (Klyp) -> Unit,
    onExportClass: ((ClassDocument) -> Unit)? = null
) {
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

        // Quick overview stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(
                    title = "Level",
                    value = homeUiState.gameStats.level.toString(),
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
                OverviewCard(
                    title = "Streak",
                    value = "${homeUiState.gameStats.currentStreak}d",
                    icon = Icons.Default.Analytics,
                    modifier = Modifier.weight(1f)
                )
                OverviewCard(
                    title = "Score",
                    value = "${(homeUiState.quizStats.averageScore * 100).toInt()}%",
                    icon = Icons.Default.Analytics,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // My Classes section
        item {
            MyClassesSection(
                classes = homeUiState.myClasses,
                onClassClick = onNavigateToClassDetails,
                onAddNewClassClick = onNavigateToNewClass,
                onViewAllClick = onNavigateToViewAllClasses,
                onExportClass = onExportClass
            )
        }

        // Recent Klyps/Content section
        item {
            RecentKlypsSection(
                klyps = homeUiState.recentKlyps,
                onKlypClick = onNavigateToKlypDetails
            )
        }

        // Assignments section
        item {
            UpcomingAssignmentsSection(
                assignments = homeUiState.upcomingAssignments
            )
        }
    }
}

/**
 * Stats tab content - shows detailed performance analytics
 */
@Composable
private fun StatsTabContent(
    homeUiState: HomeUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(all = 16.dp)
    ) {
        item {
            Text(
                text = "Performance Analytics",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Level and XP Progress
        item {
            PlayerLevelCard(gameStats = homeUiState.gameStats)
        }

        // Quick Game Stats
        item {
            QuickGameStats(gameStats = homeUiState.gameStats, quizStats = homeUiState.quizStats)
        }

        // Quiz Performance Chart
        item {
            QuizPerformanceCard(quizStats = homeUiState.quizStats)
        }

        // Subject Mastery
        if (homeUiState.quizStats.subjectBreakdown.isNotEmpty()) {
            item {
                SubjectMasterySection(subjectStats = homeUiState.quizStats.subjectBreakdown)
            }
        }

        // Weekly Goal Progress
        item {
            WeeklyGoalCard(gameStats = homeUiState.gameStats)
        }

        // Study Streak Visualization
        item {
            StudyStreakVisualization(gameStats = homeUiState.gameStats)
        }
    }
}

/**
 * Achievements tab content - shows badges, achievements, and motivational content
 */
@Composable
private fun AchievementsTabContent(
    homeUiState: HomeUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(all = 16.dp)
    ) {
        item {
            Text(
                text = "Achievements & Rewards",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Achievements Section
        if (homeUiState.gameStats.achievements.isNotEmpty()) {
            item {
                AchievementsSection(achievements = homeUiState.gameStats.achievements)
            }
        }

        // Badges Section
        if (homeUiState.gameStats.badges.isNotEmpty()) {
            item {
                BadgesSection(badges = homeUiState.gameStats.badges)
            }
        }

        // Motivational Quote
        item {
            MotivationalQuoteCard(gameStats = homeUiState.gameStats)
        }

        // Daily Challenge
        item {
            DailyChallengeCard(
                gameStats = homeUiState.gameStats,
                onChallengeClick = { /* Handle challenge click */ }
            )
        }

        // Learning Progress for each class
        if (homeUiState.learningProgress.isNotEmpty()) {
            item {
                Text(
                    text = "Learning Progress",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(homeUiState.learningProgress.size) { index ->
                val progress = homeUiState.learningProgress[index]
                LearningProgressCard(progress = progress)
            }
        }
    }
}

/**
 * Educator home content - shows educator-specific information
 */
@Composable
private fun EducatorHomeContent(
    homeUiState: HomeUiState,
    onNavigateToClassDetails: (ClassDocument) -> Unit,
    onNavigateToNewClass: () -> Unit,
    onNavigateToViewAllClasses: () -> Unit,
    onNavigateToKlypDetails: (Klyp) -> Unit,
    onExportClass: ((ClassDocument) -> Unit)? = null
) {
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

        // Quick stats for educators
        item {
            QuickStatsSection(
                stats = homeUiState.classStatistics,
                userRole = homeUiState.userRole
            )
        }

        // My Classes section
        item {
            MyClassesSection(
                classes = homeUiState.myClasses,
                onClassClick = onNavigateToClassDetails,
                onAddNewClassClick = onNavigateToNewClass,
                onViewAllClick = onNavigateToViewAllClasses,
                onExportClass = onExportClass
            )
        }

        // Recent Klyps/Content section
        item {
            RecentKlypsSection(
                klyps = homeUiState.recentKlyps,
                onKlypClick = onNavigateToKlypDetails
            )
        }
    }
}

/**
 * Overview card for quick stats display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Learning progress card for individual classes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningProgressCard(
    progress: com.klypt.data.models.LearningProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.className,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${progress.progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { (progress.progressPercentage / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.completedKlyps}/${progress.totalKlyps} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Avg: ${(progress.averageQuizScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
