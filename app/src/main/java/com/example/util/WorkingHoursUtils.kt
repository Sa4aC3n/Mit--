package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.BusinessEntity
import com.example.ui.theme.LettuceGreen
import com.example.ui.theme.LettuceGreenBg
import com.example.ui.theme.LettuceGreenDark
import com.example.ui.theme.WatermelonRedBg
import com.example.ui.theme.WatermelonRedDark
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Representation of a business's current operational status.
 */
data class BusinessOpenStatus(
    val isOpen: Boolean,
    val label: String,             // "مفتوح الآن" or "مغلق الآن"
    val containerColor: Color,      // Light tint background
    val contentColor: Color,        // Text color
    val dotColor: Color             // Dot indicator color
)

/**
 * Utility to parse localized Arabic working hours strings and dynamically determine
 * whether a business is currently OPEN ("مفتوح الآن") or CLOSED ("مغلق الآن")
 * according to the device's clock (day of week, hour, and minute).
 */
object WorkingHoursUtils {

    private val DAYS_ORDER = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

    // Calendar day mapping: SATURDAY=7, SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5, FRIDAY=6
    private val DAY_MAP = mapOf(
        "السبت" to Calendar.SATURDAY,
        "سبت" to Calendar.SATURDAY,
        "الأحد" to Calendar.SUNDAY,
        "احد" to Calendar.SUNDAY,
        "الاحد" to Calendar.SUNDAY,
        "الاثنين" to Calendar.MONDAY,
        "الإثنين" to Calendar.MONDAY,
        "اثنين" to Calendar.MONDAY,
        "الثلاثاء" to Calendar.TUESDAY,
        "ثلاثاء" to Calendar.TUESDAY,
        "الأربعاء" to Calendar.WEDNESDAY,
        "اربعاء" to Calendar.WEDNESDAY,
        "الاربعاء" to Calendar.WEDNESDAY,
        "الخميس" to Calendar.THURSDAY,
        "خميس" to Calendar.THURSDAY,
        "الجمعة" to Calendar.FRIDAY,
        "جمعة" to Calendar.FRIDAY
    )

    /**
     * Converts Arabic Eastern numerals (٠-٩) into Western digits (0-9).
     */
    fun normalizeDigits(input: String): String {
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        val western = "0123456789"
        var res = input
        for (i in arabic.indices) {
            res = res.replace(arabic[i], western[i])
        }
        return res
    }

    /**
     * Parses a single time string like "08:00 ص", "02:30 م", "12:00 م", "10:30" into
     * minutes elapsed from midnight (0..1439).
     */
    fun parseTimeToMinutes(timeStr: String): Int? {
        val clean = normalizeDigits(timeStr.trim())
        val pattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(ص|صباحاً|صباحا|م|مساءً|مساءا|am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(clean)
        if (!matcher.find()) return null

        var hour = matcher.group(1)?.toIntOrNull() ?: return null
        val minute = matcher.group(2)?.toIntOrNull() ?: 0
        val ampm = matcher.group(3)?.lowercase() ?: ""

        val isPm = ampm.contains("م") || ampm.contains("مساء") || ampm.contains("pm")
        val isAm = ampm.contains("ص") || ampm.contains("صباح") || ampm.contains("am")

        if (isPm) {
            if (hour < 12) hour += 12
        } else if (isAm) {
            if (hour == 12) hour = 0
        } else {
            // No AM/PM explicitly provided
            if (hour in 1..7) {
                // Heuristic: Afternoon/evening hours like 2 to 7 usually mean PM in business hours
                hour += 12
            }
        }

        return hour * 60 + minute
    }

    /**
     * Extracts all operating intervals [startMinutes, endMinutes] from a text snippet.
     */
    fun extractIntervals(text: String): List<Pair<Int, Int>> {
        val pattern = Pattern.compile(
            "(\\d{1,2}(?::\\d{2})?\\s*(?:ص|صباحاً|صباحا|م|مساءً|مساءا|am|pm)?)\\s*(?:-|إلى|الي|حتى|to)\\s*(\\d{1,2}(?::\\d{2})?\\s*(?:ص|صباحاً|صباحا|م|مساءً|مساءا|am|pm)?)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        val intervals = mutableListOf<Pair<Int, Int>>()

        while (matcher.find()) {
            val startStr = matcher.group(1) ?: continue
            val endStr = matcher.group(2) ?: continue
            val s = parseTimeToMinutes(startStr) ?: continue
            var e = parseTimeToMinutes(endStr) ?: continue

            // If end time is midnight (12:00 ص) following an evening/afternoon start, treat it as 1440 mins
            if (e == 0 && s > 720) {
                e = 1440
            }
            intervals.add(Pair(s, e))
        }
        return intervals
    }

    /**
     * Parses the set of open days of the week (Calendar day constants) from text.
     */
    fun parseOpenDays(text: String): Set<Int> {
        val allDays = (1..7).toSet()
        if (text.contains("طوال الأسبوع") || text.contains("طوال أيام الأسبوع") ||
            text.contains("طوال الاسبوع") || text.contains("يومياً") ||
            text.contains("يوميا") || text.contains("كل يوم") || text.contains("24 ساعة")
        ) {
            return allDays
        }

        // Extract day range, usually in parentheses: (السبت - الخميس) or (الأحد - الخميس)
        val p = Pattern.compile("\\(([^)]+)\\)")
        val m = p.matcher(text)
        val content = if (m.find()) m.group(1) ?: text else text

        fun normalizeDayName(d: String): String {
            val clean = d.trim()
            for (key in DAYS_ORDER) {
                val bareKey = key.replace("أ", "").replace("إ", "").replace("ا", "")
                val bareClean = clean.replace("أ", "").replace("إ", "").replace("ا", "")
                if (bareClean.contains(bareKey) || bareKey.contains(bareClean)) {
                    return key
                }
            }
            return clean
        }

        val rangePattern = Pattern.compile("([^\\s()\\-]+)\\s*(?:-|إلى|الي|حتى)\\s*([^\\s()\\-]+)")
        val rm = rangePattern.matcher(content)
        if (rm.find()) {
            val d1 = normalizeDayName(rm.group(1) ?: "")
            val d2 = normalizeDayName(rm.group(2) ?: "")

            val idx1 = DAYS_ORDER.indexOf(d1)
            val idx2 = DAYS_ORDER.indexOf(d2)

            if (idx1 != -1 && idx2 != -1) {
                val days = mutableSetOf<Int>()
                var curr = idx1
                while (true) {
                    val dayName = DAYS_ORDER[curr]
                    DAY_MAP[dayName]?.let { days.add(it) }
                    if (curr == idx2) break
                    curr = (curr + 1) % 7
                }
                return days
            }
        }

        return allDays
    }

    /**
     * Evaluates whether the business is currently open based on its working hours
     * string and the current device time/day.
     */
    fun isBusinessOpenNow(workingHours: String?, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (workingHours.isNullOrBlank()) {
            // Default to open if no hours specified
            return true
        }

        val normalized = normalizeDigits(workingHours.trim())

        // 24-hour / always open keywords
        if (normalized.contains("24 ساعة") || normalized.contains("طوال اليوم") ||
            normalized.contains("مفتوح دائماً") || normalized.contains("دائماً") ||
            normalized.contains("24/7") || normalized.contains("24/24")
        ) {
            return true
        }

        // Explicit closure indicators
        if (normalized.contains("مغلق مؤقتاً") || normalized.contains("مغلق نهائياً") ||
            normalized.contains("تحت الصيانة")
        ) {
            return false
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        var mainText = normalized

        // Check if there is a Friday-specific clause (e.g. "... / الجمعة: 01:30 م - 10:30 م")
        if (normalized.contains("الجمعة:") || normalized.contains("/ الجمعة") || normalized.contains("الجمعة :")) {
            val parts = normalized.split(Pattern.compile("/?\\s*الجمعة\\s*:?"), limit = 2)
            if (parts.size >= 2) {
                mainText = parts[0]
                val fridayText = parts[1]

                if (dayOfWeek == Calendar.FRIDAY) {
                    val fridayIntervals = extractIntervals(fridayText)
                    if (fridayIntervals.isEmpty()) {
                        return false // Closed on Friday if no hours specified
                    }
                    for ((start, end) in fridayIntervals) {
                        if (start < end) {
                            if (currentMinutes in start until end) return true
                        } else {
                            if (currentMinutes >= start || currentMinutes < end) return true
                        }
                    }
                    return false
                }
            }
        } else if (normalized.contains("مغلق الجمعة") && dayOfWeek == Calendar.FRIDAY) {
            return false
        }

        val openDays = parseOpenDays(mainText)
        val intervals = extractIntervals(mainText)

        // If no intervals could be extracted, treat as open
        if (intervals.isEmpty()) {
            return true
        }

        val yesterdayDow = if (dayOfWeek == Calendar.SUNDAY) Calendar.SATURDAY else dayOfWeek - 1

        for ((start, end) in intervals) {
            if (start < end) {
                // Same-day shift (e.g. 09:00 ص - 10:00 م)
                if (dayOfWeek in openDays && currentMinutes >= start && currentMinutes < end) {
                    return true
                }
            } else {
                // Overnight shift (e.g. 08:00 ص - 02:00 ص or 06:00 م - 02:00 ص)
                // 1) Evening part on the opening day
                if (currentMinutes >= start && dayOfWeek in openDays) {
                    return true
                }
                // 2) Early morning part extending from yesterday's opening day
                if (currentMinutes < end && yesterdayDow in openDays) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Returns full visual and text status for a business to be bound directly to Compose UI chips.
     */
    fun getStatusInfo(workingHours: String?, calendar: Calendar = Calendar.getInstance()): BusinessOpenStatus {
        val isOpen = isBusinessOpenNow(workingHours, calendar)
        return BusinessOpenStatus(
            isOpen = isOpen,
            label = if (isOpen) "مفتوح الآن" else "مغلق الآن",
            containerColor = if (isOpen) LettuceGreenBg else WatermelonRedBg,
            contentColor = if (isOpen) LettuceGreenDark else WatermelonRedDark,
            dotColor = if (isOpen) LettuceGreen else WatermelonRedDark
        )
    }
}

/**
 * Extension property to easily check if a business is currently open according to phone clock.
 */
val BusinessEntity.isCurrentlyOpen: Boolean
    get() = WorkingHoursUtils.isBusinessOpenNow(this.workingHours)
