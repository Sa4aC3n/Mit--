package com.example.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.util.ExcelFileImportHelper
import com.example.util.ExcelImportParsedResult
import com.example.util.FileExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDataManagementScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val backupRecords by viewModel.adminBackupRecords.collectAsState()
    val lastRestoreReport by viewModel.lastRestoreReport.collectAsState()
    val lastImportSummary by viewModel.lastImportSummary.collectAsState()
    val isLoading by viewModel.isDataOperationLoading.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cloud Sync, 1: Backup & Restore, 2: Bulk Import Wizard

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
    var showSmartTemplateDialog by remember { mutableStateOf(false) }
    var lastParsedExcelInfo by remember { mutableStateOf<ExcelImportParsedResult?>(null) }

    // Excel and CSV File Picker Launcher
    val excelFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val parsedResult = ExcelFileImportHelper.parseUploadedFile(context, uri)
                if (parsedResult.errorMessage != null) {
                    viewModel.showToast("خطأ أثناء قراءة الملف: ${parsedResult.errorMessage}")
                } else if (parsedResult.totalRows == 0) {
                    viewModel.showToast("الملف المرفوع لا يحتوي على بيانات صالحة")
                } else {
                    lastParsedExcelInfo = parsedResult
                    importTextData = parsedResult.csvFormattedText
                    selectedTab = 2 // Switch to Bulk Import Wizard
                    viewModel.showToast("تم استخراج ${parsedResult.totalRows} سجل من ملف (${parsedResult.fileName}) بنجاح 📊")
                }
            } catch (e: Exception) {
                viewModel.showToast("حدث خطأ أثناء معالجة الملف: ${e.localizedMessage}")
            }
        }
    }

    if (showSmartTemplateDialog) {
        SmartExcelTemplateDialog(
            viewModel = viewModel,
            onDismiss = { showSmartTemplateDialog = false },
            onInjectCsvRow = { row ->
                if (importTextData.isBlank() || !importTextData.contains("اسم النشاط")) {
                    importTextData = "اسم النشاط,التصنيف,التخصص,رقم الهاتف,رقم هاتف إضافي,واتساب,العنوان,المنطقة,المدينة,أيام العمل,مواعيد العمل,الوصف,رابط الخريطة,الفيسبوك,الموقع\n$row"
                } else {
                    importTextData = importTextData.trimEnd() + "\n" + row
                }
            }
        )
    }

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

            // Direct Excel Upload Quick Action Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF107C41).copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = Color(0xFF107C41),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "رفع واستيراد ملف Excel / CSV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MetGhamrNavy
                            )
                            Text(
                                text = "دعم كامل لصيغ .xlsx, .xls, .csv مع الفحص والتدقيق",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            excelFilePickerLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("upload_excel_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع ملف EXCEL 📤", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Uploaded Excel Parsed Summary Banner
            if (lastParsedExcelInfo != null) {
                val parsed = lastParsedExcelInfo!!
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "تم تحميل ملف: ${parsed.fileName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534)
                                )
                            }

                            IconButton(
                                onClick = { lastParsedExcelInfo = null },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نوع الملف: ${parsed.fileType}", fontSize = 11.sp, color = TextSecondary)
                            Text("عدد السجلات: ${parsed.totalRows} صف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Text("الأعمدة: ${parsed.headers.size} عمود", fontSize = 11.sp, color = TextSecondary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val rowsToProcess = if (lastParsedExcelInfo != null && lastParsedExcelInfo!!.rows.isNotEmpty()) {
                                        lastParsedExcelInfo!!.rows
                                    } else {
                                        parseInputCsv(importTextData)
                                    }
                                    if (rowsToProcess.isNotEmpty()) {
                                        viewModel.processBulkImportAdmin(
                                            importType = selectedImportType,
                                            rowsData = rowsToProcess,
                                            mode = selectedImportMode,
                                            isDryRun = true
                                        )
                                    } else {
                                        viewModel.showToast("لا توجد بيانات للمعالجة")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص تجريبي (Dry Run)", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val rowsToProcess = if (lastParsedExcelInfo != null && lastParsedExcelInfo!!.rows.isNotEmpty()) {
                                        lastParsedExcelInfo!!.rows
                                    } else {
                                        parseInputCsv(importTextData)
                                    }
                                    if (rowsToProcess.isNotEmpty()) {
                                        viewModel.processBulkImportAdmin(
                                            importType = selectedImportType,
                                            rowsData = rowsToProcess,
                                            mode = selectedImportMode,
                                            isDryRun = false
                                        )
                                    } else {
                                        viewModel.showToast("لا توجد بيانات للمعالجة")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد فوري للسيرفر 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("المزامنة السحابية ☁️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("النسخ والاستعادة (${backupRecords.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("الاستيراد الجماعي Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    // Cloud Sync Tab
                    CloudSyncManagementTab(
                        viewModel = viewModel,
                        cloudSyncStatus = cloudSyncStatus,
                        firestoreSyncStatus = firestoreSyncStatus
                    )
                }

                1 -> {
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
                                    onDownload = {
                                        val backupJson = """
                                        {
                                          "id": "${record.id}",
                                          "fileName": "${record.fileName}",
                                          "backupType": "${record.backupType}",
                                          "sizeBytes": ${record.sizeBytes},
                                          "createdAt": ${record.createdAt},
                                          "createdBy": "${record.createdBy}",
                                          "checksum": "${record.checksum}",
                                          "recordCounts": ${record.recordCountsJson}
                                        }
                                        """.trimIndent()
                                        FileExportUtils.downloadAndShareFile(
                                            context = context,
                                            fileName = record.fileName,
                                            content = backupJson,
                                            mimeType = "application/json",
                                            title = "تنزيل أو مشاركة ملف النسخة الاحتياطية"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
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

                        // Step 1: Download Templates & Smart Dropdowns
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                            Text("الخطوة 1: تنزيل قوالب البيانات ونظام القوائم المنسدلة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        Surface(
                                            color = Color(0xFFE0F2FE),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "Excel + Dropdown",
                                                color = Color(0xFF0369A1),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Featured Button: Open Smart Excel & Dropdown Suite
                                    Button(
                                        onClick = { showSmartTemplateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("فتح مركز قالب Excel والإدخال بالقوائم المنسدلة ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Row of Actions: Download Templates & Upload User Excel
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val excelXml = viewModel.getImportTemplateExcel()
                                                FileExportUtils.downloadAndShareFile(
                                                    context = context,
                                                    fileName = "قالب_استيراد_أنشطة_ميت_غمر_الشامل.xls",
                                                    content = excelXml,
                                                    mimeType = "application/vnd.ms-excel",
                                                    title = "فتح أو حفظ قالب إكسل الشامل (Excel Workbook)"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تنزيل قالب (.xls)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val categoriesExcel = viewModel.getCategoriesCatalogExcel()
                                                FileExportUtils.downloadAndShareFile(
                                                    context = context,
                                                    fileName = "فهرس_التصنيفات_والتخصصات_ميت_غمر.xls",
                                                    content = categoriesExcel,
                                                    mimeType = "application/vnd.ms-excel",
                                                    title = "فتح أو حفظ فهرس التصنيفات (Excel)"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("فهرس التصنيفات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                excelFilePickerLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفع ملف Excel 📤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 12.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = MetGhamrNavy,
                                            unfocusedBorderColor = BorderLight
                                        ),
                                        placeholder = { Text("لصق محتوى ملف CSV/Excel أو تحضير البيانات هنا...\nمثال:\nاسم النشاط, التخصص, رقم الهاتف, المنطقة\nمطعم المشويات, مشويات, 01000000000, شارع الحرية", fontSize = 11.sp, color = TextMuted) },
                                        modifier = Modifier.fillMaxWidth().height(110.dp)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                excelFilePickerLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اختيار ملف من الهاتف 📁", fontSize = 11.sp)
                                        }

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
                                                val parsedRows = if (lastParsedExcelInfo != null && lastParsedExcelInfo!!.rows.isNotEmpty() && importTextData == lastParsedExcelInfo!!.csvFormattedText) {
                                                    lastParsedExcelInfo!!.rows
                                                } else {
                                                    parseInputCsv(importTextData)
                                                }
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
                                                val parsedRows = if (lastParsedExcelInfo != null && lastParsedExcelInfo!!.rows.isNotEmpty() && importTextData == lastParsedExcelInfo!!.csvFormattedText) {
                                                    lastParsedExcelInfo!!.rows
                                                } else {
                                                    parseInputCsv(importTextData)
                                                }
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

                        // Cleanup Tool for previously corrupted imports
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MetGhamrRed, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("تنظيف الأنشطة المستوردة التالفة / الوهمية", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrRed)
                                            Text("حذف أي سجلات سابقة أضيفت بالخطأ مثل «نشاط مستورد 1» بنقرة واحدة", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.cleanupCorruptedImportedBusinesses()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MetGhamrRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تنظيف الآن 🧹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MetGhamrNavy,
                            unfocusedBorderColor = BorderLight
                        ),
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

// Utility to parse pasted CSV lines using robust ExcelFileImportHelper
private fun parseInputCsv(csvText: String): List<Map<String, String>> {
    val table = ExcelFileImportHelper.parseCsvText(csvText)
    if (table.isEmpty()) return emptyList()

    val headers = table.first()
    val dataRows = mutableListOf<Map<String, String>>()

    for (i in 1 until table.size) {
        val values = table[i]
        val map = mutableMapOf<String, String>()
        headers.forEachIndexed { idx, header ->
            if (idx < values.size && header.isNotBlank()) {
                map[header] = values[idx]
            }
        }
        if (map.values.any { it.isNotBlank() }) {
            dataRows.add(map)
        }
    }

    return dataRows
}

@Composable
fun CloudSyncManagementTab(
    viewModel: DirectoryViewModel,
    cloudSyncStatus: com.example.data.firebase.CloudSyncStatus,
    firestoreSyncStatus: com.example.data.firebase.FirestoreSyncStatus
) {
    var isSyncingNow by remember { mutableStateOf(false) }
    var isPullingNow by remember { mutableStateOf(false) }
    var isFirestoreSyncing by remember { mutableStateOf(false) }

    val lastSyncFormatted = if (cloudSyncStatus.lastSyncTimestamp > 0) {
        SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale("ar")).format(Date(cloudSyncStatus.lastSyncTimestamp))
    } else {
        "لم تتم مزامنة يدوية بعد"
    }

    val lastFirestoreSyncFormatted = if (firestoreSyncStatus.lastSyncTimestamp > 0) {
        SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale("ar")).format(Date(firestoreSyncStatus.lastSyncTimestamp))
    } else {
        "تلقائي عند بدء التشغيل"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firestore Authoritative Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚡", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "سحابة Firestore — Source of Truth",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "قاعدة البيانات السحابية المركزية المعتمدة",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Surface(
                            color = OpenGreen.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(OpenGreen)
                                )
                                Text(
                                    text = "سحابي نشط 🟢",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "تعمل قاعدة بيانات Google Cloud Firestore كمصدر الحقيقة الرئيسي لكافة أنشطة ومراجعات دليل ميت غمر، مع مزامنة تفاضلية ذكية (Incremental Delta Sync) تعتمد على الطابع الزمني وتخزين محلي فائق السرعة عبر كاش التطبيق.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Firestore Incremental Sync Action Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "المزامنة التفاضلية السحابية (Incremental Delta Sync)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MetGhamrNavy
                            )
                            Text(
                                text = "جلب التغييرات والتحديثات الجديدة فقط دون إعادة تحميل قاعدة البيانات بالكامل",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("آخر فحص دلتا:", fontSize = 11.sp, color = TextSecondary)
                        Text(lastFirestoreSyncFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حالة المحرك:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = firestoreSyncStatus.message.take(45),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = VerifiedBlue
                        )
                    }

                    // Full Pull Button
                    Button(
                        onClick = {
                            isFirestoreSyncing = true
                            viewModel.refreshData { success, count ->
                                isFirestoreSyncing = false
                            }
                        },
                        enabled = !isFirestoreSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isFirestoreSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري جلب وتحديث الأنشطة من السحابة...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جلب وتحديث كافة الأنشطة من Firestore فوراً (360+ نشاط) 📥", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Cloud Upload Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.uploadCategoriesToCloud()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رفع التصنيفات ☁️", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.uploadAllBusinessesToCloud()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رفع الأنشطة ☁️", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live Server KPI Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊 أنشطة السيرفر", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${cloudSyncStatus.cloudCount}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy
                        )
                        Text("نشاط مثبت بالسحاب", fontSize = 10.sp, color = MetGhamrTeal)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄 البث المباشر", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (cloudSyncStatus.isListening) "نشط Live" else "معطل",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cloudSyncStatus.isListening) OpenGreen else MetGhamrRed
                        )
                        Text("تحديث فوري ثنائي", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Status Details Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تفاصيل حالة الاتصال والمزامنة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MetGhamrNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("آخر تحديث سحابي:", fontSize = 11.sp, color = TextSecondary)
                        Text(lastSyncFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رسالة المحرك:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = cloudSyncStatus.lastSyncMessage.take(45),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MetGhamrTeal
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مسار عقدة السحاب:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "/businesses",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MetGhamrNavy
                        )
                    }
                }
            }
        }

        // Action 1: Push All to Server
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MetGhamrTeal, modifier = Modifier.size(24.dp))
                        Text(
                            text = "تثبيت ورفع كافة الأنشطة إلى السيرفر السحابي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MetGhamrNavy
                        )
                    }

                    Text(
                        text = "يقوم برفع كافة الأنشطة المعتمدة في هذا الجهاز إلى خادم Firebase السحابي، لضمان استلام كافة المستخدمين على أجهزتهم لأحدث الأنشطة المضافة والمكتشفة.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            isSyncingNow = true
                            viewModel.syncAllBusinessesToCloudServer { success, _ ->
                                isSyncingNow = false
                            }
                        },
                        enabled = !isSyncingNow && !isPullingNow,
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الرفع والتثبيت السحابي...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تثبيت ومزامنة كافة الأنشطة على السيرفر الآن 🚀", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action 2: Pull All from Server
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrNavy.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(24.dp))
                        Text(
                            text = "جلب وتحديث الأنشطة من السيرفر السحابي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MetGhamrNavy
                        )
                    }

                    Text(
                        text = "يقوم بتحميل كافة الأنشطة والتعديلات الجديدة المسجلة على الخادم السحابي وتحديث قاعدة البيانات المحلية على هذا الجهاز فوراً.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    OutlinedButton(
                        onClick = {
                            isPullingNow = true
                            viewModel.pullBusinessesFromCloudServer { success, _ ->
                                isPullingNow = false
                            }
                        },
                        enabled = !isSyncingNow && !isPullingNow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPullingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MetGhamrNavy, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الجلب والتحديث...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جلب ومطابقة الأنشطة من السيرفر السحابي 📥", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Explanatory Multi-Device Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = OpenGreen.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 20.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "المزامنة التلقائية اللحظية (Real-time Live Sync)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OpenGreen
                        )
                        Text(
                            text = "بفضل تقنية ChildEventListener في Firebase، فإن أي نشاط يتم إضافته أو تعديله أو اعتماده من محرك الجمع الذكي أو الاستيراد الجماعي يتم بثه وتحديثه تلقائياً لدى جميع المستخدمين على أجهزتهم دون الحاجة لإعادة تشغيل التطبيق.",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
