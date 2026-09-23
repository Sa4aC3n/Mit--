package com.example.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Authoritative Firebase Cloud Messaging (FCM) Service for Mit Ghamr Directory.
 * Handles token generation, device-to-user binding in Cloud Firestore, notification channels,
 * deep linking to specific businesses, and bulk-import notification protection.
 */
class MetGhamrFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MetGhamrFCM"
        const val CHANNEL_ID_GENERAL = "mit_ghamr_directory_general"
        const val CHANNEL_ID_NEW_BUSINESS = "mit_ghamr_new_business"
        const val EXTRA_BUSINESS_ID = "extra_business_id"
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_NEW_BUSINESSES = "new_businesses"

        /**
         * Subscribe client device to common public notification topics.
         */
        fun subscribeToDefaultTopics() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL_USERS)
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NEW_BUSINESSES)
            } catch (e: Exception) {
                Log.w(TAG, "Topic subscription warning: ${e.message}")
            }
        }

        /**
         * Authoritative helper to persist device token to Cloud Firestore.
         */
        fun syncDeviceToken(context: Context, token: String) {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_generic"
            } catch (e: Exception) {
                "device_${System.currentTimeMillis()}"
            }

            val devicePayload = hashMapOf(
                "token" to token,
                "deviceId" to deviceId,
                "platform" to "android",
                "appVersion" to BuildConfig.VERSION_NAME,
                "versionCode" to BuildConfig.VERSION_CODE,
                "updatedAt" to System.currentTimeMillis(),
                "enabled" to true
            )

            // If user is authenticated, link device under users/{uid}/devices/{deviceId}
            val currentUser = auth.currentUser
            if (currentUser != null) {
                devicePayload["uid"] = currentUser.uid
                devicePayload["userEmail"] = currentUser.email ?: ""
                db.collection("users")
                    .document(currentUser.uid)
                    .collection("devices")
                    .document(deviceId)
                    .set(devicePayload, SetOptions.merge())
                    .addOnFailureListener { Log.w(TAG, "Failed to link device token to user doc: ${it.message}") }
            }

            // Always store in global root devices registry for broadcast & anonymous notifications
            db.collection("devices")
                .document(deviceId)
                .set(devicePayload, SetOptions.merge())
                .addOnFailureListener { Log.w(TAG, "Failed to sync device token to Firestore: ${it.message}") }
        }

        /**
         * Disassociates device token on user logout.
         */
        fun onUserLogout(context: Context) {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return
            val deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: return
            } catch (e: Exception) {
                return
            }

            db.collection("users")
                .document(currentUser.uid)
                .collection("devices")
                .document(deviceId)
                .update("enabled", false)
                .addOnFailureListener { Log.w(TAG, "Failed to disable user device token: ${it.message}") }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM registration token refreshed.")
        syncDeviceToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "دليل ميت غمر"
        val body = notification?.body ?: data["body"] ?: "نشاط جديد متاح الآن"
        val businessId = data["businessId"]
        val notificationType = data["type"] ?: "GENERAL"

        createNotificationChannels()
        showNotification(title, body, businessId, notificationType)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val newBusinessChannel = NotificationChannel(
                CHANNEL_ID_NEW_BUSINESS,
                "أنشطة وخدمات جديدة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات فورية عند إضافة واعتماد أنشطة جديدة في دليل ميت غمر"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "إعلانات وتنبيهات عامة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تنبيهات وتحديثات النظام العامة"
            }

            notificationManager.createNotificationChannel(newBusinessChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun showNotification(title: String, message: String, businessId: String?, type: String) {
        val channelId = if (type == "NEW_BUSINESS") CHANNEL_ID_NEW_BUSINESS else CHANNEL_ID_GENERAL

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            businessId?.let { putExtra(EXTRA_BUSINESS_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            businessId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // If type is bulk summary, use fixed notification ID 100 to prevent notification flooding
        val notificationId = if (type == "BULK_SUMMARY") 100 else (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
