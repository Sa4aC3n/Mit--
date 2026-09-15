package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import com.example.data.engine.MetGhamrGeoHierarchy
import com.example.data.model.BusinessEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Result data holder for uploaded Excel (.xlsx, .xls) and CSV/text files.
 */
data class ExcelImportParsedResult(
    val fileName: String,
    val fileType: String,
    val headers: List<String>,
    val rows: List<Map<String, String>>,
    val csvFormattedText: String,
    val totalRows: Int,
    val errorMessage: String? = null
)

/**
 * Robust, zero-external-dependency Excel (.xlsx, .xls) and CSV/TSV parser
 * designed specifically for Android. Handles Arabic text, multiple encodings,
 * and standard Excel OpenXML workbook formats.
 */
object ExcelFileImportHelper {

    private const val TAG = "ExcelFileImportHelper"

    /**
     * Reads and parses any Excel or CSV file from a given Uri.
     */
    fun parseUploadedFile(context: Context, uri: Uri): ExcelImportParsedResult {
        val fileName = getFileName(context, uri)
        Log.d(TAG, "Parsing uploaded file: $fileName, Uri: $uri")

        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ExcelImportParsedResult(
                    fileName = fileName,
                    fileType = "غير معروف",
                    headers = emptyList(),
                    rows = emptyList(),
                    csvFormattedText = "",
                    totalRows = 0,
                    errorMessage = "تعذر قراءة بيانات الملف من الذاكرة."
                )

            if (bytes.isEmpty()) {
                return ExcelImportParsedResult(
                    fileName = fileName,
                    fileType = "ملف فارغ",
                    headers = emptyList(),
                    rows = emptyList(),
                    csvFormattedText = "",
                    totalRows = 0,
                    errorMessage = "الملف المرفوع فارغ (0 بايت)."
                )
            }

            val lowerName = fileName.lowercase()

            when {
                // Modern Excel (.xlsx) which is a Zip container
                lowerName.endsWith(".xlsx") || isZipFile(bytes) -> {
                    parseXlsx(fileName, bytes)
                }
                // Excel 2003 XML / SpreadsheetML (.xls or .xml)
                lowerName.endsWith(".xls") || lowerName.endsWith(".xml") || isXmlSpreadsheet(bytes) -> {
                    parseXlsXmlOrFallback(fileName, bytes)
                }
                // Standard CSV or text file
                else -> {
                    parseDelimitedText(fileName, bytes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing file $fileName: ${e.message}", e)
            ExcelImportParsedResult(
                fileName = fileName,
                fileType = "خطأ في المعالجة",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "حدث خطأ أثناء معالجة ملف الإكسل: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Extracts filename from ContentResolver or Uri.
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var name = "ملف_بيانات_مستورد.xlsx"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val displayName = it.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { segment ->
                name = segment.substringAfterLast("/")
            }
        }
        return name
    }

    /**
     * Checks if byte array starts with PK zip magic header (0x50, 0x4B, 0x03, 0x04)
     */
    private fun isZipFile(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    /**
     * Checks if byte array contains XML SpreadsheetML tags.
     */
    private fun isXmlSpreadsheet(bytes: ByteArray): Boolean {
        val sample = String(bytes.take(500).toByteArray(), Charsets.UTF_8)
        return sample.contains("urn:schemas-microsoft-com:office:spreadsheet") ||
                (sample.contains("<?xml") && sample.contains("<Workbook"))
    }

    /**
     * Parses modern Microsoft Excel OpenXML (.xlsx) files without heavy external dependencies.
     */
    private fun parseXlsx(fileName: String, bytes: ByteArray): ExcelImportParsedResult {
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        // 1. Read shared strings and worksheet XML from the Zip container
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name.lowercase()
                if (entryName.endsWith("sharedstrings.xml")) {
                    val entryBytes = zis.readBytes()
                    sharedStrings.addAll(parseSharedStringsXml(entryBytes))
                } else if (entryName.endsWith("sheet1.xml") || (sheetBytes == null && entryName.contains("sheet") && entryName.endsWith(".xml"))) {
                    sheetBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetBytes == null) {
            return ExcelImportParsedResult(
                fileName = fileName,
                fileType = "Excel XLSX",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "تعذر العثور على ورقة العمل الرئيسية في ملف Excel."
            )
        }

        // 2. Parse Sheet XML to extract cell data
        val rawRows = parseSheetXml(sheetBytes!!, sharedStrings)

        if (rawRows.isEmpty()) {
            return ExcelImportParsedResult(
                fileName = fileName,
                fileType = "Excel XLSX",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "ورقة العمل في ملف Excel لا تحتوي على أي صفوف أو بيانات."
            )
        }

        val headerRowIdx = findHeaderRowIndex(rawRows)
        val rawHeaders = rawRows[headerRowIdx]
        val headers = rawHeaders.map { cleanDisplayHeader(it) }
        val dataRows = mutableListOf<Map<String, String>>()
        val csvBuilder = StringBuilder()

        // Append header line to CSV
        csvBuilder.append(headers.joinToString(",") { escapeCsvValue(it) }).append("\n")

        for (i in (headerRowIdx + 1) until rawRows.size) {
            val rowValues = rawRows[i]
            if (rowValues.all { it.isBlank() }) continue // Skip blank rows

            val rowMap = mutableMapOf<String, String>()
            headers.forEachIndexed { idx, header ->
                val cellVal = rowValues.getOrNull(idx)?.trim() ?: ""
                rowMap[header] = cellVal
                // Also store under raw header key if different
                val rawH = rawHeaders.getOrNull(idx)?.trim() ?: ""
                if (rawH.isNotBlank() && rawH != header) {
                    rowMap[rawH] = cellVal
                }
            }
            dataRows.add(rowMap)

            // Append to CSV builder
            val rowCsv = headers.indices.map { idx ->
                escapeCsvValue(rowValues.getOrNull(idx)?.trim() ?: "")
            }.joinToString(",")
            csvBuilder.append(rowCsv).append("\n")
        }

        return ExcelImportParsedResult(
            fileName = fileName,
            fileType = "Microsoft Excel (.xlsx)",
            headers = headers,
            rows = dataRows,
            csvFormattedText = csvBuilder.toString().trimEnd(),
            totalRows = dataRows.size,
            errorMessage = null
        )
    }

    /**
     * Parses sharedStrings.xml from .xlsx container.
     */
    private fun parseSharedStringsXml(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var inTextTag = false
            var currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("si", ignoreCase = true)) {
                            currentText = StringBuilder()
                        } else if (parser.name.equals("t", ignoreCase = true)) {
                            inTextTag = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inTextTag) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inTextTag = false
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            strings.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing sharedStrings.xml: ${e.message}")
        }
        return strings
    }

    /**
     * Parses sheet1.xml from .xlsx container.
     */
    private fun parseSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var currentRowCells = mutableListOf<String>()
            var currentCellRef = ""
            var currentCellType = ""
            var currentCellValue = StringBuilder()
            var inValueTag = false
            var inInlineStringTag = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> {
                                currentRowCells = mutableListOf()
                            }
                            "c" -> {
                                currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                currentCellValue = StringBuilder()
                            }
                            "v" -> {
                                inValueTag = true
                            }
                            "t" -> {
                                inInlineStringTag = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inValueTag || inInlineStringTag) {
                            currentCellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "v" -> inValueTag = false
                            "t" -> inInlineStringTag = false
                            "c" -> {
                                val rawVal = currentCellValue.toString().trim()
                                val resolvedVal = if (currentCellType == "s") {
                                    val stringIndex = rawVal.toIntOrNull()
                                    if (stringIndex != null && stringIndex in sharedStrings.indices) {
                                        sharedStrings[stringIndex]
                                    } else {
                                        rawVal
                                    }
                                } else {
                                    rawVal
                                }

                                // Handle column positioning (e.g. A1, B1, C1)
                                val colIndex = getColumnIndexFromRef(currentCellRef)
                                while (currentRowCells.size < colIndex) {
                                    currentRowCells.add("") // Pad missing empty cells
                                }
                                currentRowCells.add(resolvedVal)
                            }
                            "row" -> {
                                if (currentRowCells.isNotEmpty()) {
                                    rows.add(currentRowCells)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing sheet XML: ${e.message}")
        }
        return rows
    }

    /**
     * Converts cell reference like "A1", "B2", "AA5" to 0-based column index.
     */
    private fun getColumnIndexFromRef(ref: String): Int {
        if (ref.isBlank()) return 0
        var colLetters = ""
        for (char in ref) {
            if (char.isLetter()) colLetters += char.uppercaseChar()
            else break
        }
        if (colLetters.isEmpty()) return 0

        var result = 0
        for (i in colLetters.indices) {
            result *= 26
            result += (colLetters[i] - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }

    /**
     * Parses Excel XML / SpreadsheetML (.xls XML format).
     */
    private fun parseXlsXmlOrFallback(fileName: String, bytes: ByteArray): ExcelImportParsedResult {
        return try {
            val textContent = decodeTextWithEncodings(bytes)
            if (textContent.contains("<Row") && textContent.contains("<Cell")) {
                parseSpreadsheetMlXml(fileName, textContent)
            } else {
                parseDelimitedText(fileName, bytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to delimited text for $fileName: ${e.message}")
            parseDelimitedText(fileName, bytes)
        }
    }

    /**
     * Parses SpreadsheetML XML (<Workbook><Worksheet><Table><Row><Cell><Data>...</Data>).
     */
    private fun parseSpreadsheetMlXml(fileName: String, xmlContent: String): ExcelImportParsedResult {
        val rawRows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlContent.reader())

        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var currentData = StringBuilder()
        var inData = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("Row", ignoreCase = true)) {
                        currentRow = mutableListOf()
                    } else if (parser.name.equals("Data", ignoreCase = true)) {
                        inData = true
                        currentData = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inData) {
                        currentData.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("Data", ignoreCase = true)) {
                        inData = false
                        currentRow.add(currentData.toString().trim())
                    } else if (parser.name.equals("Row", ignoreCase = true)) {
                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                            rawRows.add(currentRow)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (rawRows.isEmpty()) {
            return ExcelImportParsedResult(
                fileName = fileName,
                fileType = "Excel XML (.xls)",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "لم يتم العثور على بيانات صالحة في ملف Excel XML."
            )
        }

        val headerRowIdx = findHeaderRowIndex(rawRows)
        val rawHeaders = rawRows[headerRowIdx]
        val headers = rawHeaders.map { cleanDisplayHeader(it) }
        val dataRows = mutableListOf<Map<String, String>>()
        val csvBuilder = StringBuilder()

        csvBuilder.append(headers.joinToString(",") { escapeCsvValue(it) }).append("\n")

        for (i in (headerRowIdx + 1) until rawRows.size) {
            val rowValues = rawRows[i]
            if (rowValues.all { it.isBlank() }) continue

            val rowMap = mutableMapOf<String, String>()
            headers.forEachIndexed { idx, header ->
                val cellVal = rowValues.getOrNull(idx) ?: ""
                rowMap[header] = cellVal
                val rawH = rawHeaders.getOrNull(idx) ?: ""
                if (rawH.isNotBlank() && rawH != header) {
                    rowMap[rawH] = cellVal
                }
            }
            dataRows.add(rowMap)

            val rowCsv = headers.indices.map { idx ->
                escapeCsvValue(rowValues.getOrNull(idx) ?: "")
            }.joinToString(",")
            csvBuilder.append(rowCsv).append("\n")
        }

        return ExcelImportParsedResult(
            fileName = fileName,
            fileType = "Microsoft Excel XML (.xls)",
            headers = headers,
            rows = dataRows,
            csvFormattedText = csvBuilder.toString().trimEnd(),
            totalRows = dataRows.size,
            errorMessage = null
        )
    }

    /**
     * Parses standard CSV / TSV / Semicolon text files with auto delimiter and encoding detection.
     */
    private fun parseDelimitedText(fileName: String, bytes: ByteArray): ExcelImportParsedResult {
        val text = decodeTextWithEncodings(bytes)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExcelImportParsedResult(
                fileName = fileName,
                fileType = "CSV / نص",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "الملف المرفوع لا يحتوي على أسطر صالحة."
            )
        }

        // Detect delimiter (, or ; or \t)
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.count { it == '\t' } >= 3 -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }

        val parsedLines = lines.map { parseCsvLine(it, delimiter) }
        if (parsedLines.isEmpty()) {
            return ExcelImportParsedResult(
                fileName = fileName,
                fileType = "CSV",
                headers = emptyList(),
                rows = emptyList(),
                csvFormattedText = "",
                totalRows = 0,
                errorMessage = "تعذر قراءة أعمدة الملف."
            )
        }

        val headerRowIdx = findHeaderRowIndex(parsedLines)
        val rawHeaders = parsedLines[headerRowIdx]
        val headers = rawHeaders.map { cleanDisplayHeader(it) }
        val dataRows = mutableListOf<Map<String, String>>()
        val csvBuilder = StringBuilder()

        csvBuilder.append(headers.joinToString(",") { escapeCsvValue(it) }).append("\n")

        for (i in (headerRowIdx + 1) until parsedLines.size) {
            val values = parsedLines[i]
            if (values.all { it.isBlank() }) continue

            val rowMap = mutableMapOf<String, String>()
            headers.forEachIndexed { idx, header ->
                val cellVal = values.getOrNull(idx)?.trim() ?: ""
                rowMap[header] = cellVal
                val rawH = rawHeaders.getOrNull(idx)?.trim() ?: ""
                if (rawH.isNotBlank() && rawH != header) {
                    rowMap[rawH] = cellVal
                }
            }
            dataRows.add(rowMap)

            val rowCsv = headers.indices.map { idx ->
                escapeCsvValue(values.getOrNull(idx)?.trim() ?: "")
            }.joinToString(",")
            csvBuilder.append(rowCsv).append("\n")
        }

        return ExcelImportParsedResult(
            fileName = fileName,
            fileType = if (delimiter == '\t') "ملف نصي TSV" else "ملف CSV",
            headers = headers,
            rows = dataRows,
            csvFormattedText = csvBuilder.toString().trimEnd(),
            totalRows = dataRows.size,
            errorMessage = null
        )
    }

    /**
     * Finds the index of the row containing column headers.
     * Skips banners, empty rows, or merged titles.
     */
    fun findHeaderRowIndex(rawRows: List<List<String>>): Int {
        if (rawRows.isEmpty()) return 0
        val maxCheck = minOf(6, rawRows.size)
        var bestRow = 0
        var highestScore = -1

        for (i in 0 until maxCheck) {
            val row = rawRows[i]
            var score = 0
            val joined = row.joinToString(" ") { cleanHeaderKey(it) }
            
            if (joined.contains("اسم") || joined.contains("name")) score += 5
            if (joined.contains("هاتف") || joined.contains("موبايل") || joined.contains("phone") || joined.contains("تليفون")) score += 5
            if (joined.contains("تصنيف") || joined.contains("category") || joined.contains("قسم")) score += 4
            if (joined.contains("عنوان") || joined.contains("address") || joined.contains("منطق") || joined.contains("area")) score += 4
            if (joined.contains("تخصص") || joined.contains("specialty")) score += 3
            if (joined.contains("واتس") || joined.contains("whatsapp")) score += 2
            if (joined.contains("مواعيد") || joined.contains("ساعات") || joined.contains("hours")) score += 2
            if (joined.contains("وصف") || joined.contains("description")) score += 2

            val nonEmptyCount = row.count { it.isNotBlank() }
            if (nonEmptyCount >= 3) score += 2

            if (score > highestScore) {
                highestScore = score
                bestRow = i
            }
        }

        return if (highestScore >= 3) bestRow else 0
    }

    /**
     * Cleans headers for clean UI display.
     */
    fun cleanDisplayHeader(header: String): String {
        return header.trim()
            .replace("*", "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Normalizes a header key for resilient fuzzy matching.
     */
    fun cleanHeaderKey(key: String): String {
        return CategoryNormalizer.normalizeArabic(key)
            .replace("*", "")
            .replace("(", " ")
            .replace(")", " ")
            .replace("/", " ")
            .replace("-", " ")
            .replace("_", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Cleans and formats phone numbers.
     * Converts Eastern Arabic numerals (٠-٩), removes decimal `.0` from Excel floats,
     * and ensures proper phone format.
     */
    fun cleanPhoneNumber(raw: String): String {
        if (raw.isBlank()) return ""
        var clean = raw.trim()
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8")
            .replace("٩", "9")

        // Strip Excel floating point suffix like 1012345678.0
        if (clean.endsWith(".0")) {
            clean = clean.substringBefore(".0")
        }

        val digitsOnly = clean.filter { it.isDigit() || it == '+' }

        // Format Egyptian numbers
        if (digitsOnly.length == 10 && (digitsOnly.startsWith("10") || digitsOnly.startsWith("11") || digitsOnly.startsWith("12") || digitsOnly.startsWith("15"))) {
            return "0$digitsOnly"
        }
        if (digitsOnly.length == 9 && digitsOnly.startsWith("50")) {
            return "0$digitsOnly"
        }
        return digitsOnly
    }

    /**
     * Smartly extracts a fully normalized BusinessEntity from any raw map (CSV/Excel row).
     * Handles dozens of Arabic and English column synonyms, positional fallback,
     * auto-categorization via context hints, and cleans all fields.
     */
    fun extractBusinessEntityFromRow(row: Map<String, String>, index: Int): BusinessEntity {
        // Build normalized lookup map
        val normalizedEntries = mutableMapOf<String, String>()
        for ((k, v) in row) {
            val cleanedK = cleanHeaderKey(k)
            if (cleanedK.isNotBlank() && v.isNotBlank()) {
                normalizedEntries[cleanedK] = v.trim()
            }
        }

        // Helper to retrieve value matching multiple synonyms
        fun getValue(vararg candidateKeys: String): String {
            for (candidate in candidateKeys) {
                val cleanCand = cleanHeaderKey(candidate)
                normalizedEntries[cleanCand]?.let { if (it.isNotBlank()) return it }
                val found = normalizedEntries.entries.find { (k, v) ->
                    v.isNotBlank() && (k == cleanCand || k.contains(cleanCand) || cleanCand.contains(k))
                }
                if (found != null) return found.value
            }
            return ""
        }

        // 1. Extract Business Name
        var name = getValue(
            "اسم النشاط او المنشاة", "اسم النشاط", "الاسم", "اسم المحل", "اسم العيادة", "اسم الطبيب",
            "اسم المنشاة", "اسم المنشأة", "اسم المنشأه", "اسم الشركة", "اسم الشركه", "اسم الصيدلية",
            "اسم الصيدليه", "اسم المتجر", "اسم المعرض", "اسم المكان", "النشاط", "الاسم التجاري",
            "الاسم بالعربي", "اسم النشاط التجاري", "العنوان التجاري", "المنشاة", "المنشأة",
            "name", "business name", "business_name", "title", "company", "store", "shop", "clinic"
        )

        // Positional fallback for name if none of the header synonyms matched
        if (name.isBlank()) {
            for ((_, v) in row) {
                val trimmed = v.trim()
                if (trimmed.length >= 2 && !trimmed.startsWith("http") && !trimmed.matches("[0-9\\+\\-\\s\\.\\,]+".toRegex())) {
                    name = trimmed
                    break
                }
            }
        }
        if (name.isBlank()) {
            name = "نشاط تجاري $index"
        }

        // 2. Extract Category & Specialty
        val rawCategory = getValue(
            "التصنيف الرئيسي", "التصنيف", "التصنيفات", "القسم", "القسم الرئيسي", "المجال",
            "نوع النشاط", "نوع الخدمة", "الفئة", "القطاع", "category", "cat", "section", "type", "industry"
        )

        val rawSpecialty = getValue(
            "التخصص القسم الفرعي", "التخصص الفرعي", "التخصص", "القسم الفرعي", "التخصص الدقيق",
            "التخصص الطبي", "النوع", "الفرعي", "specialty", "subcategory", "sub_category", "specialization"
        )

        val rawDescription = getValue(
            "وصف النشاط والخدمات", "وصف النشاط", "الوصف", "نبذة", "عن المكان", "عن النشاط",
            "الخدمات", "الخدمات المقدمة", "تفاصيل", "ملاحظات", "description", "services", "about", "details", "notes"
        )

        // Resolve Category and Specialty with context intelligence
        val resolvedCat = CategoryNormalizer.resolveCategory(
            rawCategory = rawCategory,
            contextHint = "$name $rawSpecialty $rawDescription"
        )
        val resolvedSpec = CategoryNormalizer.resolveSpecialty(
            category = resolvedCat,
            rawSpecialty = rawSpecialty,
            nameHint = name
        )

        // 3. Extract Phone Numbers
        var rawPhone = getValue(
            "رقم الهاتف الاساسي", "رقم الهاتف", "الهاتف", "الموبايل", "رقم الموبايل", "التليفون",
            "رقم التليفون", "تليفون", "موبايل", "هاتف", "الجوال", "رقم الجوال", "الخط الساخن",
            "phone", "mobile", "tel", "telephone", "phone1", "primary_phone"
        )
        if (rawPhone.isBlank()) {
            // Search row values for Egyptian phone pattern
            for ((_, v) in row) {
                val cleaned = cleanPhoneNumber(v)
                if (cleaned.length in 7..14 && (cleaned.startsWith("01") || cleaned.startsWith("050") || cleaned.startsWith("20") || cleaned.startsWith("02"))) {
                    rawPhone = cleaned
                    break
                }
            }
        }
        val phone = cleanPhoneNumber(rawPhone).ifBlank { "050" + (1000000 + (index * 13) % 8999999) }

        val rawPhoneSecondary = getValue(
            "رقم هاتف اضافي", "رقم هاتف إضافي", "هاتف 2", "موبايل 2", "تليفون 2", "رقم اخر",
            "هاتف اخر", "خط اضافي", "تليفون ارضي", "phone2", "secondary_phone", "mobile2"
        )
        val phoneSecondary = cleanPhoneNumber(rawPhoneSecondary).takeIf { it.isNotBlank() }

        val rawWhatsapp = getValue(
            "رقم الواتساب", "واتساب", "واتس", "الواتساب", "رقم الواتس", "whatsapp", "whats", "wa"
        )
        val whatsapp = cleanPhoneNumber(rawWhatsapp).takeIf { it.isNotBlank() }

        // 4. Extract Location Details
        var address = getValue(
            "العنوان بالتفصيل", "العنوان", "عنوان النشاط", "المكان", "مكان النشاط", "الشارع",
            "تفاصيل العنوان", "موقع النشاط", "address", "location", "street"
        )
        if (address.isBlank()) address = "مدينة ميت غمر"

        var area = getValue(
            "المنطقة او القرية", "المنطقة", "القرية", "الحي", "المجاورة", "المنطقة القرية",
            "الشارع المنطقة", "area", "district", "village", "neighborhood"
        )
        if (area.isBlank()) {
            val matchedArea = MetGhamrGeoHierarchy.villagesAndUnits.find {
                address.contains(it.nameAr) || name.contains(it.nameAr)
            }
            area = matchedArea?.nameAr ?: "وسط البلد"
        }

        var city = getValue(
            "المدينة المركز", "المدينة", "المركز", "المحافظة", "city", "town", "center"
        )
        if (city.isBlank()) city = "مدينة ميت غمر"

        // 5. Extract Working Hours & Days
        val workingDays = getValue(
            "ايام العمل", "أيام العمل", "الايام", "الأيام", "أيام التشغيل", "working_days", "days"
        )
        val workingHoursInput = getValue(
            "مواعيد العمل من الى", "مواعيد العمل", "ساعات العمل", "اوقات العمل", "أوقات العمل",
            "working_hours", "hours", "time", "schedule"
        )
        val workingHours = when {
            workingDays.isNotBlank() && workingHoursInput.isNotBlank() -> "$workingDays ($workingHoursInput)"
            workingHoursInput.isNotBlank() -> workingHoursInput
            workingDays.isNotBlank() -> "$workingDays (09:00 ص - 10:00 م)"
            else -> "يومياً من 09:00 ص إلى 10:00 م"
        }

        // 6. Extract Online Links
        val facebookUrl = getValue(
            "صفحة الفيسبوك", "رابط الفيسبوك", "فيسبوك", "الفيسبوك", "فيس بوك", "فيس", "facebook", "fb", "facebook_url"
        ).takeIf { it.isNotBlank() }

        val websiteUrl = getValue(
            "الموقع الالكتروني", "الموقع الإلكتروني", "الموقع الرسمي", "الموقع", "رابط الموقع", "website", "site", "web", "url"
        ).takeIf { it.isNotBlank() }

        val description = if (rawDescription.isNotBlank()) {
            rawDescription
        } else {
            "$name - $resolvedSpec بـ $area، $city."
        }

        return BusinessEntity(
            id = "imp_biz_" + UUID.randomUUID().toString().take(8),
            name = name,
            categoryId = resolvedCat.id,
            categoryName = resolvedCat.nameAr,
            specialty = resolvedSpec,
            phone = phone,
            phoneSecondary = phoneSecondary,
            whatsapp = whatsapp,
            city = city,
            address = address,
            area = area,
            facebookUrl = facebookUrl,
            websiteUrl = websiteUrl,
            workingHours = workingHours,
            description = description,
            isVerified = true,
            isActive = true,
            dataQualityScore = 92,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Splits a multi-line CSV string into a 2D list of values respecting quotes and delimiters.
     */
    fun parseCsvText(csvText: String): List<List<String>> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first()
        val delimiter = when {
            firstLine.count { it == '\t' } >= 3 -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }

        return lines.map { parseCsvLine(it, delimiter) }
    }

    /**
     * Splits a CSV line respecting quotes.
     */
    fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * Escapes a single string for CSV output.
     */
    private fun escapeCsvValue(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim()
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    /**
     * Decodes byte array with auto-fallback to UTF-8, Windows-1256 (Arabic Windows), and ISO-8859-6.
     */
    private fun decodeTextWithEncodings(bytes: ByteArray): String {
        // Check for BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }

        // Try standard UTF-8
        try {
            val utf8 = String(bytes, Charsets.UTF_8)
            if (!utf8.contains("")) {
                return utf8
            }
        } catch (_: Exception) {}

        // Try Windows-1256 for Arabic Excel exported on Windows
        try {
            val win1256 = Charset.forName("windows-1256")
            return String(bytes, win1256)
        } catch (_: Exception) {}

        return String(bytes, Charsets.UTF_8)
    }
}
