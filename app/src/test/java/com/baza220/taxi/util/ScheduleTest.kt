package com.baza220.taxi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTest {

    private val now = Schedule.of(LocalDate.of(2026, 10, 11), LocalTime.of(9, 0))

    @Test
    fun `подача сегодня утром`() {
        val picked = Schedule.of(LocalDate.of(2026, 10, 11), LocalTime.of(6, 30))
        assertEquals("сегодня к 06:30", Schedule.pickupShort(picked, now))
        assertEquals("Подача сегодня к 06:30", Schedule.pickupLabel(picked, now))
    }

    @Test
    fun `подача завтра вечером`() {
        val picked = Schedule.of(LocalDate.of(2026, 10, 12), LocalTime.of(21, 15))
        assertEquals("завтра к 21:15", Schedule.pickupShort(picked, now))
    }

    @Test
    fun `подача через несколько дней того же года`() {
        val picked = Schedule.of(LocalDate.of(2026, 10, 16), LocalTime.of(8, 5))
        assertEquals("16 окт к 08:05", Schedule.pickupShort(picked, now))
    }

    @Test
    fun `подача в другом году названа с годом`() {
        val picked = Schedule.of(LocalDate.of(2027, 3, 1), LocalTime.of(0, 40))
        assertEquals("1 мар 2027 к 00:40", Schedule.pickupShort(picked, now))
    }

    @Test
    fun `немедленной поездке подпись не нужна`() {
        assertNull(Schedule.pickupShort(null, now))
        assertNull(Schedule.pickupLabel(null, now))
    }

    @Test
    fun `на сервер уходит формат rfс 3339 со смещением`() {
        val picked = Schedule.of(LocalDate.of(2026, 10, 11), LocalTime.of(6, 30))
        val sent = Schedule.toRfc3339(picked)
        assertEquals(true, Regex("""^2026-10-11T06:30:00[+-]\d{2}:\d{2}$""").matches(sent))
    }
}
