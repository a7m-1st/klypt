package com.klypt.repository

import com.klypt.service.TwilioService
import com.klypt.service.TwilioVerificationResult
import com.klypt.service.TwilioVerificationCheckResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwilioRepository @Inject constructor(
    private val twilioService: TwilioService
) {
    
    suspend fun sendVerification(
        phoneNumber: String,
        channel: String = "sms"
    ): Result<TwilioVerificationResult> {
        return twilioService.sendVerification(phoneNumber, channel)
    }
    
    suspend fun verifyCode(
        phoneNumber: String,
        code: String
    ): Result<TwilioVerificationCheckResult> {
        return twilioService.verifyCode(phoneNumber, code)
    }
}
