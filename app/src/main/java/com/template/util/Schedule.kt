package com.template.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Время подачи. Телефон выбирает локальный час, на сервер уходит момент времени в
 * формате RFC 3339 со смещением, сервер хранит его в UTC и отвечает миллисекундами
 * эпохи — так не зависит от того, в каком часовом поясе стоит сессия в Postgres.
 */
object Schedule {

    private val MONTHS = arrayOf(
        "янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"
    )
    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")
    private val RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    fun toRfc3339(epochMillis: Long): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            .format(RFC3339)

    fun of(date: LocalDate, time: LocalTime): Long =
        LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** «сегодня к 06:30», «завтра к 21:15», «11 окт к 06:30» — пусто для немедленной поездки. */
    fun pickupShort(epochMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String? {
        val value = epochMillis ?: return null
        val zone = ZoneId.systemDefault()
        val picked = Instant.ofEpochMilli(value).atZone(zone)
        val day = dayWord(picked.toLocalDate(), Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate())
        return "$day к ${CLOCK.format(picked)}"
    }

    fun pickupLabel(epochMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String? =
        pickupShort(epochMillis, nowMillis)?.let { "Подача $it" }

    private fun dayWord(picked: LocalDate, today: LocalDate): String = when {
        picked == today -> "сегодня"
        picked == today.plusDays(1) -> "завтра"
        picked.year == today.year -> "${picked.dayOfMonth} ${MONTHS[picked.monthValue - 1]}"
        else -> "${picked.dayOfMonth} ${MONTHS[picked.monthValue - 1]} ${picked.year}"
    }
}
