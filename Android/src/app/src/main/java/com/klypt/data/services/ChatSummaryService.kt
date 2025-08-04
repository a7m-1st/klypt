package com.klypt.data.services

import android.util.Log
import com.klypt.data.DatabaseManager
import com.klypt.data.Model
import com.klypt.data.UserRole
import com.klypt.data.models.ChatSummary
import com.klypt.data.models.SummaryMessage
import com.klypt.data.models.generateChatSummaryId
import com.klypt.ui.common.chat.ChatMessage
import com.klypt.ui.common.chat.ChatMessageAudioClip
import com.klypt.ui.common.chat.ChatMessageText
import com.klypt.ui.common.chat.ChatSide
import com.klypt.ui.llmchat.LlmChatModelHelper
import com.klypt.ui.llmchat.LlmModelInstance
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ChatSummaryService @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    
    /**
     * Generates a bullet point summary from chat messages using LLM
     */
    suspend fun generateBulletPointSummary(
        messages: List<ChatMessage>,
        model: Model,
        onLoadingStateChange: (Boolean) -> Unit = {}
    ): String {
        return try {
            onLoadingStateChange(true)
            
            // Filter and prepare messages for summarization
            val relevantMessages = messages.filter { 
                it.side == ChatSide.USER || it.side == ChatSide.AGENT 
            }
            
            if (relevantMessages.isEmpty()) {
                onLoadingStateChange(false)
                return "No messages to summarize."
            }
            
            // Build conversation text with context length limit
            val conversationText = buildConversationText(relevantMessages)
            
            // Create summarization prompt
            val summaryPrompt = """
Please create a comprehensive bullet point summary of this conversation between a user and an AI assistant. 

The summary should include:
- Main topics discussed
- Key questions asked by the user
- Important information provided by the AI
- Any conclusions or outcomes reached

Format the summary in markdown with clear bullet points and sections.

Conversation:
$conversationText

Summary:
""".trim()
            
            // Use LLM to generate summary
            val summary = generateLlmSummary(model, summaryPrompt)
            onLoadingStateChange(false)
            
            if (summary.isNotEmpty()) {
                summary
            } else {
                // Fallback to rule-based summary if LLM fails
                generateFallbackSummary(relevantMessages)
            }
            
        } catch (e: Exception) {
            Log.e("ChatSummaryService", "Error generating LLM summary", e)
            onLoadingStateChange(false)
            // Fallback to rule-based summary
            generateFallbackSummary(messages.filter { 
                it.side == ChatSide.USER || it.side == ChatSide.AGENT 
            })
        }
    }
    
    /**
     * Builds conversation text from messages with context length limit
     */
    private fun buildConversationText(messages: List<ChatMessage>, maxChars: Int = 8000): String {
        val conversationBuilder = StringBuilder()
        var currentLength = 0
        
        for (message in messages) {
            val messageText = when (message) {
                is ChatMessageText -> {
                    val role = if (message.side == ChatSide.USER) "User" else "AI"
                    "$role: ${message.content.trim()}\n\n"
                }
                is ChatMessageAudioClip -> {
                    val role = if (message.side == ChatSide.USER) "User" else "AI"
                    "$role: [Audio message - ${getDurationText(message)}]\n\n"
                }
                else -> ""
            }
            
            if (currentLength + messageText.length > maxChars) {
                conversationBuilder.append("[... conversation truncated due to length ...]\n")
                break
            }
            
            conversationBuilder.append(messageText)
            currentLength += messageText.length
        }
        
        return conversationBuilder.toString()
    }
    
    /**
     * Generates summary using LLM model
     */
    private suspend fun generateLlmSummary(model: Model, prompt: String): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Check if model instance is available
                if (model.instance == null) {
                    Log.e("ChatSummaryService", "Model instance is null")
                    continuation.resume("")
                    return@suspendCancellableCoroutine
                }
                
                val instance = model.instance as LlmModelInstance
                val fullResponse = StringBuilder()
                
                LlmChatModelHelper.runInference(
                    model = model,
                    input = prompt,
                    resultListener = { partialResult, done ->
                        fullResponse.append(partialResult)
                        if (done) {
                            continuation.resume(fullResponse.toString())
                        }
                    },
                    cleanUpListener = {
                        // Cleanup if needed
                    }
                )
                
            } catch (e: Exception) {
                Log.e("ChatSummaryService", "Error in LLM inference", e)
                continuation.resume("")
            }
        }
    }
    
    /**
     * Generates a fallback rule-based summary when LLM is not available
     */
    private fun generateFallbackSummary(messages: List<ChatMessage>): String {
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
        model: Model,
        sessionTitle: String? = null,
        onLoadingStateChange: (Boolean) -> Unit = {}
    ): Result<ChatSummary> {
        return try {
            val summary = generateBulletPointSummary(messages, model, onLoadingStateChange)
            val originalMessages = convertToSummaryMessages(messages)
            
            val chatSummary = ChatSummary(
                _id = generateChatSummaryId(userId, classCode),
                userId = userId,
                userRole = userRole.name.lowercase(),
                classCode = classCode,
                sessionTitle = sessionTitle ?: "Chat Session - ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                bulletPointSummary = summary,
                originalMessages = originalMessages,
                modelUsed = model.name,
                isSharedWithEducator = userRole == UserRole.STUDENT // Students can share with educators by default
            )
            
            val success = databaseManager.saveChatSummary(chatSummary)
            if (success) {
                Result.success(chatSummary)
            } else {
                Result.failure(Exception("Failed to save chat summary to database"))
            }
        } catch (e: Exception) {
            onLoadingStateChange(false)
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
