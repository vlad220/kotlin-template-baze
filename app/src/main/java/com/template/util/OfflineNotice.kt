package com.template.util

/**
 * Что именно перестаёт обновляться без связи — для плашки на экране. Роль берётся из
 * маршрута навигации: не все экраны одинаково чувствительны к отсутствию сети.
 */
object OfflineNotice {

    fun whatBreaks(route: String?): String = when {
        route == null -> "данные и чат"
        route.startsWith("chat") -> "сообщения в чате"
        route.startsWith("map") -> "карта и позиция на ней"
        route.startsWith("auth") || route.startsWith("forgot") -> "вход по SMS"
        else -> "данные и чат"
    }
}