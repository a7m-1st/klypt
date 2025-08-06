package com.klypt.ui.navigation

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
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
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
import com.klypt.ui.klypdetails.KlypDetailsDestination
import com.klypt.ui.klypdetails.KlypDetailsScreen
import com.klypt.ui.otp.OtpEntryScreen
import com.klypt.ui.otp.OtpViewModel
import com.klypt.ui.quiz.QuizDestination
import com.klypt.ui.quiz.QuizScreen
import com.klypt.ui.quizeditor.QuizEditorDestination
import com.klypt.ui.quizeditor.QuizEditorScreen
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
          classCode = classCode,
          className = className,
          onNavigateToSummaryReview = { summary, model, messages ->
            // Store the data in the temporary holder (without class context for regular chats)
            SummaryNavigationData.storeSummaryData(summary, model, messages)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          }
        )
      }
    }

    // LLM chat for klyp discussion with model selection
    composable(
      route = "llm-chat-for-class/{classCode}/{title}/{content}/{modelName}",
      arguments = listOf(
        navArgument("classCode") { type = NavType.StringType },
        navArgument("title") { type = NavType.StringType },
        navArgument("content") { type = NavType.StringType },
        navArgument("modelName") { type = NavType.StringType }
      ),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val viewModel: LlmChatViewModel = hiltViewModel(backStackEntry)
      val classCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
      val title = java.net.URLDecoder.decode(encodedTitle, "UTF-8")
      val encodedContent = backStackEntry.arguments?.getString("content") ?: ""
      val content = java.net.URLDecoder.decode(encodedContent, "UTF-8")
      val encodedModelName = backStackEntry.arguments?.getString("modelName") ?: ""
      val modelName = java.net.URLDecoder.decode(encodedModelName, "UTF-8")

      // Find and select the chosen model
      val selectedModel = TASK_LLM_CHAT.models.find { it.name == modelName } ?: TASK_LLM_CHAT.models.firstOrNull()
      selectedModel?.let { model ->
        modelManagerViewModel.selectModel(model)

        LlmChatScreen(
          viewModel = viewModel,
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = { navController.navigateUp() },
          classCode = classCode,
          className = title,
          onNavigateToSummaryReview = { summary, model, messages ->
            // Store the data in the temporary holder (without class context for klyp discussions)
            SummaryNavigationData.storeSummaryData(summary, model, messages)
            // Navigate to summary review screen
            navController.navigate("${SummaryReviewDestination.route}/${model.name}")
          },
          initialContent = content
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
          onNavigateToAddClass = { title, content ->
            // Navigate to NewClass screen to create new class
            // The pending data is already stored in SummaryNavigationData
            navController.navigate(NewClassDestination.route)
          },
          onSaveComplete = {
            // Clear summary data and navigate to home
            SummaryNavigationData.clearSummaryData()
            // Mark that home should refresh
            SummaryNavigationData.setShouldRefreshHome(true)
            
            navController.navigate("home") {
              popUpTo("home") { inclusive = true }
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
        },
        onClassCreated = { classCode, className ->
          // Handle class creation when we have pending summary data
          val encodedClassName = java.net.URLEncoder.encode(className, "UTF-8")
          
          // The class and summary have already been saved, just navigate to class code display
          try {
            val userRole = userContextProvider.getCurrentUserRole()
            val educatorId = if (userRole == UserRole.EDUCATOR) {
              userContextProvider.getCurrentUserId()
            } else {
              userContextProvider.getCurrentUserId() ?: "educator_001"
            }
            
            // Navigate directly to class code display
            navController.navigate("${ClassCodeDisplayDestination.route}/$classCode/$encodedClassName/$educatorId") {
              launchSingleTop = true
            }
          } catch (e: Exception) {
            // Fallback to home if there's an error
            navController.navigate("home") {
              popUpTo("home") { inclusive = true }
            }
          }
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
          // Navigate to LLM Chat for klyp creation with class context
          navController.navigate("llm-chat-for-class/$classCode/${java.net.URLEncoder.encode("New Klyp", "UTF-8")}")
        },
        onNavigateToKlypDetails = { klyp ->
          Log.d(TAG, "=== onNavigateToKlypDetails clicked ===")
          Log.d(TAG, "Klyp ID: ${klyp._id}, Title: ${klyp.title}")
          Log.d(TAG, "Class Code: ${klyp.classCode}")
          Log.d(TAG, "Questions: ${klyp.questions.size}")
          
          // Navigate to KlypDetailsScreen with klyp data
          val encodedKlypId = java.net.URLEncoder.encode(klyp._id, "UTF-8")  
          val encodedKlypTitle = java.net.URLEncoder.encode(klyp.title, "UTF-8")
          val encodedClassCode = java.net.URLEncoder.encode(klyp.classCode, "UTF-8")
          val encodedMainBody = java.net.URLEncoder.encode(klyp.mainBody, "UTF-8")
          val questionsJson = klyp.questions.joinToString("|") { question ->
            "${question.questionText}~${question.options.joinToString(",")}~${question.correctAnswer}"
          }
          val encodedQuestions = java.net.URLEncoder.encode(questionsJson, "UTF-8")
          val encodedCreatedAt = java.net.URLEncoder.encode(klyp.createdAt, "UTF-8")
          
          navController.navigate("${KlypDetailsDestination.route}/$encodedKlypId/$encodedKlypTitle/$encodedClassCode/$encodedMainBody/$encodedQuestions/$encodedCreatedAt")
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

    // Klyp Details Screen
    composable(
      route = "${KlypDetailsDestination.route}/{klypId}/{klypTitle}/{classCode}/{mainBody}/{questions}/{createdAt}",
      arguments = listOf(
        navArgument("klypId") { type = NavType.StringType },
        navArgument("klypTitle") { type = NavType.StringType },
        navArgument("classCode") { type = NavType.StringType },
        navArgument("mainBody") { type = NavType.StringType },
        navArgument("questions") { type = NavType.StringType },
        navArgument("createdAt") { type = NavType.StringType }
      ),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val encodedKlypId = backStackEntry.arguments?.getString("klypId") ?: ""
      val encodedKlypTitle = backStackEntry.arguments?.getString("klypTitle") ?: ""
      val encodedClassCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val encodedMainBody = backStackEntry.arguments?.getString("mainBody") ?: ""
      val encodedQuestions = backStackEntry.arguments?.getString("questions") ?: ""
      val encodedCreatedAt = backStackEntry.arguments?.getString("createdAt") ?: ""
      
      // Decode the parameters
      val klypId = java.net.URLDecoder.decode(encodedKlypId, "UTF-8")
      val klypTitle = java.net.URLDecoder.decode(encodedKlypTitle, "UTF-8")
      val classCode = java.net.URLDecoder.decode(encodedClassCode, "UTF-8")
      val mainBody = java.net.URLDecoder.decode(encodedMainBody, "UTF-8")
      val questionsString = java.net.URLDecoder.decode(encodedQuestions, "UTF-8")
      val createdAt = java.net.URLDecoder.decode(encodedCreatedAt, "UTF-8")
      
      // Parse questions from the encoded string
      val questions = if (questionsString.isNotEmpty()) {
        questionsString.split("|").mapNotNull { questionData ->
          val parts = questionData.split("~")
          if (parts.size >= 3) {
            val questionText = parts[0]
            val options = parts[1].split(",")
            val correctAnswer = parts[2].firstOrNull() ?: 'A'
            Question(
              questionText = questionText,
              options = options,
              correctAnswer = correctAnswer
            )
          } else null
        }
      } else emptyList()
      
      // Create the Klyp object
      val klyp = Klyp(
        _id = klypId,
        title = klypTitle,
        classCode = classCode,
        mainBody = mainBody,
        questions = questions,
        createdAt = createdAt
      )
      
      if (klypId.isBlank()) {
        Log.e("GalleryNavGraph", "KlypDetailsScreen received blank klypId, navigating back")
        LaunchedEffect(Unit) {
          navController.navigateUp()
        }
        return@composable
      }
      
      KlypDetailsScreen(
        klyp = klyp,
        onNavigateBack = { navController.navigateUp() },
        onNavigateToLLMChat = { classCode, title, content, modelName ->
          // Navigate to LLM Chat with class context and selected model
          val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
          val encodedContent = java.net.URLEncoder.encode(content, "UTF-8")
          val encodedModelName = java.net.URLEncoder.encode(modelName, "UTF-8")
          navController.navigate("llm-chat-for-class/$classCode/$encodedTitle/$encodedContent/$encodedModelName")
        },
        onNavigateToQuiz = { klypForQuiz ->
          // Navigate to Quiz screen
          val encodedQuizKlypId = java.net.URLEncoder.encode(klypForQuiz._id, "UTF-8")
          val encodedQuizKlypTitle = java.net.URLEncoder.encode(klypForQuiz.title, "UTF-8")
          val encodedQuizClassCode = java.net.URLEncoder.encode(klypForQuiz.classCode, "UTF-8")
          val encodedQuizMainBody = java.net.URLEncoder.encode(klypForQuiz.mainBody, "UTF-8")
          val quizQuestionsJson = klypForQuiz.questions.joinToString("|") { question ->
            "${question.questionText}~${question.options.joinToString(",")}~${question.correctAnswer}"
          }
          val encodedQuizQuestions = java.net.URLEncoder.encode(quizQuestionsJson, "UTF-8")
          val encodedQuizCreatedAt = java.net.URLEncoder.encode(klypForQuiz.createdAt, "UTF-8")
          
          navController.navigate("${QuizDestination.route}/$encodedQuizKlypId/$encodedQuizKlypTitle/$encodedQuizClassCode/$encodedQuizMainBody/$encodedQuizQuestions/$encodedQuizCreatedAt")
        },
        onNavigateToQuizEditor = { klypForEditor, model ->
          // Navigate to Quiz Editor screen
          navigateToQuizEditor(navController, klypForEditor, model)
        }
      )
    }

    // Quiz Screen
    composable(
      route = "${QuizDestination.route}/{klypId}/{klypTitle}/{classCode}/{mainBody}/{questions}/{createdAt}",
      arguments = listOf(
        navArgument("klypId") { type = NavType.StringType },
        navArgument("klypTitle") { type = NavType.StringType },
        navArgument("classCode") { type = NavType.StringType },
        navArgument("mainBody") { type = NavType.StringType },
        navArgument("questions") { type = NavType.StringType },
        navArgument("createdAt") { type = NavType.StringType }
      ),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val encodedKlypId = backStackEntry.arguments?.getString("klypId") ?: ""
      val encodedKlypTitle = backStackEntry.arguments?.getString("klypTitle") ?: ""
      val encodedClassCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val encodedMainBody = backStackEntry.arguments?.getString("mainBody") ?: ""
      val encodedQuestions = backStackEntry.arguments?.getString("questions") ?: ""
      val encodedCreatedAt = backStackEntry.arguments?.getString("createdAt") ?: ""
      
      // Decode the parameters
      val klypId = java.net.URLDecoder.decode(encodedKlypId, "UTF-8")
      val klypTitle = java.net.URLDecoder.decode(encodedKlypTitle, "UTF-8")
      val classCode = java.net.URLDecoder.decode(encodedClassCode, "UTF-8")
      val mainBody = java.net.URLDecoder.decode(encodedMainBody, "UTF-8")
      val questionsString = java.net.URLDecoder.decode(encodedQuestions, "UTF-8")
      val createdAt = java.net.URLDecoder.decode(encodedCreatedAt, "UTF-8")
      
      // Parse questions from the encoded string
      val questions = if (questionsString.isNotEmpty()) {
        questionsString.split("|").mapNotNull { questionData ->
          val parts = questionData.split("~")
          if (parts.size >= 3) {
            val questionText = parts[0]
            val options = parts[1].split(",")
            val correctAnswer = parts[2].firstOrNull() ?: 'A'
            Question(
              questionText = questionText,
              options = options,
              correctAnswer = correctAnswer
            )
          } else null
        }
      } else emptyList()
      
      // Create the Klyp object
      val klyp = Klyp(
        _id = klypId,
        title = klypTitle,
        classCode = classCode,
        mainBody = mainBody,
        questions = questions,
        createdAt = createdAt
      )
      
      if (klypId.isBlank()) {
        Log.e("GalleryNavGraph", "QuizScreen received blank klypId, navigating back")
        LaunchedEffect(Unit) {
          navController.navigateUp()
        }
        return@composable
      }
      
      QuizScreen(
        klyp = klyp,
        onNavigateBack = { navController.navigateUp() },
        onQuizCompleted = { score, totalQuestions ->
          Log.d("GalleryNavGraph", "Quiz completed with score: $score/$totalQuestions")
          // Navigate back to the klyp details screen
          navController.navigateUp()
        },
        userContextProvider = userContextProvider
      )
    }
    
    // Quiz Editor Screen
    composable(
      route = "${QuizEditorDestination.route}/{klypId}/{klypTitle}/{classCode}/{mainBody}/{createdAt}/{modelName}",
      arguments = listOf(
        navArgument("klypId") { type = NavType.StringType },
        navArgument("klypTitle") { type = NavType.StringType },
        navArgument("classCode") { type = NavType.StringType },
        navArgument("mainBody") { type = NavType.StringType },
        navArgument("createdAt") { type = NavType.StringType },
        navArgument("modelName") { type = NavType.StringType }
      )
    ) { backStackEntry ->
      val klypId = backStackEntry.arguments?.getString("klypId") ?: ""
      val klypTitle = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("klypTitle") ?: "", "UTF-8")
      val classCode = backStackEntry.arguments?.getString("classCode") ?: ""
      val mainBody = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("mainBody") ?: "", "UTF-8")
      val createdAt = backStackEntry.arguments?.getString("createdAt") ?: ""
      val modelName = backStackEntry.arguments?.getString("modelName") ?: ""
      
      // Get the model
      val model = getModelByName(modelName)
      
      if (model == null || klypId.isBlank()) {
        Log.e("GalleryNavGraph", "QuizEditorScreen received invalid parameters")
        LaunchedEffect(Unit) {
          navController.navigateUp()
        }
        return@composable
      }
      
      // Create the Klyp object (questions will be loaded by the editor)
      val klyp = Klyp(
        _id = klypId,
        title = klypTitle,
        classCode = classCode,
        mainBody = mainBody,
        questions = emptyList(), // Will be loaded by the editor
        createdAt = createdAt
      )
      
      QuizEditorScreen(
        klyp = klyp,
        model = model,
        onNavigateBack = { navController.navigateUp() },
        onSaveCompleted = {
          Log.d("GalleryNavGraph", "Quiz saved successfully, navigating back")
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

fun navigateToQuizEditor(
  navController: NavHostController,
  klyp: Klyp,
  model: Model
) {
  // URL encode the strings that might contain special characters
  val encodedTitle = java.net.URLEncoder.encode(klyp.title, "UTF-8")
  val encodedMainBody = java.net.URLEncoder.encode(klyp.mainBody, "UTF-8")
  
  val route = "${QuizEditorDestination.route}/${klyp._id}/$encodedTitle/${klyp.classCode}/$encodedMainBody/${klyp.createdAt}/${model.name}"
  navController.navigate(route)
}

fun getModelFromNavigationParam(entry: NavBackStackEntry, task: Task): Model? {
  var modelName = entry.arguments?.getString("modelName") ?: ""
  if (modelName.isEmpty()) {
    modelName = task.models[0].name
  }
  val model = getModelByName(modelName)
  return model
}
