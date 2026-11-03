package com.template.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class AppTile(val title: String, val icon: ImageVector, val route: String, val tint: java.lang.Integer? = null)

private val tiles = listOf(
    AppTile("Чаты", Icons.AutoMirrored.Outlined.Chat, "chat_list"),
    AppTile("Профиль", Icons.Outlined.AccountCircle, "profile"),
    AppTile("Помощь", Icons.Outlined.HelpOutline, "help"),
    AppTile("Ошибки", Icons.Outlined.BugReport, "feedback"),
    AppTile("Поддержать", Icons.Outlined.FavoriteBorder, "donation"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val colors = MaterialTheme.colorScheme
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Шаблон", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Заголовок вашего приложения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navController.navigate("auth") }) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Войти")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Главная героиня-кнопка — сюда поставите главное действие вашего приложения
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.82f))))
                        .padding(22.dp)
                ) {
                    Column(Modifier.align(Alignment.BottomStart)) {
                        Text("Главное действие", style = MaterialTheme.typography.headlineSmall, color = colors.onPrimary, fontWeight = FontWeight.Bold)
                        Text("Опишите его в этой карточке", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tiles) { tile ->
                    Card(onClick = { navController.navigate(tile.route) }, modifier = Modifier.fillMaxWidth().height(110.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tile.icon, null, modifier = Modifier.size(30.dp), tint = colors.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}