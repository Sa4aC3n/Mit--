package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.*

@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showAppUpdateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Logo & Emblem Card
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MetGhamrNavy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Logo",
                tint = MetGhamrGold,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "دليل ميت غمر",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MetGhamrNavy
            )
        )

        Surface(
            color = MetGhamrGold.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { showAppUpdateDialog = true }
        ) {
            Text(
                text = "الإصدار v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • تحقق من التحديث 🔄",
                color = MetGhamrNavy,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ajilika Branding Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ajilika Technologies",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )

                Text(
                    text = "Ajilika — Egyptian Roots. Digital Future.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MetGhamrGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "تطبيق دليل ميت غمر هو المنصة المحلية الموثوقة الأولى للربط بين المواطنين والأنشطة والخدمات التجارية والطبية والصناعية داخل مركز ومدينة ميت غمر والقرى التابعة لها.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coverage Scope Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📍 النطاق الجغرافي المغطى في الدليل",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يشمل الدليل شوارع ميت غمر الرئيسية (شارع الحرية، المحطة، بورسعيد، الجلاء) بالإضافة إلى جميع الوحدات المحلية والقرى: صهرجت الكبرى، أتميدة، كوم النور، دنديط، تفهنا الأشراف، بشلا، سنفا، كفر شكر مدخل المدينة.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "حقوق الطبع والنشر © 2026 Ajilika Technologies. جميع الحقوق محفوظة.",
            fontSize = 10.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAppUpdateDialog) {
        AppUpdateDialog(
            onDismiss = { showAppUpdateDialog = false }
        )
    }
}
