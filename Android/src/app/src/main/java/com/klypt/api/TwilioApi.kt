package com.klypt.api

import retrofit2.Response
import retrofit2.http.*

interface TwilioApi {
    
    @POST("v2/Services/{ServiceSid}/Verifications")
    @FormUrlEncoded
    suspend fun sendVerification(
        @Path("ServiceSid") serviceSid: String,
        @Field("To") to: String,
        @Field("Channel") channel: String = "sms"
    ): Response<TwilioVerificationResponse>
    
    @POST("v2/Services/{ServiceSid}/VerificationCheck")
    @FormUrlEncoded
    suspend fun verifyCode(
        @Path("ServiceSid") serviceSid: String,
        @Field("To") to: String,
        @Field("Code") code: String
    ): Response<TwilioVerificationCheckResponse>
}

data class TwilioVerificationResponse(
    val sid: String,
    val service_sid: String,
    val account_sid: String,
    val to: String,
    val channel: String,
    val status: String,
    val valid: Boolean,
    val date_created: String,
    val date_updated: String,
    val url: String
)

data class TwilioVerificationCheckResponse(
    val sid: String,
    val service_sid: String,
    val account_sid: String,
    val to: String,
    val channel: String?,
    val status: String,
    val valid: Boolean,
    val date_created: String,
    val date_updated: String
)
