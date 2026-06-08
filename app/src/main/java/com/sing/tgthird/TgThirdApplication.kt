package com.sing.tgthird

import android.app.Application
import com.sing.tgthird.di.AppContainer

class TgThirdApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer()
    }
}
