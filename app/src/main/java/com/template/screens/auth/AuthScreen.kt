package com.template.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.template.R
import com.template.util.SmsAutofillHelper
import androidx.navigation.NavController
import com.template.data.repository.WarehouseRepository
import com.template.presentation.theme.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WarehouseRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val smsAutofillHelper = remember { SmsAutofillHelper(context) }

    LaunchedEffect(codeSent) {
        if (codeSent) {
            smsAutofillHelper.startListening { code = it }
            focusRequester.requestFocus()
        }
    }

    DisposableEffect(Unit) {
        onDispose { smsAutofillHelper.stopListening() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocalTaxi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AI-Такси",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Войдите в свой аккаунт",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; codeSent = false }, text = { Text("Email") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Телефон") })
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { navController.navigate("forgot_password") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Забыли пароль?", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Заполните все поля")
                            }
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            repository.login(email.trim(), password).onSuccess {
                                navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                            }.onFailure {
                                snackbarHostState.showSnackbar(it.message ?: "Ошибка авторизации")
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Войти")
                    }
                }
            } else {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Номер телефона") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!codeSent) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() } },
                        label = { Text("Телефон") },
                        placeholder = { Text("+7 (XXX) XXX-XX-XX") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (phone.replace("\\s+".toRegex(), "").length < 11) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Введите корректный номер телефона")
                                }
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                repository.sendCode(phone).onSuccess {
                                    codeSent = true
                                    snackbarHostState.showSnackbar("Код отправлен")
                                }.onFailure {
                                    snackbarHostState.showSnackbar(it.message ?: "Ошибка отправки кода")
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isLoading && phone.replace("\\s+".toRegex(), "").length >= 11
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Получить код")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("Код из SMS") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (code.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Введите код") }
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                repository.verifyCode(phone, code).onSuccess {
                                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                                }.onFailure { e ->
                                    isLoading = false
                                    snackbarHostState.showSnackbar(e.message ?: "Ошибка проверки кода")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Подтвердить", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { codeSent = false }) {
                        Text("Изменить номер")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.height(16.dp))
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
        }
    }
}
