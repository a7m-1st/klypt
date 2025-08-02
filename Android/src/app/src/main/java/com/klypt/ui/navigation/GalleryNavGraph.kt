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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.klypt.ui.llmsingleturn.LlmSingleTurnDestination
import com.klypt.ui.llmsingleturn.LlmSingleTurnScreen
import com.klypt.ui.llmsingleturn.LlmSingleTurnViewModel
import com.klypt.ui.login.LoginScreen
import com.klypt.ui.login.LoginViewModel
import com.klypt.ui.login.RoleSelectionScreen
import com.klypt.ui.modelmanager.ModelManager
import com.klypt.ui.modelmanager.ModelManagerViewModel
import com.klypt.ui.otp.OtpEntryScreen
import com.klypt.ui.otp.OtpViewModel

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
    // Start with role selection, then login if not authenticated, home if authenticated
    startDestination = "role_selection",
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    modifier = modifier,
  ) {
    // Placeholder root screen
    composable(route = ROUTE_PLACEHOLDER) { Text("") }

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
              // Student goes directly to home
              navController.navigate("home") {
                popUpTo("login") { inclusive = true }
              }
            }
            UserRole.EDUCATOR -> {
              // Educator goes to OTP verification with phone number
              navController.navigate("otp_verify/${uiState.phoneNumber}") {
                popUpTo("login") { inclusive = true }
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
      com.klypt.ui.signup.SignupScreen(
        onNext = { navController.navigateUp() }
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
          navController.navigate("home")
        },
        onNavigateBack = {
          navController.navigateUp()
        },
        viewModel = hiltViewModel()
      )
    }

    composable("home") {
      var showModelManager by remember { mutableStateOf(false) }
      var pickedTask by remember { mutableStateOf<Task?>(null) }

      HomeScreen(
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
        )
      }
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
