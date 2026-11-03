package com.template.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.template.data.local.AppDatabase
import com.template.data.local.entity.ChatMessageEntity
import com.template.data.repository.WarehouseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: ChatViewModel = viewModel(
        key = conversationId,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(
                    messageDao = db.messageDao(),
                    repository = WarehouseRepository()
                ) as T
            }
        }
    )
    val chatState by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(conversationId) {
        viewModel.open(conversationId)
    }

    DisposableEffect(conversationId) {
        onDispose { viewModel.leave() }
    }

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ChatHeader(
            peerName = chatState.peerName,
            onBack = onBack
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items = chatState.messages, key = { it.id }) { message ->
                MessageBubble(message = message, isMine = message.senderId == "me")
            }
        }

        chatState.notice?.let {
            Text(
                text = it,
                color = colors.tertiary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        ChatInput(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.send(inputText)
                    inputText = ""
                    scope.launch {
                        if (chatState.messages.isNotEmpty()) {
                            listState.animateScrollToItem(chatState.messages.size)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatHeader(peerName: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.onBackground)
        }
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = peerName, color = colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun MessageBubble(message: ChatMessageEntity, isMine: Boolean) {
    val colors = MaterialTheme.colorScheme
    val bubble = if (isMine) colors.primary else colors.surfaceVariant
    val textColor = if (isMine) colors.onPrimary else colors.onSurface
    val timeColor = if (isMine) colors.onPrimary.copy(alpha = 0.7f) else colors.onSurfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box(modifier = Modifier.widthIn(max = 280.dp).animateContentSize()) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                ),
                color = bubble,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                    SelectionContainer {
                        Text(text = message.message, color = textColor, fontSize = 15.sp, lineHeight = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Text(text = formatTime(message.timestamp), color = timeColor, fontSize = 11.sp)
                        if (isMine) {
                            Spacer(modifier = Modifier.width(3.dp))
                            MessageStatusIcon(isRead = message.isRead)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(isRead: Boolean) {
    val colors = MaterialTheme.colorScheme
    val icon: ImageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done
    val tint: Color = if (isRead) colors.onPrimary else colors.onPrimary.copy(alpha = 0.55f)
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
}

@Composable
private fun ChatInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить", tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (text.isEmpty()) Text("Написать сообщение...", color = colors.onSurfaceVariant, fontSize = 15.sp)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (text.isNotBlank()) {
            IconButton(onClick = onSend, modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.primary)) {
                Icon(Icons.Default.Send, contentDescription = "Отправить", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            }
        } else {
            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Mic, contentDescription = "Голосовое сообщение", tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
