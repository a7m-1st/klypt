/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klypt.ui.common

import com.klypt.BuildConfig

/**
 * Central configuration object for API keys and secrets.
 * 
 * This object provides secure access to API keys that are injected at build time
 * from local.properties file. Never hardcode sensitive keys directly in source code.
 */
object ApiKeyConfig {
    
    // Twilio Configuration
    val twilioAccountSid: String
        get() = BuildConfig.TWILIO_ACCOUNT_SID.ifEmpty { 
            throw IllegalStateException("TWILIO_ACCOUNT_SID not configured in local.properties") 
        }
    
    val twilioAuthToken: String
        get() = BuildConfig.TWILIO_AUTH_TOKEN.ifEmpty { 
            throw IllegalStateException("TWILIO_AUTH_TOKEN not configured in local.properties") 
        }
    
    val twilioVerificationServiceSid: String
        get() = BuildConfig.TWILIO_VERIFICATION_SERVICE_SID.ifEmpty { 
            throw IllegalStateException("TWILIO_VERIFICATION_SERVICE_SID not configured in local.properties") 
        }
    
    // Hugging Face Configuration
    val huggingFaceApiKey: String
        get() = BuildConfig.HUGGING_FACE_API_KEY.ifEmpty { 
            throw IllegalStateException("HUGGING_FACE_API_KEY not configured in local.properties") 
        }
    
    // Helper methods to check if keys are configured
    fun isTwilioConfigured(): Boolean {
        return BuildConfig.TWILIO_ACCOUNT_SID.isNotEmpty() && 
               BuildConfig.TWILIO_AUTH_TOKEN.isNotEmpty() &&
               BuildConfig.TWILIO_VERIFICATION_SERVICE_SID.isNotEmpty()
    }
    
    fun isHuggingFaceConfigured(): Boolean {
        return BuildConfig.HUGGING_FACE_API_KEY.isNotEmpty()
    }
}
