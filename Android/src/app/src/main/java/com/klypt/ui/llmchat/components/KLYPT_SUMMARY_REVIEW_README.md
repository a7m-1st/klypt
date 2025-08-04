# Klypt Summary Review Flow

This document describes how to use the new Klypt summary review functionality that allows users to review and edit AI-generated summaries before saving them to the database.

## Components Overview

### 1. SummaryReviewScreen
- **Location**: `com.klypt.ui.llmchat.components.SummaryReviewScreen`
- **Purpose**: Provides a UI for reviewing and editing generated summaries
- **Features**:
  - Edit session title
  - Edit AI-generated summary content
  - View summary statistics (message count, key points, character count)
  - Save changes to database

### 2. SummaryReviewViewModel
- **Location**: `com.klypt.ui.llmchat.components.SummaryReviewViewModel`
- **Purpose**: Manages the business logic for updating summaries
- **Features**:
  - Loading state management
  - Database update operations
  - Error handling

### 3. Updated ChatSummaryViewModel
- **Location**: `com.klypt.ui.llmchat.components.ChatSummaryHandler`
- **Purpose**: Enhanced to support navigation to review screen
- **Changes**:
  - `onSuccess` callback now passes the created `ChatSummary` object
  - Supports navigation flow to review screen

## Integration Guide

### Step 1: Update Navigation

Add the navigation callback to your LLM chat screens:

```kotlin
LlmChatScreen(
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    onNavigateToSummaryReview = { summary, model, messages ->
        // Navigate to summary review screen
        // You can use any navigation framework (NavController, custom navigation, etc.)
        navigateToSummaryReview(summary, model, messages)
    },
    viewModel = hiltViewModel()
)
```

### Step 2: Set Up Summary Review Screen

Add the summary review screen to your navigation destinations:

```kotlin
// In your navigation graph or composable
SummaryReviewScreen(
    summary = summary,
    model = model,
    messages = messages,
    onNavigateBack = {
        // Navigate back to chat screen
        navigateUp()
    },
    onSaveComplete = {
        // Handle successful save (e.g., show success message, navigate back)
        navigateUp()
    }
)
```

### Step 3: Use the Navigation Helper (Optional)

For simpler integration, you can use the provided navigation helper:

```kotlin
KlyptSummaryNavigation(
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp
)
```

## Flow Description

1. **User initiates summary**: User clicks the "Klypt" button in the chat interface
2. **Loading state**: `SummaryLoadingScreen` is displayed while the LLM generates the summary
3. **Summary creation**: `ChatSummaryService` generates the summary using the LLM and saves it to database
4. **Navigation**: Upon successful creation, the app navigates to `SummaryReviewScreen`
5. **Review and edit**: User can:
   - Edit the session title
   - Modify the AI-generated summary content
   - View statistics about the conversation
6. **Save changes**: User can save their edits, which updates the database entry
7. **Return to chat**: After saving, user returns to the chat interface

## Database Changes

### New Method Added
- `DatabaseManager.updateChatSummary(chatSummary: ChatSummary): Boolean`
  - Updates an existing chat summary document in the database
  - Preserves the original document ID
  - Updates all editable fields (title, summary content, etc.)

## Error Handling

The system includes comprehensive error handling:
- Network/database errors during summary generation
- Validation errors (empty fields, missing user context)
- Loading state management during database operations
- User-friendly error messages with toast notifications

## Customization Options

### UI Customization
- Modify `SummaryReviewScreen` to change the appearance
- Adjust card layouts, spacing, and styling to match your app theme

### Navigation Customization  
- Implement custom navigation logic in the callback functions
- Use different navigation frameworks (Jetpack Navigation, custom solutions)

### Business Logic Customization
- Extend `SummaryReviewViewModel` for additional validation
- Modify `ChatSummaryService` for different summary generation strategies

## Example Usage

See `KlyptSummaryNavigation.kt` for a complete example of how to integrate the summary review flow into your existing chat interface.

## Testing Considerations

When testing the summary review flow:
1. Test with various message lengths and types
2. Verify database updates work correctly
3. Test error scenarios (network failures, validation errors)
4. Ensure loading states display properly
5. Verify navigation works in both directions (to and from review screen)
