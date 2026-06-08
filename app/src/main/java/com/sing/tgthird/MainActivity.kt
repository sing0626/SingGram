package com.sing.tgthird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sing.tgthird.ui.TgThirdApp
import com.sing.tgthird.ui.theme.TgThirdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as TgThirdApplication).container.telegramRepository
        setContent {
            TgThirdTheme {
                TgThirdApp(repository = repository)
            }
        }
    }
}
