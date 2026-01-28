package com.industrialvision.systems

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.industrialvision.systems.navigation.IndustrialVisionNavHost
import com.industrialvision.systems.ui.theme.IndustrialVisionTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Industrial Vision Systems
 *
 * Entry point for the application, hosting the Compose UI
 * and navigation infrastructure.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen while loading initial data
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        enableEdgeToEdge()

        setContent {
            IndustrialVisionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IndustrialVisionApp(
                        onInitialized = { keepSplash = false }
                    )
                }
            }
        }
    }
}

@Composable
fun IndustrialVisionApp(
    onInitialized: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Signal initialization complete
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onInitialized()
    }

    IndustrialVisionNavHost(navController = navController)
}
