package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BusinessCard
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@Composable
fun FavoritesScreen(
    viewModel: DirectoryViewModel
) {
    val favorites by viewModel.favorites.collectAsState()
    val allBusinesses by viewModel.filteredBusinesses.collectAsState()

    val favIds = favorites.map { it.businessId }.toSet()
    val favBusinesses = allBusinesses.filter { favIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "قائمة المفضلة ❤️",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "الأنشطة والعيادات والمحلات المحفوظة للوصول السريع",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (favBusinesses.isNotEmpty()) {
                Surface(
                    color = SkyBlueContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${favBusinesses.size} نشاط",
                        color = SkyBlueDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (favBusinesses.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.FavoriteBorder,
                title = "قائمة المفضلة فارغة",
                description = "يمكنك حفظ أنشطتك المفضلة في ميت غمر للوصول إليها بسرعة في أي وقت ودون الحاجة لإعادة البحث.",
                actionButtonText = "استكشف الأنشطة الآن",
                onActionClick = { viewModel.navigateTo(ScreenRoute.Home.route) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favBusinesses) { business ->
                    BusinessCard(
                        business = business,
                        isFavorite = true,
                        onClick = { viewModel.selectBusiness(business.id) },
                        onFavoriteToggle = {
                            viewModel.toggleFavorite(business.id, true)
                        }
                    )
                }
            }
        }
    }
}
