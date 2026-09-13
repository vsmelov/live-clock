package com.vsmelov.liveclock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsmelov.liveclock.i18n.Localization
import com.vsmelov.liveclock.ui.theme.LiveClockTheme

/**
 * The configuration screen: settings, today's log and the full action list.
 * The widget does not need it — the app can go unopened.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(applicationContext),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Swapping the context is what makes the language picker work.
            // This Compose version resolves stringResource through LocalContext,
            // with LocalConfiguration as the invalidation trigger, so both are
            // provided: the screen then recomposes the moment the language changes.
            val base = LocalContext.current
            val localized = remember(uiState.language, base) {
                Localization.contextFor(base, uiState.language)
            }

            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
            ) {
                LiveClockTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        MainScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
