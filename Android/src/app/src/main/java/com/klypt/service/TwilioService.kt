package com.klypt.service

import android.util.Log
import com.klypt.api.TwilioApi
import com.klypt.api.TwilioVerificationResponse
import com.klypt.api.TwilioVerificationCheckResponse
import com.klypt.storage.TokenManager
import com.klypt.ui.common.ApiKeyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TwilioService"
@Singleton
class TwilioService @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    private fun createTwilioApi(): TwilioApi {
        // Try to get credentials from TokenManager first (user-saved credentials)
        var accountSid = tokenManager.getTwilioSid()
        var authToken = tokenManager.getTwilioAuthToken()
        
        // If not found in TokenManager, try to get from BuildConfig (local.properties)
        if (accountSid.isNullOrEmpty() || authToken.isNullOrEmpty()) {
            try {
                if (ApiKeyConfig.isTwilioConfigured()) {
                    accountSid = ApiKeyConfig.twilioAccountSid
                    authToken = ApiKeyConfig.twilioAuthToken
                }
            } catch (e: IllegalStateException) {
                // BuildConfig keys not configured, continue with TokenManager approach
            }
        }
        
        if (accountSid.isNullOrEmpty() || authToken.isNullOrEmpty()) {
            throw IllegalStateException("Twilio credentials not found. Please save SID and Auth Token or configure them in local.properties")
        }
        
        val credentials = Credentials.basic(accountSid, authToken)
        
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://verify.twilio.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(TwilioApi::class.java)
    }
    
    suspend fun sendVerification(
        phoneNumber: String,
        channel: String = "sms"
    ): Result<TwilioVerificationResult> = withContext(Dispatchers.IO) {
        try {
            val api = createTwilioApi()
            val serviceSid = ApiKeyConfig.twilioVerificationServiceSid

            if (serviceSid.isNullOrEmpty())
                return@withContext Result.failure(IllegalStateException("Twilio Verification Service SID not found"))
            
            val response = api.sendVerification(serviceSid, phoneNumber, channel.uppercase())

            Log.d(TAG, "Response: $response")
            
            if (response.isSuccessful) {
                val verification = response.body()!!
                Result.success(
                    TwilioVerificationResult(
                        sid = verification.sid,
                        serviceSid = verification.service_sid,
                        accountSid = verification.account_sid,
                        to = verification.to,
                        channel = verification.channel,
                        status = verification.status,
                        valid = verification.valid,
                        dateCreated = verification.date_created,
                        dateUpdated = verification.date_updated,
                        url = verification.url
                    )
                )
            } else {
                Result.failure(Exception("Failed to send verification: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyCode(
        phoneNumber: String,
        code: String
    ): Result<TwilioVerificationCheckResult> = withContext(Dispatchers.IO) {
        try {
            val api = createTwilioApi()
            val serviceSid = ApiKeyConfig.twilioVerificationServiceSid

            if(serviceSid.isNullOrEmpty())
                return@withContext Result.failure(IllegalStateException("Twilio Verification Service SID not found"))
            
            val response = api.verifyCode(serviceSid, phoneNumber, code)
            
            if (response.isSuccessful) {
                val verificationCheck = response.body()!!
                Result.success(
                    TwilioVerificationCheckResult(
                        sid = verificationCheck.sid,
                        serviceSid = verificationCheck.service_sid,
                        accountSid = verificationCheck.account_sid,
                        to = verificationCheck.to,
                        channel = verificationCheck.channel ?: "",
                        status = verificationCheck.status,
                        valid = verificationCheck.valid,
                        dateCreated = verificationCheck.date_created,
                        dateUpdated = verificationCheck.date_updated
                    )
                )
            } else {
                Result.failure(Exception("Failed to verify code: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class TwilioVerificationResult(
    val sid: String,
    val serviceSid: String,
    val accountSid: String,
    val to: String,
    val channel: String,
    val status: String,
    val valid: Boolean,
    val dateCreated: String,
    val dateUpdated: String,
    val url: String
)

data class TwilioVerificationCheckResult(
    val sid: String,
    val serviceSid: String,
    val accountSid: String,
    val to: String,
    val channel: String,
    val status: String,
    val valid: Boolean,
    val dateCreated: String,
    val dateUpdated: String
)
