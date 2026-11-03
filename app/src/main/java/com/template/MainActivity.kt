package com.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.template.presentation.components.OfflineBanner
import com.template.presentation.screens.AdBanner
import com.template.presentation.screens.AgreementScreen
import com.template.presentation.screens.AuthScreen
import com.template.presentation.screens.ChatListScreen
import com.template.presentation.screens.ChatScreen
import com.template.presentation.screens.DonationScreen
import com.template.presentation.screens.FeedbackScreen
import com.template.presentation.screens.ForgotPasswordScreen
import com.template.presentation.screens.MainScreen
import com.template.presentation.screens.ProfileScreen
import com.template.screens.help.HelpScreen
import com.template.screens.map.MapPickerScreen
import com.template.presentation.theme.AppTheme
import com.template.presentation.theme.ThemeManager
import com.template.util.AppUpdateInfo
import com.template.util.AutoUpdateCheck
import com.template.util.AppUpdater
import com.template.util.UpdateDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        setContent {
            AppTheme(darkTheme = ThemeManager.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        var route by remember { mutableStateOf<String?>(null) }
                        DisposableEffect(navController) {
                            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                                route = destination.route
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose { navController.removeOnDestinationChangedListener(listener) }
                        }
                        if (route == "main") {
                            AdBanner()
                        }
                        var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                        AutoUpdateCheck(onUpdateAvailable = { updateInfo = it })
                        updateInfo?.let { update ->
                            UpdateDialog(
                                update = update,
                                onDismiss = { updateInfo = null },
                                onUpdate = {
                                    AppUpdater.downloadAndInstall(this@MainActivity, update)
                                    updateInfo = null
                                }
                            )
                        }
                        OfflineBanner(route)
                        NavHost(navController = navController, startDestination = "agreement") {
                            composable("agreement") {
                                AgreementScreen(
                                    onAccept = { navController.navigate("auth") { popUpTo("agreement") { inclusive = true } } },
                                    onDecline = { finish() }
                                )
                            }
                            composable("auth") { AuthScreen(navController) }
                            composable("forgot_password") { ForgotPasswordScreen(onBack = { navController.popBackStack() }) }
                            composable("main") { MainScreen(navController) }
                            composable("help") { HelpScreen(navController) }
                            composable(
                                route = "map_picker/{lat}/{lon}/{purpose}",
                                arguments = listOf(
                                    navArgument("lat") { type = NavType.StringType; defaultValue = "57.1535" },
                                    navArgument("lon") { type = NavType.StringType; defaultValue = "65.5343" },
                                    navArgument("purpose") { type = NavType.StringType; defaultValue = "from" }
                                )
                            ) { backStackEntry ->
                                val args = backStackEntry.arguments
                                MapPickerScreen(
                                    navController = navController,
                                    purpose = args?.getString("purpose") ?: "from",
                                    title = when (args?.getString("purpose")) {
                                        "to" -> "Куда едем?"
                                        "driver" -> "Где вы находитесь?"
                                        else -> "Откуда едем?"
                                    },
                                    initialLat = args?.getString("lat")?.toDoubleOrNull() ?: 57.1535,
                                    initialLon = args?.getString("lon")?.toDoubleOrNull() ?: 65.5343
                                )
                            }
                            composable("chat_list") { ChatListScreen(onChatClick = { convId -> navController.navigate("chat_room/$convId") }) }
                            composable("chat_room/{conversationId}") { backStackEntry ->
                                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: "1"
                                ChatScreen(conversationId = conversationId, onBack = { navController.popBackStack() })
                            }
                            composable("donation") { DonationScreen(navController) }
                            composable("feedback") { FeedbackScreen(onBack = { navController.popBackStack() }) }
                            composable("profile") { ProfileScreen(onBack = { navController.popBackStack() }, navController = navController) }
                        }
                    }
                }
            }
        }
    }
}