package com.sing.tgthird.di

import com.sing.tgthird.core.repository.FakeTelegramRepository
import com.sing.tgthird.core.repository.TelegramRepository

class AppContainer {
    val telegramRepository: TelegramRepository = FakeTelegramRepository()
}
