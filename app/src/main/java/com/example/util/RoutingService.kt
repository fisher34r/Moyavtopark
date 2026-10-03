package com.example.util

import android.util.Log
import com.example.data.local.RouteCacheDao
import com.example.data.local.SettingsPreferences
import com.example.data.model.RouteCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

sealed class RouteResult {
    data class Success(
        val distanceKm: Double,
        val provider: String,
        val fromCache: Boolean,
        val originFormatted: String? = null,
        val destFormatted: String? = null
    ) : RouteResult()

    data class Error(val message: String) : RouteResult()
}

data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val displayName: String
)

class RoutingService(
    private val routeCacheDao: RouteCacheDao,
    private val settings: SettingsPreferences
) {
    companion object {
        private const val TAG = "RoutingService"
        private const val USER_AGENT = "MoyAvtopark-GrainCarrier/1.0 (Android; Logistics)"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun normalizeLocation(text: String): String {
        return text.trim().lowercase().replace(Regex("[\\s,.-]+"), " ")
    }

    suspend fun calculateDistance(
        origin: String,
        destination: String,
        forceRefresh: Boolean = false
    ): RouteResult = withContext(Dispatchers.IO) {
        val trimmedOrigin = origin.trim()
        val trimmedDest = destination.trim()

        if (trimmedOrigin.isBlank() || trimmedDest.isBlank()) {
            return@withContext RouteResult.Error("Укажите оба пункта: погрузку и выгрузку")
        }

        val normOrigin = normalizeLocation(trimmedOrigin)
        val normDest = normalizeLocation(trimmedDest)

        if (normOrigin == normDest) {
            return@withContext RouteResult.Success(
                distanceKm = 0.0,
                provider = "LOCAL",
                fromCache = true
            )
        }

        // 1. Check local offline cache
        if (!forceRefresh) {
            val cached = routeCacheDao.getCachedRoute(normOrigin, normDest)
                ?: routeCacheDao.getCachedRoute(normDest, normOrigin)

            if (cached != null) {
                Log.d(TAG, "Route loaded from cache: $normOrigin -> $normDest = ${cached.distanceKm} km")
                return@withContext RouteResult.Success(
                    distanceKm = cached.distanceKm,
                    provider = cached.provider,
                    fromCache = true
                )
            }
        }

        // 2. Try Yandex if API key is configured
        val yandexKey = settings.yandexApiKey.trim()
        if (yandexKey.isNotBlank()) {
            val yandexResult = calculateWithYandex(trimmedOrigin, trimmedDest, yandexKey)
            if (yandexResult is RouteResult.Success) {
                // Save to cache
                routeCacheDao.insertRoute(
                    RouteCacheEntity(
                        originNormalized = normOrigin,
                        destinationNormalized = normDest,
                        distanceKm = yandexResult.distanceKm,
                        provider = "YANDEX"
                    )
                )
                return@withContext yandexResult
            }
        }

        // 3. Fallback to Open Source (Nominatim / Photon + OSRM)
        try {
            val originPoint = geocodeOpen(trimmedOrigin)
                ?: return@withContext RouteResult.Error("Не удалось найти пункт погрузки: \"$trimmedOrigin\". Уточните название города или района.")

            val destPoint = geocodeOpen(trimmedDest)
                ?: return@withContext RouteResult.Error("Не удалось найти пункт выгрузки: \"$trimmedDest\". Уточните название города или района.")

            val rawDistanceKm = routeWithOsrm(originPoint, destPoint)
                ?: return@withContext RouteResult.Error("Не удалось проложить маршрут между пунктами. Проверьте названия.")

            val detourMultiplier = 1.0 + (settings.routeDetourPercent.coerceIn(0.0, 50.0) / 100.0)
            val finalKm = ((rawDistanceKm * detourMultiplier) * 10.0).roundToInt() / 10.0

            // Cache result
            routeCacheDao.insertRoute(
                RouteCacheEntity(
                    originNormalized = normOrigin,
                    destinationNormalized = normDest,
                    distanceKm = finalKm,
                    provider = "OSRM"
                )
            )

            RouteResult.Success(
                distanceKm = finalKm,
                provider = "OSRM",
                fromCache = false,
                originFormatted = originPoint.displayName,
                destFormatted = destPoint.displayName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Routing error", e)
            RouteResult.Error("Ошибка расчёта: ${e.localizedMessage ?: "Сетевой сбой"}. Введите расстояние вручную.")
        }
    }

    private fun geocodeOpen(query: String): GeoPoint? {
        // Strategy 1: OSM Nominatim
        val nominatimPoint = geocodeNominatim(query)
        if (nominatimPoint != null) return nominatimPoint

        // Strategy 2: Photon (Komoot) fallback
        return geocodePhoton(query)
    }

    private fun geocodeNominatim(query: String): GeoPoint? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1&countrycodes=ru,by,kz"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val jsonArray = org.json.JSONArray(body)
                if (jsonArray.length() == 0) return null

                val item = jsonArray.getJSONObject(0)
                val lat = item.getDouble("lat")
                val lon = item.getDouble("lon")
                val displayName = item.optString("display_name", query)
                GeoPoint(lat = lat, lon = lon, displayName = displayName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim geocoding failed for $query: ${e.message}")
            null
        }
    }

    private fun geocodePhoton(query: String): GeoPoint? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() == 0) return null

                val first = features.getJSONObject(0)
                val geometry = first.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val lon = coordinates.getDouble(0)
                val lat = coordinates.getDouble(1)

                val properties = first.optJSONObject("properties")
                val name = properties?.optString("name") ?: query
                val city = properties?.optString("city")
                val display = if (!city.isNullOrBlank()) "$name, $city" else name

                GeoPoint(lat = lat, lon = lon, displayName = display)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Photon geocoding failed for $query: ${e.message}")
            null
        }
    }

    private fun routeWithOsrm(origin: GeoPoint, dest: GeoPoint): Double? {
        return try {
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.lon},${origin.lat};${dest.lon},${dest.lat}?overview=false"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optString("code") != "Ok") return null

                val routes = json.optJSONArray("routes") ?: return null
                if (routes.length() == 0) return null

                val firstRoute = routes.getJSONObject(0)
                val distanceMeters = firstRoute.getDouble("distance")
                distanceMeters / 1000.0
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM routing failed: ${e.message}")
            null
        }
    }

    private fun calculateWithYandex(origin: String, dest: String, apiKey: String): RouteResult {
        return try {
            val p1 = geocodeYandex(origin, apiKey)
                ?: return RouteResult.Error("Яндекс не нашел точку: $origin")
            val p2 = geocodeYandex(dest, apiKey)
                ?: return RouteResult.Error("Яндекс не нашел точку: $dest")

            // Use OSRM for routing using high-accuracy Yandex coordinates
            val distanceKm = routeWithOsrm(p1, p2)
                ?: return RouteResult.Error("Не удалось проложить маршрут между точками")

            val detourMultiplier = 1.0 + (settings.routeDetourPercent.coerceIn(0.0, 50.0) / 100.0)
            val finalKm = ((distanceKm * detourMultiplier) * 10.0).roundToInt() / 10.0

            RouteResult.Success(
                distanceKm = finalKm,
                provider = "YANDEX+OSRM",
                fromCache = false,
                originFormatted = p1.displayName,
                destFormatted = p2.displayName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Yandex calculation failed: ${e.message}")
            RouteResult.Error("Ошибка Яндекс API: ${e.localizedMessage}")
        }
    }

    private fun geocodeYandex(query: String, apiKey: String): GeoPoint? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://geocode-maps.yandex.ru/1.x/?apikey=$apiKey&geocode=$encoded&format=json&results=1"
            val request = Request.Builder()
                .url(url)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val responseObj = json.getJSONObject("response")
                val geoObjectCollection = responseObj.getJSONObject("GeoObjectCollection")
                val featureMembers = geoObjectCollection.getJSONArray("featureMember")
                if (featureMembers.length() == 0) return null

                val first = featureMembers.getJSONObject(0).getJSONObject("GeoObject")
                val posStr = first.getJSONObject("Point").getString("pos") // "lon lat"
                val parts = posStr.split(" ")
                val lon = parts[0].toDouble()
                val lat = parts[1].toDouble()
                val text = first.getJSONObject("metaDataProperty")
                    .getJSONObject("GeocoderMetaData")
                    .getString("text")

                GeoPoint(lat = lat, lon = lon, displayName = text)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Yandex geocode error: ${e.message}")
            null
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        routeCacheDao.clearCache()
    }

    suspend fun getCachedRoutesCount(): Int = withContext(Dispatchers.IO) {
        routeCacheDao.getCacheCount()
    }
}
