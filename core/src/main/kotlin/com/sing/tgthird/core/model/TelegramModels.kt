package com.sing.tgthird.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

data class LoginCredentials(
    val apiId: Int,
    val apiHash: String,
    val phoneNumber: String
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Connecting : AuthState
    data class WaitingForCode(val phoneNumber: String) : AuthState
    data class WaitingForPassword(val phoneNumber: String) : AuthState
    data class Ready(val user: TelegramUser) : AuthState
    data class Error(val message: String) : AuthState
}

data class TelegramUser(
    val id: String,
    val displayName: String,
    val username: String? = null
)

enum class DialogKind {
    User,
    Group,
    Channel,
    Unknown
}

data class ChatDialog(
    val id: String,
    val title: String,
    val kind: DialogKind,
    val unreadCount: Int,
    val lastMessage: String?,
    val lastMessageAt: Instant?
)

data class Message(
    val id: String,
    val chatId: String,
    val text: String,
    val authorName: String?,
    val sentAt: Instant?,
    val outgoing: Boolean
)

fun emptyMessages(): Flow<List<Message>> = flowOf(emptyList())
