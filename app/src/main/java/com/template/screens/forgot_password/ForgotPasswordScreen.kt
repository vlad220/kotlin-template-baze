package com.template.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Сброс пароля") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Восстановление пароля", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Введите email, привязанный к аккаунту.\nМы отправим ссылку для сброса пароля.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (email.isBlank()) { scope.launch { snackbarHostState.showSnackbar("Введите email") }; return@Button }
                scope.launch { snackbarHostState.showSnackbar("Ссылка для сброса пароля отправлена на email") }
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) { Text("Отправить") }
        }
    }
}
