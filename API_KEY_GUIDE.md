# API Key Management in Kotlin/Android

This guide explains how to securely manage API keys in your Kotlin Android project, similar to how you use `.env` files in React Native.

## Overview

Unlike React Native which uses `.env` files, Android projects use different approaches for managing environment variables and API keys:

1. **local.properties** - For development (like .env)
2. **BuildConfig** - Compile-time constants
3. **Encrypted SharedPreferences** - Runtime storage

## Setup

### 1. Configure local.properties

Your `local.properties` file now contains your API keys:

```properties
# API Keys - Add your sensitive keys here
TWILIO_ACCOUNT_SID=your_actual_twilio_sid
TWILIO_AUTH_TOKEN=your_actual_auth_token
HUGGING_FACE_API_KEY=your_actual_api_key
```

**Important:** 
- Replace the placeholder values with your actual API keys
- This file is automatically ignored by Git
- Never commit this file to version control

### 2. Access API Keys in Code

Use the `ApiKeyConfig` object to access your API keys:

```kotlin
import com.klypt.ui.common.ApiKeyConfig

// Get Twilio credentials
val twilioSid = ApiKeyConfig.twilioAccountSid
val twilioToken = ApiKeyConfig.twilioAuthToken

// Get Hugging Face API key
val hfApiKey = ApiKeyConfig.huggingFaceApiKey

// Check if keys are configured
if (ApiKeyConfig.isTwilioConfigured()) {
    // Use Twilio services
}
```

### 3. TwilioService Integration

Your `TwilioService` has been updated to automatically:
1. First try to get credentials from user-saved preferences (TokenManager)
2. Fallback to API keys from `local.properties` (BuildConfig)

This provides flexibility for both development and user-configured credentials.

## Build Configuration

The `build.gradle.kts` now includes:

```kotlin
// Load local.properties file
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Inject API keys into BuildConfig
buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"${localProperties.getProperty("TWILIO_ACCOUNT_SID", "")}\"")
buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"${localProperties.getProperty("TWILIO_AUTH_TOKEN", "")}\"")
buildConfigField("String", "HUGGING_FACE_API_KEY", "\"${localProperties.getProperty("HUGGING_FACE_API_KEY", "")}\"")
```

## Comparison with React Native

| React Native | Android/Kotlin |
|--------------|----------------|
| `.env` file | `local.properties` |
| `process.env.API_KEY` | `BuildConfig.API_KEY` or `ApiKeyConfig.apiKey` |
| `react-native-config` | Built-in Gradle support |
| `.env.local`, `.env.prod` | Build variants (debug/release) |

## Adding New API Keys

1. Add the key to `local.properties`:
   ```properties
   NEW_API_KEY=your_new_api_key
   ```

2. Add BuildConfig field in `build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "NEW_API_KEY", "\"${localProperties.getProperty("NEW_API_KEY", "")}\"")
   ```

3. Add getter to `ApiKeyConfig.kt`:
   ```kotlin
   val newApiKey: String
       get() = BuildConfig.NEW_API_KEY.ifEmpty { 
           throw IllegalStateException("NEW_API_KEY not configured in local.properties") 
       }
   ```

## Security Best Practices

1. **Never hardcode API keys** in source code
2. **Use different keys** for debug/release builds
3. **Validate keys** before using them
4. **Use encrypted storage** for user-provided credentials
5. **Rotate keys** regularly

## For Production

For production builds, consider:
- Using Build Variants for different environments
- CI/CD environment variables
- Android Keystore for signing
- ProGuard/R8 obfuscation

## Troubleshooting

If you get "API key not configured" errors:
1. Check `local.properties` exists and contains your keys
2. Clean and rebuild the project
3. Verify BuildConfig is generated correctly
4. Check if `buildConfig = true` is enabled in build.gradle.kts

## Example Usage

```kotlin
class MyApiService @Inject constructor() {
    
    private fun createApiClient(): ApiClient {
        val apiKey = ApiKeyConfig.huggingFaceApiKey
        
        return ApiClient.Builder()
            .baseUrl("https://api.huggingface.co/")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
    }
}
```
