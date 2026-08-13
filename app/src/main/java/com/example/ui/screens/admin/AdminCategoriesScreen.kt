package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@Composable
fun AdminCategoriesScreen(viewModel: DirectoryViewModel) {
    val categories = viewModel.categories

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة التصنيفات والدليل",
        currentRoute = ScreenRoute.AdminCategories.route
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "التصنيفات الرئيسية المسجلة (${categories.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MetGhamrNavy
                    )
                    Button(
                        onClick = { viewModel.showToast("تم تعطيل إضافة تصنيف جديد مؤقتاً بالنسخة الحالية") },
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة تصنيف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(categories, key = { it.id }) { cat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MetGhamrGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(cat.nameAr, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MetGhamrNavy)
                                    Text("${cat.subcategories.size} تصنيف فرعي", fontSize = 12.sp, color = TextMuted)
                                }
                            }

                            Row {
                                IconButton(onClick = { viewModel.showToast("تعديل التصنيف") }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MetGhamrNavy)
                                }
                            }
                        }

                        if (cat.subcategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("التخصصات الفرعية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrGoldDark)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cat.subcategories.take(4).forEach { sub ->
                                    Surface(
                                        color = MetGhamrCream,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            sub.nameAr,
                                            fontSize = 10.sp,
                                            color = MetGhamrNavy,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
