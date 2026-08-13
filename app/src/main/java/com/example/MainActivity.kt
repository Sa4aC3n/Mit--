package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LoginPromptDialog
import com.example.ui.components.MetGhamrBottomNavBar
import com.example.ui.components.MetGhamrTopAppBar
import com.example.ui.screens.*
import com.example.ui.screens.admin.*
import com.example.ui.theme.MetGhamrDirectoryTheme
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("healthCheck").setValue("Met Ghamr Directory Connected")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MetGhamrDirectoryTheme {
                MetGhamrMainApp()
            }
        }
    }
}

@Composable
fun MetGhamrMainApp(
    viewModel: DirectoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentRoute by viewModel.currentRoute.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showLoginPrompt by viewModel.showLoginPrompt.collectAsState()

    // BackHandler to handle system back navigation
    BackHandler(enabled = currentRoute != ScreenRoute.Home.route) {
        viewModel.popBackStack()
    }

    // Toast messages display
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val mainTabs = listOf(
        ScreenRoute.Home.route,
        ScreenRoute.Categories.route,
        ScreenRoute.Search.route,
        ScreenRoute.Favorites.route,
        ScreenRoute.UserProfile.route
    )

    val showBottomBar = currentRoute in mainTabs && currentRoute != ScreenRoute.Login.route

    val screenTitle = when (currentRoute) {
        ScreenRoute.Login.route -> "تسجيل الدخول"
        ScreenRoute.BusinessDetail.route -> "تفاصيل المنشأة"
        ScreenRoute.Reviews.route -> "التقييمات والمراجعات"
        ScreenRoute.WriteReview.route -> "كتابة تقييم"
        ScreenRoute.MyReviews.route -> "تقييماتي ومراجعاتي"
        ScreenRoute.Notifications.route -> "مركز الإشعارات"
        ScreenRoute.AdminDashboard.route -> "لوحة التحكم والإدارة"
        ScreenRoute.AboutApp.route -> "عن تطبيق دليل ميت غمر"
        ScreenRoute.UserProfile.route -> "الملف الشخصي والتوثيق"
        ScreenRoute.Categories.route -> "تصنيفات دليل ميت غمر"
        ScreenRoute.Search.route -> "البحث الشامل في ميت غمر"
        ScreenRoute.Favorites.route -> "الأنشطة المفضلة"
        else -> "دليل ميت غمر"
    }

    // Login Prompt Modal Dialog for Guest protected actions
    if (showLoginPrompt) {
        LoginPromptDialog(
            onDismiss = { viewModel.dismissLoginPrompt() },
            onLoginClick = {
                viewModel.dismissLoginPrompt()
                viewModel.navigateTo(ScreenRoute.Login.route)
            },
            onProviderLogin = { provider ->
                viewModel.loginWithProvider(provider)
            }
        )
    }

    Scaffold(
        topBar = {
            val selfTopBarRoutes = listOf(
                ScreenRoute.Login.route,
                ScreenRoute.BusinessDetail.route,
                ScreenRoute.Reviews.route,
                ScreenRoute.WriteReview.route,
                ScreenRoute.MyReviews.route,
                ScreenRoute.AdminDashboard.route,
                ScreenRoute.AdminBusinesses.route,
                ScreenRoute.AdminCategories.route,
                ScreenRoute.AdminUsers.route,
                ScreenRoute.AdminReviews.route,
                ScreenRoute.AdminContributions.route,
                ScreenRoute.AdminReports.route,
                ScreenRoute.AdminNotifications.route,
                ScreenRoute.AdminAnalytics.route,
                ScreenRoute.AdminAuditLogs.route,
                ScreenRoute.AdminSettings.route,
                ScreenRoute.AdminDataManagement.route,
                ScreenRoute.AiAssistant.route
            )
            if (currentRoute in selfTopBarRoutes) {
                // These screens render their own TopAppBar internally
            } else if (showBottomBar) {
                MetGhamrTopAppBar(
                    title = "دليل ميت غمر",
                    isSecondaryScreen = false,
                    unreadNotifCount = unreadNotifCount,
                    onNotifClick = { viewModel.navigateTo(ScreenRoute.Notifications.route) },
                    onAdminClick = { viewModel.navigateTo(ScreenRoute.AdminDashboard.route) },
                    onAboutClick = { viewModel.navigateTo(ScreenRoute.AboutApp.route) },
                    onProfileClick = { viewModel.navigateTo(ScreenRoute.UserProfile.route) }
                )
            } else {
                MetGhamrTopAppBar(
                    title = screenTitle,
                    isSecondaryScreen = true,
                    unreadNotifCount = unreadNotifCount,
                    onBackClick = { viewModel.popBackStack() },
                    onAboutClick = { viewModel.navigateTo(ScreenRoute.AboutApp.route) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                MetGhamrBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> viewModel.navigateTo(route) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                ScreenRoute.Login.route -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onContinueAsGuest = {
                            viewModel.popBackStack()
                        }
                    )
                }

                ScreenRoute.Home.route -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { viewModel.navigateTo(ScreenRoute.Search.route) },
                        onNavigateToCategories = { viewModel.navigateTo(ScreenRoute.Categories.route) }
                    )
                }

                ScreenRoute.Categories.route -> {
                    CategoriesScreen(
                        viewModel = viewModel,
                        onCategorySelected = {
                            viewModel.navigateTo(ScreenRoute.Home.route)
                        }
                    )
                }

                ScreenRoute.Search.route -> {
                    SearchDirectoryScreen(viewModel = viewModel)
                }

                ScreenRoute.Favorites.route -> {
                    FavoritesScreen(viewModel = viewModel)
                }

                ScreenRoute.BusinessDetail.route -> {
                    BusinessDetailScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.Reviews.route -> {
                    ReviewsScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onWriteReviewClick = { viewModel.navigateTo(ScreenRoute.WriteReview.route) }
                    )
                }

                ScreenRoute.WriteReview.route -> {
                    WriteReviewScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.MyReviews.route -> {
                    MyReviewsScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onNavigateToBusiness = { businessId ->
                            viewModel.selectBusiness(businessId)
                        }
                    )
                }

                ScreenRoute.AddBusiness.route -> {
                    AddBusinessScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onContributionSubmitted = { contribId ->
                            viewModel.selectContribution(contribId)
                        }
                    )
                }

                ScreenRoute.SuggestEdit.route -> {
                    SuggestEditScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.ReportIncorrectData.route -> {
                    ReportIncorrectDataScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.MyContributions.route -> {
                    MyContributionsScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onNavigateToAddBusiness = { viewModel.navigateTo(ScreenRoute.AddBusiness.route) }
                    )
                }

                ScreenRoute.ContributionDetail.route -> {
                    ContributionDetailScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.Notifications.route -> {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.AdminDashboard.route -> {
                    AdminDashboardScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminBusinesses.route -> {
                    AdminBusinessesScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminCategories.route -> {
                    AdminCategoriesScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminUsers.route -> {
                    AdminUsersScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminReviews.route -> {
                    AdminReviewsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminContributions.route -> {
                    AdminContributionsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminReports.route -> {
                    AdminReportsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminNotifications.route -> {
                    AdminNotificationsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminAnalytics.route -> {
                    AdminAnalyticsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminAuditLogs.route -> {
                    AdminAuditLogsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminSettings.route -> {
                    AdminSettingsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminDataManagement.route -> {
                    AdminDataManagementScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.AiAssistant.route -> {
                    com.example.ui.screens.ai.AiAssistantScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onNavigateToDetail = { id -> viewModel.selectBusiness(id) }
                    )
                }

                ScreenRoute.AboutApp.route -> {
                    AboutAppScreen(
                        onBackClick = { viewModel.popBackStack() }
                    )
                }

                ScreenRoute.UserProfile.route -> {
                    UserProfileScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onNavigateToFavorites = { viewModel.navigateTo(ScreenRoute.Favorites.route) },
                        onNavigateToAbout = { viewModel.navigateTo(ScreenRoute.AboutApp.route) }
                    )
                }

                else -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { viewModel.navigateTo(ScreenRoute.Search.route) },
                        onNavigateToCategories = { viewModel.navigateTo(ScreenRoute.Categories.route) },
                        onNavigateToAiAssistant = { viewModel.navigateTo(ScreenRoute.AiAssistant.route) }
                    )
                }
            }
        }
    }
}
