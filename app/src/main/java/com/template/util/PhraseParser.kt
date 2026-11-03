package com.template.util

/**
 * Разбор свободной фразы («из центра на вокзал за 250») на поля заявки.
 * Роль задаёт экран: та же фраза на экране клиента — заявка, на экране водителя —
 * его точка. Здесь определяются только адреса и цена.
 */
data class ParsedPhrase(
    val fromText: String?,
    val toText: String?,
    val price: Int?
)

object PhraseParser {
    private val FROM_PREPS = setOf("из", "от", "со", "с")
    private val TO_PREPS = setOf("на", "в", "во", "до", "к")
    private val STOP = FROM_PREPS + TO_PREPS +
        setOf("за", "по", "мимо", "через", "или", "и", "но", "для", "без", "где")
    private val MONEY_IN_TOKEN = Regex("^(\\d{2,5})(?:руб\\w*|₽|р)$")
    private val BARE_DIGITS = Regex("^\\d{2,5}$")

    fun parse(raw: String): ParsedPhrase? {
        val words = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        val (price, priceTokens) = findPrice(lower(words))
        val (from, to) = extractPhrases(words.filterIndexed { i, _ -> i !in priceTokens })

        return ParsedPhrase(fromText = clean(from), toText = clean(to), price = price)
    }

    /** Цена: «за 250», «250 рублей», «250₽». Возвращает сумму и токены, которые в неё вошли. */
    private fun findPrice(lowered: List<String>): Pair<Int?, Set<Int>> {
        for (i in lowered.indices) {
            val token = lowered[i]
            MONEY_IN_TOKEN.find(token)?.let { return Pair(it.groupValues[1].toIntOrNull(), setOf(i)) }
            if (!BARE_DIGITS.matches(token)) continue
            val value = token.toIntOrNull()
            if (i > 0 && lowered[i - 1] == "за") return Pair(value, setOf(i - 1, i))
            val next = lowered.getOrNull(i + 1)
            if (next == "₽" || next != null && next.startsWith("руб")) return Pair(value, setOf(i, i + 1))
        }
        return Pair(null, emptySet())
    }

    private fun extractPhrases(words: List<String>): Pair<String?, String?> {
        val lowered = lower(words)
        val taken = BooleanArray(words.size)
        var from: String? = null
        var to: String? = null
        var i = 0
        while (i < words.size) {
            if (!taken[i] && from == null && lowered[i] in FROM_PREPS) {
                val end = collect(lowered, i, taken)
                from = join(words, i + 1, end)
                i = end
                continue
            }
            if (!taken[i] && to == null && lowered[i] in TO_PREPS) {
                val end = collect(lowered, i, taken)
                to = join(words, i + 1, end)
                i = end
                continue
            }
            i++
        }
        if (to == null) {
            val leftovers = words.filterIndexed { j, _ -> !taken[j] && lowered[j] !in STOP }
            if (leftovers.isNotEmpty()) to = leftovers.joinToString(" ")
        }
        return Pair(from, to)
    }

    /** Помечает предлог и всё, что до следующего предлога; возвращает индекс стоп-слова. */
    private fun collect(lowered: List<String>, start: Int, taken: BooleanArray): Int {
        taken[start] = true
        var j = start + 1
        while (j < lowered.size) {
            if (lowered[j] in STOP) break
            taken[j] = true
            j++
        }
        return j
    }

    private fun join(words: List<String>, from: Int, until: Int): String? =
        if (until <= from) null else words.subList(from, until).joinToString(" ")

    private fun lower(words: List<String>) = words.map { it.lowercase().trimEnd('.', ',', '!', '?') }

    private fun clean(value: String?): String? =
        value?.trim()?.trimEnd('.', ',', '!', '?')?.trim()?.ifEmpty { null }
}
