package com.sing.tgthird.tdlib

import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.model.ChatDialog
import com.sing.tgthird.core.model.LoginCredentials
import com.sing.tgthird.core.model.Message
import com.sing.tgthird.core.model.emptyMessages
import com.sing.tgthird.core.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TdLibTelegramRepository : TelegramRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _dialogs = MutableStateFlow<List<ChatDialog>>(emptyList())
    override val dialogs: StateFlow<List<ChatDialog>> = _dialogs.asStateFlow()

    override fun messagesFor(chatId: String?): Flow<List<Message>> = emptyMessages()

    override suspend fun startLogin(credentials: LoginCredentials) {
        _authState.value = AuthState.Error("TDLib native binding is not wired yet.")
    }

    override suspend fun submitCode(code: String) {
        _authState.value = AuthState.Error("TDLib native binding is not wired yet.")
    }

    override suspend fun submitPassword(password: String) {
        _authState.value = AuthState.Error("TDLib native binding is not wired yet.")
    }

    override suspend fun sendText(chatId: String, text: String) {
        _authState.value = AuthState.Error("TDLib native binding is not wired yet.")
    }

    override suspend fun logout() {
        _authState.value = AuthState.SignedOut
    }
}
