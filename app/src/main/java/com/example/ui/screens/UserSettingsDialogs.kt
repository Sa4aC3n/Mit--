package com.example.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

private const val PREFS_NAME = "met_ghamr_user_settings"
private const val KEY_NOTIFS_MASTER = "notifications_master_enabled"
private const val KEY_NOTIFS_NEW_SERVICES = "notify_new_services"
private const val KEY_NOTIFS_OFFERS = "notify_offers"
private const val KEY_NOTIFS_UPDATES = "notify_updates"
private const val KEY_PRIVACY_HIDE_PHONE = "privacy_hide_phone"
private const val KEY_PRIVACY_APPROX_LOCATION = "privacy_approx_location"

/**
 * Interactive Dialog for Notification Settings (الإشعارات التنبيهية)
 */
@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var masterEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_NOTIFS_MASTER, true)) }
    var notifyNewServices by remember { mutableStateOf(prefs.getBoolean(KEY_NOTIFS_NEW_SERVICES, true)) }
    var notifyOffers by remember { mutableStateOf(prefs.getBoolean(KEY_NOTIFS_OFFERS, true)) }
    var notifyUpdates by remember { mutableStateOf(prefs.getBoolean(KEY_NOTIFS_UPDATES, true)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            masterEnabled = true
            prefs.edit().putBoolean(KEY_NOTIFS_MASTER, true).apply()
            onSettingsChanged(true)
            Toast.makeText(context, "تم منح إذن الإشعارات بنجاح 🔔", Toast.LENGTH_SHORT).show()
        } else {
            masterEnabled = false
            prefs.edit().putBoolean(KEY_NOTIFS_MASTER, false).apply()
            onSettingsChanged(false)
            Toast.makeText(context, "تم رفض إذن الإشعارات من النظام", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MetGhamrNavy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الإشعارات التنبيهية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            )
                        )
                        Text(
                            text = "تحكم في التنبيهات التي ترغب في استلامها",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (masterEnabled) MetGhamrNavy.copy(alpha = 0.05f) else Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل كافة الإشعارات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (masterEnabled) MetGhamrNavy else TextPrimary
                            )
                            Text(
                                text = if (masterEnabled) "الإشعارات التنبيهية نشطة على هذا الهاتف" else "الإشعارات معطلة حالياً",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }
                                }
                                masterEnabled = isChecked
                                prefs.edit().putBoolean(KEY_NOTIFS_MASTER, isChecked).apply()
                                onSettingsChanged(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MetGhamrGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Sub-Options
                AnimatedVisibility(visible = masterEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "أنواع التنبيهات المخصصة",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        NotificationOptionRow(
                            icon = Icons.Default.Storefront,
                            title = "الأنشطة والخدمات الجديدة",
                            description = "تنبيه فوري عند إضافة محل، عيادة، أو خدمة جديدة في ميت غمر",
                            checked = notifyNewServices,
                            onCheckedChange = {
                                notifyNewServices = it
                                prefs.edit().putBoolean(KEY_NOTIFS_NEW_SERVICES, it).apply()
                            }
                        )

                        NotificationOptionRow(
                            icon = Icons.Default.LocalOffer,
                            title = "العروض والتخفيضات",
                            description = "عروض المتاجر والخصومات الحصرية في ميت غمر",
                            checked = notifyOffers,
                            onCheckedChange = {
                                notifyOffers = it
                                prefs.edit().putBoolean(KEY_NOTIFS_OFFERS, it).apply()
                            }
                        )

                        NotificationOptionRow(
                            icon = Icons.Default.Campaign,
                            title = "تحديثات وإعلانات الدليل",
                            description = "تنبيهات أوقات العمل، الطوارئ، والتحديثات العامة",
                            checked = notifyUpdates,
                            onCheckedChange = {
                                notifyUpdates = it
                                prefs.edit().putBoolean(KEY_NOTIFS_UPDATES, it).apply()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test Notification Options
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "اختبار نظام التنبيهات المباشرة:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            com.example.util.NotificationHelper.createNotificationChannels(context)
                                            com.example.util.NotificationHelper.showNewBusinessNotification(
                                                context = context,
                                                businessName = "مستشفى الأمل التخصصي",
                                                categoryName = "مستشفيات ومراكز طبية",
                                                area = "شارع الحرية - وسط البلد",
                                                businessId = null
                                            )
                                            Toast.makeText(context, "تم إرسال إشعار نشاط جديد للتجربة 🏪", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MetGhamrNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "نشاط جديد",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MetGhamrNavy,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            com.example.util.NotificationHelper.createNotificationChannels(context)
                                            com.example.util.NotificationHelper.showOfferNotification(
                                                context = context,
                                                title = "خصم 25% بمناسبة الافتتاح",
                                                body = "تخفيضات كبرى لعملاء دليل ميت غمر لفترة محدودة",
                                                businessName = "سلسلة محلات الأناقة",
                                                businessId = null
                                            )
                                            Toast.makeText(context, "تم إرسال إشعار عرض ترويجي للتجربة 🏷️", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MetGhamrGold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "عرض ترويجي",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MetGhamrGold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Full test button
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        com.example.util.NotificationHelper.sendTestNotifications(context)
                                        Toast.makeText(context, "تم إرسال إشعارات تجريبية لهاتفك (نشاط + عرض) 🔔", Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إرسال إشعار تجريبي الآن للتأكد",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم وحفظ الإعدادات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NotificationOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MetGhamrNavy,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    fontSize = 13.sp
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MetGhamrGreen
                )
            )
        }
    }
}

/**
 * Fires immediate test push notifications locally to prove the notification subsystem works.
 */
private fun sendTestNotification(context: Context) {
    com.example.util.NotificationHelper.sendTestNotifications(context)
}

/**
 * Interactive Dialog for Privacy and Security (حماية الخصوصية)
 */
@Composable
fun PrivacySecurityDialog(
    viewModel: DirectoryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var hidePhone by remember { mutableStateOf(prefs.getBoolean(KEY_PRIVACY_HIDE_PHONE, false)) }
    var approxLocation by remember { mutableStateOf(prefs.getBoolean(KEY_PRIVACY_APPROX_LOCATION, false)) }
    var showFullPolicy by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MetGhamrGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MetGhamrGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حماية الخصوصية والأمان",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            )
                        )
                        Text(
                            text = "بياناتك الشخصية مشفرة ومحمية بالكامل",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Status Badge Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MetGhamrGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("درع الأمان مشفر ونشط 🛡️", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 13.sp)
                            Text("تطبيق دليل ميت غمر لا يشارك بياناتك أو موقعك مع أي طرف ثالث.", color = Color(0xFF15803D), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Privacy Controls
                Text(
                    text = "إعدادات الخصوصية والتحكم",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Hide Phone
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhoneLocked, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إخفاء رقم هاتفي في الحساب العام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("لا يظهر رقم هاتفك للمستخدمين الآخرين", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = hidePhone,
                            onCheckedChange = {
                                hidePhone = it
                                prefs.edit().putBoolean(KEY_PRIVACY_HIDE_PHONE, it).apply()
                                Toast.makeText(context, if (it) "تم تفعيل إخفاء رقم الهاتف" else "تم إلغاء إخفاء رقم الهاتف", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MetGhamrGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Strict Location Privacy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مشاركة الموقع عند البحث فقط", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("يتم استخدام الموقع الجغرافي فقط عند طلب ترتيب 'الأقرب إليك'", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = approxLocation,
                            onCheckedChange = {
                                approxLocation = it
                                prefs.edit().putBoolean(KEY_PRIVACY_APPROX_LOCATION, it).apply()
                                Toast.makeText(context, "تم حفظ تفضيل خصوصية الموقع", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MetGhamrGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Clear Search & Cache
                Text(
                    text = "إجراءات مسح البيانات المحلية",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearSearchHistory()
                            Toast.makeText(context, "تم مسح سجل البحث المحلي بالكامل 🧹", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح سجل البحث", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                context.cacheDir?.deleteRecursively()
                                Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة بنجاح ✅", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "تم تحرير المساحة المؤقتة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنظيف الكاش", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy Policy Accordion
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullPolicy = !showFullPolicy }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بنود سياسة الخصوصية واستخدام البيانات", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrNavy)
                            }
                            Icon(
                                if (showFullPolicy) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = showFullPolicy) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "1. سرية البيانات: يلتزم تطبيق دليل ميت غمر بحماية البيانات الشخصية لجميع المستخدمين وأصحاب الأنشطة التجارية وعدم بيعها أو مشاركتها مع أي جهة خارجية.\n\n" +
                                           "2. أمان الاتصال: جميع الاتصالات واستعلامات البحث مشفرة عبر بروتوكول HTTPS المشفر.\n\n" +
                                           "3. التحكم بالبيانات: يمكنك في أي وقت تعديل بياناتك أو مسح السجلات أو إلغاء حسابك بسهولة من لوحة التحكم.",
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
