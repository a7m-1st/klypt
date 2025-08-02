package com.klypt.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface TwilioApiService {
    
    @FormUrlEncoded
    @POST("v2/Services/{serviceSid}/Verifications")
    suspend fun sendVerification(
        @Path("serviceSid") serviceSid: String,
        @Field("To") phoneNumber: String,
        @Field("Channel") channel: String = "sms",
        @Header("Authorization") authorization: String
    ): Response<TwilioVerificationResponse>
    
    @FormUrlEncoded
    @POST("v2/Services/{serviceSid}/VerificationCheck")
    suspend fun verifyCode(
        @Path("serviceSid") serviceSid: String,
        @Field("To") phoneNumber: String,
        @Field("Code") code: String,
        @Header("Authorization") authorization: String
    ): Response<TwilioVerificationCheckResponse>
}

data class TwilioVerificationResponse(
    val sid: String,
    val serviceSid: String,
    val accountSid: String,
    val to: String,
    val channel: String,
    val status: String,
    val valid: Boolean,
    val dateCreated: String,
    val dateUpdated: String,
    val lookup: Map<String, Any>?,
    val amount: String?,
    val payee: String?,
    val sendCodeAttempts: List<Map<String, Any>>?,
    val url: String
)

data class TwilioVerificationCheckResponse(
    val sid: String,
    val serviceSid: String,
    val accountSid: String,
    val to: String,
    val channel: String,
    val status: String,
    val valid: Boolean,
    val amount: String?,
    val payee: String?,
    val dateCreated: String,
    val dateUpdated: String
)

data class TwilioErrorResponse(
    val code: Int,
    val message: String,
    val moreInfo: String,
    val status: Int
)
