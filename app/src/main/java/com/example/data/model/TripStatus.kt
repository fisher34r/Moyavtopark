package com.example.data.model

enum class TripStatus(val label: String) {
    PLANNED("Запланирован"),
    LOADING("На погрузке"),
    IN_TRANSIT("В пути"),
    UNLOADED("Выгружен"),
    PAID("Оплачен")
}
