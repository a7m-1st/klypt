/*
 * Example of how to integrate the enhanced home screen with dummy data
 * into your existing navigation system.
 */

// In your GalleryNavGraph.kt or similar navigation file, replace the existing home route with:

composable("home") {
    var showModelManager by remember { mutableStateOf(false) }
    var pickedTask by remember { mutableStateOf<Task?>(null) }

    // Use the enhanced home screen with educational content
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
    )

    // Keep the existing model manager overlay
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

/*
 * Additional setup notes:
 * 
 * 1. Make sure to add the Hilt dependency injection for the new repository:
 *    In your di/DatabaseModule.kt or similar:
 */

@Module
@InstallIn(SingletonComponent::class)
object EducationalContentModule {
    
    @Provides
    @Singleton
    fun provideEducationalContentRepository(): EducationalContentRepository {
        return EducationalContentRepository()
    }
}

/*
 * 2. If you want to initialize dummy data at app startup, add this to your Application class:
 */

class KlyptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize dummy data for development/demo
        DummyDataGenerator.initializeDummyData()
    }
}

/*
 * 3. Update your strings.xml to include any new strings if needed:
 */

<!-- Add to res/values/strings.xml -->
<string name="welcome_student">Welcome back, %s!</string>
<string name="welcome_educator">Hello, %s!</string>
<string name="recent_klyps">Recent Learning Content</string>
<string name="my_classes">My Classes</string>
<string name="upcoming_assignments">Upcoming Assignments</string>
<string name="ai_features">AI-Powered Features</string>
<string name="switch_role">Switch Role</string>
<string name="no_content_available">No content available</string>
<string name="loading_content">Loading your content...</string>
