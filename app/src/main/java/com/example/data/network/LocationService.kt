package com.example.data.network

import android.util.Log
import com.example.data.model.Settlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LocationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _isLoadingOnline = MutableStateFlow(false)
    val isLoadingOnline: StateFlow<Boolean> = _isLoadingOnline

    private val _onlineStatus = MutableStateFlow("Готово к поиску")
    val onlineStatus: StateFlow<String> = _onlineStatus

    private val _totalSettlementsCount = MutableStateFlow(0)
    val totalSettlementsCount: StateFlow<Int> = _totalSettlementsCount

    // Combined in-memory cache of settlements
    private val settlementsCache = mutableListOf<Settlement>()
    private val cacheLock = Any()

    init {
        // Load initial core database of grain ports, elevators, and agricultural hubs
        synchronized(cacheLock) {
            settlementsCache.addAll(getCoreGrainLogisticsSettlements())
            _totalSettlementsCount.value = settlementsCache.size
        }
    }

    /**
     * Загружает полный справочник населенных пунктов РФ из интернета
     */
    suspend fun loadSettlementsFromInternet() {
        withContext(Dispatchers.IO) {
            _isLoadingOnline.value = true
            _onlineStatus.value = "Загрузка населенных пунктов из сети..."
            try {
                // Reliable public GitHub repository with all official Russian cities
                val url = "https://raw.githubusercontent.com/pensnarik/russian-cities/master/russian-cities.json"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GrainTruckTracker/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val jsonArray = JSONArray(bodyString)
                        val loaded = mutableListOf<Settlement>()

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val name = item.optString("name", "")
                            val subject = item.optString("subject", "")
                            val district = item.optString("district", "")

                            if (name.isNotBlank()) {
                                loaded.add(
                                    Settlement(
                                        name = name,
                                        type = "г.",
                                        region = subject,
                                        district = district,
                                        isPortOrTerminal = isKnownGrainPort(name)
                                    )
                                )
                            }
                        }

                        synchronized(cacheLock) {
                            val existingNames = settlementsCache.map { it.name.lowercase() }.toSet()
                            for (s in loaded) {
                                if (s.name.lowercase() !in existingNames) {
                                    settlementsCache.add(s)
                                }
                            }
                            _totalSettlementsCount.value = settlementsCache.size
                        }

                        _onlineStatus.value = "База обновлена (${settlementsCache.size} нас. пунктов)"
                        Log.d("LocationService", "Loaded ${loaded.size} cities from internet. Total: ${settlementsCache.size}")
                    }
                } else {
                    _onlineStatus.value = "Офлайн-база активна (${settlementsCache.size} пунктов)"
                }
            } catch (e: Exception) {
                Log.w("LocationService", "Could not fetch online cities list: ${e.message}")
                _onlineStatus.value = "Офлайн-база активна (${settlementsCache.size} пунктов)"
            } finally {
                _isLoadingOnline.value = false
            }
        }
    }

    /**
     * Поиск населенных пунктов:
     * 1. Совпадения из истории рейсов пользователя (наивысший приоритет)
     * 2. Порты и терминалы выгрузки
     * 3. Локальная база городов и станиц
     * 4. Если запрос от 3 символов — онлайн-поиск через OpenStreetMap Nominatim API
     */
    suspend fun searchSettlements(
        query: String,
        knownTripLocations: List<String> = emptyList()
    ): List<Settlement> {
        val cleanQuery = query.trim().lowercase()

        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Settlement>()
            val addedKeys = mutableSetOf<String>()

            // 1. Match from history
            if (cleanQuery.isNotBlank()) {
                knownTripLocations.forEach { loc ->
                    if (loc.lowercase().contains(cleanQuery)) {
                        val key = loc.lowercase()
                        if (key !in addedKeys) {
                            addedKeys.add(key)
                            results.add(
                                Settlement(
                                    name = loc.trim(),
                                    type = "",
                                    isFromHistory = true
                                )
                            )
                        }
                    }
                }
            }

            // 2. Match from cached database
            val localMatches = synchronized(cacheLock) {
                if (cleanQuery.isBlank()) {
                    settlementsCache.take(20)
                } else {
                    settlementsCache.filter { item ->
                        item.name.lowercase().contains(cleanQuery) ||
                        item.region.lowercase().contains(cleanQuery) ||
                        item.district.lowercase().contains(cleanQuery)
                    }
                }
            }

            // Sort matches: starts with query first, ports first
            val sortedLocal = localMatches.sortedWith(
                compareByDescending<Settlement> { it.isPortOrTerminal }
                    .thenByDescending { it.name.lowercase().startsWith(cleanQuery) }
                    .thenBy { it.name }
            )

            for (s in sortedLocal) {
                val key = s.name.lowercase()
                if (key !in addedKeys) {
                    addedKeys.add(key)
                    results.add(s)
                    if (results.size >= 30) break
                }
            }

            // 3. If query has >= 3 chars, perform live online search via OpenStreetMap Nominatim
            if (cleanQuery.length >= 3 && results.size < 15) {
                try {
                    val onlineNominatim = queryNominatimApi(cleanQuery)
                    for (s in onlineNominatim) {
                        val key = s.name.lowercase()
                        if (key !in addedKeys) {
                            addedKeys.add(key)
                            results.add(s)
                            if (results.size >= 30) break
                        }
                    }
                } catch (e: Exception) {
                    Log.d("LocationService", "Nominatim lookup skipped or failed: ${e.message}")
                }
            }

            results
        }
    }

    /**
     * Поиск через OpenStreetMap Nominatim API для нахождения малых поселков, хуторов и станиц
     */
    private fun queryNominatimApi(query: String): List<Settlement> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&countrycodes=ru&addressdetails=1&limit=6"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GrainTruckTrackerApp/1.0 (Android)")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val jsonArray = JSONArray(body)
        val onlineList = mutableListOf<Settlement>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val displayName = obj.optString("display_name", "")
            val address = obj.optJSONObject("address")

            val cityOrTown = address?.optString("city", "")
                ?.ifBlank { address.optString("town", "") }
                ?.ifBlank { address.optString("village", "") }
                ?.ifBlank { address.optString("hamlet", "") }
                ?.ifBlank { address.optString("isolated_dwelling", "") }
                ?: ""

            val state = address?.optString("state", "") ?: ""
            val county = address?.optString("county", "") ?: ""

            val name = if (cityOrTown.isNotBlank()) cityOrTown else displayName.split(",").firstOrNull() ?: query

            if (name.isNotBlank()) {
                onlineList.add(
                    Settlement(
                        name = name.trim(),
                        type = "",
                        region = state.trim(),
                        district = county.trim(),
                        isPortOrTerminal = isKnownGrainPort(name),
                        isOnlineResult = true
                    )
                )
            }
        }

        return onlineList
    }

    private fun isKnownGrainPort(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("новороссийск") ||
               lower.contains("тамань") ||
               lower.contains("ростов") ||
               lower.contains("азов") ||
               lower.contains("ейск") ||
               lower.contains("туапсе") ||
               lower.contains("темрюк") ||
               lower.contains("таганрог") ||
               lower.contains("кавказ")
    }

    /**
     * Базовый офлайн-справочник зерновых терминалов, портов, станиц и элеваторных центров
     */
    private fun getCoreGrainLogisticsSettlements(): List<Settlement> {
        return listOf(
            // Ключевые порты и зерновые терминалы (выгрузка)
            Settlement("Новороссийск (НЗТ / КСК)", "г.", "Краснодарский край", isPortOrTerminal = true),
            Settlement("Тамань (ОТЭКО)", "пос.", "Краснодарский край", "Темрюкский р-н", isPortOrTerminal = true),
            Settlement("Ростов-на-Дону (Зерновой терминал)", "г.", "Ростовская область", isPortOrTerminal = true),
            Settlement("Азов (Порт Азов)", "г.", "Ростовская область", isPortOrTerminal = true),
            Settlement("Ейск (Порт Ейск)", "г.", "Краснодарский край", isPortOrTerminal = true),
            Settlement("Таганрог (Морской порт)", "г.", "Ростовская область", isPortOrTerminal = true),
            Settlement("Туапсе (Зерновой терминал)", "г.", "Краснодарский край", isPortOrTerminal = true),
            Settlement("Темрюк (Морской порт)", "г.", "Краснодарский край", isPortOrTerminal = true),
            Settlement("Кавказ (Порт Кавказ)", "пос.", "Краснодарский край", isPortOrTerminal = true),
            Settlement("Астрахань (Зерновой порт)", "г.", "Астраханская область", isPortOrTerminal = true),

            // Краснодарский край (элеваторы, тока, хозяйства)
            Settlement("Каневская", "ст-ца", "Краснодарский край", "Каневской р-н"),
            Settlement("Павловская", "ст-ца", "Краснодарский край", "Павловский р-н"),
            Settlement("Тимашевск", "г.", "Краснодарский край", "Тимашевский р-н"),
            Settlement("Усть-Лабинск", "г.", "Краснодарский край", "Усть-Лабинский р-н"),
            Settlement("Кореновск", "г.", "Краснодарский край", "Кореновский р-н"),
            Settlement("Кропоткин", "г.", "Краснодарский край", "Кавказский р-н"),
            Settlement("Тихорецк", "г.", "Краснодарский край", "Тихорецкий р-н"),
            Settlement("Ленинградская", "ст-ца", "Краснодарский край", "Ленинградский р-н"),
            Settlement("Брюховецкая", "ст-ца", "Краснодарский край", "Брюховецкий р-н"),
            Settlement("Староминская", "ст-ца", "Краснодарский край", "Староминский р-н"),
            Settlement("Кущёвская", "ст-ца", "Краснодарский край", "Кущёвский р-н"),
            Settlement("Динская", "ст-ца", "Краснодарский край", "Динской р-н"),
            Settlement("Славянск-на-Кубани", "г.", "Краснодарский край"),
            Settlement("Выселки", "ст-ца", "Краснодарский край", "Выселковский р-н"),
            Settlement("Новопокровская", "ст-ца", "Краснодарский край", "Новопокровский р-н"),
            Settlement("Крыловская", "ст-ца", "Краснодарский край", "Крыловский р-н"),
            Settlement("Тбилисская", "ст-ца", "Краснодарский край", "Тбилисский р-н"),
            Settlement("Гулькевичи", "г.", "Краснодарский край", "Гулькевичский р-н"),
            Settlement("Армавир", "г.", "Краснодарский край"),
            Settlement("Лабинск", "г.", "Краснодарский край"),
            Settlement("Новокубанск", "г.", "Краснодарский край"),
            Settlement("Абинск", "г.", "Краснодарский край"),
            Settlement("Крымск", "г.", "Краснодарский край"),
            Settlement("Краснодар", "г.", "Краснодарский край"),

            // Ростовская область
            Settlement("Сальск", "г.", "Ростовская область", "Сальский р-н"),
            Settlement("Зерноград", "г.", "Ростовская область", "Зерноградский р-н"),
            Settlement("Песчанокопское", "с.", "Ростовская область", "Песчанокопский р-н"),
            Settlement("Целина", "пос.", "Ростовская область", "Целинский р-н"),
            Settlement("Егорлыкская", "ст-ца", "Ростовская область", "Егорлыкский р-н"),
            Settlement("Семикаракорск", "г.", "Ростовская область"),
            Settlement("Миллерово", "г.", "Ростовская область"),
            Settlement("Морозовск", "г.", "Ростовская область"),
            Settlement("Цимлянск", "г.", "Ростовская область"),
            Settlement("Волгодонск", "г.", "Ростовская область"),
            Settlement("Белая Калитва", "г.", "Ростовская область"),
            Settlement("Каменск-Шахтинский", "г.", "Ростовская область"),
            Settlement("Шахты", "г.", "Ростовская область"),
            Settlement("Батайск", "г.", "Ростовская область"),

            // Ставропольский край
            Settlement("Ставрополь", "г.", "Ставропольский край"),
            Settlement("Изобильный", "г.", "Ставропольский край"),
            Settlement("Светлоград", "г.", "Ставропольский край"),
            Settlement("Ипатово", "г.", "Ставропольский край"),
            Settlement("Благодарный", "г.", "Ставропольский край"),
            Settlement("Будённовск", "г.", "Ставропольский край"),
            Settlement("Новоалександровск", "г.", "Ставропольский край"),
            Settlement("Зеленокумск", "г.", "Ставропольский край"),
            Settlement("Невинномысск", "г.", "Ставропольский край"),
            Settlement("Минеральные Воды", "г.", "Ставропольский край"),

            // Воронежская и Волгоградская области
            Settlement("Воронеж", "г.", "Воронежская область"),
            Settlement("Россошь", "г.", "Воронежская область"),
            Settlement("Лиски", "г.", "Воронежская область"),
            Settlement("Павловск", "г.", "Воронежская область"),
            Settlement("Острогожск", "г.", "Воронежская область"),
            Settlement("Борисоглебск", "г.", "Воронежская область"),
            Settlement("Калач", "г.", "Воронежская область"),
            Settlement("Волгоград", "г.", "Волгоградская область"),
            Settlement("Михайловка", "г.", "Волгоградская область"),
            Settlement("Урюпинск", "г.", "Волгоградская область"),
            Settlement("Новоаннинский", "г.", "Волгоградская область"),
            Settlement("Калач-на-Дону", "г.", "Волгоградская область"),
            Settlement("Фролово", "г.", "Волгоградская область"),
            Settlement("Котельниково", "г.", "Волгоградская область"),

            // Черноземье и Поволжье
            Settlement("Тамбов", "г.", "Тамбовская область"),
            Settlement("Мичуринск", "г.", "Тамбовская область"),
            Settlement("Липецк", "г.", "Липецкая область"),
            Settlement("Елец", "г.", "Липецкая область"),
            Settlement("Грязи", "г.", "Липецкая область"),
            Settlement("Белгород", "г.", "Белгородская область"),
            Settlement("Старый Оскол", "г.", "Белгородская область"),
            Settlement("Курск", "г.", "Курская область"),
            Settlement("Саратов", "г.", "Саратовская область"),
            Settlement("Энгельс", "г.", "Саратовская область"),
            Settlement("Балашов", "г.", "Саратовская область"),
            Settlement("Самара", "г.", "Самарская область"),
            Settlement("Сызрань", "г.", "Самарская область"),
            Settlement("Пенза", "г.", "Пензенская область"),
            Settlement("Оренбург", "г.", "Оренбургская область"),
            Settlement("Барнаул", "г.", "Алтайский край")
        )
    }
}
