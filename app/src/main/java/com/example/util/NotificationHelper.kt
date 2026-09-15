package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BusinessEntity
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centralized Notification Manager for Met Ghamr Directory
 * Handles:
 * 1. Android Notification Channels (Activities, Offers, Updates)
 * 2. Instant system notifications for newly added businesses
 * 3. Promotional offers & discounts alerts
 * 4. Permission & SharedPreferences preference checks
 */
object NotificationHelper {
    const val PREFS_NAME = "met_ghamr_user_settings"
    const val KEY_NOTIFS_MASTER = "notifications_master_enabled"
    const val KEY_NOTIFS_NEW_SERVICES = "notify_new_services"
    const val KEY_NOTIFS_OFFERS = "notify_offers"
    const val KEY_NOTIFS_UPDATES = "notify_updates"

    const val CHANNEL_NEW_ACTIVITIES = "met_ghamr_new_activities"
    const val CHANNEL_OFFERS = "met_ghamr_offers"
    const val CHANNEL_UPDATES = "met_ghamr_updates"

    private val notifIdGenerator = AtomicInteger(2000)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelNew = NotificationChannel(
                CHANNEL_NEW_ACTIVITIES,
                "الأنشطة والخدمات الجديدة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند إضافة محل أو عيادة أو خدمة جديدة في ميت غمر"
                enableVibration(true)
                enableLights(true)
            }

            val channelOffers = NotificationChannel(
                CHANNEL_OFFERS,
                "العروض والتخفيضات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "عروض المتاجر والخصومات الترويجية الحصرية في ميت غمر"
                enableVibration(true)
                enableLights(true)
            }

            val channelUpdates = NotificationChannel(
                CHANNEL_UPDATES,
                "تحديثات وإعلانات الدليل",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات أوقات العمل والطوارئ وتحديثات الدليل العامة"
            }

            notificationManager.createNotificationChannels(listOf(channelNew, channelOffers, channelUpdates))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isAllowed(context: Context, specificKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val master = prefs.getBoolean(KEY_NOTIFS_MASTER, true)
        if (!master) return false
        return prefs.getBoolean(specificKey, true)
    }

    /**
     * Dispatches an Android system notification when a new business/activity is added
     */
    fun showNewBusinessNotification(
        context: Context,
        businessName: String,
        categoryName: String,
        area: String,
        businessId: String? = null
    ) {
        if (!isAllowed(context, KEY_NOTIFS_NEW_SERVICES)) return
        createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!businessId.isNullOrBlank()) {
                putExtra("businessId", businessId)
                putExtra("route", "detail")
            } else {
                putExtra("route", "home")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🏪 نشاط جديد في ميت غمر: $businessName"
        val subtitle = "$categoryName • ${area.ifBlank { "ميت غمر" }}"
        val bigText = "تمت إضافة \"$businessName\" ($categoryName) في منطقة $area إلى دليل ميت غمر الرسمي. انقر لاستعراض التفاصيل والعنوان والاتصال المباشر."

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_ACTIVITIES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notifIdGenerator.incrementAndGet(), notification)
    }

    fun showNewBusinessNotification(context: Context, business: BusinessEntity) {
        showNewBusinessNotification(
            context = context,
            businessName = business.name,
            categoryName = business.categoryName,
            area = business.area.ifBlank { business.address },
            businessId = business.id
        )
    }

    /**
     * Dispatches an Android system notification for promotional offers & discounts
     */
    fun showOfferNotification(
        context: Context,
        title: String,
        body: String,
        businessName: String? = null,
        businessId: String? = null
    ) {
        if (!isAllowed(context, KEY_NOTIFS_OFFERS)) return
        createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!businessId.isNullOrBlank()) {
                putExtra("businessId", businessId)
                putExtra("route", "detail")
            } else {
                putExtra("route", "notifications")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifTitle = "🏷️ عرض ترويجي حصري: $title"
        val notifText = if (!businessName.isNullOrBlank()) "$businessName: $body" else body
        val bigText = if (!businessName.isNullOrBlank()) {
            "خصم وعرض خاص من [$businessName]: $body.\nتصفح العرض الآن واستفد من الخصومات داخل ميت غمر!"
        } else {
            "$body.\nلا تفوّت عروض وتخفيضات محلات ومتاجر ميت غمر الحصرية!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_OFFERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notifTitle)
            .setContentText(notifText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notifIdGenerator.incrementAndGet(), notification)
    }

    /**
     * Dispatches an Android system notification for directory general updates
     */
    fun showUpdateNotification(
        context: Context,
        title: String,
        body: String
    ) {
        if (!isAllowed(context, KEY_NOTIFS_UPDATES)) return
        createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("route", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📢 $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notifIdGenerator.incrementAndGet(), notification)
    }

    /**
     * Sends both realistic test notifications to verify system works end-to-end
     */
    fun sendTestNotifications(context: Context) {
        createNotificationChannels(context)
        // 1. New activity test notification
        showNewBusinessNotification(
            context = context,
            businessName = "مستشفى الأمل التخصصي",
            categoryName = "مستشفيات ومراكز طبية",
            area = "شارع الحرية - وسط البلد",
            businessId = null
        )
        // 2. Promotional offer test notification
        showOfferNotification(
            context = context,
            title = "خصم 25% على كافة المشتريات بمناسبة الافتتاح",
            body = "تخفيضات كبرى لعملاء دليل ميت غمر لفترة محدودة",
            businessName = "سلسلة محلات الأناقة",
            businessId = null
        )
    }
}
