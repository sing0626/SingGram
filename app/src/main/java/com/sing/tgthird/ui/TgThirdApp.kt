package com.sing.tgthird.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.repository.TelegramRepository
import com.sing.tgthird.ui.screen.AuthScreen
import com.sing.tgthird.ui.screen.ChatScreen

@Composable
fun TgThirdApp(repository: TelegramRepository) {
    val authState by repository.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Ready -> ChatScreen(repository = repository, user = state.user)
        else -> AuthScreen(repository = repository, authState = state)
    }
}
