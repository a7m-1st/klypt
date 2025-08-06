# Model Selection for Chat Navigation

## Overview

This implementation adds a model selection dialog to the KlypDetailsScreen to prevent context loss when navigating to chat. Previously, when users clicked "Discuss in Chat", they would be taken to the chat with a default model, and if they later changed models, their conversation context would be lost.

## Changes Made

### 1. ModelSelectionDialog.kt (New File)
- Created a new dialog component that displays available LLM models
- Shows model information including name, size, download status, and description
- Allows users to select a specific model before proceeding to chat
- Provides a clean, user-friendly interface with proper Material 3 design

### 2. KlypDetailsScreen.kt (Modified)
- Added `ModelManagerViewModel` parameter to access available models
- Updated `onNavigateToLLMChat` callback to include model name parameter
- Added state management for showing/hiding the model selection dialog
- Modified "Discuss in Chat" button to show dialog instead of direct navigation

### 3. GalleryNavGraph.kt (Modified)
- Updated navigation callback to include model name parameter
- Added new navigation route: `"llm-chat-for-class/{classCode}/{title}/{modelName}"`
- Enhanced route handler to find and select the user-chosen model
- Ensures proper model initialization before chat screen loads

## User Flow

1. User clicks "Discuss in Chat" button on KlypDetailsScreen
2. Model selection dialog appears showing available AI models
3. User selects their preferred model from the list
4. Navigation proceeds to chat screen with the selected model pre-initialized
5. Chat context is preserved since the model is set from the beginning

## Benefits

- **Context Preservation**: No loss of conversation when model is pre-selected
- **User Choice**: Users can choose the best model for their specific needs
- **Better UX**: Clear model information helps users make informed decisions
- **Performance**: Model is initialized only once, reducing startup time in chat

## Technical Details

### Model Information Displayed
- Model name and version
- Download status (downloaded, downloading, not downloaded)
- Model size in human-readable format
- Model description/capabilities

### Navigation Parameters
- `classCode`: The class identifier for context
- `title`: The klyp title for reference
- `modelName`: The selected model name for initialization

### Error Handling
- Falls back to first available model if selected model not found
- Graceful handling of navigation cancellation
- Proper logging for debugging purposes

## Usage

The implementation is automatic and requires no additional setup. Users will see the model selection dialog whenever they click "Discuss in Chat" from any Klyp details screen.
