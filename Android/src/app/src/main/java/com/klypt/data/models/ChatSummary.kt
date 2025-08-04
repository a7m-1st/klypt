package com.klypt.data.models

import java.text.SimpleDateFormat
import java.util.*

data class ChatSummary(
    val _id: String,
    val type: String = "chat_summary",
    val userId: String, // Student or Educator ID
    val userRole: String, // "student" or "educator"
    val classCode: String, // Associated class
    val sessionTitle: String,
    val bulletPointSummary: String, // Main formatted summary
    val originalMessages: List<SummaryMessage>, // Original message data for reference
    val modelUsed: String, // Which AI model was used for the conversation
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
    val isSharedWithEducator: Boolean = false // For students to share with educator
)

data class SummaryMessage(
    val content: String,
    val type: String, // "text", "audio", "image"
    val timestamp: Long,
    val isUser: Boolean // true if from user, false if from AI
)

/**
 * Helper function to generate a unique ID for ChatSummary
 */
fun generateChatSummaryId(userId: String, classCode: String): String {
    val timestamp = System.currentTimeMillis()
    return "summary_${userId}_${classCode}_$timestamp"
}
