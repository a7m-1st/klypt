# Database Explorer Access Guide

## Issue Fixed
The app crash was caused by trying to launch `DatabaseExplorerActivity` from the `GalleryApplication.onCreate()` method, which is not allowed without proper context and flags.

## Solution
I've removed the automatic launch and created proper debug utilities that can be accessed from within the app.

## How to Access Database Explorer

### Option 1: Using DebugUtils Directly
You can launch the Database Explorer from any Activity or Composable using:

```kotlin
// From an Activity
DebugUtils.launchDatabaseExplorer(this)

// From a Composable
val context = LocalContext.current
DebugUtils.launchDatabaseExplorer(context)
```

### Option 2: Using Debug UI Components

#### Floating Debug Button
Add this to any screen to get a floating debug menu:

```kotlin
@Composable
fun MyScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Your screen content here
        
        // Add floating debug button (only shows in debug builds)
        DebugFloatingButton()
    }
}
```

#### Debug Menu Button (for App Bars)
Add this to any TopAppBar or toolbar:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar() {
    TopAppBar(
        title = { Text("My Screen") },
        actions = {
            // Add debug menu button (only shows in debug builds)
            DebugMenuButton()
        }
    )
}
```

### Option 3: Add to Main Navigation (Recommended)

To make it easily accessible throughout the app, you can modify your main navigation or home screen to include a debug option. Here's an example:

```kotlin
// In your main screen or navigation
@Composable
fun MainScreen() {
    Column {
        // Your main content
        
        // Debug section (only in debug builds)
        if (BuildConfig.DEBUG) {
            val context = LocalContext.current
            Button(
                onClick = { DebugUtils.launchDatabaseExplorer(context) },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Open Database Explorer")
            }
        }
    }
}
```

## Features Available in Database Explorer

The DatabaseExplorerActivity provides:

1. **Overview Tab**: Database statistics and health information
2. **Search Tab**: Search across all collections
3. **Analytics Tab**: Data analytics and insights
4. **Actions Tab**: Various debugging actions including:
   - Health checks
   - Data export
   - Print operations to logs
   - Find incomplete data
   - Find orphaned references

## Debug Build Only
All debug features are automatically hidden in release builds using `BuildConfig.DEBUG` checks.

## Implementation Files Created
- `DebugUtils.kt` - Utility functions and UI components for debug access
- `DatabaseExplorerActivity.kt` - Main database exploration interface
- `DatabaseExplorer.kt` - Backend utility for database operations

## Next Steps
1. Choose one of the access methods above
2. Add it to your desired screen/location
3. Build and run in debug mode
4. Access the database explorer through your chosen method

The app should now start without crashing, and you can access the database exploration features through proper UI interactions rather than automatic launching.
