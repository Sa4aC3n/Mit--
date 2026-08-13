package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDataManagementScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val backupRecords by viewModel.adminBackupRecords.collectAsState()
    val lastRestoreReport by viewModel.lastRestoreReport.collectAsState()
    val lastImportSummary by viewModel.lastImportSummary.collectAsState()
    val isLoading by viewModel.isDataOperationLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Backup & Restore, 1: Bulk Import Wizard

    // Backup Create Dialog State
    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var selectedBackupType by remember { mutableStateOf(BackupType.FULL) }

    // Restore Confirm Dialog State
    var selectedBackupToRestore by remember { mutableStateOf<BackupRecordEntity?>(null) }
    var restoreConfirmInput by remember { mutableStateOf("") }

    // Bulk Import Wizard State
    var selectedImportType by remember { mutableStateOf(BulkImportType.BUSINESSES) }
    var selectedImportMode by remember { mutableStateOf(ImportMode.UPSERT) }
    var importTextData by remember { mutableStateOf("") }

    AdminLayout(
        viewModel = viewModel,
        title = "🗄️ إدارة البيانات والنسخ الاحتياطي",
        currentRoute = com.example.ui.viewmodel.ScreenRoute.AdminDataManagement.route,
        onBackClick = onBackClick
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
        ) {
            // Source of Truth Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MetGhamrNavy,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Backend Database = SOURCE OF TRUTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy
                        )
                        Text(
                            text = "RealmDB / Room = LOCAL CACHE | النسخ والاستعادة من الخادم الرئيسي فقط",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MetGhamrNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("النسخ والاستعادة (${backupRecords.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("الاستيراد الجماعي Excel/CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MetGhamrTeal
                )
            }

            when (selectedTab) {
                0 -> {
                    // Backup & Restore Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "النسخ الاحتياطية الرسمية",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "سجل نسخ Snapshot الخادم المشفرة",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )
                                }

                                Button(
                                    onClick = { showCreateBackupDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("create_backup_button")
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إنشاء نسخة جديدة", fontSize = 12.sp)
                                }
                            }
                        }

                        // Last Restore Report Banner
                        if (lastRestoreReport != null) {
                            item {
                                RestoreReportCard(
                                    report = lastRestoreReport!!,
                                    onDismiss = { viewModel.clearLastRestoreReport() }
                                )
                            }
                        }

                        if (backupRecords.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا توجد نسخ احتياطية مسجلة حتى الآن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("اضغط على \"إنشاء نسخة جديدة\" للبدء بحفظ لقطة شاملة لقاعدة البيانات.", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        } else {
                            items(backupRecords) { record ->
                                BackupRecordCard(
                                    record = record,
                                    onRestore = { selectedBackupToRestore = record },
                                    onDriveUpload = { viewModel.uploadBackupToGoogleDriveAdmin(record) },
                                    onDownload = { viewModel.showToast("تم بدء تحميل ملف ${record.fileName} للجهاز local storage") }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Bulk Import Wizard Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "معالج الاستيراد الجماعي للبيانات (Bulk Import Wizard)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "استيراد ملفات Excel/CSV مباشرة لخادم الخلفية الرئيسي مع الفحص المسبق والتحقق من التكرار.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }

                        // Step 1: Download Templates
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = MetGhamrNavy.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("1", fontWeight = FontWeight.Bold, color = MetGhamrNavy, fontSize = 13.sp)
                                            }
                                        }
                                        Text("الخطوة 1: تنزيل قوالب البيانات القياسية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val csv = viewModel.getImportTemplateCsv(BulkImportType.BUSINESSES)
                                                viewModel.showToast("تم إنتاج وتنزيل قالب الأنشطة التجارية CSV/Excel بنجاح")
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قالب الأنشطة (Excel/CSV)", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val csv = viewModel.getImportTemplateCsv(BulkImportType.CATEGORIES)
                                                viewModel.showToast("تم إنتاج وتنزيل قالب التصنيفات CSV/Excel بنجاح")
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قالب التصنيفات", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Step 2: Choose Configuration
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = MetGhamrNavy.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("2", fontWeight = FontWeight.Bold, color = MetGhamrNavy, fontSize = 13.sp)
                                            }
                                        }
                                        Text("الخطوة 2: تحديد نوع البيانات ونمط المعالجة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Text("نوع السجلات المراد استيرادها:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = selectedImportType == BulkImportType.BUSINESSES,
                                            onClick = { selectedImportType = BulkImportType.BUSINESSES },
                                            label = { Text("الأنشطة والعيادات", fontSize = 11.sp) }
                                        )
                                        FilterChip(
                                            selected = selectedImportType == BulkImportType.CATEGORIES,
                                            onClick = { selectedImportType = BulkImportType.CATEGORIES },
                                            label = { Text("التصنيفات والاقسام", fontSize = 11.sp) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("نمط التعامل مع السجلات المكررة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = selectedImportMode == ImportMode.UPSERT,
                                            onClick = { selectedImportMode = ImportMode.UPSERT },
                                            label = { Text(ImportMode.UPSERT.titleAr, fontSize = 11.sp) }
                                        )
                                        FilterChip(
                                            selected = selectedImportMode == ImportMode.INSERT_ONLY,
                                            onClick = { selectedImportMode = ImportMode.INSERT_ONLY },
                                            label = { Text(ImportMode.INSERT_ONLY.titleAr, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 3: Paste / Upload File Data
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = MetGhamrNavy.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("3", fontWeight = FontWeight.Bold, color = MetGhamrNavy, fontSize = 13.sp)
                                            }
                                        }
                                        Text("الخطوة 3: إضافة بيانات الملف للربط والتدقيق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    OutlinedTextField(
                                        value = importTextData,
                                        onValueChange = { importTextData = it },
                                        placeholder = { Text("لصق محتوى ملف CSV/Excel أو تحضير البيانات هنا...\nمثال:\nاسم النشاط, التخصص, رقم الهاتف, المنطقة\nمطعم المشويات, مشويات, 01000000000, شارع الحرية", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().height(110.dp)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                importTextData = "اسم النشاط,التصنيف,التخصص,رقم الهاتف,المنطقة\n" +
                                                        "مطعم ابن البلد للمأكولات,مطاعم,مشويات,01011112222,شارع الحرية\n" +
                                                        "صيدلية د. أحمد علي,صيدليات,خدمة 24 ساعة,01122223333,ميدان المحطة\n" +
                                                        "عيادة د. أسماء,أطباء,أطفال,01233334444,صهرجت الكبرى"
                                                viewModel.showToast("تم تحميل بيانات تجريبية بالنموذج")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("تعبئة نموذج تجريبي", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { importTextData = "" },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("مسح", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Step 4: Dry Run & Production Execution
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = MetGhamrNavy.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("4", fontWeight = FontWeight.Bold, color = MetGhamrNavy, fontSize = 13.sp)
                                            }
                                        }
                                        Text("الخطوة 4: التثبت بالمعاينة التشغيلية والاستيراد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val parsedRows = parseInputCsv(importTextData)
                                                if (parsedRows.isEmpty()) {
                                                    viewModel.showToast("يرجى إدخال أسطر البيانات أولاً")
                                                } else {
                                                    viewModel.processBulkImportAdmin(
                                                        importType = selectedImportType,
                                                        rowsData = parsedRows,
                                                        mode = selectedImportMode,
                                                        isDryRun = true
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("فحص تجريبي (Dry Run)", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val parsedRows = parseInputCsv(importTextData)
                                                if (parsedRows.isEmpty()) {
                                                    viewModel.showToast("يرجى إدخال أسطر البيانات أولاً")
                                                } else {
                                                    viewModel.processBulkImportAdmin(
                                                        importType = selectedImportType,
                                                        rowsData = parsedRows,
                                                        mode = selectedImportMode,
                                                        isDryRun = false
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تنفيذ الاستيراد الفعلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Display Import Summary Card
                        if (lastImportSummary != null) {
                            item {
                                ImportResultSummaryCard(
                                    summary = lastImportSummary!!,
                                    onDismiss = { viewModel.clearLastImportSummary() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Create Backup Options
    if (showCreateBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBackupDialog = false },
            title = { Text("إنشاء نسخة احتياطية جديدة", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر نوع النسخة المطلوب استخراجها من قاعدة البيانات الرئيسية:", fontSize = 12.sp)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedBackupType == BackupType.FULL) MetGhamrNavy.copy(alpha = 0.08f) else Color.White
                        ),
                        border = if (selectedBackupType == BackupType.FULL) androidx.compose.foundation.BorderStroke(1.5.dp, MetGhamrNavy) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBackupType = BackupType.FULL }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(BackupType.FULL.titleAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
                            Text(BackupType.FULL.descriptionAr, fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedBackupType == BackupType.OPERATIONAL) MetGhamrNavy.copy(alpha = 0.08f) else Color.White
                        ),
                        border = if (selectedBackupType == BackupType.OPERATIONAL) androidx.compose.foundation.BorderStroke(1.5.dp, MetGhamrNavy) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBackupType = BackupType.OPERATIONAL }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(BackupType.OPERATIONAL.titleAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
                            Text(BackupType.OPERATIONAL.descriptionAr, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFullBackupAdmin(selectedBackupType)
                        showCreateBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                ) {
                    Text("بدء الإنشاء والتشفير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBackupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog 2: Restore Safety Confirmation
    if (selectedBackupToRestore != null) {
        val backup = selectedBackupToRestore!!
        AlertDialog(
            onDismissRequest = {
                selectedBackupToRestore = null
                restoreConfirmInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MetGhamrRed)
                    Text("تأكيد استعادة قاعدة البيانات ⚠️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MetGhamrRed)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أنت على وشك استعادة النسخة الاحتياطية (${backup.fileName}).",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = OpenGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = OpenGreen, modifier = Modifier.size(20.dp))
                            Text(
                                text = "سيتم إنشاء نسخة سلامة تلقائية (Pre-Restore Safety Backup) قبل تطبيق التغييرات لتجنب أي إخفاق.",
                                fontSize = 11.sp,
                                color = OpenGreen
                            )
                        }
                    }

                    Text("لتأكيد عملية الاستعادة، اكتب كلمة RESTORE أدناه:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = restoreConfirmInput,
                        onValueChange = { restoreConfirmInput = it },
                        placeholder = { Text("RESTORE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreConfirmInput.trim() == "RESTORE") {
                            viewModel.restoreFullBackupAdmin(backup, restoreConfirmInput)
                            selectedBackupToRestore = null
                            restoreConfirmInput = ""
                        } else {
                            viewModel.showToast("يرجى كتابة كلمة RESTORE بالتأكيد")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed)
                ) {
                    Text("تأكيد الاستعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedBackupToRestore = null
                    restoreConfirmInput = ""
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun BackupRecordCard(
    record: BackupRecordEntity,
    onRestore: () -> Unit,
    onDriveUpload: () -> Unit,
    onDownload: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale("ar")).format(Date(record.createdAt))
    val sizeMb = "%.2f MB".format(record.sizeBytes.toDouble() / (1024 * 1024))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = record.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MetGhamrNavy
                        )
                        Text(
                            text = "$dateStr • $sizeMb",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = when (record.storageLocation) {
                        "GOOGLE_DRIVE" -> VerifiedBlue.copy(alpha = 0.15f)
                        else -> MetGhamrTeal.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (record.storageLocation == "GOOGLE_DRIVE") "☁️ Google Drive" else "💾 Local / Server",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.storageLocation == "GOOGLE_DRIVE") VerifiedBlue else MetGhamrTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            // Checksum and Record breakdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                    Text(
                        text = "SHA-256 Checksum: ${record.checksum.take(16)}...",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "نوع النسخة: ${record.backupType} | المُنشيء: ${record.createdBy}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة النسخة", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onDriveUpload,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رفع للـ Drive", fontSize = 11.sp)
                }

                IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "تحميل", tint = MetGhamrNavy)
                }
            }
        }
    }
}

@Composable
fun RestoreReportCard(
    report: RestoreReportSummary,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OpenGreen.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, OpenGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OpenGreen)
                    Text("تقرير استعادة قاعدة البيانات بنجاح 🎉", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OpenGreen)
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                }
            }

            Text(
                text = "السجلات التي تمت استعادتها: ${report.businessesRestored} نشاط، ${report.usersRestored} مستخدم، ${report.reviewsRestored} مراجعة، ${report.categoriesRestored} تصنيف.",
                fontSize = 11.sp
            )

            if (report.preRestoreSafetyBackupId != null) {
                Text(
                    text = "تم إنشاء نسخة سلامة تلقائية تلقائياً بالمعرف: ${report.preRestoreSafetyBackupId}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ImportResultSummaryCard(
    summary: ImportResultSummary,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isDryRun) MetGhamrNavy.copy(alpha = 0.08f) else OpenGreen.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (summary.isDryRun) MetGhamrNavy else OpenGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (summary.isDryRun) Icons.Default.FindInPage else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (summary.isDryRun) MetGhamrNavy else OpenGreen
                    )
                    Text(
                        text = if (summary.isDryRun) "نتائج الفحص التجريبي (Dry Run Result)" else "تقرير الاستيراد الجماعي الفعلي 🚀",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (summary.isDryRun) MetGhamrNavy else OpenGreen
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي الأسطر: ${summary.totalRows}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("الناجح/الصالح: ${summary.successCount}", fontSize = 11.sp, color = OpenGreen, fontWeight = FontWeight.Bold)
                Text("المحدث: ${summary.updatedCount}", fontSize = 11.sp, color = MetGhamrTeal)
                Text("الأخطاء: ${summary.failedCount}", fontSize = 11.sp, color = MetGhamrRed, fontWeight = FontWeight.Bold)
            }

            if (summary.errors.isNotEmpty()) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                Text("أخطاء الملاحظة والتوصيات المترتبة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrRed)
                summary.errors.take(3).forEach { err ->
                    Text(
                        text = "• السطر ${err.rowNumber}: [${err.field}] ${err.error} -> التوصية: ${err.suggestedFix}",
                        fontSize = 10.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// Utility to parse pasted CSV lines
private fun parseInputCsv(csvText: String): List<Map<String, String>> {
    val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.size <= 1) return emptyList()

    val headers = lines.first().split(",").map { it.trim() }
    val dataRows = mutableListOf<Map<String, String>>()

    for (i in 1 until lines.size) {
        val values = lines[i].split(",").map { it.trim() }
        val map = mutableMapOf<String, String>()
        headers.forEachIndexed { idx, header ->
            if (idx < values.size) {
                map[header] = values[idx]
            }
        }
        dataRows.add(map)
    }

    return dataRows
}
