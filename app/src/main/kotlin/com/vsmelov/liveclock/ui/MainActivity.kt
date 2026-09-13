package com.vsmelov.liveclock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsmelov.liveclock.ui.theme.LiveClockTheme

/**
 * Конфигурационный экран: настройки, лог за сегодня и полная сетка действий.
 * Виджету не нужен — приложение можно ни разу не открыть.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LiveClockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel(factory = MainViewModel.factory(applicationContext)),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
