package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AuthProvider
import com.example.data.model.BusinessEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetGhamrTopAppBar(
    title: String = "دليل ميت غمر",
    isSecondaryScreen: Boolean = false,
    unreadNotifCount: Int = 0,
    onBackClick: (() -> Unit)? = null,
    onNotifClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        color = MetGhamrNavy,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSecondaryScreen) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAboutClick,
                        modifier = Modifier.testTag("top_bar_about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "عن التطبيق",
                            tint = MetGhamrGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MetGhamrNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Branding Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAboutClick() }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MetGhamrGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "شعار ميت غمر",
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Ajilika — ميت غمر",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MetGhamrGoldLight,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAdminClick,
                        modifier = Modifier.testTag("admin_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "لوحة التحكم",
                            tint = MetGhamrGold
                        )
                    }

                    Box {
                        IconButton(
                            onClick = onNotifClick,
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "الإشعارات",
                                tint = Color.White
                            )
                        }
                        if (unreadNotifCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MetGhamrRed)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadNotifCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "الحساب",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetGhamrBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceCard,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val navItems = listOf(
            NavTab("home", "الرئيسية", Icons.Default.Home, Icons.Outlined.Home),
            NavTab("categories", "التصنيفات", Icons.Default.GridView, Icons.Outlined.GridView),
            NavTab("search", "البحث", Icons.Default.Search, Icons.Outlined.Search),
            NavTab("favorites", "المفضلة", Icons.Default.Favorite, Icons.Outlined.FavoriteBorder),
            NavTab("profile", "حسابي", Icons.Default.Person, Icons.Outlined.Person)
        )

        navItems.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MetGhamrNavy,
                    selectedTextColor = MetGhamrNavy,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = MetGhamrGoldLight.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("nav_item_${tab.route}")
            )
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BusinessCard(
    business: BusinessEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("business_card_${business.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge + Verified + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MetGhamrNavy.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = business.categoryName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MetGhamrNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (business.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VerifiedBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "موثق",
                                color = VerifiedBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isFavorite) MetGhamrRed else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = business.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Specialty
            Text(
                text = business.specialty,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Village/Area
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "العنوان",
                    tint = MetGhamrTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${business.area} - ${business.address}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hours & Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Working Status Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (business.isOpenNow) OpenGreen else ClosedRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (business.isOpenNow) "مفتوح الآن (${business.workingHours})" else "مغلق حالياً",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (business.isOpenNow) OpenGreen else ClosedRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "التقييم",
                        tint = MetGhamrGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${business.ratingAverage} (${business.ratingCount})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Call & WhatsApp Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${business.phone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Button
                if (!business.whatsapp.isNull_orEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val url = "https://api.whatsapp.com/send?phone=${business.whatsapp}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        border = ButtonDefaults.outlinedToolboxBorder(color = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "واتساب",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Fallback helper for string extensions
private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()
private fun ButtonDefaults.outlinedToolboxBorder(color: Color) =
    androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
fun RatingStarsDisplay(rating: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val icon = when {
                i <= rating -> Icons.Default.Star
                i - rating < 1.0f -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MetGhamrGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun InteractiveRatingSelector(
    currentRating: Float,
    onRatingSelected: (Float) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingSelected(i.toFloat()) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (i <= currentRating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "تقييم $i",
                    tint = MetGhamrGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ProviderBadge(provider: AuthProvider) {
    val bg = Color(provider.brandColor)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = provider.displayName,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProviderBadge(providerName: String) {
    val provider = try {
        AuthProvider.valueOf(providerName.uppercase())
    } catch (_: Exception) {
        AuthProvider.GOOGLE
    }
    ProviderBadge(provider = provider)
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxValue = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "توزيع المنشآت حسب الفئات الرئيسيـة",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.forEach { (label, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            modifier = Modifier.width(100.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val ratio = count.toFloat() / maxValue
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MetGhamrTeal)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MetGhamrNavy
                            ),
                            modifier = Modifier.width(30.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactFeaturedCard(
    business: BusinessEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("featured_card_${business.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MetGhamrGoldLight.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "مميز ⭐",
                        color = MetGhamrGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isFavorite) MetGhamrRed else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = business.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${business.categoryName} • ${business.specialty}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MetGhamrGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${business.ratingAverage}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = business.area,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun resolveCategoryIcon(iconName: String, categoryId: String? = null): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "Coffee" -> Icons.Default.Coffee
        "MedicalServices" -> Icons.Default.MedicalServices
        "LocalHospital" -> Icons.Default.LocalHospital
        "Biotech" -> Icons.Default.Biotech
        "Handyman" -> Icons.Default.Handyman
        "Factory" -> Icons.Default.Factory
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "LocalPharmacy" -> Icons.Default.LocalPharmacy
        "ShoppingBag", "Storefront" -> Icons.Default.Storefront
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "School" -> Icons.Default.School
        "MenuBook" -> Icons.Default.MenuBook
        "Event" -> Icons.Default.Event
        "AccountBalance" -> Icons.Default.AccountBalance
        "AccountBalanceWallet", "Bank" -> Icons.Default.AccountBalanceWallet
        "Groups" -> Icons.Default.Groups
        "Business" -> Icons.Default.Business
        "VolunteerActivism" -> Icons.Default.VolunteerActivism
        else -> when (categoryId) {
            "cat_restaurants" -> Icons.Default.Restaurant
            "cat_cafes" -> Icons.Default.Coffee
            "cat_doctors" -> Icons.Default.MedicalServices
            "cat_medical_centers" -> Icons.Default.MedicalServices
            "cat_hospitals" -> Icons.Default.LocalHospital
            "cat_radiology" -> Icons.Default.Biotech
            "cat_technicians" -> Icons.Default.Handyman
            "cat_factories" -> Icons.Default.Factory
            "cat_clubs" -> Icons.Default.FitnessCenter
            "cat_pharmacies" -> Icons.Default.LocalPharmacy
            "cat_shops" -> Icons.Default.Storefront
            "cat_automotive" -> Icons.Default.DirectionsCar
            "cat_education", "cat_schools", "cat_universities" -> Icons.Default.School
            "cat_libraries" -> Icons.Default.MenuBook
            "cat_banks" -> Icons.Default.AccountBalanceWallet
            "cat_syndicates" -> Icons.Default.Groups
            "cat_companies" -> Icons.Default.Business
            "cat_charities" -> Icons.Default.VolunteerActivism
            "cat_home_events" -> Icons.Default.Event
            "cat_government" -> Icons.Default.AccountBalance
            else -> Icons.Default.Category
        }
    }
}

@Composable
fun SkeletonBusinessCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BorderLight)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BorderLight)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BorderLight)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BorderLight)
            )
        }
    }
}

