# Class Export/Import Feature

This feature allows users to export and import classes with all their related educational content (klyps) in JSON format.

## Features Implemented

### Export Functionality
- **Location**: Class cards now include an "Export to JSON" option in their menu
- **Data Included**: 
  - Complete class details (code, title, educator, students, timestamps)
  - All related klyps with their content and questions
  - Export metadata (version, timestamp, klyp count)
- **File Format**: `{classCode}_{className}_export.json`
- **Storage**: Files are saved to the device's Downloads folder

### Import Functionality  
- **Backward Compatibility**: Supports both new format (with klyps) and legacy format (class only)
- **Auto-population**: Missing fields are automatically filled with appropriate defaults
- **Validation**: Ensures required fields are present before import
- **Feedback**: Shows success/error messages with import details

### Updated Components

1. **ClassExportViewModel**: New ViewModel handling export operations
2. **NewClassViewModel**: Enhanced import function supporting both formats
3. **ClassCard**: Added export menu option
4. **ViewAllClassesScreen**: Integrated export functionality with user feedback
5. **EnhancedHomeScreen**: Added export support to class cards on home screen

### JSON Format Structure

#### New Format (v1.0) - With Klyps
```json
{
    "exportVersion": "1.0",
    "exportTimestamp": "timestamp",
    "classDetails": {
        "classCode": "CS101",
        "classTitle": "Computer Science 101",
        // ... other class fields
    },
    "klyps": [
        {
            "_id": "klyp_123",
            "title": "Klyp Title",
            "mainBody": "Content...",
            "questions": [...],
            // ... other klyp fields
        }
    ],
    "klypCount": 1
}
```

#### Legacy Format - Class Only
```json
{
    "classCode": "CS101", 
    "classTitle": "Computer Science 101",
    // ... other class fields
}
```

## Usage

### To Export a Class:
1. Navigate to any screen showing class cards (Home or View All Classes)
2. Click the menu (⋮) on a class card
3. Select "Export to JSON"
4. File will be saved to Downloads folder
5. Success message shows the export location

### To Import a Class:
1. Navigate to "Add New Class" screen
2. Select "Import from JSON File"
3. Choose a JSON file from your device
4. Class and klyps will be imported to the database
5. Success message shows import results

## Technical Implementation

- **MediaStore API**: Used for writing files to Downloads folder (Android 10+)
- **Gson**: JSON serialization/deserialization  
- **Coroutines**: Async file operations
- **Hilt**: Dependency injection for repositories
- **StateFlow**: Reactive UI state management

## Benefits

1. **Data Portability**: Easy sharing of classes between users/devices
2. **Backup**: Export classes for backup purposes
3. **Content Distribution**: Teachers can share complete course content
4. **Migration**: Move classes between different app installations
5. **Collaboration**: Share educational content with other educators

## Error Handling

- File access permission checks
- JSON validation and parsing errors  
- Database operation failures
- User-friendly error messages
- Graceful fallbacks for missing data
