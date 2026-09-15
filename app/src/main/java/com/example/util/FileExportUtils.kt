package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileExportUtils {

    /**
     * Exports and downloads a CSV or text file to the device's Downloads folder,
     * with UTF-8 BOM so Excel opens Arabic text flawlessly, and triggers a system
     * share / open dialog so the user can open with Excel, Sheets, or save anywhere.
     */
    fun downloadAndShareFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String = "text/csv",
        title: String = "تنزيل ومشاركة الملف"
    ) {
        try {
            // Add UTF-8 Byte Order Mark (BOM) so Excel renders Arabic characters correctly
            val contentWithBom = if (mimeType == "text/csv" && !content.startsWith("\uFEFF")) {
                "\uFEFF$content"
            } else {
                content
            }
            val bytes = contentWithBom.toByteArray(Charsets.UTF_8)

            var savedToDownloads = false
            var fileUri: Uri? = null

            // 1. Save directly into device Public Downloads folder via MediaStore (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MetGhamrDirectory")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(bytes)
                            outputStream.flush()
                        }
                        fileUri = uri
                        savedToDownloads = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Android 9 and lower
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val targetFile = File(downloadsDir, fileName)
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )
                    savedToDownloads = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Always prepare a shared FileProvider file in cache for instant Open/Share with Excel
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // 3. Inform user
            val message = if (savedToDownloads) {
                "تم حفظ $fileName في مجلد التنزيلات (Downloads)"
            } else {
                "تم تجهيز الملف: $fileName"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // 4. Launch Intent chooser to open or share the file in Excel / Sheets / Files
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, "قالب بيانات دليل ميت غمر: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء تنزيل الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats a list of BusinessEntity into the exact 15-column CSV table requested:
     * name, category, specialty, phone, phone2, whatsapp, address, area, city,
     * working_days, opening_time, closing_time, description, status, Google maps location
     */
    fun generateMetGhamrDirectoryCsv(businesses: List<com.example.data.model.BusinessEntity>): String {
        val sb = java.lang.StringBuilder()
        sb.append("name,category,specialty,phone,phone2,whatsapp,address,area,city,working_days,opening_time,closing_time,description,status,Google maps location\r\n")

        for (b in businesses) {
            val name = escapeCsv(b.name)
            val category = escapeCsv(b.categoryName)
            val specialty = escapeCsv(b.specialty)
            val phone = escapeCsv(b.phone)
            val phone2 = escapeCsv(b.phoneSecondary ?: "")
            val whatsapp = escapeCsv(b.whatsapp ?: "")
            val address = escapeCsv(b.address)
            val area = escapeCsv(b.area)
            val city = escapeCsv(b.city)

            // Extract working days, opening time, closing time from workingHours
            val (days, openTime, closeTime) = parseWorkingHours(b.workingHours)
            val workingDays = escapeCsv(days)
            val openingTime = escapeCsv(openTime)
            val closingTime = escapeCsv(closeTime)

            val description = escapeCsv(b.description)
            val status = escapeCsv(if (b.isOpenNow) "مفتوح / متاح" else "مغلق حالياً")
            val mapsLocation = escapeCsv("https://www.google.com/maps?q=${b.latitude},${b.longitude}")

            sb.append("$name,$category,$specialty,$phone,$phone2,$whatsapp,$address,$area,$city,$workingDays,$openingTime,$closingTime,$description,$status,$mapsLocation\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim()
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    private fun parseWorkingHours(workingHours: String): Triple<String, String, String> {
        val trimmed = workingHours.trim()
        return try {
            if (trimmed.contains("24 ساعة")) {
                Triple("طوال الأسبوع (يومياً)", "00:00 ص (24 ساعة)", "11:59 م (24 ساعة)")
            } else if (trimmed.contains(":") || trimmed.contains("-")) {
                val daysPart = if (trimmed.contains("(") && trimmed.contains(")")) {
                    trimmed.substringAfter("(").substringBefore(")").trim()
                } else if (trimmed.contains("يومياً")) {
                    "يومياً طوال الأسبوع"
                } else {
                    "السبت - الخميس"
                }

                val timePart = trimmed.substringBefore("(").replace("يومياً:", "").trim()
                if (timePart.contains("-")) {
                    val splits = timePart.split("-")
                    val open = splits.getOrNull(0)?.trim() ?: "09:00 ص"
                    val close = splits.getOrNull(1)?.trim() ?: "10:00 م"
                    Triple(daysPart, open, close)
                } else {
                    Triple(daysPart, timePart, "")
                }
            } else {
                Triple("يومياً", trimmed, "")
            }
        } catch (_: Exception) {
            Triple("يومياً", trimmed, "")
        }
    }
}

