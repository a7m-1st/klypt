# Class Import JSON Format

When importing a class from a JSON file, the file should contain the following structure:

## Required Fields
- `classCode`: Unique identifier for the class (e.g., "CS101")
- `classTitle`: Display name for the class (e.g., "Computer Science 101")

## Optional Fields
- `educatorId`: ID of the educator who owns the class (defaults to "imported_educator")
- `studentIds`: Array of student IDs enrolled in the class (defaults to empty array)
- `updatedAt`: Timestamp of last update (defaults to current time)
- `lastSyncedAt`: Timestamp of last sync (defaults to current time)

## Example JSON Structure

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

## Minimal Example

```json
{
    "classCode": "MATH101",
    "classTitle": "Mathematics 101"
}
```

The import process will automatically add the missing fields with default values if they are not provided.
