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

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_TWILIO_SID = "twilio_sid"
        private const val KEY_TWILIO_AUTH_TOKEN = "twilio_auth_token"
        private const val KEY_TWILIO_SERVICE_SID = "twilio_service_sid"
    }
}