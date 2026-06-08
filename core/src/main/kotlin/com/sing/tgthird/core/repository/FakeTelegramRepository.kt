package com.sing.tgthird.core.repository

import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.model.ChatDialog
import com.sing.tgthird.core.model.DialogKind
import com.sing.tgthird.core.model.LoginCredentials
import com.sing.tgthird.core.model.Message
import com.sing.tgthird.core.model.TelegramUser
import com.sing.tgthird.core.model.emptyMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

class FakeTelegramRepository : TelegramRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _dialogs = MutableStateFlow(seedDialogs())
    override val dialogs: StateFlow<List<ChatDialog>> = _dialogs.asStateFlow()

    private val messageFlows = mutableMapOf(
        "saved" to MutableStateFlow(seedSavedMessages()),
        "build" to MutableStateFlow(seedBuildMessages()),
        "notes" to MutableStateFlow(seedNoteMessages())
    )

    override fun messagesFor(chatId: String?): Flow<List<Message>> {
        if (chatId == null) {
            return emptyMessages()
        }

        return messageFlows.getOrPut(chatId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override suspend fun startLogin(credentials: LoginCredentials) {
        _authState.value = AuthState.WaitingForCode(credentials.phoneNumber)
    }

    override suspend fun submitCode(code: String) {
        if (code.isBlank()) {
            _authState.value = AuthState.Error("Code is empty")
            return
        }

        _authState.value = AuthState.Ready(
            TelegramUser(
                id = "local-user",
                displayName = "Sing",
                username = "sing"
            )
        )
    }

    override suspend fun submitPassword(password: String) {
        if (password.isBlank()) {
            _authState.value = AuthState.Error("Password is empty")
        }
    }

    override suspend fun sendText(chatId: String, text: String) {
        val flow = messageFlows.getOrPut(chatId) { MutableStateFlow(emptyList()) }
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            text = text,
            authorName = null,
            sentAt = Instant.now(),
            outgoing = true
        )

        flow.value = flow.value + message
        _dialogs.value = _dialogs.value.map { dialog ->
            if (dialog.id == chatId) {
                dialog.copy(lastMessage = text, lastMessageAt = message.sentAt)
            } else {
                dialog
            }
        }
    }

    override suspend fun logout() {
        _authState.value = AuthState.SignedOut
    }
}

private fun seedDialogs(): List<ChatDialog> =
    listOf(
        ChatDialog(
            id = "saved",
            title = "Saved Messages",
            kind = DialogKind.User,
            unreadCount = 0,
            lastMessage = "Android first.",
            lastMessageAt = Instant.now()
        ),
        ChatDialog(
            id = "build",
            title = "Build Plan",
            kind = DialogKind.Group,
            unreadCount = 2,
            lastMessage = "TDLib branch can start after native setup.",
            lastMessageAt = Instant.now()
        ),
        ChatDialog(
            id = "notes",
            title = "Private Notes",
            kind = DialogKind.Channel,
            unreadCount = 0,
            lastMessage = "Keep secrets out of git.",
            lastMessageAt = Instant.now()
        )
    )

private fun seedSavedMessages(): List<Message> =
    listOf(
        Message(
            id = "saved-1",
            chatId = "saved",
            text = "Android first.",
            authorName = "Sing",
            sentAt = Instant.now(),
            outgoing = false
        )
    )

private fun seedBuildMessages(): List<Message> =
    listOf(
        Message(
            id = "build-1",
            chatId = "build",
            text = "Kotlin + Compose for UI.",
            authorName = "Codex",
            sentAt = Instant.now(),
            outgoing = false
        ),
        Message(
            id = "build-2",
            chatId = "build",
            text = "TDLib module stays separate.",
            authorName = "Codex",
            sentAt = Instant.now(),
            outgoing = false
        )
    )

private fun seedNoteMessages(): List<Message> =
    listOf(
        Message(
            id = "notes-1",
            chatId = "notes",
            text = "Keep API ID, API hash, sessions, and APK signing keys private.",
            authorName = null,
            sentAt = Instant.now(),
            outgoing = true
        )
    )
