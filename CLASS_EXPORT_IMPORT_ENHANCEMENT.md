# Class Import/Export with Duplicate Handling

## Overview
The class import/export feature has been enhanced to handle duplicate classes gracefully and provide comprehensive JSON export functionality.

## What happens when importing a duplicate class?

### Before Enhancement
- **Issue**: If a class with the same class code already existed, it would be silently overwritten without any warning or user confirmation
- **Risk**: Users could accidentally lose existing class data and associated klyps

### After Enhancement
- **Detection**: The system now checks for existing classes with the same class code before importing
- **User Choice**: When a duplicate is detected, a dialog appears with the following information:
  - Existing class name and code
  - Number of klyps that would be affected
  - Warning about data replacement
- **Options**: Users can choose to:
  - **Cancel**: Abort the import operation
  - **Overwrite**: Replace the existing class and all its klyps with the imported data

## Enhanced JSON Export Format

### New Export Structure
```json
{
  "exportVersion": "1.0",
  "exportTimestamp": "1691363200000",
  "classDetails": {
    "_id": "class_abc12345",
    "type": "class",
    "classCode": "MATH101",
    "classTitle": "Mathematics 101",
    "educatorId": "educator_001",
    "studentIds": ["student1", "student2"],
    "updatedAt": "1691363200000",
    "lastSyncedAt": "1691363200000"
  },
  "klyps": [
    {
      "_id": "klyp_xyz789",
      "type": "klyp",
      "title": "Introduction to Algebra",
      "mainBody": "This klyp covers basic algebraic concepts...",
      "questions": [
        {
          "questionText": "What is 2x + 3 = 7?",
          "options": ["x = 1", "x = 2", "x = 3", "x = 4"],
          "correctAnswer": "B"
        }
      ],
      "createdAt": "1691363200000"
    }
  ],
  "klypCount": 1
}
```

### Legacy Format Support
The system still supports importing from the old format:
```json
{
  "classCode": "MATH101",
  "classTitle": "Mathematics 101",
  "educatorId": "educator_001",
  "studentIds": ["student1", "student2"],
  "updatedAt": "1691363200000",
  "lastSyncedAt": "1691363200000"
}
```

## Export Features

### Export Location
- Files are automatically saved to the device's **Downloads** folder
- Filename format: `{classCode}_{className}_export.json`
- Example: `MATH101_Mathematics_101_export.json`

### Share Dialog
- **Automatic Share Prompt**: After successful export, a dialog appears asking if you want to share the file
- **Share Options**: Choose from various apps like email, messaging, cloud storage, etc.
- **File Permissions**: The app automatically grants read permissions to the receiving app
- **User Control**: You can choose to "Share" immediately or "Done" to dismiss without sharing

### What Gets Exported
1. **Class Details**: Complete class information including metadata
2. **All Klyps**: Every educational content item associated with the class
3. **Questions**: All questions and answers within each klyp
4. **Metadata**: Export timestamp and version for compatibility tracking

### Export Access Points
- **Class Cards**: Menu option "Export to JSON" on class cards
- **View All Classes Screen**: Export option available for each class
- **Success Feedback**: Snackbar notification confirms successful export
- **Share Dialog**: Immediate option to share the exported file

## User Experience Flow

### Import with Duplicate Detection
1. User selects JSON file to import
2. System validates JSON format and structure
3. **Duplicate Check**: System checks if class code already exists
4. If duplicate found:
   - Dialog shows existing class information
   - User can choose to cancel or overwrite
   - If overwrite selected, existing klyps are deleted first
5. Import proceeds with user's choice
6. Success message shows import results

### Export Flow
1. User clicks "Export to JSON" from class menu
2. System gathers all class data and associated klyps
3. JSON file is created and saved to Downloads folder
4. **Share Dialog appears** asking if user wants to share the file
5. User can choose to:
   - **Share**: Opens Android's share chooser with various app options
   - **Done**: Dismisses dialog and completes export
6. If sharing, user selects destination app (email, messaging, cloud storage, etc.)
7. File is shared with appropriate read permissions

## Technical Implementation

### Key Components
- `NewClassViewModel`: Enhanced with duplicate detection logic
- `ClassExportViewModel`: Handles export operations and share functionality
- `NewClassUiState`: Extended with duplicate dialog state
- `DuplicateClassDialog`: User interface for handling duplicates
- `ClassCard`: Updated with export menu option
- `ShareDialog`: Prompts user to share exported files
- `ViewAllClassesScreen`: Integrated with export and share functionality
- `EnhancedHomeScreen`: Includes share dialog for home screen exports

### Database Safety
- **Transaction-like behavior**: If class import fails, klyps are not imported
- **Overwrite protection**: Existing klyps are only deleted if user confirms overwrite
- **Error handling**: Comprehensive error messages for various failure scenarios

### File Management
- Uses Android's MediaStore API for Downloads folder access
- No special permissions required for saving to Downloads
- Automatic file naming prevents conflicts

## Benefits

### For Users
- **No data loss**: Explicit confirmation before overwriting existing classes
- **Complete backups**: Export includes all associated content
- **Clear feedback**: Status messages keep users informed
- **Flexible import**: Supports both new and legacy JSON formats
- **Easy sharing**: One-click sharing after export to any compatible app
- **No manual file hunting**: Direct access to share functionality

### For Developers
- **Robust error handling**: Comprehensive validation and error reporting
- **Extensible format**: Version-aware export format allows future enhancements
- **Clean separation**: Export logic separated into dedicated ViewModel
- **Maintainable code**: Clear separation of concerns and reusable components
- **Native sharing**: Uses Android's built-in share system with proper permissions
- **URI-based sharing**: Secure file sharing through content URIs

## Future Enhancements

### Potential Improvements
1. **Batch operations**: Export/import multiple classes at once
2. **Selective import**: Choose which klyps to import from a class
3. **Merge options**: Combine imported klyps with existing ones instead of replacing
4. **Cloud integration**: Direct import/export from cloud storage services
5. **Share functionality**: Share exported JSON files directly from the app
