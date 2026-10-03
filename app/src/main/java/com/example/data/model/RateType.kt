package com.example.data.model

enum class RateType(val title: String, val unitLabel: String, val formulaHint: String) {
    PER_TON_KM("За тонно-километр", "₽ / т·км", "Вес (т) × Расстояние (км) × Ставка"),
    PER_TON("За тонну", "₽ / т", "Вес (т) × Ставка"),
    PER_KM("За километр", "₽ / км", "Расстояние (км) × Ставка"),
    FIXED("Фиксированная сумма", "₽", "Фиксированная стоимость рейса")
}
