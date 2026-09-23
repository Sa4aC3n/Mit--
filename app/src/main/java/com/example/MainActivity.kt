package com.example

import android.content.Intent
import android.net.Uri
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
import com.example.ui.screens.intro.LampIntroScreen
import com.example.ui.screens.map.InteractiveMapScreen
import com.example.ui.screens.security.OwnerSecurityGateScreen
import com.example.ui.theme.MetGhamrDirectoryTheme
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.NotificationHelper.createNotificationChannels(this)

        // Initialize Firebase App Check (Play Integrity in Release, Debug Provider in Debug)
        com.example.data.security.MetGhamrAppCheckManager.initialize(applicationContext)

        // Initialize FCM & Sync Device Registration Token to Firestore
        try {
            com.example.data.fcm.MetGhamrFirebaseMessagingService.subscribeToDefaultTopics()
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    com.example.data.fcm.MetGhamrFirebaseMessagingService.syncDeviceToken(applicationContext, task.result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MetGhamrMainApp(
    viewModel: DirectoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    // Deep-link intent navigation from push notifications
    LaunchedEffect(activity?.intent) {
        val incomingIntent = activity?.intent
        val bizId = incomingIntent?.getStringExtra("businessId")
            ?: incomingIntent?.getStringExtra(com.example.data.fcm.MetGhamrFirebaseMessagingService.EXTRA_BUSINESS_ID)
        val route = incomingIntent?.getStringExtra("route")

        if (!bizId.isNullOrBlank()) {
            viewModel.selectBusiness(bizId)
            incomingIntent?.removeExtra("businessId")
            incomingIntent?.removeExtra(com.example.data.fcm.MetGhamrFirebaseMessagingService.EXTRA_BUSINESS_ID)
        } else if (!route.isNullOrBlank()) {
            if (route == "notifications") {
                viewModel.navigateTo(ScreenRoute.Notifications.route)
            } else if (route == "home") {
                viewModel.navigateTo(ScreenRoute.Home.route)
            }
            incomingIntent?.removeExtra("route")
        }
    }

    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val isOwnerVerified by viewModel.isOwnerVerified.collectAsState()
    val currentRoute by viewModel.currentRoute.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showLoginPrompt by viewModel.showLoginPrompt.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val allActiveBusinesses by viewModel.allActiveBusinesses.collectAsState()

    val isSyncingNow = isRefreshing || firestoreSyncStatus.isSyncing
    val syncBadgeText = when {
        isSyncingNow -> "مزامنة..."
        firestoreSyncStatus.syncedBusinessesCount > 0 -> "${firestoreSyncStatus.syncedBusinessesCount} نشاط"
        allActiveBusinesses.isNotEmpty() -> "${allActiveBusinesses.size} نشاط"
        else -> "متصل"
    }

    // Security Gate Interceptor: Only if explicitly locked by the verified owner
    if (!isAppUnlocked && isOwnerVerified) {
        OwnerSecurityGateScreen(viewModel = viewModel)
        return
    }

    // BackHandler to handle system back navigation
    BackHandler(enabled = currentRoute != ScreenRoute.Home.route && currentRoute != ScreenRoute.LampIntro.route && currentRoute != ScreenRoute.Login.route) {
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
        ScreenRoute.Emergency.route,
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
        ScreenRoute.Emergency.route -> "أرقام الطوارئ والنجدة"
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
                ScreenRoute.LampIntro.route,
                ScreenRoute.Login.route,
                ScreenRoute.EmailVerification.route,
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
                ScreenRoute.AdminDataCollector.route,
                ScreenRoute.AiAssistant.route,
                ScreenRoute.AddBusiness.route,
                ScreenRoute.InteractiveMap.route,
                ScreenRoute.Notifications.route,
                ScreenRoute.SuggestEdit.route,
                ScreenRoute.ReportIncorrectData.route,
                ScreenRoute.MyContributions.route,
                ScreenRoute.ContributionDetail.route
            )
            if (currentRoute in selfTopBarRoutes) {
                // These screens render their own TopAppBar internally
            } else if (showBottomBar) {
                MetGhamrTopAppBar(
                    title = "دليل ميت غمر",
                    isSecondaryScreen = false,
                    unreadNotifCount = unreadNotifCount,
                    showAdminControls = isOwnerVerified,
                    syncBadgeText = syncBadgeText,
                    isSyncing = isSyncingNow,
                    onSyncClick = { viewModel.refreshData() },
                    onNotifClick = { viewModel.navigateTo(ScreenRoute.Notifications.route) },
                    onAdminClick = {
                        if (isOwnerVerified) {
                            viewModel.navigateTo(ScreenRoute.AdminDashboard.route)
                        }
                    },
                    onAboutClick = { viewModel.navigateTo(ScreenRoute.AboutApp.route) },
                    onProfileClick = { viewModel.navigateTo(ScreenRoute.UserProfile.route) },
                    onLockClick = if (isOwnerVerified) { { viewModel.lockApp() } } else null
                )
            } else {
                MetGhamrTopAppBar(
                    title = screenTitle,
                    isSecondaryScreen = true,
                    unreadNotifCount = unreadNotifCount,
                    showAdminControls = isOwnerVerified,
                    syncBadgeText = syncBadgeText,
                    isSyncing = isSyncingNow,
                    onSyncClick = { viewModel.refreshData() },
                    onBackClick = { viewModel.popBackStack() },
                    onAboutClick = { viewModel.navigateTo(ScreenRoute.AboutApp.route) },
                    onLockClick = if (isOwnerVerified) { { viewModel.lockApp() } } else null
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
                ScreenRoute.LampIntro.route -> {
                    LampIntroScreen(
                        onIntroComplete = {
                            viewModel.navigateTo(ScreenRoute.Home.route, clearBackStack = true)
                        }
                    )
                }

                ScreenRoute.Login.route -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            if (!viewModel.popBackStack()) {
                                viewModel.navigateTo(ScreenRoute.Home.route)
                            }
                        },
                        onContinueAsGuest = {
                            viewModel.navigateTo(ScreenRoute.Home.route, clearBackStack = true)
                        }
                    )
                }

                ScreenRoute.EmailVerification.route -> {
                    EmailVerificationScreen(
                        viewModel = viewModel,
                        onLoginClick = {
                            viewModel.navigateTo(ScreenRoute.Login.route)
                        },
                        onBackClick = {
                            viewModel.popBackStack()
                        }
                    )
                }

                ScreenRoute.Home.route -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { viewModel.navigateTo(ScreenRoute.Search.route) },
                        onNavigateToCategories = { viewModel.navigateTo(ScreenRoute.Categories.route) },
                        onNavigateToAiAssistant = { viewModel.navigateTo(ScreenRoute.AiAssistant.route) },
                        onNavigateToMap = { viewModel.navigateTo(ScreenRoute.InteractiveMap.route) }
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

                ScreenRoute.Emergency.route -> {
                    EmergencyScreen(
                        onCallClick = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
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
                    if (isOwnerVerified) {
                        AdminDashboardScreen(viewModel = viewModel)
                    } else {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { viewModel.navigateTo(ScreenRoute.Search.route) },
                            onNavigateToCategories = { viewModel.navigateTo(ScreenRoute.Categories.route) },
                            onNavigateToAiAssistant = { viewModel.navigateTo(ScreenRoute.AiAssistant.route) },
                            onNavigateToMap = { viewModel.navigateTo(ScreenRoute.InteractiveMap.route) }
                        )
                    }
                }

                ScreenRoute.AdminBusinesses.route -> {
                    if (isOwnerVerified) AdminBusinessesScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminCategories.route -> {
                    if (isOwnerVerified) AdminCategoriesScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminUsers.route -> {
                    if (isOwnerVerified) AdminUsersScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminReviews.route -> {
                    if (isOwnerVerified) AdminReviewsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminContributions.route -> {
                    if (isOwnerVerified) AdminContributionsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminReports.route -> {
                    if (isOwnerVerified) AdminReportsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminNotifications.route -> {
                    if (isOwnerVerified) AdminNotificationsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminAnalytics.route -> {
                    if (isOwnerVerified) AdminAnalyticsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminAuditLogs.route -> {
                    if (isOwnerVerified) AdminAuditLogsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminSettings.route -> {
                    if (isOwnerVerified) AdminSettingsScreen(viewModel = viewModel)
                }

                ScreenRoute.AdminDataManagement.route -> {
                    if (isOwnerVerified) {
                        AdminDataManagementScreen(
                            viewModel = viewModel,
                            onBackClick = { viewModel.popBackStack() }
                        )
                    }
                }

                ScreenRoute.AdminDataCollector.route -> {
                    if (isOwnerVerified) {
                        AdminDataCollectorScreen(
                            viewModel = viewModel,
                            onBackClick = { viewModel.popBackStack() }
                        )
                    }
                }

                ScreenRoute.AiAssistant.route -> {
                    com.example.ui.screens.ai.AiAssistantScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() },
                        onNavigateToDetail = { id -> viewModel.selectBusiness(id) }
                    )
                }

                ScreenRoute.InteractiveMap.route -> {
                    InteractiveMapScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.popBackStack() }
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
                        onNavigateToAiAssistant = { viewModel.navigateTo(ScreenRoute.AiAssistant.route) },
                        onNavigateToMap = { viewModel.navigateTo(ScreenRoute.InteractiveMap.route) }
                    )
                }
            }
        }
    }
}
