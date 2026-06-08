package com.sing.tgthird.core.repository

import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.model.ChatDialog
import com.sing.tgthird.core.model.LoginCredentials
import com.sing.tgthird.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TelegramRepository {
    val authState: StateFlow<AuthState>
    val dialogs: StateFlow<List<ChatDialog>>

    fun messagesFor(chatId: String?): Flow<List<Message>>
    suspend fun startLogin(credentials: LoginCredentials)
    suspend fun submitCode(code: String)
    suspend fun submitPassword(password: String)
    suspend fun sendText(chatId: String, text: String)
    suspend fun logout()
}
