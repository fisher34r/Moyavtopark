package com.example.data.model

data class Settlement(
    val name: String,
    val type: String = "",
    val region: String = "",
    val district: String = "",
    val isPortOrTerminal: Boolean = false,
    val isFromHistory: Boolean = false,
    val isOnlineResult: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null
) {
    /**
     * Чистое название населенного пункта:
     * Вставляется в поле ввода без каких-либо префиксов ("Из истории", "Сеть" и т.д.)
     */
    val insertValue: String
        get() {
            var clean = name.trim()
            clean = clean
                .removePrefix("Из истории")
                .removePrefix("из истории")
                .removePrefix("Сеть")
                .removePrefix("сеть")
                .trim(' ', ':', '-', ',', '•')
            return clean
        }

    val displayName: String
        get() = insertValue

    val fullTitle: String
        get() = buildString {
            append(displayName)
            val details = listOf(district, region).filter { it.isNotBlank() }
            if (details.isNotEmpty()) {
                append(" (${details.joinToString(", ")})")
            }
        }
}
