# Educator ID Import Enhancement

## Overview

This document describes the enhancement made to the class import functionality to support proper educator ID assignment and enrollment, extending the previously implemented student ID support.

## Issue Addressed

**Problem**: When importing classes through both class code import and JSON file import, the `educatorId` field was being set to a hardcoded default value "imported_educator" instead of using the current logged-in educator's ID.

**Impact**: This caused:
- Loss of proper class ownership for educator users
- Incorrect class associations in educator profiles
- Missing class enrollment in educator's `classIds` array

## Solution Implemented

### Enhanced NewClassViewModel Constructor

Added `EducatorRepository` dependency injection to enable educator data management:

```kotlin
@HiltViewModel
class NewClassViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository,
    private val userContextProvider: UserContextProvider,
    private val studentRepository: StudentRepository,
    private val educatorRepository: EducatorRepository  // New dependency
) : ViewModel()
```

### Import Dependencies

Added necessary imports for user role handling:

```kotlin
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.UserRole
```

## Key Changes

### 1. Class Code Import Enhancement (`importClassByCode`)

**Before**: Only handled student enrollment
**After**: Handles both student and educator enrollment

#### New Logic:
- Detects current user role (Student or Educator)
- If user is an educator and the class has missing/default educatorId, sets the current user as the educator
- Creates proper educator records if they don't exist
- Updates educator's `classIds` array with the imported class

#### Code Example:
```kotlin
// Also update educatorId if current user is an educator and educatorId is missing/default
if (currentUserRole == UserRole.EDUCATOR) {
    val currentEducatorId = classData["educatorId"] as? String
    if (currentEducatorId.isNullOrEmpty() || currentEducatorId == "imported_educator") {
        updatedClassData["educatorId"] = currentUserId
    }
}
```

### 2. JSON Import Enhancement (`performImport`)

#### Enhanced Educator ID Assignment:
```kotlin
if (!containsKey("educatorId")) {
    // Check if current user is an educator and set them as the educator
    val currentUserId = userContextProvider.getCurrentUserId()
    val currentUserRole = try {
        userContextProvider.getCurrentUserRole()
    } catch (e: Exception) {
        null
    }
    
    if (currentUserRole == UserRole.EDUCATOR && currentUserId.isNotEmpty()) {
        put("educatorId", currentUserId)
    } else {
        put("educatorId", "imported_educator")
    }
}
```

#### Comprehensive Educator Enrollment:
- Creates educator records if they don't exist
- Uses `UserContextProvider.getUserDisplayName()` for proper naming
- Handles educator `classIds` array updates
- Provides fallback values for all required educator fields

## Educator Record Creation

### Default Educator Record Structure:
```kotlin
educatorData = mapOf(
    "_id" to currentUserId,
    "type" to "educator",
    "fullName" to displayName,
    "age" to 0,
    "currentJob" to "",
    "instituteName" to "",
    "phoneNumber" to currentUserId, // Uses userId as phone for educators
    "verified" to false,
    "recoveryCode" to "",
    "classIds" to emptyList<String>()
)
```

### Name Resolution Logic:
1. **Primary**: Uses `userContextProvider.getCurrentUserDisplayName()`
2. **Fallback**: Uses "Imported Educator" if display name unavailable

## Error Handling

### Robust Error Management:
- All educator operations are wrapped in try-catch blocks
- Errors don't fail the overall import process
- Detailed logging for debugging purposes
- Graceful degradation when educator operations fail

### Error Log Examples:
```kotlin
android.util.Log.w("NewClassViewModel", "Could not update educator class enrollment: ${e.message}")
android.util.Log.d("NewClassViewModel", "Updated educator classIds for user: $currentUserId")
```

## Role Detection

### Safe Role Detection:
```kotlin
val currentUserRole = try {
    userContextProvider.getCurrentUserRole()
} catch (e: Exception) {
    null
}
```

This prevents crashes when user context is unavailable and allows graceful handling of edge cases.

## Benefits

### For Educators:
1. **Proper Ownership**: Classes are correctly assigned to the importing educator
2. **Profile Integration**: Imported classes appear in educator's class list
3. **Consistent Experience**: Same enrollment behavior as creating new classes

### For Students:
1. **Maintained Functionality**: All existing student enrollment features preserved
2. **Dual Support**: Students can still import classes and get enrolled properly

### For System:
1. **Data Integrity**: Proper relationships between users and classes
2. **Role Awareness**: Import behavior adapts to user role
3. **Backward Compatibility**: Existing functionality remains intact

## Testing Scenarios

### As an Educator:
1. **Import by Class Code**: 
   - Should set you as the educator if educatorId is missing/default
   - Should add class to your `classIds` array
   - Should create educator record if doesn't exist

2. **Import from JSON**:
   - Should set you as educator for new classes
   - Should handle both new and legacy JSON formats
   - Should create complete educator record if needed

### As a Student:
1. **Import Functions**: Should work exactly as before
2. **Class Enrollment**: Should add to `enrolledClassIds` as expected

### Edge Cases:
1. **Missing User Context**: Should fallback to "imported_educator"
2. **Role Detection Failure**: Should handle gracefully
3. **Database Errors**: Should log but not crash import process

## Related Files Modified

1. **NewClassViewModel.kt**
   - Added EducatorRepository dependency
   - Enhanced importClassByCode function
   - Enhanced performImport function
   - Added UserRole import

## Future Considerations

1. **Educator Verification**: Consider checking educator verification status
2. **Permission System**: Could implement role-based permissions for import operations
3. **Bulk Operations**: Could optimize for importing multiple classes
4. **Sync Support**: Could add synchronization logic for multi-device scenarios

## Dependencies

This enhancement relies on:
- **EducatorRepository**: For educator data management
- **UserContextProvider**: For role detection and user information
- **UserRole enum**: For role-based logic
- **Hilt Dependency Injection**: For repository injection

## Backward Compatibility

✅ **Fully Backward Compatible**: All existing functionality preserved
✅ **Progressive Enhancement**: New features only activate when appropriate
✅ **Fallback Support**: Graceful degradation for edge cases
