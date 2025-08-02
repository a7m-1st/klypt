# Login Integration with Student and Educator Repositories

## Overview

The login page has been updated to use the `StudentRepository` for student login and `EducatorRepository` for educator login, eliminating the need for a generic `User.kt` class. This enables role-specific offline login capabilities and persistent user data storage using CouchDB Lite.

## Changes Made

### 1. Updated AuthRepository
- **File**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\repository\AuthRepository.kt`
- **Purpose**: Handles authentication logic and integrates with role-specific repositories
- **Features**:
  - Network-based login with fallback to offline mode
  - Role-based user data storage (Student or Educator)
  - User search functionality specific to each role
  - Statistics and user management per role
- **Key Changes**:
  - Added `UserRole` parameter to login methods
  - Uses `StudentRepository` for student operations
  - Uses `EducatorRepository` for educator operations
  - Returns appropriate data models (`Student` or `Educator`) instead of generic `User`

### 2. Enhanced StudentRepository
- **File**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\data\repositories\StudentRepository.kt`
- **Added Methods**:
  - `searchStudents(query: String)`: Search students by name
  - `getAllStudents()`: Retrieve all students from the database
- **Purpose**: Provides comprehensive student data management

### 3. Enhanced EducatorRepository
- **File**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\data\repositories\EducatorRepository.kt`
- **Added Methods**:
  - `searchEducators(query: String)`: Search educators by full name
  - `getAllEducators()`: Retrieve all educators from the database
- **Purpose**: Provides comprehensive educator data management

### 4. Data Models Used
- **Student Model**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\data\models\Student.kt`
  - Fields: `_id`, `type`, `firstName`, `lastName`, `recoveryCode`, `enrolledClassIds`, `createdAt`, `updatedAt`
- **Educator Model**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\data\models\Educator.kt`
  - Fields: `_id`, `type`, `fullName`, `age`, `currentJob`, `instituteName`, `phoneNumber`, `verified`, `recoveryCode`, `classIds`

### 5. Updated Database Module
- **File**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\di\DatabaseModule.kt`
- **Additions**:
  - Added `EducatorRepository` singleton provision
  - Maintains existing `StudentRepository` and `DatabaseManager` provisions

### 6. Updated LoginViewModel
- **File**: `c:\Users\ASUS\Desktop\gallery\Android\src\app\src\main\java\com\klypt\ui\login\LoginViewModel.kt`
- **Changes**:
  - Passes user role to all AuthRepository methods
  - Handles role-specific user search results
  - Provides role-specific offline statistics

## How It Works

### Login Flow
1. User selects role (Student or Educator) and enters credentials
2. AuthRepository attempts network login via API with role information
3. If successful, user data is saved locally using the appropriate repository:
   - Students → StudentRepository
   - Educators → EducatorRepository
4. If network fails, AuthRepository checks for existing local user data in the role-specific repository
5. If local data exists, user can login in offline mode

### Data Storage
- **Student data** is stored in CouchDB Lite with document ID format: `student::{firstName}_{lastName}`
  - Contains: firstName, lastName, enrolledClassIds, recovery codes, timestamps
- **Educator data** is stored in CouchDB Lite with document ID format: `educator::{firstName}_{lastName}`
  - Contains: fullName, institution details, verification status, classIds, contact info

### Role-Based Features
- **Students**: Search by firstName/lastName, track enrolled classes
- **Educators**: Search by fullName, manage class assignments, verification status
- **Offline Capabilities**: Both roles can login offline if previously authenticated

### Search Functionality
- Students can be searched by first name or last name
- Educators can be searched by full name
- Results are role-specific and type-safe

## Testing the Integration

To test the role-based login integration:

1. **Student Network Login**: 
   - Select "Student" role
   - Try logging in with valid student credentials when network is available
   
2. **Educator Network Login**:
   - Select "Educator" role  
   - Try logging in with valid educator credentials when network is available

3. **Offline Login**: 
   - Disable network and try logging in with previously used credentials for both roles

4. **Role-Specific Search**: 
   - Use the search functionality to find users by name for each role
   - Verify students show firstName/lastName format
   - Verify educators show fullName format

5. **Data Persistence**: 
   - Close and reopen the app to verify role-specific data persistence

## Dependencies

The integration relies on:
- CouchDB Lite for local database storage
- Hilt for dependency injection  
- Retrofit for network calls
- Coroutines for async operations
- EncryptedSharedPreferences for secure token storage
- Role-specific data models (Student, Educator)

## Future Enhancements

- Add data synchronization between local and remote databases
- Implement role-based access control for app features
- Add more robust error handling and retry mechanisms
- Implement user profile update functionality per role
- Add role-specific dashboard features
- Implement class enrollment/management workflows
