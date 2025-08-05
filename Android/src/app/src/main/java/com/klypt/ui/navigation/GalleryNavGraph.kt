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

import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.klypt.data.Model
import com.klypt.data.TASK_LLM_ASK_AUDIO
import com.klypt.data.TASK_LLM_ASK_IMAGE
import com.klypt.data.TASK_LLM_CHAT
import com.klypt.data.TASK_LLM_PROMPT_LAB
import com.klypt.data.Task
import com.klypt.data.TaskType
import com.klypt.data.UserRole
import com.klypt.data.getModelByName
import com.klypt.firebaseAnalytics
import com.klypt.data.services.UserContextProvider
import com.klypt.ui.home.EnhancedHomeScreen
import com.klypt.ui.home.HomeScreen
import com.klypt.ui.llmchat.LlmAskAudioDestination
import com.klypt.ui.llmchat.LlmAskAudioScreen
import com.klypt.ui.llmchat.LlmAskAudioViewModel
import com.klypt.ui.llmchat.LlmAskImageDestination
import com.klypt.ui.llmchat.LlmAskImageScreen
import com.klypt.ui.llmchat.LlmAskImageViewModel
import com.klypt.ui.llmchat.LlmChatDestination
import com.klypt.ui.llmchat.LlmChatScreen
import com.klypt.ui.llmchat.LlmChatViewModel
import com.klypt.ui.llmchat.SummaryReviewDestination
import com.klypt.ui.llmchat.components.SummaryReviewScreen
import com.klypt.ui.navigation.SummaryNavigationData
import com.klypt.ui.llmsingleturn.LlmSingleTurnDestination
import com.klypt.ui.llmsingleturn.LlmSingleTurnScreen
import com.klypt.ui.llmsingleturn.LlmSingleTurnViewModel
import com.klypt.ui.login.LoginScreen
import com.klypt.ui.login.LoginViewModel
import com.klypt.ui.login.RoleSelectionScreen
import com.klypt.ui.modelmanager.ModelManager
import com.klypt.ui.modelmanager.ModelManagerViewModel
import com.klypt.ui.newclass.NewClassDestination
import com.klypt.ui.newclass.NewClassScreen
import com.klypt.ui.classes.ViewAllClassesDestination
import com.klypt.ui.classes.ViewAllClassesScreen
import com.klypt.ui.classes.ClassDetailsDestination
import com.klypt.ui.classes.ClassDetailsScreen
import com.klypt.ui.classcodedisplay.ClassCodeDisplayDestination
import com.klypt.ui.classcodedisplay.ClassCodeDisplayScreen
import com.klypt.ui.otp.OtpEntryScreen
import com.klypt.ui.otp.OtpViewModel
import com.klypt.ui.signup.SignupViewModel

private const val TAG = "AGGalleryNavGraph"
private const val ROUTE_PLACEHOLDER = "placeholder"
private const val ENTER_ANIMATION_DURATION_MS = 500
private val ENTER_ANIMATION_EASING = EaseOutExpo
private const val ENTER_ANIMATION_DELAY_MS = 100

private const val EXIT_ANIMATION_DURATION_MS = 500
private val EXIT_ANIMATION_EASING = EaseOutExpo

private fun enterTween(): FiniteAnimationSpec<IntOffset> {
  return tween(
    ENTER_ANIMATION_DURATION_MS,
    easing = ENTER_ANIMATION_EASING,
    delayMillis = ENTER_ANIMATION_DELAY_MS,
  )
}

private fun exitTween(): FiniteAnimationSpec<IntOffset> {
  return tween(EXIT_ANIMATION_DURATION_MS, easing = EXIT_ANIMATION_EASING)
}

private fun AnimatedContentTransitionScope<*>.slideEnter(): EnterTransition {
  return slideIntoContainer(
    animationSpec = enterTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
  )
}

private fun AnimatedContentTransitionScope<*>.slideExit(): ExitTransition {
  return slideOutOfContainer(
    animationSpec = exitTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
  )
}

/** Navigation routes. */
@Composable
fun GalleryNavHost(
  navController: NavHostController,
  userContextProvider: UserContextProvider,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  // Track whether app is in foreground.
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START,
        Lifecycle.Event.ON_RESUME -> {
          modelManagerViewModel.setAppInForeground(foreground = true)
        }
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_PAUSE -> {
          modelManagerViewModel.setAppInForeground(foreground = false)
        }
        else -> {
          /* Do nothing for other events */
        }
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  NavHost(
    navController = navController,
    // Always start with splash to properly check authentication
    startDestination = "splash",
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    modifier = modifier,
  ) {
    // Placeholder root screen
    composable(route = ROUTE_PLACEHOLDER) { Text("") }

    // Splash screen for session check
    composable("splash") {
      var isCheckingSession by remember { mutableStateOf(true) }
      
      LaunchedEffect(Unit) {
        try {
          // Check if user has stored session data
          val hasStoredSession = userContextProvider.hasStoredSession()
          
          if (hasStoredSession) {
            // Try to restore user context
            val userId = userContextProvider.restoreUserContextAsync()
            if (userId != null) {
              // Session restored successfully - navigate to home
              navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
              }
            } else {
              // Failed to restore session - go to login
              navController.navigate("role_selection") {
                popUpTo("splash") { inclusive = true }
              }
            }
          } else {
            // No session data - go to login
            navController.navigate("role_selection") {
              popUpTo("splash") { inclusive = true }
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error during session check", e)
          // On error, go to login
          navController.navigate("role_selection") {
            popUpTo("splash") { inclusive = true }
          }
        } finally {
          isCheckingSession = false
        }
      }
      
      // Show loading indicator while checking session
      if (isCheckingSession) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      }
    }

    composable("role_selection") { backStackEntry ->
      val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry("role_selection")
      }
      val loginViewModel: LoginViewModel = hiltViewModel(parentEntry)
      
      RoleSelectionScreen(
        onNavigateToLogin = {
          navController.navigate("login")
        },
        viewModel = loginViewModel
      )
    }

    composable("login") { backStackEntry ->
      val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry("role_selection")
      }
      val loginViewModel: LoginViewModel = hiltViewModel(parentEntry)
      
      LoginScreen(
        onNavigateToHome = {
          val uiState = loginViewModel.uiState.value
          when (uiState.role) {
            UserRole.STUDENT -> {
              // For students, context is set in LoginViewModel after successful login
              navController.navigate("home") {
                popUpTo("role_selection") { inclusive = true }
              }
            }
            UserRole.EDUCATOR -> {
              // For educators, we need OTP verification first
              // Context will be set after OTP verification
              navController.navigate("otp_verify/${uiState.phoneNumber}") {
                popUpTo("role_selection") { inclusive = true }
              }
            }
          }
        },
        onNavigateToSignup = {
          navController.navigate("signup")
        },
        viewModel = loginViewModel
      )
    }

    // Signup page route
    composable("signup") {
      val signupViewModel: SignupViewModel = hiltViewModel()
      
      com.klypt.ui.signup.SignupScreen(
        onNext = { 
          // After educator signup, they need to verify their phone number
          val phoneNumber = signupViewModel.uiState.value.phoneNumber
          if (phoneNumber.isNotEmpty()) {
            navController.navigate("otp_verify/$phoneNumber") {
              popUpTo("signup") { inclusive = true }
            }
          } else {
            // If no phone number provided, go back to role selection
            navController.navigateUp()
          }
        },
        viewModel = signupViewModel
      )
    }

    composable(
      route = "otp_verify/{phoneNumber}",
      arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
    ) { backStackEntry ->
      val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
      val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry("otp_verify/$phoneNumber")
      }
      val otpViewModel: OtpViewModel = hiltViewModel(parentEntry)

      OtpEntryScreen(
        phoneNumber = phoneNumber,
        onNavigateToHome = {
          navController.navigate("home") {
            popUpTo("role_selection") { inclusive = true }
          }
        },
        onNavigateBack = {
          navController.navigate("login") {
            popUpTo("login") { inclusive = true }
          }
        },
        userContextProvider = userContextProvider,
        viewModel = hiltViewModel()
      )
    }

    composable("home") {
      var showModelManager by remember { mutableStateOf(false) }
      var pickedTask by remember { mutableStateOf<Task?>(null) }

      EnhancedHomeScreen(
        modelManagerViewModel = modelManagerViewModel,
        tosViewModel = hiltViewModel(),
        navigateToTaskScreen = { task ->
          pickedTask = task
          showModelManager = true
          firebaseAnalytics?.logEvent(
            "capability_select",
            bundleOf("capability_name" to task.type.toString()),
          )
        },
        onNavigateToNewClass = {
          navController.navigate(NewClassDestination.route)
        },
        onNavigateToViewAllClasses = {
          navController.navigate(ViewAllClassesDestination.route)
        },
        onNavigateToClassDetails = { classDoc ->
          if (classDoc._id.isNotBlank()) {
            val encodedClassId = java.net.URLEncoder.encode(classDoc._id, "UTF-8")
            navController.navigate("${ClassDetailsDestination.route}/$encodedClassId")
          } else {
            Log.e("GalleryNavGraph", "Cannot navigate to class details: class ID is blank")
          }
        },
        onLogout = {
          // Navigate back to login screen, clearing the back stack
          navController.navigate("splash") {
            popUpTo(0) { inclusive = true }
          }
        }
      )

      // Model manager.
      AnimatedVisibility(
        visible = showModelManager,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
      ) {
        val curPickedTask = pickedTask
        if (curPickedTask != null) {
          ModelManager(
            viewModel = modelManagerViewModel,
            task = curPickedTask,
            onModelClicked = { model ->
              navigateToTaskScreen(
                navController = navController,
                taskType = curPickedTask.type,
                model = model,
              )
            },
            navigateUp = { showModelManager = false },
          )
        }
      }
    }

    // LLM chat for class creation
    composable(
      route = "llm-chat-for-class/{classCode}/{className}",
      arguments = listOf(
        navArgument("classCode") { type = NavType.StringType },
        navArgument("className") { type = NavType.StringType }
      ),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmChatViewModel = hiltViewModel(backStackEntry)
      val classCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val encodedClassName = backStackEntry.arguments?.getString("className") ?: ""
      val className = java.net.URLDecoder.decode(encodedClassName, "UTF-8")

      // Use the default model for class creation
      val defaultModel = TASK_LLM_CHAT.models.firstOrNull()
      defaultModel?.let { model ->
        modelManagerViewModel.selectModel(model)

        LlmChatScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
          onNavigateToSummaryReview = { summary, model, messages ->
            // For class creation, store the class context and navigate to summary review
            val classContext = SummaryNavigationData.ClassCreationContext(classCode, className)
            SummaryNavigationData.storeSummaryData(summary, model, messages, classContext)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          }
        )
      }
    }

    // LLM chat demos.
    composable(
      route = "${LlmChatDestination.route}/{modelName}",
      arguments = listOf(navArgument("modelName") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmChatViewModel = hiltViewModel(backStackEntry)

      getModelFromNavigationParam(backStackEntry, TASK_LLM_CHAT)?.let { defaultModel ->
        modelManagerViewModel.selectModel(defaultModel)

        LlmChatScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
          onNavigateToSummaryReview = { summary, model, messages ->
            // Store the data in the temporary holder
            SummaryNavigationData.storeSummaryData(summary, model, messages)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          }
        )
      }
    }

    // LLM single turn.
    composable(
      route = "${LlmSingleTurnDestination.route}/{modelName}",
      arguments = listOf(navArgument("modelName") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmSingleTurnViewModel = hiltViewModel(backStackEntry)

      getModelFromNavigationParam(backStackEntry, TASK_LLM_PROMPT_LAB)?.let { defaultModel ->
        modelManagerViewModel.selectModel(defaultModel)

        LlmSingleTurnScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
        )
      }
    }

    // Ask image.
    composable(
      route = "${LlmAskImageDestination.route}/{modelName}",
      arguments = listOf(navArgument("modelName") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmAskImageViewModel = hiltViewModel()

      getModelFromNavigationParam(backStackEntry, TASK_LLM_ASK_IMAGE)?.let { defaultModel ->
        modelManagerViewModel.selectModel(defaultModel)

        LlmAskImageScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
          onNavigateToSummaryReview = { summary, model, messages ->
            // Store the data in the temporary holder
            SummaryNavigationData.storeSummaryData(summary, model, messages)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          }
        )
      }
    }

    // Ask audio.
    composable(
      route = "${LlmAskAudioDestination.route}/{modelName}",
      arguments = listOf(navArgument("modelName") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmAskAudioViewModel = hiltViewModel()

      getModelFromNavigationParam(backStackEntry, TASK_LLM_ASK_AUDIO)?.let { defaultModel ->
        modelManagerViewModel.selectModel(defaultModel)

        LlmAskAudioScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
          onNavigateToSummaryReview = { summary, model, messages ->
            // Store the data in the temporary holder
            SummaryNavigationData.storeSummaryData(summary, model, messages)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          }
        )
      }
    }

    // Summary Review Screen
    composable(
      route = "${SummaryReviewDestination.route}/{modelName}",
      arguments = listOf(navArgument("modelName") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      // Retrieve the stored data
      val (summary, model, messages) = SummaryNavigationData.getSummaryData()
      
      if (summary != null && model != null) {
        SummaryReviewScreen(
          summary = summary,
          model = model,
          messages = messages,
          onNavigateBack = { 
            SummaryNavigationData.clearSummaryData()
            navController.navigateUp() 
          },
          onSaveComplete = {
            Log.e("DEBUG_NAV", "=== SummaryReviewScreen onSaveComplete called ===")
            val classContext = SummaryNavigationData.getClassCreationContext()
            Log.e("DEBUG_NAV", "Class context: $classContext")
            // Mark that home should refresh when we return
            SummaryNavigationData.setShouldRefreshHome(true)
            
            if (classContext != null) {
              // We're in class creation context, navigate directly to class code display
              try {
                val userRole = userContextProvider.getCurrentUserRole()
                Log.e("DEBUG_NAV", "User role is $userRole")
                
                val educatorId = if (userRole == UserRole.EDUCATOR) {
                  val userId = userContextProvider.getCurrentUserId()
                  Log.e("DEBUG_NAV", "Educator ID is $userId")
                  userId
                } else {
                  Log.e("DEBUG_NAV", "Student creating class - using student ID as educator for this class")
                  // Allow students to create classes by using their ID as educator
                  userContextProvider.getCurrentUserId() ?: "educator_001"
                }
                
                val encodedClassName = java.net.URLEncoder.encode(classContext.className, "UTF-8")
                val route = "${ClassCodeDisplayDestination.route}/${classContext.classCode}/$encodedClassName/$educatorId"
                Log.e("DEBUG_NAV", "Navigating directly to class code display: $route")
                navController.navigate(route) {
                  launchSingleTop = true
                }
                Log.e("DEBUG_NAV", "=== Navigation to class code display completed ===")
              } catch (e: Exception) {
                Log.e("DEBUG_NAV", "ERROR during navigation: ${e.message}", e)
                // Fallback to home if there's an error
                navController.navigate("home") {
                  popUpTo("home") { inclusive = true }
                }
              }
            } else {
              // Regular summary saving, go to home and force refresh
              Log.e("DEBUG_NAV", "No class context, navigating to home")
              navController.navigate("home") {
                popUpTo("home") { inclusive = true }
              }
            }
          }
        )
      } else {
        // Fallback if data is not available
        navController.navigateUp()
      }
    }

    // New Class Screen
    composable(
      route = NewClassDestination.route,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) {
      NewClassScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToLLMChat = { classCode, className ->
          // Navigate to LLM Chat for class creation with class code and name
          navController.navigate("llm-chat-for-class/$classCode/$className")
        }
      )
    }

    // View All Classes Screen
    composable(
      route = ViewAllClassesDestination.route,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) {
      ViewAllClassesScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToAddClass = {
          navController.navigate(NewClassDestination.route)
        },
        onClassClick = { classDoc ->
          if (classDoc._id.isNotBlank()) {
            val encodedClassId = java.net.URLEncoder.encode(classDoc._id, "UTF-8")
            navController.navigate("${ClassDetailsDestination.route}/$encodedClassId")
          } else {
            Log.e("GalleryNavGraph", "Cannot navigate to class details: class ID is blank in ViewAllClassesScreen")
          }
        }
      )
    }

    // Class Details Screen
    composable(
      route = "${ClassDetailsDestination.route}/{classId}",
      arguments = listOf(navArgument("classId") { type = NavType.StringType }),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val encodedClassId = backStackEntry.arguments?.getString("classId") ?: ""
      val classId = java.net.URLDecoder.decode(encodedClassId, "UTF-8")
      
      if (classId.isBlank()) {
        Log.e("GalleryNavGraph", "ClassDetailsScreen received blank classId, navigating back")
        LaunchedEffect(Unit) {
          navController.navigateUp()
        }
        return@composable
      }
      
      ClassDetailsScreen(
        classId = classId,
        onNavigateBack = { navController.navigateUp() },
        onNavigateToAddKlyp = { classCode ->
          // For now, we'll handle adding klyps within the screen itself
          //TODO
        },
        userContextProvider = userContextProvider
      )
    }

    // Class Code Display Screen
    composable(
      route = "${ClassCodeDisplayDestination.route}/{classCode}/{className}/{educatorId}",
      arguments = listOf(
        navArgument("classCode") { type = NavType.StringType },
        navArgument("className") { type = NavType.StringType },
        navArgument("educatorId") { type = NavType.StringType }
      ),
      enterTransition = { EnterTransition.None },
      exitTransition = { ExitTransition.None },
    ) { backStackEntry ->
      Log.e("DEBUG_NAV", "=== ClassCodeDisplayScreen entered ===")
      val classCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val encodedClassName = backStackEntry.arguments?.getString("className") ?: ""
      val className = java.net.URLDecoder.decode(encodedClassName, "UTF-8")
      val educatorId = backStackEntry.arguments?.getString("educatorId") ?: ""
      
      Log.e("DEBUG_NAV", "ClassCodeDisplayScreen arguments - classCode: '$classCode', className: '$className', educatorId: '$educatorId'")
      
      // Validate arguments before proceeding
      if (classCode.isBlank() || className.isBlank() || educatorId.isBlank()) {
        Log.e("DEBUG_NAV", "INVALID ARGUMENTS - navigating to home")
        LaunchedEffect(Unit) {
          navController.navigate("home") {
            popUpTo("home") { inclusive = true }
          }
        }
        return@composable
      }
      
      Log.e("DEBUG_NAV", "Arguments valid, showing ClassCodeDisplayScreen")
      ClassCodeDisplayScreen(
        classCode = classCode,
        className = className,
        educatorId = educatorId,
        onNavigateHome = { 
          Log.e("DEBUG_NAV", "ClassCodeDisplayScreen onNavigateHome called")
          // Mark that home should refresh to show the new class
          SummaryNavigationData.setShouldRefreshHome(true)
          navController.navigate("home") {
            popUpTo("home") { inclusive = true }
          }
        },
        onNavigateBack = { 
          Log.e("DEBUG_NAV", "ClassCodeDisplayScreen onNavigateBack called")
          navController.navigateUp() 
        }
      )
    }
  }

  // Handle incoming intents for deep links
  val intent = androidx.activity.compose.LocalActivity.current?.intent
  val data = intent?.data
  if (data != null) {
    intent.data = null
    Log.d(TAG, "navigation link clicked: $data")
    if (data.toString().startsWith("com.klypt://model/")) {
      val modelName = data.pathSegments.last()
      getModelByName(modelName)?.let { model ->
        // TODO(jingjin): need to show a list of possible tasks for this model.
        navigateToTaskScreen(
          navController = navController,
          taskType = TaskType.LLM_CHAT,
          model = model,
        )
      }
    }
  }
}

fun navigateToTaskScreen(
  navController: NavHostController,
  taskType: TaskType,
  model: Model? = null,
) {
  val modelName = model?.name ?: ""
  when (taskType) {
    TaskType.LLM_CHAT -> navController.navigate("${LlmChatDestination.route}/${modelName}")
    TaskType.LLM_ASK_IMAGE -> navController.navigate("${LlmAskImageDestination.route}/${modelName}")
    TaskType.LLM_ASK_AUDIO -> navController.navigate("${LlmAskAudioDestination.route}/${modelName}")
    TaskType.LLM_PROMPT_LAB ->
      navController.navigate("${LlmSingleTurnDestination.route}/${modelName}")
    TaskType.TEST_TASK_1 -> {}
    TaskType.TEST_TASK_2 -> {}
  }
}

fun getModelFromNavigationParam(entry: NavBackStackEntry, task: Task): Model? {
  var modelName = entry.arguments?.getString("modelName") ?: ""
  if (modelName.isEmpty()) {
    modelName = task.models[0].name
  }
  val model = getModelByName(modelName)
  return model
}
