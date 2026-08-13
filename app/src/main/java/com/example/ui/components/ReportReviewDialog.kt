package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MetGhamrNavy
import com.example.ui.theme.MetGhamrRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportReviewDialog(
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, description: String) -> Unit
) {
    val reportReasons = listOf(
        "محتوى مسيء أو لغة غير لائقة",
        "إعلان تجاري أو Spam",
        "معلومات كاذبة أو مضللة",
        "انتحال شخصية أو تقييم وهمي",
        "سبب آخر"
    )

    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = MetGhamrRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "الإبلاغ عن المراجعة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "يرجى اختيار سبب الإبلاغ لمساعدتنا في الحفاظ على جودة ومصداقية الدليل:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reportReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = MetGhamrNavy)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل إضافية (اختياري)", fontSize = 12.sp) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(selectedReason, description.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("إرسال البلاغ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_report_button")
            ) {
                Text("إلغاء", color = TextSecondary, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
