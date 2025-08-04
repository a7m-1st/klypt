package com.klypt.data.services

import com.klypt.data.DatabaseManager
import com.klypt.data.UserRole
import com.klypt.data.models.ChatSummary
import com.klypt.data.models.SummaryMessage
import com.klypt.data.models.generateChatSummaryId
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.common.chat.ChatMessageAudioClip
import com.klypt.ui.common.chat.ChatMessageText
import com.klypt.ui.common.chat.ChatSide
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSummaryService @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    
    /**
     * Generates a bullet point summary from chat messages
     */
    fun generateBulletPointSummary(messages: List<ChatMessage>): String {
        val summaryBuilder = StringBuilder()
        summaryBuilder.append("## Chat Session Summary\n\n")
        
        val userMessages = messages.filter { it.side == ChatSide.USER }
        val agentMessages = messages.filter { it.side == ChatSide.AGENT }
        
        // User questions/inputs section
        if (userMessages.isNotEmpty()) {
            summaryBuilder.append("### Questions Asked / Input Provided:\n")
            userMessages.forEachIndexed { index, message ->
                when (message) {
                    is ChatMessageText -> {
                        val content = message.content.trim()
                        if (content.isNotEmpty()) {
                            summaryBuilder.append("• ${content.take(200)}${if (content.length > 200) "..." else ""}\n")
                        }
                    }
                    is ChatMessageAudioClip -> {
                        summaryBuilder.append("• [Audio input provided - ${getDurationText(message)}]\n")
                    }
                }
            }
            summaryBuilder.append("\n")
        }
        
        // AI responses section
        if (agentMessages.isNotEmpty()) {
            summaryBuilder.append("### AI Responses & Key Points:\n")
            agentMessages.forEachIndexed { index, message ->
                when (message) {
                    is ChatMessageText -> {
                        val content = message.content.trim()
                        if (content.isNotEmpty()) {
                            // Try to extract key points from AI response
                            val keyPoints = extractKeyPoints(content)
                            keyPoints.forEach { point ->
                                summaryBuilder.append("• $point\n")
                            }
                        }
                    }
                }
            }
            summaryBuilder.append("\n")
        }
        
        // Session metadata
        summaryBuilder.append("### Session Information:\n")
        summaryBuilder.append("• Total Messages: ${messages.size}\n")
        summaryBuilder.append("• User Inputs: ${userMessages.size}\n")
        summaryBuilder.append("• AI Responses: ${agentMessages.size}\n")
        
        return summaryBuilder.toString()
    }
    
    /**
     * Extracts key points from AI response text
     */
    private fun extractKeyPoints(text: String): List<String> {
        val points = mutableListOf<String>()
        
        // Split by common delimiters and extract meaningful sentences
        val sentences = text.split(Regex("[.!?;]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 20 }
        
        // Take up to 5 most informative sentences
        sentences.take(5).forEach { sentence ->
            if (sentence.length <= 150) {
                points.add(sentence)
            } else {
                // Truncate long sentences
                points.add("${sentence.take(147)}...")
            }
        }
        
        return points
    }
    
    /**
     * Gets duration text for audio messages
     */
    private fun getDurationText(audioMessage: ChatMessageAudioClip): String {
        return try {
            val duration = audioMessage.getDurationInSeconds()
            "${String.format("%.1f", duration)}s"
        } catch (e: Exception) {
            "unknown duration"
        }
    }
    
    /**
     * Converts chat messages to summary messages for storage
     */
    private fun convertToSummaryMessages(messages: List<ChatMessage>): List<SummaryMessage> {
        return messages.mapNotNull { message ->
            when (message) {
                is ChatMessageText -> {
                    if (message.content.trim().isNotEmpty()) {
                        SummaryMessage(
                            content = message.content,
                            type = "text",
                            timestamp = System.currentTimeMillis(),
                            isUser = message.side == ChatSide.USER
                        )
                    } else null
                }
                is ChatMessageAudioClip -> {
                    SummaryMessage(
                        content = "[Audio: ${getDurationText(message)}]",
                        type = "audio",
                        timestamp = System.currentTimeMillis(),
                        isUser = message.side == ChatSide.USER
                    )
                }
                else -> null
            }
        }
    }
    
    /**
     * Saves a chat summary to the database
     */
    suspend fun saveChatSummary(
        messages: List<ChatMessage>,
        userId: String,
        userRole: UserRole,
        classCode: String,
        modelName: String,
        sessionTitle: String? = null
    ): Result<ChatSummary> {
        return try {
            val summary = generateBulletPointSummary(messages)
            val originalMessages = convertToSummaryMessages(messages)
            
            val chatSummary = ChatSummary(
                _id = generateChatSummaryId(userId, classCode),
                userId = userId,
                userRole = userRole.name.lowercase(),
                classCode = classCode,
                sessionTitle = sessionTitle ?: "Chat Session - ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                bulletPointSummary = summary,
                originalMessages = originalMessages,
                modelUsed = modelName,
                isSharedWithEducator = userRole == UserRole.STUDENT // Students can share with educators by default
            )
            
            val success = databaseManager.saveChatSummary(chatSummary)
            if (success) {
                Result.success(chatSummary)
            } else {
                Result.failure(Exception("Failed to save chat summary to database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all chat summaries for a user
     */
    suspend fun getChatSummariesForUser(userId: String, userRole: UserRole): List<ChatSummary> {
        // This would typically query the database for chat summaries
        // For now, returning empty list as the query implementation would be complex
        return emptyList()
    }
}
