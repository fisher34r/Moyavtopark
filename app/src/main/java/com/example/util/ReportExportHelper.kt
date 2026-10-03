package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Trip
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong

object ReportExportHelper {

    /**
     * Экспорт реестра рейсов в PDF в точном соответствии с референсом:
     * - Заголовок: АВТОМОБИЛЬ [Госномер]
     * - Подзаголовок: Водитель: [ФИО] | Период: [Период]
     * - Таблица: № п/п, Дата, Пункт отправления, Вес нетто (кг), Тариф (руб/кг), Стоимость (руб.)
     * - ИТОГО: суммарный вес и суммарная стоимость
     */
    fun exportRegistryToPdf(
        context: Context,
        truckPlate: String,
        driverName: String,
        periodTitle: String,
        trips: List<Trip>
    ) {
        if (trips.isEmpty()) {
            Toast.makeText(context, "Нет рейсов для формирования PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val margin = 36f

            val titlePaint = Paint().apply {
                color = Color.parseColor("#104A75") // Фирменный синий цвет из референса
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#155383") // Темно-синяя шапка таблицы
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val rowBgAltPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.6f
            }

            val textPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val textBoldPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Column widths (Total width: 523pt)
            val colWidths = floatArrayOf(32f, 62f, 145f, 90f, 90f, 104f)
            val tableWidth = colWidths.sum()

            // В отчете «реестр рейсов» сортировать рейсы по дате от меньшего к большему
            val sortedTrips = trips.sortedBy { it.loadingDate }
            val rowsPerPage = 28
            val pageCount = ((sortedTrips.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

            val displayTruck = truckPlate.ifBlank {
                sortedTrips.firstOrNull()?.truckPlate?.takeIf { it.isNotBlank() } ?: "ВСЕ ТС"
            }
            val displayDriver = driverName.ifBlank {
                sortedTrips.firstOrNull()?.driverName?.takeIf { it.isNotBlank() } ?: "Все водители"
            }

            for (pageIndex in 1..pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                var y = margin + 20f

                // Заголовок документа
                canvas.drawText("АВТОМОБИЛЬ ${displayTruck.uppercase(Locale.forLanguageTag("ru"))}", margin, y, titlePaint)
                y += 16f

                canvas.drawText("Водитель: $displayDriver | Период: $periodTitle", margin, y, subtitlePaint)
                y += 20f

                // Таблица: Шапка
                val headerHeight = 22f
                canvas.drawRect(margin, y, margin + tableWidth, y + headerHeight, headerBgPaint)

                var x = margin
                val headers = arrayOf("№ п/п", "Дата", "Пункт отправления", "Вес нетто (кг)", "Тариф (руб/кг)", "Стоимость (руб.)")
                for (i in headers.indices) {
                    val w = colWidths[i]
                    val title = headers[i]
                    val textW = headerTextPaint.measureText(title)
                    val textX = when (i) {
                        0 -> x + (w - textW) / 2f
                        3, 4, 5 -> x + w - textW - 6f
                        else -> x + 6f
                    }
                    canvas.drawText(title, textX, y + 14.5f, headerTextPaint)
                    canvas.drawRect(x, y, x + w, y + headerHeight, borderPaint)
                    x += w
                }
                y += headerHeight

                // Строки данных
                val rowHeight = 18f
                val startIdx = (pageIndex - 1) * rowsPerPage
                val endIdx = (startIdx + rowsPerPage).coerceAtMost(sortedTrips.size)

                for (idx in startIdx until endIdx) {
                    val trip = sortedTrips[idx]
                    val isAlt = idx % 2 == 1
                    if (isAlt) {
                        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, rowBgAltPaint)
                    }

                    val rowNum = (idx + 1).toString()
                    val dateStr = Formatters.formatDate(trip.loadingDate)
                    val originStr = trip.loadingLocation.ifBlank { "Манойлино" }
                    val weightKg = (trip.weightTons * 1000.0).roundToLong()
                    val weightStr = Formatters.formatWeightKgExact(weightKg)

                    val tariffKg = if (weightKg > 0) trip.totalPrice / weightKg else 0.0
                    val tariffStr = String.format(Locale.forLanguageTag("ru"), "%.2f", tariffKg)
                    val costStr = Formatters.formatMoneyRub(trip.totalPrice)

                    x = margin
                    val rowValues = arrayOf(rowNum, dateStr, originStr, weightStr, tariffStr, costStr)
                    for (c in rowValues.indices) {
                        val w = colWidths[c]
                        val text = rowValues[c]
                        val curPaint = if (c == 0 || c == 5) textBoldPaint else textPaint
                        val textW = curPaint.measureText(text)
                        val textX = when (c) {
                            0 -> x + (w - textW) / 2f
                            3, 4, 5 -> x + w - textW - 6f
                            else -> x + 6f
                        }
                        canvas.drawText(text, textX, y + 12.5f, curPaint)
                        canvas.drawRect(x, y, x + w, y + rowHeight, borderPaint)
                        x += w
                    }
                    y += rowHeight
                }

                // ИТОГО (на последней странице)
                if (pageIndex == pageCount) {
                    val footerHeight = 22f
                    val totalWeightKg = sortedTrips.sumOf { it.weightTons * 1000.0 }.roundToLong()
                    val totalPrice = sortedTrips.sumOf { it.totalPrice }

                    // Фоновая плашка итогов
                    val footerBgPaint = Paint().apply {
                        color = Color.parseColor("#EFF6FF")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(margin, y, margin + tableWidth, y + footerHeight, footerBgPaint)

                    // Колонки 0, 1, 2 объединены под "ИТОГО:"
                    val mergedWidth1 = colWidths[0] + colWidths[1] + colWidths[2]
                    canvas.drawText("ИТОГО:", margin + 8f, y + 15f, textBoldPaint)
                    canvas.drawRect(margin, y, margin + mergedWidth1, y + footerHeight, borderPaint)

                    // Вес нетто итог
                    val w3 = colWidths[3]
                    val x3 = margin + mergedWidth1
                    val totalWeightStr = Formatters.formatWeightKgExact(totalWeightKg)
                    val twW = textBoldPaint.measureText(totalWeightStr)
                    canvas.drawText(totalWeightStr, x3 + w3 - twW - 6f, y + 15f, textBoldPaint)
                    canvas.drawRect(x3, y, x3 + w3, y + footerHeight, borderPaint)

                    // Тариф пустой
                    val w4 = colWidths[4]
                    val x4 = x3 + w3
                    canvas.drawRect(x4, y, x4 + w4, y + footerHeight, borderPaint)

                    // Итоговая стоимость
                    val w5 = colWidths[5]
                    val x5 = x4 + w4
                    val totalCostStr = Formatters.formatMoneyRub(totalPrice)
                    val tcPaint = Paint(textBoldPaint).apply {
                        color = Color.parseColor("#0F5132")
                    }
                    val tcW = tcPaint.measureText(totalCostStr)
                    canvas.drawText(totalCostStr, x5 + w5 - tcW - 6f, y + 15f, tcPaint)
                    canvas.drawRect(x5, y, x5 + w5, y + footerHeight, borderPaint)

                    y += footerHeight + 15f

                    // Подпись внизу
                    val notePaint = Paint().apply {
                        color = Color.parseColor("#94A3B8")
                        textSize = 8f
                        isAntiAlias = true
                    }
                    val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")).format(Date())
                    canvas.drawText("Документ сформирован автоматически в системе Учет зерновозов • $timestamp", margin, y, notePaint)
                }

                pdfDoc.finishPage(page)
            }

            val safeTruck = (truckPlate.ifBlank { "Автопарк" }).replace("[^a-zA-Z0-9а-яА-Я]".toRegex(), "_")
            val fileName = "Реестр_рейсов_${safeTruck}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            shareFile(context, file, "application/pdf", "Реестр рейсов $displayTruck")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка формирования PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Экспорт реестра рейсов в Excel (.xlsx файл в формате Office Open XML).
     * Вид идентичен оригиналу: заголовок, подзаголовок, таблица с форматированием,
     * ИТОГО, подпись.
     */
    fun exportRegistryToExcel(
        context: Context,
        truckPlate: String,
        driverName: String,
        periodTitle: String,
        trips: List<Trip>
    ) {
        if (trips.isEmpty()) {
            Toast.makeText(context, "Нет рейсов для выгрузки в Excel", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // В отчете «реестр рейсов» сортировать рейсы по дате от меньшего к большему
            val sortedTrips = trips.sortedBy { it.loadingDate }
            val displayTruck = truckPlate.ifBlank {
                sortedTrips.firstOrNull()?.truckPlate?.takeIf { it.isNotBlank() } ?: "ВСЕ ТС"
            }
            val displayDriver = driverName.ifBlank {
                sortedTrips.firstOrNull()?.driverName?.takeIf { it.isNotBlank() } ?: "Все водители"
            }

            val safeTruck = (truckPlate.ifBlank { "Автопарк" }).replace("[^a-zA-Z0-9а-яА-Я]".toRegex(), "_")
            val fileName = "Реестр_рейсов_${safeTruck}_${System.currentTimeMillis()}.xlsx"
            val file = File(context.cacheDir, fileName)

            ZipOutputStream(FileOutputStream(file)).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
                writeZipEntry(zip, "_rels/.rels", RELS_XML)
                writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
                writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
                writeZipEntry(zip, "xl/worksheets/sheet1.xml",
                    buildSheetXml(sortedTrips, displayTruck, displayDriver, periodTitle))
            }

            shareFile(
                context, file,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Реестр рейсов Excel $displayTruck"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка выгрузки в Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Вспомогательные методы для генерации XLSX ──────────────────────────

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun xmlEscape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")

    private fun inlineCell(col: Int, row: Int, value: String, styleId: Int): String {
        val ref = "${'A' + col}$row"
        val escaped = xmlEscape(value)
        return """<c r="$ref" t="inlineStr" s="$styleId"><is><t>$escaped</t></is></c>"""
    }

    /**
     * Формирует XML листа с полным видом реестра:
     * Строка 1: Заголовок «АВТОМОБИЛЬ [госномер]» (объединение A1:F1)
     * Строка 2: «Водитель: ... | Период: ...» (объединение A2:F2)
     * Строка 3: пустая
     * Строка 4: Шапка таблицы
     * Строки 5..N+4: Данные
     * Строка N+5: ИТОГО (объединение A:C)
     * Строка N+7: Подпись «Сформировано в системе...»
     */
    private fun buildSheetXml(
        trips: List<Trip>,
        displayTruck: String,
        displayDriver: String,
        periodTitle: String
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"""")
        sb.append(""" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")

        // Ширины колонок
        sb.append("<cols>")
        sb.append("""<col min="1" max="1" width="8" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="14" customWidth="1"/>""")
        sb.append("""<col min="3" max="3" width="25" customWidth="1"/>""")
        sb.append("""<col min="4" max="4" width="16" customWidth="1"/>""")
        sb.append("""<col min="5" max="5" width="16" customWidth="1"/>""")
        sb.append("""<col min="6" max="6" width="20" customWidth="1"/>""")
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Строка 1: Заголовок — стиль 4 (крупный жирный синий)
        val titleText = "АВТОМОБИЛЬ ${displayTruck.uppercase(Locale.forLanguageTag("ru"))}"
        sb.append("""<row r="1" ht="24">""")
        sb.append(inlineCell(0, 1, titleText, 4))
        sb.append("</row>")

        // Строка 2: Подзаголовок — стиль 5 (курсив серый)
        val subtitleText = "Водитель: $displayDriver | Период: $periodTitle"
        sb.append("""<row r="2">""")
        sb.append(inlineCell(0, 2, subtitleText, 5))
        sb.append("</row>")

        // Строка 3: пустая
        sb.append("""<row r="3"/>""")

        // Строка 4: Шапка таблицы — стиль 3 (белый жирный на синем фоне)
        val headers = arrayOf("№ п/п", "Дата", "Пункт отправления", "Вес нетто (кг)", "Тариф (руб/кг)", "Стоимость (руб.)")
        sb.append("""<row r="4">""")
        headers.forEachIndexed { i, h -> sb.append(inlineCell(i, 4, h, 3)) }
        sb.append("</row>")

        // Данные (начиная со строки 5)
        trips.forEachIndexed { idx, trip ->
            val rowNum = idx + 5
            val weightKg = (trip.weightTons * 1000.0).roundToLong()
            val tariffKg = if (weightKg > 0) trip.totalPrice / weightKg else 0.0

            // Чередование фона: чётные — стиль 1 (обычный), нечётные — стиль 6 (с серым фоном)
            val normalStyle = if (idx % 2 == 1) 6 else 1
            val boldNormalStyle = if (idx % 2 == 1) 7 else 2

            val values = arrayOf(
                (idx + 1).toString(),
                Formatters.formatDate(trip.loadingDate),
                trip.loadingLocation.ifBlank { "Манойлино" },
                Formatters.formatWeightKgExact(weightKg),
                String.format(Locale.forLanguageTag("ru"), "%.2f", tariffKg),
                Formatters.formatMoneyRub(trip.totalPrice)
            )

            sb.append("""<row r="$rowNum">""")
            values.forEachIndexed { i, v ->
                val style = if (i == 0 || i == 5) boldNormalStyle else normalStyle
                sb.append(inlineCell(i, rowNum, v, style))
            }
            sb.append("</row>")
        }

        // ИТОГО — стиль 8 (жирный на голубом фоне), стиль 9 (зелёный цвет для итоговой суммы)
        val totalRowNum = trips.size + 5
        val totalWeightKg = trips.sumOf { it.weightTons * 1000.0 }.roundToLong()
        val totalPrice = trips.sumOf { it.totalPrice }

        sb.append("""<row r="$totalRowNum">""")
        sb.append(inlineCell(0, totalRowNum, "ИТОГО:", 8))
        sb.append(inlineCell(1, totalRowNum, "", 8))
        sb.append(inlineCell(2, totalRowNum, "", 8))
        sb.append(inlineCell(3, totalRowNum, Formatters.formatWeightKgExact(totalWeightKg), 8))
        sb.append(inlineCell(4, totalRowNum, "", 8))
        sb.append(inlineCell(5, totalRowNum, Formatters.formatMoneyRub(totalPrice), 9))
        sb.append("</row>")

        // Подпись (через строку)
        val noteRowNum = totalRowNum + 2
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")).format(Date())
        sb.append("""<row r="$noteRowNum">""")
        sb.append(inlineCell(0, noteRowNum, "Сформировано в системе Учет зерновозов: $timestamp", 10))
        sb.append("</row>")

        sb.append("</sheetData>")

        // Объединения ячеек
        sb.append("""<mergeCells count="3">""")
        sb.append("""<mergeCell ref="A1:F1"/>""")
        sb.append("""<mergeCell ref="A2:F2"/>""")
        sb.append("""<mergeCell ref="A${totalRowNum}:C${totalRowNum}"/>""")
        sb.append("</mergeCells>")

        sb.append("</worksheet>")
        return sb.toString()
    }

    // ── XML-шаблоны для XLSX (Office Open XML) ───────────────────────────

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Реестр рейсов" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    // Стили (индексы cellXfs):
    // 0 — по умолчанию (без рамок)
    // 1 — обычный с рамками
    // 2 — жирный с рамками
    // 3 — шапка таблицы (белый жирный на #155383)
    // 4 — заголовок (крупный жирный синий #104A75)
    // 5 — подзаголовок (курсив серый #475569)
    // 6 — обычный с рамками + серый фон #F8FAFC (чередование строк)
    // 7 — жирный с рамками + серый фон #F8FAFC
    // 8 — ИТОГО (жирный на голубом #EFF6FF, рамка сверху толстая)
    // 9 — ИТОГО стоимость (жирный зелёный #0F5132 на голубом #EFF6FF)
    // 10 — подпись (мелкий серый #94A3B8)
    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="7">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
<font><b/><sz val="14"/><color rgb="FF104A75"/><name val="Calibri"/></font>
<font><i/><sz val="11"/><color rgb="FF475569"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FF0F5132"/><name val="Calibri"/></font>
<font><sz val="9"/><color rgb="FF94A3B8"/><name val="Calibri"/></font>
</fonts>
<fills count="5">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF155383"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/></patternFill></fill>
</fills>
<borders count="3">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right><top style="thin"><color rgb="FFCBD5E1"/></top><bottom style="thin"><color rgb="FFCBD5E1"/></bottom><diagonal/></border>
<border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right><top style="medium"><color rgb="FF155383"/></top><bottom style="thin"><color rgb="FFCBD5E1"/></bottom><diagonal/></border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="11">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
<xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
<xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
<xf numFmtId="0" fontId="1" fillId="4" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
<xf numFmtId="0" fontId="5" fillId="4" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
<xf numFmtId="0" fontId="6" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
</styleSheet>"""

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Открыть / отправить документ"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Не найдено приложение для открытия файла", Toast.LENGTH_LONG).show()
        }
    }
}
