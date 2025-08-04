package com.klypt.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun saveTwilioSid(sid: String) {
        sharedPreferences.edit().putString(KEY_TWILIO_SID, sid).apply()
    }

    fun getTwilioSid(): String? {
        return sharedPreferences.getString(KEY_TWILIO_SID, null)
    }

    fun saveTwilioAuthToken(authToken: String) {
        sharedPreferences.edit().putString(KEY_TWILIO_AUTH_TOKEN, authToken).apply()
    }

    fun getTwilioAuthToken(): String? {
        return sharedPreferences.getString(KEY_TWILIO_AUTH_TOKEN, null)
    }

    fun saveTwilioServiceSid(serviceSid: String) {
        sharedPreferences.edit().putString(KEY_TWILIO_SERVICE_SID, serviceSid).apply()
    }

    fun getTwilioServiceSid(): String? {
        return sharedPreferences.getString(KEY_TWILIO_SERVICE_SID, null)
    }
    
    /**
     * Save student identification for session persistence
     */
    fun saveStudentIdentification(firstName: String, lastName: String) {
        val studentData = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "userType" to "STUDENT"
        )
        val jsonData = gson.toJson(studentData)
        sharedPreferences.edit().putString(KEY_USER_IDENTIFICATION, jsonData).apply()
    }
    
    /**
     * Get stored student identification
     * @return Pair of (firstName, lastName) or null if not found
     */
    fun getStudentIdentification(): Pair<String, String>? {
        val jsonData = sharedPreferences.getString(KEY_USER_IDENTIFICATION, null)
        return jsonData?.let { 
            try {
                val studentData = gson.fromJson(it, Map::class.java) as Map<*, *>
                if (studentData["userType"] == "STUDENT") {
                    val firstName = studentData["firstName"] as? String
                    val lastName = studentData["lastName"] as? String
                    if (firstName != null && lastName != null) {
                        Pair(firstName, lastName)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Save educator identification for session persistence
     */
    fun saveEducatorIdentification(phoneNumber: String, fullName: String?) {
        val educatorData = mapOf(
            "phoneNumber" to phoneNumber,
            "fullName" to (fullName ?: ""),
            "userType" to "EDUCATOR"
        )
        val jsonData = gson.toJson(educatorData)
        sharedPreferences.edit().putString(KEY_USER_IDENTIFICATION, jsonData).apply()
    }
    
    /**
     * Get stored educator identification
     * @return Pair of (phoneNumber, fullName) or null if not found
     */
    fun getEducatorIdentification(): Pair<String, String>? {
        val jsonData = sharedPreferences.getString(KEY_USER_IDENTIFICATION, null)
        return jsonData?.let {
            try {
                val educatorData = gson.fromJson(it, Map::class.java) as Map<*, *>
                if (educatorData["userType"] == "EDUCATOR") {
                    val phoneNumber = educatorData["phoneNumber"] as? String
                    val fullName = educatorData["fullName"] as? String ?: ""
                    if (phoneNumber != null) {
                        Pair(phoneNumber, fullName)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_USER_IDENTIFICATION = "user_identification" 
        private const val KEY_TWILIO_SID = "twilio_sid"
        private const val KEY_TWILIO_AUTH_TOKEN = "twilio_auth_token"
        private const val KEY_TWILIO_SERVICE_SID = "twilio_service_sid"
    }
}