package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class EmergencyContact(
    val id: String,
    val title: String,
    val description: String,
    val number: String,
    val isNational: Boolean,
    val emoji: String,
    val badgeBg: Color,
    val badgeText: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onCallClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "national", "local"

    val nationalContacts = remember {
        listOf(
            EmergencyContact(
                id = "nat_ambulance",
                title = "الإسعاف المركزي",
                description = "طوارئ الحالات الحرجة ونقل المرضى",
                number = "123",
                isNational = true,
                emoji = "🚑",
                badgeBg = Color(0xFFFFECEE),
                badgeText = WatermelonRedDark
            ),
            EmergencyContact(
                id = "nat_police",
                title = "شرطة النجدة",
                description = "بلاغات النجدة والأمن العام",
                number = "122",
                isNational = true,
                emoji = "🚓",
                badgeBg = Color(0xFFE0F2FE),
                badgeText = SkyBlueDeep
            ),
            EmergencyContact(
                id = "nat_fire",
                title = "الحماية المدنية والإطفاء",
                description = "طوارئ الحرائق والإنقاذ النهري",
                number = "180",
                isNational = true,
                emoji = "🚒",
                badgeBg = Color(0xFFFEF3C7),
                badgeText = Color(0xFFB45309)
            ),
            EmergencyContact(
                id = "nat_gas",
                title = "طوارئ الغاز الطبيعي",
                description = "تسريبات وأعطال شبكة الغاز",
                number = "129",
                isNational = true,
                emoji = "🔥",
                badgeBg = Color(0xFFFCE7F3),
                badgeText = Color(0xFFBE185D)
            ),
            EmergencyContact(
                id = "nat_electricity",
                title = "طوارئ الكهرباء",
                description = "انقطاع التيار وأعطال المحولات",
                number = "121",
                isNational = true,
                emoji = "⚡",
                badgeBg = Color(0xFFFEF9C3),
                badgeText = Color(0xFFA16207)
            ),
            EmergencyContact(
                id = "nat_water",
                title = "طوارئ مياه الشرب",
                description = "كسور المواسير والصرف الصحي",
                number = "125",
                isNational = true,
                emoji = "💧",
                badgeBg = Color(0xFFE0F7FA),
                badgeText = Color(0xFF00838F)
            )
        )
    }

    val localContacts = remember {
        listOf(
            EmergencyContact(
                id = "loc_hospital",
                title = "طوارئ مستشفى ميت غمر العام",
                description = "استقبال طوارئ وحوادث وعناية مركزة على مدار 24 ساعة",
                number = "0506900100",
                isNational = false,
                emoji = "🏥",
                badgeBg = Color(0xFFE0F2FE),
                badgeText = SkyBlueDeep
            ),
            EmergencyContact(
                id = "loc_police_station",
                title = "قسم شرطة بندر ميت غمر",
                description = "مأمور وبلاغات قسم شرطة ميت غمر المحلية",
                number = "0506900222",
                isNational = false,
                emoji = "👮",
                badgeBg = Color(0xFFF1F5F9),
                badgeText = TextPrimary
            ),
            EmergencyContact(
                id = "loc_blood_bank",
                title = "بنك الدم الإقليمي بميت غمر",
                description = "توفير فصائل الدم ومشتقاته للحالات الحرجة والعمليات",
                number = "0506900333",
                isNational = false,
                emoji = "🩸",
                badgeBg = Color(0xFFFFECEE),
                badgeText = WatermelonRedDark
            ),
            EmergencyContact(
                id = "loc_fever_hospital",
                title = "طوارئ مستشفى حميات ميت غمر",
                description = "استقبال وطوارئ الأمراض المعدية والباطنية",
                number = "0506904123",
                isNational = false,
                emoji = "🩺",
                badgeBg = Color(0xFFF0FDF4),
                badgeText = LettuceGreenDark
            ),
            EmergencyContact(
                id = "loc_traffic_rescue",
                title = "أوناش ومرور ميت غمر",
                description = "إنقاذ وتيسير الحوادث المرورية بطريق ميت غمر",
                number = "0506905544",
                isNational = false,
                emoji = "🚨",
                badgeBg = Color(0xFFFFF7ED),
                badgeText = Color(0xFFC2410C)
            )
        )
    }

    val allContacts = remember(nationalContacts, localContacts) {
        nationalContacts + localContacts
    }

    val filteredContacts = remember(searchQuery, selectedFilter, allContacts) {
        allContacts.filter { contact ->
            val matchesFilter = when (selectedFilter) {
                "national" -> contact.isNational
                "local" -> !contact.isNational
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    contact.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    contact.description.contains(searchQuery.trim(), ignoreCase = true) ||
                    contact.number.contains(searchQuery.trim())

            matchesFilter && matchesQuery
        }
    }

    fun makeCall(phone: String) {
        if (phone.isNotBlank()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        }
    }

    fun copyNumber(phone: String, label: String) {
        clipboardManager.setText(AnnotatedString(phone))
        Toast.makeText(context, "تم نسخ رقم $label ($phone) إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .testTag("emergency_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Emergency Banner (Styled with Original App Color Palette)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, WatermelonRedLight.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_header_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Right Alert Badge
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(WatermelonRedBg)
                            .border(1.5.dp, WatermelonRedLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "تحذير وطوارئ",
                            tint = WatermelonRedDark,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "شاشة أرقام الطوارئ السريعة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "اتصال مباشر ومجاني بكافة خدمات الطوارئ الرسمية في ميت غمر بنقرة واحدة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Search & Filter Row
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_search_field"),
                        placeholder = {
                            Text(
                                "ابحث عن خدمة طوارئ (إسعاف، شرطة، مطافئ، غاز...)",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SkyBluePrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Category Filter Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" },
                            label = { Text("الكل (${allContacts.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "national",
                            onClick = { selectedFilter = "national" },
                            label = { Text("طوارئ وطنية (6)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "local",
                            onClick = { selectedFilter = "local" },
                            label = { Text("ميت غمر المحلية (5)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 3. National Emergency Section Header (when showing national)
        val showNationalHeader = (selectedFilter == "all" || selectedFilter == "national") &&
                filteredContacts.any { it.isNational }

        if (showNationalHeader) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SkyBluePrimary)
                    )
                    Text(
                        text = "خطوط النجدة والطوارئ الوطنية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            val nationalFiltered = filteredContacts.filter { it.isNational }
            items(nationalFiltered, key = { it.id }) { contact ->
                NationalEmergencyCardItem(
                    contact = contact,
                    onCallClick = { makeCall(contact.number) },
                    onCopyClick = { copyNumber(contact.number, contact.title) }
                )
            }
        }

        // 4. Local Mit Ghamr Emergency Section Header (when showing local)
        val showLocalHeader = (selectedFilter == "all" || selectedFilter == "local") &&
                filteredContacts.any { !it.isNational }

        if (showLocalHeader) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(LettuceGreenDark)
                    )
                    Text(
                        text = "طوارئ ومستشفيات ميت غمر المحلية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            val localFiltered = filteredContacts.filter { !it.isNational }
            items(localFiltered, key = { it.id }) { contact ->
                LocalHospitalEmergencyCardItem(
                    contact = contact,
                    onCallClick = { makeCall(contact.number) },
                    onCopyClick = { copyNumber(contact.number, contact.title) }
                )
            }
        }

        // 5. Emergency Best Practices & Guidelines Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueSoftBg),
                border = BorderStroke(1.dp, SkyBlueContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = SkyBlueDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "إرشادات هامة عند الإبلاغ عن طوارئ بميت غمر",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SkyBlueDark,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Text(
                        text = "• حدد عنوانك بدقة: اسم الشارع أو القرية وأقرب معلم أو ميدان معروف.\n" +
                                "• اشرح الحالة بهدوء واذكر عدد المصابين أو طبيعة العطل.\n" +
                                "• لا تغلق المكالمة حتى يؤكد موظف العمليات استلام البلاغ وتحريك الفرق.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * National Emergency Card: Number badge and info on right, call button on left.
 */
@Composable
fun NationalEmergencyCardItem(
    contact: EmergencyContact,
    onCallClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("emergency_card_${contact.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Contact Info & Number Badge (Right side in RTL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Number Badge with soft background tint
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(contact.badgeBg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.emoji,
                            fontSize = 14.sp
                        )
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = contact.badgeText,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                // Title and description
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = contact.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = contact.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Call Action Button & Copy Button (Left side in RTL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyBluePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("call_button_${contact.number}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اتصال",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                }

                // Quick Copy Button
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ الرقم",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Local Hospitals & Emergency Card: Name and description start from the right,
 * and the phone number is placed directly underneath the Call button on the left.
 */
@Composable
fun LocalHospitalEmergencyCardItem(
    contact: EmergencyContact,
    onCallClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("emergency_card_${contact.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right side: Name and Description starting from the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = contact.emoji,
                        fontSize = 17.sp
                    )
                    Text(
                        text = contact.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.5.sp
                        )
                    )
                }

                Text(
                    text = contact.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                )
            }

            // Left side: Call Button + Phone Number directly underneath
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LettuceGreenDark
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("call_button_${contact.number}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اتصال",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                }

                // Phone number placed directly under the call button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = contact.badgeBg,
                    border = BorderStroke(1.dp, contact.badgeText.copy(alpha = 0.25f)),
                    onClick = onCopyClick,
                    modifier = Modifier.testTag("phone_badge_${contact.number}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = contact.badgeText,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الرقم",
                            tint = contact.badgeText.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
