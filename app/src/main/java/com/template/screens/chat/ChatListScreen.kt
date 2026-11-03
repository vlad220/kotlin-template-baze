package com.template.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.template.data.network.OrderDto
import com.template.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Одна переписка = один заказ: комната на сервере открыта по id заказа. */
data class ChatThread(
    val orderId: String,
    val title: String,
    val subtitle: String,
    val live: Boolean
)

data class ChatListUiState(
    val threads: List<ChatThread> = emptyList(),
    val isLoading: Boolean = true,
    val notice: String? = null
)

class ChatListViewModel(
    private val repository: WarehouseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val mine = repository.myOrders("client")
            val driven = repository.myOrders("driver")
            if (mine.isFailure && driven.isFailure) {
                _uiState.value = ChatListUiState(
                    isLoading = false,
                    notice = "Поездки не загрузились. Нужны интернет и вход в аккаунт."
                )
                return@launch
            }

            val rows = buildList {
                mine.getOrNull().orEmpty().forEach { add(threadFor(it, forClient = true)) }
                driven.getOrNull().orEmpty().forEach { add(threadFor(it, forClient = false)) }
            }.filter { it.orderId.isNotBlank() }

            // Живые поездки сверху: в чате по завершённой заказу пишут редко.
            val ordered = rows.filter { it.live } + rows.filter { !it.live }
            _uiState.value = ChatListUiState(threads = ordered.take(MAX_THREADS), isLoading = false)
        }
    }

    private fun threadFor(order: OrderDto, forClient: Boolean): ChatThread {
        val to = order.to_address ?: "адрес не указан"
        val status = statusLabel(order.status, forClient)
        return ChatThread(
            orderId = order.id ?: "",
            title = if (forClient) order.driver_name ?: "Водитель ещё не найден" else "Поездка до $to",
            subtitle = if (forClient) "Поездка до $to · $status" else "Клиент · $status",
            live = order.status in LIVE_STATUSES
        )
    }

    private fun statusLabel(status: String?, forClient: Boolean): String = when (status) {
        "new" -> if (forClient) "ищем машину" else "новая заявка"
        "taken" -> "заказ принят"
        "arrived" -> "водитель приехал"
        "started" -> "в пути"
        "finished" -> "завершена"
        "cancelled" -> "отменена"
        else -> "статус неизвестен"
    }

    companion object {
        private const val MAX_THREADS = 20
        private val LIVE_STATUSES = setOf("new", "taken", "arrived", "started")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onChatClick: (String) -> Unit) {
    val viewModel: ChatListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatListViewModel(repository = WarehouseRepository()) as T
            }
        }
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Переписки по поездкам", color = colors.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Чат открыт по каждому заказу: водителю — пока он ведёт поездку, клиенту — по своей заявке.",
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        state.notice?.let {
            Text(it, color = colors.error, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            state.threads.isEmpty() && state.notice == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Пока нет поездок", color = colors.onSurfaceVariant)
                    Text(
                        "Вызовите машину или возьмите заявку — переписка появится здесь.",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.threads, key = { it.orderId }) { thread ->
                    ThreadRow(thread = thread, onClick = { onChatClick(thread.orderId) })
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: ChatThread, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    thread.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    thread.title,
                    color = colors.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    thread.subtitle,
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (thread.live) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.secondary)
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(colors.outline.copy(alpha = 0.35f)))
    }
}
