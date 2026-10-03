package com.example.util

object TripNumberUtils {
    private val numberRegex = Regex("""\d+""")

    /**
     * Формирует следующий номер рейса:
     * Берет последний введенный номер (например: "Рейс №12", "Рейс #4", "15"),
     * находит числовую часть, прибавляет 1 и сохраняет формат префикса.
     * Если предыдущих данных нет, возвращает "Рейс №1".
     */
    fun generateNextTripNumber(lastTripNumber: String?): String {
        if (lastTripNumber.isNullOrBlank()) {
            return "Рейс №1"
        }

        val trimmed = lastTripNumber.trim()
        val matches = numberRegex.findAll(trimmed).toList()

        if (matches.isNotEmpty()) {
            val lastMatch = matches.last()
            val num = lastMatch.value.toLongOrNull()
            if (num != null) {
                val nextNum = num + 1
                val prefix = trimmed.substring(0, lastMatch.range.first)
                val suffix = trimmed.substring(lastMatch.range.last + 1)
                val finalPrefix = if (prefix.isBlank()) "Рейс №" else prefix
                return "$finalPrefix$nextNum$suffix"
            }
        }

        return "Рейс №1"
    }
}
