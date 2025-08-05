# Database Explorer Documentation

This document explains how to use the new database exploration features that have been added to the Klypt application.

## Overview

The database exploration system provides comprehensive tools to:
- View all database content
- Search across collections
- Analyze data integrity
- Generate analytics
- Debug database issues
- Export database content

## Components

### 1. EducationalContentRepository (Enhanced)

The main repository now includes the following exploration methods:

#### Core Exploration Methods
- `getDatabaseOverview()` - Get overview with counts and sample data
- `getAllRawDocuments(collectionType)` - Get all raw documents from a collection
- `getDocumentDetails(collectionType, documentId)` - Get detailed info about a specific document
- `searchAllCollections(searchTerm)` - Search across all collections
- `getDatabaseAnalytics()` - Get comprehensive analytics
- `exportAllDatabaseContent()` - Export all data in structured format

#### Collection Types
```kotlin
enum class DatabaseCollectionType {
    STUDENTS, EDUCATORS, CLASSES, KLYPS
}
```

### 2. DatabaseExplorer Utility

A dedicated utility class for debugging and exploration:

#### Methods
- `printDatabaseOverview()` - Print overview to logs
- `printAllDocuments(collectionType)` - Print all documents of a type
- `printDocumentDetails(collectionType, documentId)` - Print specific document details
- `searchAndPrint(searchTerm)` - Search and print results
- `printDatabaseAnalytics()` - Print analytics to logs
- `exportDatabaseToLogs()` - Export all data to logs
- `runHealthCheck()` - Comprehensive health check
- `findIncompleteStudents()` - Find students with missing data
- `findOrphanedReferences()` - Find broken references between collections

### 3. DatabaseExplorerActivity (UI)

A debug activity with a user-friendly interface featuring:

#### Tabs
1. **Overview** - Database overview with statistics
2. **Search** - Search functionality across all collections
3. **Analytics** - Data analytics and insights
4. **Actions** - Various debugging actions

## Usage Examples

### 1. Basic Usage in Code

```kotlin
@Inject
lateinit var educationalContentRepository: EducationalContentRepository

@Inject 
lateinit var databaseExplorer: DatabaseExplorer

// Get database overview
lifecycleScope.launch {
    val overview = educationalContentRepository.getDatabaseOverview()
    Log.d("Database", "Overview: $overview")
}

// Search all collections
lifecycleScope.launch {
    val results = educationalContentRepository.searchAllCollections("john")
    Log.d("Database", "Search results: $results")
}

// Get analytics
lifecycleScope.launch {
    val analytics = educationalContentRepository.getDatabaseAnalytics()
    Log.d("Database", "Analytics: $analytics")
}

// Run health check
lifecycleScope.launch {
    databaseExplorer.runHealthCheck()
}
```

### 2. Using the Debug Activity

Add the activity to your `AndroidManifest.xml`:

```xml
<activity
    android:name=".ui.debug.DatabaseExplorerActivity"
    android:exported="false"
    android:theme="@style/Theme.Klypt" />
```

Launch it from your app:

```kotlin
// In debug builds only
if (BuildConfig.DEBUG) {
    startActivity(Intent(this, DatabaseExplorerActivity::class.java))
}
```

### 3. Getting Specific Collection Data

```kotlin
// Get all raw student documents
lifecycleScope.launch {
    val students = educationalContentRepository.getAllRawDocuments(
        EducationalContentRepository.DatabaseCollectionType.STUDENTS
    )
    students.forEach { student ->
        Log.d("Database", "Student: $student")
    }
}

// Get details for a specific student
lifecycleScope.launch {
    val details = educationalContentRepository.getDocumentDetails(
        EducationalContentRepository.DatabaseCollectionType.STUDENTS,
        "student_john_doe"
    )
    Log.d("Database", "Student details: $details")
}
```

### 4. Analytics and Health Checks

```kotlin
// Get comprehensive analytics
lifecycleScope.launch {
    val analytics = educationalContentRepository.getDatabaseAnalytics()
    
    val overview = analytics["overview"] as? Map<String, Any>
    val totalStudents = overview?.get("total_students")
    val totalClasses = overview?.get("total_classes")
    
    Log.d("Analytics", "Students: $totalStudents, Classes: $totalClasses")
}

// Run health checks
lifecycleScope.launch {
    databaseExplorer.runHealthCheck()
    databaseExplorer.findIncompleteStudents()
    databaseExplorer.findOrphanedReferences()
}
```

## Understanding the Output

### Database Overview Structure
```kotlin
{
    "students": {
        "total_count": 10,
        "sample_raw_data": [...],
        "mapped_count": 8,
        "sample_mapped": [...]
    },
    "educators": {...},
    "classes": {...},
    "klyps": {...},
    "database_health": {
        "total_documents": 50,
        "seeded": true,
        "cache_status": {...}
    }
}
```

### Analytics Structure
```kotlin
{
    "overview": {
        "total_students": 10,
        "total_educators": 3,
        "total_classes": 5,
        "total_klyps": 15
    },
    "class_analytics": {
        "average_students_per_class": 2.5,
        "enrollment_distribution": {...}
    },
    "data_quality": {
        "data_integrity_score": 85.5,
        "students_with_empty_fields": 2
    },
    "mapping_success_rates": {...}
}
```

## Best Practices

### 1. Use in Debug Builds Only
```kotlin
if (BuildConfig.DEBUG) {
    // Database exploration code
}
```

### 2. Check Logs
All exploration methods output detailed information to logs with tag "DatabaseExplorer" or "EducationalContentRepository".

### 3. Regular Health Checks
Run health checks periodically during development:
```kotlin
// In your debug menu or admin panel
databaseExplorer.runHealthCheck()
```

### 4. Monitor Data Quality
```kotlin
lifecycleScope.launch {
    val analytics = educationalContentRepository.getDatabaseAnalytics()
    val dataQuality = analytics["data_quality"] as? Map<String, Any>
    val integrityScore = dataQuality?.get("data_integrity_score") as? Double
    
    if (integrityScore != null && integrityScore < 80.0) {
        Log.w("Database", "Data integrity score is low: $integrityScore%")
        // Take corrective action
    }
}
```

## Troubleshooting

### Common Issues

1. **Empty or Null Data**: Check if database seeding completed successfully
2. **Mapping Failures**: Look at mapping success rates in analytics
3. **Orphaned References**: Use `findOrphanedReferences()` to identify broken links
4. **Performance Issues**: Monitor document counts and consider pagination

### Debug Steps

1. Run `runHealthCheck()` first to get overall status
2. Use `printDatabaseOverview()` to see high-level statistics
3. Check specific collections with `printAllDocuments()`
4. Search for specific issues with `searchAllCollections()`
5. Use `getDatabaseAnalytics()` for detailed analysis

## Security Notes

- These features should only be available in debug builds
- Never expose database exploration endpoints in production
- Be careful with logging sensitive information
- Consider adding authentication for debug features

## Adding Custom Exploration Features

You can extend the system by:

1. Adding new methods to `EducationalContentRepository`
2. Creating custom analytics in `getDatabaseAnalytics()`
3. Adding new actions to `DatabaseExplorer`
4. Extending the UI in `DatabaseExplorerActivity`

Example custom method:
```kotlin
suspend fun getCustomAnalytics(): Map<String, Any> {
    // Your custom analytics logic
    return mapOf(
        "custom_metric" to calculateCustomMetric(),
        "custom_data" to getCustomData()
    )
}
```
