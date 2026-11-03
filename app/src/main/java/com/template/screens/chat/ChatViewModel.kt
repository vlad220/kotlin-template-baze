package com.template.presentation.screens

import com.template.data.local.dao.MessageDao
import com.template.data.local.entity.ChatMessageEntity
import com.template.data.network.ApiClient
import com.template.data.repository.WarehouseRepository
import com.template.data.websocket.ChatWebSocketManager
import com.template.data.websocket.WsEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val peerName: String = "Чат по поездке",
    val notice: String? = null,
    val typingUserId: String? = null
)

/**
 * Диалог по одной поездке: комната сокета — id заказа, его знают клиент и водитель этого
 * заказа. Сервер заводит беседу по заказу, когда закрепляет его за водителем, и пишет
 * сообщения в базу. История на телефоне — зеркало: обратно с сервера сообщения не
 * подтягиваются, поэтому после смены устройства переписка пустая.
 */
class ChatViewModel(
    private val messageDao: MessageDao,
    private val repository: WarehouseRepository,
    private val wsManager: ChatWebSocketManager = ChatWebSocketManager.instance
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState

    private var room: String? = null

    fun open(orderId: String) {
        room = orderId
        _chatState.value = _chatState.value.copy(isLoading = true)

        if (!wsManager.ensureConnected()) {
            _chatState.value = _chatState.value.copy(
                notice = "Без входа переписка не работает — сообщения останутся в телефоне"
            )
        } else {
            wsManager.join(orderId)
        }

        viewModelScope.launch {
            repository.getOrder(orderId).onSuccess { order ->
                val to = order.to_address
                _chatState.value = _chatState.value.copy(
                    peerName = when {
                        order.driver_name != null && to != null -> "${order.driver_name} · $to"
                        to != null -> "Поездка до $to"
                        else -> "Чат по поездке"
                    }
                )
            }
        }

        viewModelScope.launch {
            messageDao.getMessages(ME, orderId).collect { msgs ->
                _chatState.value = _chatState.value.copy(messages = msgs, isLoading = false)
            }
        }

        viewModelScope.launch {
            wsManager.eventsFlow.collect { event ->
                when (event) {
                    is WsEvent.RoomDenied -> {
                        if (event.conversationId != room) return@collect
                        _chatState.value = _chatState.value.copy(
                            notice = if (event.onSend) "Сервер не принял сообщение"
                            else "В эту поездку нельзя писать: вы не её участник"
                        )
                    }
                    is WsEvent.Disconnected -> _chatState.value = _chatState.value.copy(
                        notice = "Связь с чатом потеряна (${event.reason})"
                    )
                    is WsEvent.NewMessage -> {
                        if (event.conversationId != room) return@collect
                        val sender = event.msg["senderId"] as? String
                        // Сервер шлёт сообщение всем в комнате, включая автора — свой эх отбрасываем
                        if (sender == null || sender == ApiClient.getUserId()) return@collect
                        val text = event.msg["text"] as? String ?: return@collect
                        val stamp = (event.msg["createdAt"] as? String)?.toLongOrNull()
                            ?: System.currentTimeMillis()
                        messageDao.insert(
                            ChatMessageEntity(
                                senderId = orderId,
                                recipientId = ME,
                                message = text,
                                timestamp = stamp,
                                isRead = false,
                                status = "received"
                            )
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun send(text: String) {
        val orderId = room ?: return
        val body = text.trim()
        if (body.isEmpty()) return

        if (!wsManager.sendMessage(orderId, body)) {
            _chatState.value = _chatState.value.copy(notice = "Сообщение не ушло — нет связи")
            return
        }
        _chatState.value = _chatState.value.copy(notice = null)
        viewModelScope.launch {
            messageDao.insert(
                ChatMessageEntity(
                    senderId = ME,
                    recipientId = orderId,
                    message = body,
                    timestamp = System.currentTimeMillis(),
                    isRead = true,
                    status = "sent"
                )
            )
        }
    }

    fun leave() {
        room?.let { wsManager.leave(it) }
        room = null
    }

    companion object {
        /** Локальная метка «я»: на проводе настоящий id из токена, а не эта строка. */
        private const val ME = "me"
    }
}
