package com.example.ui.screens.admin

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBusinessesScreen(viewModel: DirectoryViewModel) {
    val allBusinesses by viewModel.allBusinessesAdmin.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var filterActiveOnly by remember { mutableStateOf(false) }
    var showSmartAddDialog by remember { mutableStateOf(false) }

    val filteredList = remember(allBusinesses, searchQuery, filterActiveOnly) {
        allBusinesses.filter {
            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)) &&
                    (!filterActiveOnly || it.isActive)
        }
    }

    if (showSmartAddDialog) {
        SmartExcelTemplateDialog(
            viewModel = viewModel,
            onDismiss = { showSmartAddDialog = false }
        )
    }

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة الأنشطة التجاريّة",
        currentRoute = ScreenRoute.AdminBusinesses.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم النشاط أو رقم الهاتف...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MetGhamrNavy) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips & Smart Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "الأنشطة (${filteredList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MetGhamrNavy
                    )
                    FilterChip(
                        selected = filterActiveOnly,
                        onClick = { filterActiveOnly = !filterActiveOnly },
                        label = { Text("النشطة فقط", fontSize = 11.sp) },
                        leadingIcon = if (filterActiveOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Button(
                    onClick = { showSmartAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قالب Excel / إضافة نشاط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد أنشطة تجارية متطابقة مع البحث.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { business ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            business.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MetGhamrNavy
                                        )
                                        if (business.isVerified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = "موثق",
                                                tint = MetGhamrGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    // Status Badge
                                    Surface(
                                        color = if (business.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (business.isActive) "نشط" else "موقوف",
                                            color = if (business.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${business.categoryName} • ${business.specialty}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(business.phone, fontSize = 12.sp, color = TextDark)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Icon(Icons.Default.Place, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${business.city} • ${business.area} - ${business.address}", fontSize = 12.sp, color = TextDark)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = MetGhamrGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${business.ratingAverage} (${business.ratingCount} تقييم)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.toggleBusinessActiveAdmin(business.id, !business.isActive)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                if (business.isActive) "إيقاف مؤقت" else "تنشيط",
                                                color = if (business.isActive) Color(0xFFC62828) else Color(0xFF2E7D32),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.selectBusiness(business.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("معاينة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
}
