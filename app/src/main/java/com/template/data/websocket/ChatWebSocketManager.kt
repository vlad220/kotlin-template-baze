package com.template.data.websocket

import android.util.Log
import com.template.data.network.ApiClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class WsEvent {
    data class NewMessage(val conversationId: String, val msg: Map<String, Any?>) : WsEvent()
    data class UserTyping(val conversationId: String, val userId: String) : WsEvent()
    data class Disconnected(val reason: String) : WsEvent()
    /** Сервер не пускает в комнату заказа тому, кто в нём не участник. */
    data class RoomDenied(val conversationId: String, val onSend: Boolean) : WsEvent()
}

/**
 * Сокет чата: комната — id заказа, его знают клиент и водитель этого заказа.
 * Сервер берёт отправителя из JWT соединения, поэтому свой id в сообщение не вставляем.
 */
class ChatWebSocketManager {
    companion object {
        val instance = ChatWebSocketManager()
    }

    private var ws: WebSocket? = null
    private var socketUrl: String? = null
    private val events = Channel<WsEvent>(Channel.BUFFERED)
    val eventsFlow: Flow<WsEvent> = events.receiveAsFlow()
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
    private val joinedRooms = mutableSetOf<String>()

    /** False — входа нет: сообщения уходить некуда, экран обязан сказать это прямо. */
    fun ensureConnected(): Boolean {
        val url = ApiClient.chatSocketUrl() ?: return false
        if (socketUrl == url && ws != null) return true
        socketUrl = url
        ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                for (room in joinedRooms) ws.send(JSONArray().apply { put("join"); put(room) }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val arr = JSONArray(text)
                    when (arr.optString(0)) {
                        "new_message" -> {
                            val data = arr.optJSONObject(1) ?: return
                            events.trySend(WsEvent.NewMessage(data.optString("conversationId"), data.toMap()))
                        }
                        "user_typing" -> {
                            val data = arr.optJSONObject(1) ?: return
                            events.trySend(WsEvent.UserTyping(data.optString("conversationId"), data.optString("userId")))
                        }
                        "join_denied", "send_denied" ->
                            events.trySend(WsEvent.RoomDenied(arr.optString(1), arr.optString(0) == "send_denied"))
                    }
                } catch (e: Exception) { Log.e("WsChat", "Parse error", e) }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                this@ChatWebSocketManager.ws = null
                socketUrl = null
                events.trySend(WsEvent.Disconnected(t.message ?: "нет связи с сервером"))
            }
        })
        return true
    }

    val connected: Boolean get() = ws != null

    fun join(conversationId: String) {
        joinedRooms.add(conversationId)
        ws?.send(JSONArray().apply { put("join"); put(conversationId) }.toString())
    }

    fun leave(conversationId: String) {
        joinedRooms.remove(conversationId)
        ws?.send(JSONArray().apply { put("leave"); put(conversationId) }.toString())
    }

    /** @return false — сокета нет, сообщение осталось только в телефоне. */
    fun sendMessage(conversationId: String, text: String): Boolean {
        val socket = ws ?: return false
        val payload = JSONObject()
            .put("conversationId", conversationId)
            .put("text", text)
        return socket.send(JSONArray().apply { put("send_message"); put(payload) }.toString())
    }
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (key in keys()) {
        map[key] = when (val v = get(key)) {
            is JSONObject -> v.toMap()
            is JSONArray -> (0 until v.length()).map { i -> v.opt(i) }
            else -> v
        }
    }
    return map
}
