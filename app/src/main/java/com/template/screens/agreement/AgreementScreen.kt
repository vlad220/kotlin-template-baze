package com.template.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val PRIVACY_URL = "https://vsem72.ru/taxi-privacy.html"

@Composable
fun AgreementScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Пользовательское соглашение", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("""
1. Общие положения
1.1. Настоящее Пользовательское соглашение регулирует отношения между администрацией приложения «AI-Такси» и пользователем.
1.2. Используя приложение, вы соглашаетесь с условиями настоящего соглашения.
2. Предмет соглашения
2.1. Приложение «AI-Такси» предоставляет возможность заказать машину (клиент) и принимать заказы (водитель), включая карту, поиск водителей рядом, чат и оценку поездки.
2.2. Приложение является бесплатным, но может содержать добровольные пожертвования на развитие проекта.
3. Права и обязанности
3.1. Пользователь обязуется не размещать ложную информацию, не использовать приложение для мошенничества, соблюдать законодательство РФ.
3.2. Администрация имеет право блокировать доступ при нарушении соглашения и изменять условия с уведомлением.
4. Персональные данные
4.1. Приложение собирает данные, необходимые для работы: телефон, геолокацию, адреса поездок, переписку в чате. Полный перечень и порядок обработки — в Политике конфиденциальности (ссылка под текстом соглашения).
4.2. Данные не продаются и не передаются третьим лицам, кроме случаев, предусмотренных законом и Политикой конфиденциальности.
5. Заключительные положения
5.1. Приложение предоставляется «как есть». Администрация не несёт ответственности за возможные убытки.
5.2. Все споры решаются в досудебном порядке.
                """.trimIndent(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        val uriHandler = LocalUriHandler.current
        TextButton(onClick = { uriHandler.openUri(PRIVACY_URL) }, modifier = Modifier.fillMaxWidth()) {
            Text("📄 Политика конфиденциальности (откроется в браузере)", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Spacer(Modifier.width(8.dp))
            Text("Я ознакомился и принимаю условия соглашения", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = agreed) { Text("Продолжить") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDecline) { Text("Отказаться и выйти") }
        Spacer(Modifier.height(16.dp))
    }
}
