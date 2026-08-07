package com.example.data

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val suggestedActions: List<String> = emptyList()
)
