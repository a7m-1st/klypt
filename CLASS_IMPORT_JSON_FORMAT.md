# Class Import/Export JSON Format

When importing or exporting a class from/to a JSON file, the file can contain the following structure:

## New Format (v1.0) - With Klyps

The recommended format that includes all class details and related educational content:

```json
{
    "exportVersion": "1.0",
    "exportTimestamp": "1640995200000",
    "classDetails": {
        "classCode": "CS101",
        "classTitle": "Computer Science 101",
        "educatorId": "educator_001",
        "studentIds": ["student_001", "student_002", "student_003"],
        "updatedAt": "1640995200000",
        "lastSyncedAt": "1640995200000"
    },
    "klyps": [
        {
            "_id": "klyp_123",
            "type": "klyp",
            "title": "Introduction to Programming",
            "mainBody": "This klyp covers basic programming concepts...",
            "questions": [
                {
                    "questionText": "What is a variable?",
                    "options": ["A container for data", "A function", "A loop", "An operator"],
                    "correctAnswer": "A"
                }
            ],
            "createdAt": "1640995200000"
        }
    ],
    "klypCount": 1
}
```

## Legacy Format - Class Only

The older format that only includes class information:

```json
{
    "classCode": "CS101",
    "classTitle": "Computer Science 101",
    "educatorId": "educator_001",
    "studentIds": ["student_001", "student_002", "student_003"],
    "updatedAt": "1640995200000",
    "lastSyncedAt": "1640995200000"
}
```

## Required Fields

### For New Format:
- `classDetails.classCode`: Unique identifier for the class (e.g., "CS101")
- `classDetails.classTitle`: Display name for the class (e.g., "Computer Science 101")

### For Legacy Format:
- `classCode`: Unique identifier for the class (e.g., "CS101")
- `classTitle`: Display name for the class (e.g., "Computer Science 101")

## Optional Fields

### Class Details:
- `educatorId`: ID of the educator who owns the class (defaults to "imported_educator")
- `studentIds`: Array of student IDs enrolled in the class (defaults to empty array)
- `updatedAt`: Timestamp of last update (defaults to current time)
- `lastSyncedAt`: Timestamp of last sync (defaults to current time)

### Export Metadata (New Format Only):
- `exportVersion`: Version of the export format (currently "1.0")
- `exportTimestamp`: When the export was created
- `klypCount`: Number of klyps included in the export

### Klyp Details:
- `_id`: Unique identifier for the klyp (auto-generated if not provided)
- `type`: Type of content (defaults to "klyp")
- `title`: Title of the educational content (defaults to "Imported Klyp")
- `mainBody`: Main content/body of the klyp (defaults to empty string)
- `questions`: Array of quiz questions (defaults to empty array)
- `createdAt`: Creation timestamp (defaults to current time)

## Export Process

Classes can be exported to JSON format with all their related klyps by:
1. Using the "Export to JSON" option in the class card menu
2. The file will be saved to the Downloads folder with format: `{classCode}_{className}_export.json`
3. The export includes all class details and all related educational content (klyps)

## Import Process

The import process supports both formats:
1. **New Format**: Imports the class and all associated klyps
2. **Legacy Format**: Imports only the class information

Both formats will create a new class in the database with auto-generated IDs if not provided.

## Minimal Example

```json
{
    "classCode": "MATH101",
    "classTitle": "Mathematics 101"
}
```

The import process will automatically add the missing fields with default values if they are not provided.
