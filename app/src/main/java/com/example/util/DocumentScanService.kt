package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.local.SettingsPreferences
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Модель распознанного рейса из накладной или ведомости зерновоза
 */
data class ScannedTripItem(
    val tripNumber: String = "",
    val cargoType: String = "Пшеница",
    val loadingLocation: String = "",
    val unloadingLocation: String = "",
    val weightTons: Double = 0.0,
    val distanceKm: Double = 0.0,
    val ttnNumber: String = "",
    val truckPlate: String = "",
    val driverName: String = "",
    val customerName: String = "",
    val dateText: String = "",
    val rateValue: Double = 4.5,
    val isSelected: Boolean = true
)

sealed class ScanResult {
    data class Success(
        val trips: List<ScannedTripItem>,
        val documentType: String // "ТТН / Накладная", "Ведомость / Реестр"
    ) : ScanResult()

    data class Error(val message: String) : ScanResult()
}

class DocumentScanService(
    private val context: Context,
    private val settings: SettingsPreferences
) {
    companion object {
        private const val TAG = "DocumentScanService"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun scanDocumentFromUri(imageUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext ScanResult.Error(
                "API-ключ Gemini не настроен. Пожалуйста, укажите ваш бесплатный ключ Gemini AI в Настройках приложения."
            )
        }

        val base64Image = try {
            encodeImageToBase64(imageUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}", e)
            return@withContext ScanResult.Error("Не удалось прочитать изображение: ${e.localizedMessage}")
        } ?: return@withContext ScanResult.Error("Не удалось обработать изображение.")

        val prompt = """
            Ты — специализированный ассистент по оцифровке документов зерновозов и грузоперевозок (ТТН, транспортные накладные, талоны весовой, рукописные ведомости, реестры рейсов).
            Внимательно изучи прикреплённое изображение. Это может быть как печатный документ (1-Т, ТрН, квитанция), так и рукописная таблица/записи от руки весовщика или водителя.
            
            Извлеки все отдельные рейсы/перевозки, упомянутые в документе. Если это реестр или таблица из нескольких строк, извлеки каждую строку как отдельный рейс.
            
            Верни СТРОГО валидный JSON (без markdown-оберток ```json и без лишнего текста) следующего формата:
            {
              "documentType": "ТТН" или "Ведомость",
              "trips": [
                {
                  "tripNumber": "номер рейса если есть",
                  "cargoType": "культура/груз, например Пшеница, Ячмень, Кукуруза, Подсолнечник (по умолчанию Пшеница)",
                  "loadingLocation": "пункт погрузки / элеватор / ток / хозяйство",
                  "unloadingLocation": "пункт выгрузки / элеватор / порт / комбинат",
                  "weightTons": 28.5,
                  "distanceKm": 0,
                  "ttnNumber": "номер ТТН, талона или накладной",
                  "truckPlate": "госномер зерновоза/прицепа если указан",
                  "driverName": "ФИО водителя если указано",
                  "customerName": "заказчик или грузоотправитель если указан",
                  "date": "дата в формате DD.MM.YYYY если есть"
                }
              ]
            }
            
            Важные правила:
            1. Вес должен быть в тоннах (если указано в килограммах, например 28500 кг, переведи в 28.5 т). Ищи вес нетто.
            2. Если какие-то поля не найдены на фото, верни для них пустую строку "" или 0 для чисел.
            3. Если это рукописная ведомость с несколькими строками, обязательно добавь в массив "trips" каждую строку.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            })
        }

        // Try gemini-1.5-flash endpoint
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code ${response.code}: $responseBody")
                return@withContext ScanResult.Error(
                    "Ошибка Gemini API (${response.code}): Проверьте правильность API-ключа в Настройках."
                )
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Network call failed: ${e.message}", e)
            ScanResult.Error("Ошибка связи с сервером распознавания: ${e.localizedMessage}")
        }
    }

    private fun parseGeminiResponse(jsonString: String): ScanResult {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return ScanResult.Error("Нейросеть не смогла распознать данные в документе.")
            }
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            val cleanedJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val dataObj = JSONObject(cleanedJson)
            val docType = dataObj.optString("documentType", "Документ")
            val tripsArray = dataObj.optJSONArray("trips") ?: JSONArray()

            val defaultRate = settings.defaultRateValue
            val defaultDriver = settings.defaultDriver
            val defaultTruck = settings.defaultTruck
            val defaultCargo = settings.defaultCargo

            val resultTrips = mutableListOf<ScannedTripItem>()
            for (i in 0 until tripsArray.length()) {
                val item = tripsArray.getJSONObject(i)
                val cargo = item.optString("cargoType", "").ifBlank { defaultCargo }
                val weight = item.optDouble("weightTons", 0.0).let { if (it > 0) it else 0.0 }
                val driver = item.optString("driverName", "").ifBlank { defaultDriver }
                val truck = item.optString("truckPlate", "").ifBlank { defaultTruck }

                resultTrips.add(
                    ScannedTripItem(
                        tripNumber = item.optString("tripNumber", ""),
                        cargoType = cargo,
                        loadingLocation = item.optString("loadingLocation", ""),
                        unloadingLocation = item.optString("unloadingLocation", ""),
                        weightTons = weight,
                        distanceKm = item.optDouble("distanceKm", 0.0),
                        ttnNumber = item.optString("ttnNumber", ""),
                        truckPlate = truck,
                        driverName = driver,
                        customerName = item.optString("customerName", ""),
                        dateText = item.optString("date", ""),
                        rateValue = defaultRate,
                        isSelected = true
                    )
                )
            }

            if (resultTrips.isEmpty()) {
                ScanResult.Error("В документе не обнаружено записей о рейсах зерновоза.")
            } else {
                ScanResult.Success(resultTrips, docType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            ScanResult.Error("Не удалось разобрать ответ распознавания: ${e.localizedMessage}")
        }
    }

    private fun encodeImageToBase64(imageUri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (originalBitmap == null) return null

        // Downscale image to max 1600px dimension for fast network upload and low bandwidth
        val maxDimension = 1600
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (width > height) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Преобразование ScannedTripItem в готовую сущность Trip для сохранения в базу данных
     */
    fun createTripFromScanned(
        item: ScannedTripItem,
        tripNumberGenerator: () -> String
    ): Trip {
        val parsedDate = parseDateToMillis(item.dateText)
        val distance = if (item.distanceKm > 0) item.distanceKm else settings.lastDistanceKm.let { if (it > 0) it else 280.0 }
        val rateVal = if (item.rateValue > 0) item.rateValue else settings.defaultRateValue
        val finalPrice = Trip.calculatePrice(
            weight = item.weightTons,
            distance = distance,
            rateType = settings.defaultRateType,
            rateValue = rateVal
        )

        return Trip(
            tripNumber = item.tripNumber.ifBlank { tripNumberGenerator() },
            cargoType = item.cargoType.ifBlank { settings.defaultCargo },
            loadingDate = parsedDate,
            unloadingDate = parsedDate,
            loadingLocation = item.loadingLocation,
            unloadingLocation = item.unloadingLocation,
            weightTons = item.weightTons,
            distanceKm = distance,
            rateType = settings.defaultRateType,
            rateValue = rateVal,
            totalPrice = finalPrice,
            status = TripStatus.IN_TRANSIT,
            ttnNumber = item.ttnNumber,
            customerName = item.customerName,
            driverName = item.driverName.ifBlank { settings.defaultDriver },
            truckPlate = item.truckPlate.ifBlank { settings.defaultTruck },
            fuelConsumptionRate = settings.defaultFuelConsumptionRate,
            fuelPricePerLiter = settings.defaultFuelPricePerLiter,
            driverSalaryPercent = settings.defaultDriverSalaryPercent,
            driverSalaryAmount = if (finalPrice > 0) finalPrice * (settings.defaultDriverSalaryPercent / 100.0) else 0.0,
            notes = "Распознано со скана документа"
        )
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val formats = listOf("dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
        for (fmt in formats) {
            try {
                val parsed = SimpleDateFormat(fmt, Locale.getDefault()).parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
