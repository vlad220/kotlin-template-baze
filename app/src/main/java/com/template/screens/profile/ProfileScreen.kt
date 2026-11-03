package com.template.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.template.presentation.components.SocialShareRow
import com.template.presentation.theme.ThemeManager
import com.template.presentation.viewmodel.ProfileViewModel

data class ProfileScreenState(
    val name: String = "",
    val phone: String = "",
    val role: String = "Пассажир",
    val position: String = "",
    val skills: String = "",
    val city: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    navController: NavController? = null,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbarHost.showSnackbar("Профиль сохранён")
            viewModel.clearSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isEditing) {
                OutlinedTextField(value = state.name, onValueChange = { viewModel.updateName(it) },
                    label = { Text("Имя") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = state.phone, onValueChange = { viewModel.updatePhone(it) },
                    label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Пассажир", "Водитель").forEach { role ->
                        FilterChip(selected = state.role == role,
                            onClick = { viewModel.updateRole(role) }, label = { Text(role) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val positions = listOf("Кладовщик", "Комплектовщик", "Логист", "Супервайзер", "Другое")
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.position,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Должность") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        positions.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = { viewModel.updatePosition(p); expanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = state.skills, onValueChange = { viewModel.updateSkills(it) },
                    label = { Text("Навыки и специализация") },
                    supportingText = { Text("${state.skills.length}/200") },
                    modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 3)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { viewModel.saveProfile() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить")
                }
            } else {
                SelectionContainer {
                    Column {
                        Text(state.name.ifBlank { "Не указано" }, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        Text(state.city, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.role, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        if (state.position.isNotBlank()) {
                            Text(state.position, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.phone.ifBlank { "Телефон не указан" }) }
                            if (state.skills.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Навыки и специализация:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(state.skills, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (state.rating > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                if (i < state.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${state.rating} (${state.reviewCount})", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(onClick = { navController?.navigate("photos") },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Фото сотрудника")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Отзывы", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Пока нет отзывов. Коллеги и руководство\nмогут оставить отзывы о работе.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = { viewModel.toggleEdit() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Редактировать профиль")
                }

                Spacer(modifier = Modifier.height(16.dp))

                var showShare by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showShare = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Поделиться профилем")
                }

                if (showShare) {
                    AlertDialog(
                        onDismissRequest = { showShare = false },
                        title = { Text("Поделиться профилем") },
                        text = {
                            Column {
                                Text(
"Мой профиль в приложении AI-Такси: ${state.name}, ${state.position}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SocialShareRow(
                                    shareText = "Мой профиль в приложении AI-Такси: ${state.name}, ${state.position}",
                                    shareUrl = "https://vsem72.ru"
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showShare = false }) { Text("Закрыть") }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Приложение", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Тёмная тема")
                        Switch(
                            checked = ThemeManager.isDarkTheme,
                            onCheckedChange = { ThemeManager.toggle(context) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Поделиться приложением", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    SocialShareRow(
                        shareText = "AI-Такси: клиент сам называет цену поездки",
                        shareUrl = "https://vsem72.ru"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}