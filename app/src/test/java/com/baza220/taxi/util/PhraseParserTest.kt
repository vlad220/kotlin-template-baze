package com.baza220.taxi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhraseParserTest {

    @Test
    fun `заявка клиента с ценой`() {
        val p = PhraseParser.parse("из центра на вокзал за 250")!!
        assertEquals("центра", p.fromText)
        assertEquals("вокзал", p.toText)
        assertEquals(250, p.price)
    }

    @Test
    fun `цена словом рубли и два адреса в именах`() {
        val p = PhraseParser.parse("Из ТЦ Гудок до аэропорта Рощино 400 рублей")!!
        assertEquals("ТЦ Гудок", p.fromText)
        assertEquals("аэропорта Рощино", p.toText)
        assertEquals(400, p.price)
    }

    @Test
    fun `точка водителя с номером дома не считается ценой`() {
        val p = PhraseParser.parse("еду на Широтную, 63")!!
        assertNull(p.fromText)
        assertEquals("Широтную, 63", p.toText)
        assertNull(p.price)
    }

    @Test
    fun `точка водителя`() {
        val p = PhraseParser.parse("ищу клиентов на Широтной, 10")!!
        assertNull(p.fromText)
        assertEquals("Широтной, 10", p.toText)
        assertNull(p.price)
    }

    @Test
    fun `без предлога остаток становится пунктом назначения`() {
        val p = PhraseParser.parse("Вокзал")!!
        assertNull(p.fromText)
        assertEquals("Вокзал", p.toText)
    }

    @Test
    fun `глагол не попадает в адрес если есть предлог`() {
        val p = PhraseParser.parse("доехать до центра")!!
        assertEquals("центра", p.toText)
        assertNull(p.fromText)
    }

    @Test
    fun `только цена`() {
        val p = PhraseParser.parse("за 300")!!
        assertEquals(300, p.price)
        assertNull(p.fromText)
        assertNull(p.toText)
    }

    @Test
    fun `цена в знаке рубля`() {
        val p = PhraseParser.parse("на речник 250₽")!!
        assertEquals(250, p.price)
        assertEquals("речник", p.toText)
    }

    @Test
    fun `пустая фраза`() {
        assertNull(PhraseParser.parse("   "))
    }
}
