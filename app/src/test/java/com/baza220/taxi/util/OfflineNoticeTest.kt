package com.baza220.taxi.util

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineNoticeTest {

    @Test
    fun `экран водителя без связи теряет точку и заявки`() {
        assertEquals("ваша точка на линии и новые заявки", OfflineNotice.whatBreaks("driver"))
    }

    @Test
    fun `карта выбора точки`() {
        assertEquals(
            "карта и позиция машины",
            OfflineNotice.whatBreaks("map_picker/{lat}/{lon}/{purpose}")
        )
    }

    @Test
    fun `чат с водителем`() {
        assertEquals("сообщения водителю", OfflineNotice.whatBreaks("chat_room/{conversationId}"))
        assertEquals("сообщения водителю", OfflineNotice.whatBreaks("chat_list"))
    }

    @Test
    fun `заявка клиента и её статус`() {
        assertEquals("расчёт дороги и статус заказа", OfflineNotice.whatBreaks("client"))
        assertEquals("расчёт дороги и статус заказа", OfflineNotice.whatBreaks("my_orders"))
    }

    @Test
    fun `экран вне такси и неизвестный маршрут получают общий текст`() {
        assertEquals("заказ, заявки и чат", OfflineNotice.whatBreaks("help"))
        assertEquals("заказ, заявки и чат", OfflineNotice.whatBreaks(null))
    }
}
