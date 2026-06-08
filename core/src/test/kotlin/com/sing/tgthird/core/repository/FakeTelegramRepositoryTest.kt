package com.sing.tgthird.core.repository

import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.model.LoginCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTelegramRepositoryTest {
    @Test
    fun loginMovesFromCodeChallengeToReady() = runTest {
        val repository = FakeTelegramRepository()

        repository.startLogin(LoginCredentials(apiId = 1, apiHash = "hash", phoneNumber = "+85200000000"))
        assertEquals(AuthState.WaitingForCode("+85200000000"), repository.authState.value)

        repository.submitCode("12345")
        assertTrue(repository.authState.value is AuthState.Ready)
    }

    @Test
    fun sendTextAppendsMessage() = runTest {
        val repository = FakeTelegramRepository()
        val before = repository.messagesFor("saved").first().size

        repository.sendText(chatId = "saved", text = "hello")

        val messages = repository.messagesFor("saved").first()
        assertEquals(before + 1, messages.size)
        assertEquals("hello", messages.last().text)
    }
}
