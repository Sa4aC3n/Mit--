package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.model.seed.InitialDataSeed
import com.example.data.firebase.CloudSyncStatus
import com.example.data.repository.DirectoryRepository
import com.example.data.security.PinVerifyResult
import com.example.data.security.SecurityAuditEntry
import com.example.util.ArabicNormalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SearchSortOption(val titleAr: String) {
    RELEVANCE("الأكثر صلة"),
    RATING("الأعلى تقييمًا"),
    NEWEST("الأحدث"),
    REVIEWS_COUNT("الأكثر تقييمات")
}

sealed class ScreenRoute(
    val route: String,
    val title: String,
    val isBottomTab: Boolean = false
) {
    data object LampIntro : ScreenRoute("lamp_intro", "بداية التطبيق")
    data object Splash : ScreenRoute("splash", "ترحيب")
    data object Login : ScreenRoute("login", "تسجيل الدخول")
    data object EmailVerification : ScreenRoute("email_verification", "تأكيد البريد الإلكتروني")
    data object Home : ScreenRoute("home", "الرئيسية", isBottomTab = true)
    data object Categories : ScreenRoute("categories", "التصنيفات", isBottomTab = true)
    data object Emergency : ScreenRoute("emergency", "الطوارئ", isBottomTab = true)
    data object Search : ScreenRoute("search", "البحث", isBottomTab = true)
    data object Favorites : ScreenRoute("favorites", "المفضلة", isBottomTab = true)
    data object UserProfile : ScreenRoute("profile", "حسابي", isBottomTab = true)

    data object BusinessDetail : ScreenRoute("detail", "تفاصيل المنشأة")
    data object Reviews : ScreenRoute("reviews", "التقييمات والمراجعات")
    data object WriteReview : ScreenRoute("write_review", "كتابة تقييم")
    data object MyReviews : ScreenRoute("my_reviews", "تقييماتي ومراجعاتي")
    data object AddBusiness : ScreenRoute("add_business", "إضافة نشاط تجاري")
    data object SuggestEdit : ScreenRoute("suggest_edit", "اقتراح تعديل بيانات")
    data object ReportIncorrectData : ScreenRoute("report_data", "الإبلاغ عن خطأ")
    data object MyContributions : ScreenRoute("my_contributions", "مساهماتي")
    data object ContributionDetail : ScreenRoute("contribution_detail", "تفاصيل المساهمة")
    data object Notifications : ScreenRoute("notifications", "الإشعارات")
    data object AdminDashboard : ScreenRoute("admin_dashboard", "لوحة التحكم الرئيسية")
    data object AdminBusinesses : ScreenRoute("admin_businesses", "إدارة الأنشطة")
    data object AdminCategories : ScreenRoute("admin_categories", "إدارة التصنيفات")
    data object AdminUsers : ScreenRoute("admin_users", "إدارة المستخدمين")
    data object AdminReviews : ScreenRoute("admin_reviews", "التقييمات والمراجعات")
    data object AdminContributions : ScreenRoute("admin_contributions", "إدارة المساهمات")
    data object AdminReports : ScreenRoute("admin_reports", "إدارة البلاغات")
    data object AdminNotifications : ScreenRoute("admin_notifications", "إرسال الإشعارات")
    data object AdminAnalytics : ScreenRoute("admin_analytics", "التحليلات وجودة البيانات")
    data object AdminAuditLogs : ScreenRoute("admin_audit_logs", "سجل النشاط الإداري")
    data object AdminSettings : ScreenRoute("admin_settings", "إعدادات المشرفين والأدوار")
    data object AdminDataManagement : ScreenRoute("admin_data_management", "إدارة البيانات والنسخ الاحتياطي")
    data object AdminDataCollector : ScreenRoute("admin_data_collector", "محرك جمع وإثراء البيانات")
    data object AiAssistant : ScreenRoute("ai_assistant", "مساعد ميت غمر الذكي")
    data object InteractiveMap : ScreenRoute("interactive_map", "خريطة الخدمات والمستشفيات")
    data object AboutApp : ScreenRoute("about", "عن التطبيق")
    data object Settings : ScreenRoute("settings", "الإعدادات")
}

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DirectoryRepository = DirectoryRepository(
        AppDatabase.getInstance(application).directoryDao(),
        application
    )

    val categories: StateFlow<List<CategoryItem>> = repository.categories
    val categoriesList: List<CategoryItem> get() = repository.categories.value

    // --- State Holders ---
    val authState: StateFlow<AuthState> = repository.authState
    val currentUser: StateFlow<UserAccount?> = repository.currentUser

    // Security & Owner Protection Gate
    val isAppUnlocked: StateFlow<Boolean> = repository.isAppUnlocked
    val failedPinAttempts: StateFlow<Int> = repository.failedPinAttempts
    val lockoutRemainingSeconds: StateFlow<Int> = repository.lockoutRemainingSeconds
    val securityLogs: StateFlow<List<SecurityAuditEntry>> = repository.securityLogs

    // Cloud Synchronization State (Firestore Source of Truth & Realtime)
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = repository.cloudSyncStatus
    val firestoreSyncStatus: StateFlow<com.example.data.firebase.FirestoreSyncStatus> = repository.firestoreSyncStatus

    fun triggerIncrementalFirestoreSync(forceFull: Boolean = false, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.triggerIncrementalFirestoreSync(forceFull)
            res.onSuccess { count ->
                val msg = if (count > 0) "تمت مزامنة $count سجل سحابي بنجاح ☁️⚡" else "البيانات السحابية متطابقة ومحدثة بالكامل ✅"
                showToast(msg)
                onComplete(true, msg)
            }.onFailure { err ->
                val msg = "تعذر إتمام المزامنة السحابية: ${err.localizedMessage}"
                showToast(msg)
                onComplete(false, msg)
            }
        }
    }

    val isOwnerVerified: StateFlow<Boolean> = repository.currentUser
        .map { repository.isOwnerAccount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isOwnerAccount(repository.currentUser.value))

    fun verifyPin(enteredPin: String): PinVerifyResult {
        val result = repository.verifyMasterPin(enteredPin)
        if (result is PinVerifyResult.Success) {
            showToast("تم التحقق بنجاح من هوية المالك ورمز PIN 🔐✨")
        }
        return result
    }

    fun lockApp() {
        repository.lockApp()
        showToast("تم قفل التطبيق برمز PIN بنجاح 🔒")
    }

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    private var pendingAction: (() -> Unit)? = null

    private val _currentRoute = MutableStateFlow<String>(ScreenRoute.LampIntro.route)
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedBusinessId = MutableStateFlow<String?>(null)
    val selectedBusinessId: StateFlow<String?> = _selectedBusinessId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val selectedCategory: StateFlow<CategoryItem?> = _selectedCategory.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow<SubcategoryItem?>(null)
    val selectedSubcategory: StateFlow<SubcategoryItem?> = _selectedSubcategory.asStateFlow()

    private val _selectedArea = MutableStateFlow("الكل")
    val selectedArea: StateFlow<String> = _selectedArea.asStateFlow()

    private val _openNowOnly = MutableStateFlow(false)
    val openNowOnly: StateFlow<Boolean> = _openNowOnly.asStateFlow()

    private val _verifiedOnly = MutableStateFlow(false)
    val verifiedOnly: StateFlow<Boolean> = _verifiedOnly.asStateFlow()

    private val _minRating = MutableStateFlow(0f)
    val minRating: StateFlow<Float> = _minRating.asStateFlow()

    private val _searchSortOption = MutableStateFlow(SearchSortOption.RELEVANCE)
    val searchSortOption: StateFlow<SearchSortOption> = _searchSortOption.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("مطاعم مشويات", "أطباء أطفال", "صيدلية 24 ساعة", "كهربائي منازل", "عيادة أسنان")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminAnalytics?>(null)
    val adminStats: StateFlow<AdminAnalytics?> = _adminStats.asStateFlow()

    init {
        loadAdminAnalytics()
    }

    fun requestProtectedAction(action: () -> Unit) {
        if (currentUser.value != null) {
            action()
        } else {
            pendingAction = action
            _showLoginPrompt.value = true
        }
    }

    fun dismissLoginPrompt() {
        _showLoginPrompt.value = false
        pendingAction = null
    }

    // --- Data Flows ---
    val favorites: StateFlow<List<FavoriteEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allBusinessesAdmin: StateFlow<List<BusinessEntity>> = repository.allBusinessesAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveBusinesses: StateFlow<List<BusinessEntity>> = repository.allActiveBusinesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesWithCounts: StateFlow<List<CategoryItem>> = combine(
        repository.categories,
        allActiveBusinesses
    ) { catList, activeList ->
        val countsMap = activeList.groupingBy { it.categoryId }.eachCount()
        catList.map { cat ->
            val realCount = countsMap[cat.id] ?: 0
            val subcatsWithCounts = cat.subcategories.map { sub ->
                val subCount = activeList.count { b ->
                    b.categoryId == cat.id && (
                        b.specialty.contains(sub.nameAr, ignoreCase = true) ||
                        sub.keywords.any { kw -> b.specialty.contains(kw, ignoreCase = true) || b.description.contains(kw, ignoreCase = true) }
                    )
                }
                sub.copy(count = subCount)
            }
            cat.copy(
                count = if (realCount > 0) realCount else cat.count,
                subcategories = subcatsWithCounts
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialDataSeed.categories)

    fun saveCategory(category: CategoryItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveCategory(category)
            showToast("تم حفظ وتحديث تصنيف «${category.nameAr}» بنجاح ✅✨")
            onSuccess()
        }
    }

    fun deleteCategory(categoryId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteCategory(categoryId)
            if (success) {
                showToast("تم حذف التصنيف بنجاح 🗑️")
                onSuccess()
            } else {
                showToast("تعذر العثور على التصنيف المطلوب")
            }
        }
    }

    fun resetCategoriesToDefault() {
        viewModelScope.launch {
            repository.resetCategoriesToDefault()
            showToast("تمت استعادة التصنيفات الافتراضية بنجاح 🔄")
        }
    }

    val featuredBusinesses: StateFlow<List<BusinessEntity>> = allActiveBusinesses
        .map { list -> list.filter { it.isVerified || it.ratingAverage >= 4.5f }.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topRatedBusinesses: StateFlow<List<BusinessEntity>> = allActiveBusinesses
        .map { list -> list.sortedWith(compareByDescending<BusinessEntity> { it.ratingAverage }.thenByDescending { it.ratingCount }).take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedBusinesses: StateFlow<List<BusinessEntity>> = allActiveBusinesses
        .map { list -> list.sortedByDescending { it.updatedAt }.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshData(onComplete: (Boolean, Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.triggerIncrementalFirestoreSync(forceFull = true)
            _isRefreshing.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _toastMessage.value = if (count > 0) {
                    "تم جلب وتحديث $count نشاط من سحابة Firestore بنجاح ☁️✨"
                } else {
                    "تم الاتصال بالسحابة - كافة الأنشطة محدثة بنجاح ✨"
                }
                onComplete(true, count)
            } else {
                _toastMessage.value = "تعذر الاتصال بالسحابة - تم عرض البيانات المخزنة محلياً ⚡"
                onComplete(false, 0)
            }
        }
    }

    fun uploadCategoriesToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.uploadCategoriesToFirestore()
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                _toastMessage.value = "تم رفع $count تصنيف إلى Firestore بنجاح ☁️✅"
                onComplete(true, "تم رفع $count تصنيف بنجاح")
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "حدث خطأ"
                _toastMessage.value = "فشل الرفع: $err"
                onComplete(false, err)
            }
        }
    }

    fun uploadAllBusinessesToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.uploadAllBusinessesToFirestore()
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                _toastMessage.value = "تم رفع $count نشاط إلى Firestore بنجاح ☁️✅"
                onComplete(true, "تم رفع $count نشاط بنجاح")
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "حدث خطأ"
                _toastMessage.value = "فشل الرفع: $err"
                onComplete(false, err)
            }
        }
    }

    val allReviewsAdmin: StateFlow<List<ReviewEntity>> = repository.getAllReviewsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Businesses Stream
    @Suppress("UNCHECKED_CAST")
    val filteredBusinesses: StateFlow<List<BusinessEntity>> = combine(
        repository.allActiveBusinesses,
        _searchQuery,
        _selectedCategory,
        _selectedSubcategory,
        _selectedArea,
        _openNowOnly,
        _verifiedOnly,
        _minRating,
        _searchSortOption
    ) { flows: Array<Any?> ->
        val businesses = flows[0] as List<BusinessEntity>
        val query = flows[1] as String
        val category = flows[2] as CategoryItem?
        val subcategory = flows[3] as SubcategoryItem?
        val area = flows[4] as String
        val openNow = flows[5] as Boolean
        val verified = flows[6] as Boolean
        val ratingThreshold = flows[7] as Float
        val sortOption = flows[8] as SearchSortOption

        val filtered = businesses.filter { b ->
            val matchesQuery = query.isBlank() ||
                    ArabicNormalizer.matches(b.name, query) ||
                    ArabicNormalizer.matches(b.specialty, query) ||
                    ArabicNormalizer.matches(b.categoryName, query) ||
                    ArabicNormalizer.matches(b.city, query) ||
                    ArabicNormalizer.matches(b.area, query) ||
                    ArabicNormalizer.matches(b.address, query) ||
                    ArabicNormalizer.matches(b.description, query) ||
                    ArabicNormalizer.matches(b.phone, query) ||
                    (ArabicNormalizer.normalizePhone(query).isNotEmpty() &&
                            ArabicNormalizer.normalizePhone(b.phone).contains(ArabicNormalizer.normalizePhone(query)))

            val matchesCategory = category == null || b.categoryId == category.id
            val matchesSubcategory = subcategory == null || 
                    ArabicNormalizer.matches(b.specialty, subcategory.nameAr) ||
                    subcategory.keywords.any { kw -> ArabicNormalizer.matches(b.specialty, kw) || ArabicNormalizer.matches(b.description, kw) }

            val matchesArea = area == "الكل" || b.area.contains(area) || b.city.contains(area)
            val matchesOpenNow = !openNow || com.example.util.WorkingHoursUtils.isBusinessOpenNow(b.workingHours)
            val matchesVerified = !verified || b.isVerified
            val matchesRating = b.ratingAverage >= ratingThreshold

            matchesQuery && matchesCategory && matchesSubcategory && matchesArea && matchesOpenNow && matchesVerified && matchesRating
        }

        // Apply Sorting
        when (sortOption) {
            SearchSortOption.RELEVANCE -> {
                if (query.isBlank()) {
                    filtered.sortedWith(compareByDescending<BusinessEntity> { it.isVerified }.thenByDescending { it.ratingAverage })
                } else {
                    filtered.sortedWith(
                        compareByDescending<BusinessEntity> { b ->
                            ArabicNormalizer.calculateRelevanceScore(
                                businessName = b.name,
                                specialty = b.specialty,
                                categoryName = b.categoryName,
                                area = b.area,
                                description = b.description,
                                phone = b.phone,
                                query = query
                            )
                        }.thenByDescending { it.ratingAverage }
                    )
                }
            }
            SearchSortOption.RATING -> {
                filtered.sortedWith(compareByDescending<BusinessEntity> { it.ratingAverage }.thenByDescending { it.ratingCount })
            }
            SearchSortOption.NEWEST -> {
                filtered.sortedByDescending { it.updatedAt }
            }
            SearchSortOption.REVIEWS_COUNT -> {
                filtered.sortedByDescending { it.ratingCount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Business & Reviews
    val selectedBusiness: StateFlow<BusinessEntity?> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getBusinessById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedBusinessReviews: StateFlow<List<ReviewEntity>> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getReviewsForBusiness(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSelectedBusinessFavorite: StateFlow<Boolean> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(false) else repository.isFavorite(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val relatedBusinesses: StateFlow<List<BusinessEntity>> = selectedBusiness
        .flatMapLatest { business ->
            if (business == null) flowOf(emptyList())
            else repository.getBusinessesByCategory(business.categoryId)
                .map { list ->
                    list.filter { it.id != business.id }
                        .sortedWith(
                            compareByDescending<BusinessEntity> { it.specialty.contains(business.specialty, ignoreCase = true) }
                                .thenByDescending { it.area == business.area }
                                .thenByDescending { it.ratingAverage }
                        )
                        .take(6)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions & Navigation History ---
    private val navigationHistory = java.util.ArrayDeque<String>().apply { add(ScreenRoute.LampIntro.route) }

    fun navigateTo(route: String, clearBackStack: Boolean = false) {
        if (clearBackStack) {
            navigationHistory.clear()
        }
        if (navigationHistory.peekLast() != route) {
            navigationHistory.addLast(route)
        }
        _currentRoute.value = route
    }

    fun popBackStack(): Boolean {
        if (navigationHistory.size > 1) {
            navigationHistory.removeLast()
            val previousRoute = navigationHistory.peekLast() ?: ScreenRoute.Home.route
            _currentRoute.value = previousRoute
            return true
        }
        return false
    }

    fun selectBusiness(id: String) {
        _selectedBusinessId.value = id
        navigateTo(ScreenRoute.BusinessDetail.route)
        viewModelScope.launch {
            repository.incrementView(id)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMinRating(rating: Float) {
        _minRating.value = rating
    }

    fun setSortOption(option: SearchSortOption) {
        _searchSortOption.value = option
    }

    fun toggleOpenNow() {
        _openNowOnly.value = !_openNowOnly.value
    }

    fun toggleVerified() {
        _verifiedOnly.value = !_verifiedOnly.value
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            val updated = (_recentSearches.value.filterNot { it.equals(trimmed, ignoreCase = true) }).toMutableList()
            updated.add(0, trimmed)
            _recentSearches.value = updated.take(10)
        }
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filterNot { it == query }
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedSubcategory.value = null
        _selectedArea.value = "الكل"
        _openNowOnly.value = false
        _verifiedOnly.value = false
        _minRating.value = 0f
        _searchSortOption.value = SearchSortOption.RELEVANCE
    }

    fun selectCategory(category: CategoryItem?) {
        _selectedCategory.value = category
        _selectedSubcategory.value = null
    }

    fun selectSubcategory(subcategory: SubcategoryItem?) {
        _selectedSubcategory.value = subcategory
    }

    fun selectArea(area: String) {
        _selectedArea.value = area
    }

    fun toggleOpenNowFilter() {
        _openNowOnly.value = !_openNowOnly.value
    }

    fun toggleVerifiedFilter() {
        _verifiedOnly.value = !_verifiedOnly.value
    }

    fun toggleFavorite(businessId: String, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(businessId, currentFav)
            showToast(if (!currentFav) "تمت الإضافة للمفضلة ❤️" else "تم الحذف من المفضلة")
        }
    }

    // User's My Reviews Flow
    val myReviews: StateFlow<List<ReviewEntity>> = repository.myReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitReview(rating: Float, comment: String, onComplete: (Boolean) -> Unit = {}) {
        val businessId = _selectedBusinessId.value ?: return
        viewModelScope.launch {
            val result = repository.submitReview(businessId, rating, comment)
            if (result.isSuccess) {
                showToast("شكراً لك! تم حفظ ونشر تقييمك بنجاح ⭐")
                onComplete(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "تعذر إرسال التقييم"
                showToast(errorMsg)
                onComplete(false)
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val success = repository.deleteReview(reviewId)
            if (success) {
                showToast("تم حذف تقييمك بنجاح")
            } else {
                showToast("تعذر حذف التقييم")
            }
        }
    }

    fun toggleHelpfulVote(reviewId: String) {
        requestProtectedAction {
            viewModelScope.launch {
                val isVotedNow = repository.toggleHelpfulVote(reviewId)
                if (isVotedNow) {
                    showToast("شكراً لك! تم تسجيل إعجابك بالمراجعة 👍")
                } else {
                    showToast("تم إلغاء الإعجاب بالمراجعة")
                }
            }
        }
    }

    fun hasUserVotedHelpful(reviewId: String): Flow<Boolean> {
        return repository.hasUserVotedHelpful(reviewId)
    }

    fun reportReview(reviewId: String, reason: String, description: String, onComplete: () -> Unit = {}) {
        requestProtectedAction {
            viewModelScope.launch {
                val result = repository.reportReview(reviewId, reason, description)
                if (result.isSuccess) {
                    showToast("شكراً لك! تم إرسال البلاغ وسيتم مراجعته من قِبل الإدارة 🚩")
                    onComplete()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "تعذر إرسال البلاغ"
                    showToast(errorMsg)
                }
            }
        }
    }

    fun getRatingDistribution(businessId: String, callback: (RatingDistribution) -> Unit) {
        viewModelScope.launch {
            val dist = repository.getRatingDistribution(businessId)
            callback(dist)
        }
    }

    fun getUserReviewForBusiness(businessId: String, callback: (ReviewEntity?) -> Unit) {
        viewModelScope.launch {
            val review = repository.getUserReviewForBusiness(businessId)
            callback(review)
        }
    }

    // Email Verification & Email Authentication State
    val pendingVerificationEmail = MutableStateFlow<String>("")
    val isAuthLoading = MutableStateFlow<Boolean>(false)
    val authErrorMessage = MutableStateFlow<String?>(null)

    fun clearAuthError() {
        authErrorMessage.value = null
    }

    fun setPendingVerificationEmail(email: String) {
        pendingVerificationEmail.value = email
    }

    fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()
        val cleanName = displayName.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            authErrorMessage.value = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
            showToast("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            return
        }
        if (cleanPass.length < 6) {
            authErrorMessage.value = "كلمة المرور يجب أن لا تقل عن 6 أحرف"
            showToast("كلمة المرور يجب أن لا تقل عن 6 أحرف")
            return
        }

        viewModelScope.launch {
            isAuthLoading.value = true
            authErrorMessage.value = null
            val result = repository.registerWithEmail(cleanEmail, cleanPass, cleanName)
            isAuthLoading.value = false
            when (result) {
                is EmailAuthResult.VerificationRequired -> {
                    pendingVerificationEmail.value = result.email
                    showToast("تم إرسال رسالة التحقق إلى ${result.email} ✉️")
                    navigateTo(ScreenRoute.EmailVerification.route)
                    onSuccess()
                }
                is EmailAuthResult.Error -> {
                    authErrorMessage.value = result.message
                    showToast(result.message)
                }
                is EmailAuthResult.Success -> {
                    pendingVerificationEmail.value = result.user.email
                    navigateTo(ScreenRoute.EmailVerification.route)
                    onSuccess()
                }
            }
        }
    }

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            authErrorMessage.value = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
            showToast("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            return
        }

        viewModelScope.launch {
            isAuthLoading.value = true
            authErrorMessage.value = null
            val result = repository.loginWithEmail(cleanEmail, cleanPass)
            isAuthLoading.value = false
            when (result) {
                is EmailAuthResult.Success -> {
                    showToast("أهلاً بك! تم تسجيل الدخول بنجاح 👋")
                    _showLoginPrompt.value = false
                    val actionToRun = pendingAction
                    pendingAction = null
                    actionToRun?.invoke()
                    if (_currentRoute.value == ScreenRoute.Login.route) {
                        navigateTo(ScreenRoute.Home.route, clearBackStack = true)
                    } else {
                        popBackStack()
                    }
                    onSuccess()
                }
                is EmailAuthResult.VerificationRequired -> {
                    pendingVerificationEmail.value = result.email
                    showToast("البريد الإلكتروني غير مؤكد. يرجى تفعيل الحساب.")
                    navigateTo(ScreenRoute.EmailVerification.route)
                }
                is EmailAuthResult.Error -> {
                    authErrorMessage.value = result.message
                    showToast(result.message)
                }
            }
        }
    }

    fun resendVerificationEmail(email: String = "") {
        val targetEmail = email.trim().ifBlank { pendingVerificationEmail.value.trim() }
        if (targetEmail.isBlank()) {
            showToast("البريد الإلكتروني غير محدد")
            return
        }
        viewModelScope.launch {
            isAuthLoading.value = true
            val result = repository.resendVerificationEmail(targetEmail)
            isAuthLoading.value = false
            if (result.isSuccess) {
                showToast("تم إعادة إرسال رابط التحقق إلى $targetEmail بنجاح ✉️")
            } else {
                showToast("تعذر إعادة إرسال الرابط: ${result.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val targetEmail = email.trim().ifBlank { pendingVerificationEmail.value.trim() }
        if (targetEmail.isBlank()) {
            showToast("يرجى إدخال البريد الإلكتروني أولاً")
            return
        }
        viewModelScope.launch {
            isAuthLoading.value = true
            val result = repository.sendPasswordReset(targetEmail)
            isAuthLoading.value = false
            if (result.isSuccess) {
                showToast("تم إرسال رابط تعيين كلمة المرور إلى $targetEmail ✉️")
            } else {
                showToast("تعذر الإرسال: ${result.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun loginWithProvider(provider: AuthProvider) {
        viewModelScope.launch {
            val result = repository.loginWithProvider(provider)
            if (result.isSuccess) {
                showToast("أهلاً بك! تم تسجيل الدخول بنجاح عبر ${provider.displayName} 👋")
                _showLoginPrompt.value = false
                val actionToRun = pendingAction
                pendingAction = null
                actionToRun?.invoke()
                if (_currentRoute.value == ScreenRoute.Login.route) {
                    navigateTo(ScreenRoute.Home.route, clearBackStack = true)
                }
            } else {
                showToast("تعذر تسجيل الدخول، يرجى المحاولة مرة أخرى")
            }
        }
    }

    fun updateUserProfile(displayName: String, phone: String?) {
        repository.updateUserProfile(displayName, phone)
        showToast("تم تحديث بيانات الملف الشخصي بنجاح ✨")
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            navigateTo(ScreenRoute.Login.route, clearBackStack = true)
            showToast("تم تسجيل الخروج بنجاح")
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val success = repository.deleteAccount()
            if (success) {
                showToast("تم حذف الحساب والبيانات الشخصية وفقاً لسياسة الخصوصية")
            } else {
                showToast("تعذر حذف الحساب حالياً")
            }
        }
    }

    // --- Admin Operations ---
    fun loadAdminAnalytics() {
        viewModelScope.launch {
            _adminStats.value = repository.getAnalytics()
        }
    }

    fun toggleBusinessActive(id: String, currentActive: Boolean) {
        viewModelScope.launch {
            repository.toggleActive(id, currentActive)
            loadAdminAnalytics()
            showToast(if (!currentActive) "تم تفعيل المنشأة" else "تم إخفاء المنشأة")
        }
    }

    fun toggleBusinessVerified(id: String, currentVerified: Boolean) {
        viewModelScope.launch {
            repository.toggleVerified(id, currentVerified)
            loadAdminAnalytics()
            showToast(if (!currentVerified) "تم توثيق المنشأة ✔️" else "تم إلغاء التوثيق")
        }
    }

    fun deleteBusiness(id: String) {
        viewModelScope.launch {
            repository.deleteBusiness(id)
            loadAdminAnalytics()
            showToast("تم حذف المنشأة")
        }
    }

    fun saveBusiness(
        id: String?,
        name: String,
        categoryId: String,
        categoryName: String,
        specialty: String,
        phone: String,
        phoneSec: String?,
        whatsapp: String?,
        city: String = "مدينة ميت غمر",
        address: String,
        area: String,
        workingHours: String,
        description: String,
        facebookUrl: String? = null,
        websiteUrl: String? = null
    ) {
        viewModelScope.launch {
            val business = BusinessEntity(
                id = id ?: ("b_custom_" + System.currentTimeMillis()),
                name = name,
                categoryId = categoryId,
                categoryName = categoryName,
                specialty = specialty,
                description = description,
                phone = phone,
                phoneSecondary = phoneSec,
                whatsapp = whatsapp,
                city = city,
                address = address,
                area = area,
                facebookUrl = facebookUrl,
                websiteUrl = websiteUrl,
                workingHours = workingHours,
                isOpenNow = true,
                isVerified = true,
                isActive = true,
                ratingAverage = 5.0f,
                ratingCount = 1,
                viewCount = 10,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(business)
            loadAdminAnalytics()
            showToast("تم حفظ بيانات المنشأة بنجاح ✨")
        }
    }

    fun addBusinessDirect(
        business: BusinessEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveBusiness(business)
            com.example.util.NotificationHelper.showNewBusinessNotification(getApplication(), business)
            repository.sendNotification(
                title = "نشاط جديد في ميت غمر 🏪: ${business.name}",
                body = "${business.categoryName} (${business.specialty}) - ${business.area.ifBlank { business.address }}",
                categoryId = business.categoryId,
                businessId = business.id,
                type = "NEW_BUSINESS",
                businessName = business.name
            )
            repository.logAuditEvent(
                action = "ADD_BUSINESS_SMART_DROPDOWN",
                entityType = "BUSINESS",
                entityId = business.id,
                detailsJson = "Direct smart dropdown entry: ${business.name} (${business.categoryName} - ${business.specialty})"
            )
            loadAdminAnalytics()
            showToast("تمت إضافة المنشأة (${business.name}) وتفعيل التنبيه بنجاح ✨")
            onSuccess()
        }
    }

    fun updateBusinessDirect(
        updatedBusiness: BusinessEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveBusiness(updatedBusiness)
            repository.logAuditEvent(
                action = "UPDATE_BUSINESS_SUPER_ADMIN",
                entityType = "BUSINESS",
                entityId = updatedBusiness.id,
                detailsJson = "Super Admin updated business: ${updatedBusiness.name}"
            )
            loadAdminAnalytics()
            showToast("تم تحديث كافة بيانات ${updatedBusiness.name} بنجاح ✨")
            onSuccess()
        }
    }

    fun moderateReview(reviewId: String, status: String) {
        viewModelScope.launch {
            repository.updateReviewStatus(reviewId, status)
            showToast("تم تحديث حالة المراجعة إلى $status")
        }
    }

    fun sendBroadcastNotification(title: String, body: String) {
        viewModelScope.launch {
            repository.sendNotification(title, body)
            showToast("تم إرسال الإشعار الفوري بنجاح 🔔")
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            showToast("تم تحديد الكل ككمقروء")
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==========================================
    // PHASE 9: USER CONTRIBUTIONS VIEWMODEL METHODS
    // ==========================================

    val myContributions: StateFlow<List<UserContributionEntity>> = repository.getUserContributions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedContributionId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedContribution: StateFlow<UserContributionEntity?> = selectedContributionId.flatMapLatest { id ->
        flow {
            if (id == null) {
                emit(null)
            } else {
                emit(repository.getContributionById(id))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val possibleDuplicates = MutableStateFlow<List<BusinessEntity>>(emptyList())
    val isCheckingDuplicates = MutableStateFlow(false)

    fun checkPossibleDuplicates(name: String, phone: String) {
        viewModelScope.launch {
            isCheckingDuplicates.value = true
            val results = repository.searchPossibleDuplicates(name, phone)
            possibleDuplicates.value = results
            isCheckingDuplicates.value = false
        }
    }

    fun clearPossibleDuplicates() {
        possibleDuplicates.value = emptyList()
    }

    fun selectContribution(id: String) {
        selectedContributionId.value = id
        navigateTo(ScreenRoute.ContributionDetail.route)
    }

    fun submitAddBusiness(payload: AddBusinessPayload, onSuccess: (UserContributionEntity) -> Unit) {
        requestProtectedAction {
            viewModelScope.launch {
                val result = repository.submitAddBusinessContribution(payload)
                result.onSuccess { contribution ->
                    showToast("تم إرسال النشاط التجاري للمراجعة بنجاح 🎉")
                    onSuccess(contribution)
                }.onFailure { err ->
                    showToast(err.message ?: "حدث خطأ أثناء إرسال الطلب")
                }
            }
        }
    }

    fun submitSuggestEdit(
        businessId: String,
        businessName: String,
        fieldName: String,
        oldValue: String,
        newValue: String,
        reason: String,
        onSuccess: () -> Unit
    ) {
        requestProtectedAction {
            viewModelScope.launch {
                val result = repository.submitSuggestEditContribution(
                    businessId = businessId,
                    businessName = businessName,
                    fieldName = fieldName,
                    oldValue = oldValue,
                    newValue = newValue,
                    reason = reason
                )
                result.onSuccess {
                    showToast("تم إرسال اقتراح التعديل بنجاح ✨")
                    onSuccess()
                }.onFailure { err ->
                    showToast(err.message ?: "حدث خطأ أثناء إرسال الاقتراح")
                }
            }
        }
    }

    fun submitReportData(
        businessId: String,
        businessName: String,
        reportType: ContributionType,
        reason: String,
        onSuccess: () -> Unit
    ) {
        requestProtectedAction {
            viewModelScope.launch {
                val result = repository.submitReportIncorrectDataContribution(
                    businessId = businessId,
                    businessName = businessName,
                    reportType = reportType,
                    reason = reason
                )
                result.onSuccess {
                    showToast("تم تسليم البلاغ بنجاح وجاري المراجعة 👍")
                    onSuccess()
                }.onFailure { err ->
                    showToast(err.message ?: "حدث خطأ أثناء إرسال البلاغ")
                }
            }
        }
    }

    fun cancelContribution(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.cancelContribution(id)
            result.onSuccess {
                showToast("تم إلغاء المساهمة بنجاح")
                onSuccess()
            }.onFailure { err ->
                showToast(err.message ?: "فشل إلغاء المساهمة")
            }
        }
    }

    // ==========================================
    // PHASE 10: ADMIN / MODERATION VIEWMODEL LOGIC
    // ==========================================

    private val _adminKPIs = MutableStateFlow(AdminKPIsSummary())
    val adminKPIs: StateFlow<AdminKPIsSummary> = _adminKPIs.asStateFlow()

    private val _dataQualityMetrics = MutableStateFlow(DataQualityMetrics())
    val dataQualityMetrics: StateFlow<DataQualityMetrics> = _dataQualityMetrics.asStateFlow()

    val adminContributions: StateFlow<List<UserContributionEntity>> =
        repository.getAllContributionsAdmin().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val adminUserAccounts: StateFlow<List<UserAccountEntity>> =
        repository.getAllUserAccountsAdmin().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val adminAuditLogs: StateFlow<List<AuditLogEntity>> =
        repository.getAllAuditLogs().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val adminReviews: StateFlow<List<ReviewEntity>> =
        repository.getAllReviewsAdmin().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val adminReports: StateFlow<List<ReviewReportEntity>> =
        repository.getAllReportsAdmin().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    fun refreshAdminKPIs() {
        viewModelScope.launch {
            _adminKPIs.value = repository.getAdminSummaryKPIs()
            _dataQualityMetrics.value = repository.getAdminDataQualityMetrics()
        }
    }

    fun approveContributionAdmin(contributionId: String, note: String?) {
        viewModelScope.launch {
            val res = repository.approveContributionAdmin(contributionId, note)
            res.onSuccess {
                showToast("تمت الموافقة على المساهمة بنجاح ✅")
                refreshAdminKPIs()
            }.onFailure {
                showToast(it.message ?: "حدث خطأ أثناء تنفيذ الطلب")
            }
        }
    }

    fun rejectContributionAdmin(contributionId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.rejectContributionAdmin(contributionId, reason)
            res.onSuccess {
                showToast("تم رفض المساهمة وتسجيل السبب ❌")
                refreshAdminKPIs()
            }.onFailure {
                showToast(it.message ?: "حدث خطأ أثناء التنفيذ")
            }
        }
    }

    fun suspendUserAdmin(userId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.suspendUserAccountAdmin(userId, reason)
            res.onSuccess {
                showToast("تم حظر الحساب بنجاح 🚫")
            }.onFailure {
                showToast(it.message ?: "فشل حظر الحساب")
            }
        }
    }

    fun restoreUserAdmin(userId: String) {
        viewModelScope.launch {
            val res = repository.restoreUserAccountAdmin(userId)
            res.onSuccess {
                showToast("تم إلغاء حظر الحساب وتنشيطه 🟢")
            }.onFailure {
                showToast(it.message ?: "فشل إلغاء الحظر")
            }
        }
    }

    fun toggleBusinessActiveAdmin(businessId: String, isActive: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleBusinessActiveAdmin(businessId, isActive)
            res.onSuccess {
                showToast(if (isActive) "تم تنشيط النشاط التجاري بالدليل 🟢" else "تم إيقاف النشاط التجاري مؤقتاً 🔴")
                refreshAdminKPIs()
            }
        }
    }

    fun deleteReviewAdmin(reviewId: String) {
        viewModelScope.launch {
            val res = repository.deleteReviewAdmin(reviewId)
            res.onSuccess {
                showToast("تم حذف التقييم بواسطة المشرف 🗑️")
                refreshAdminKPIs()
            }
        }
    }

    fun resolveReportAdmin(reportId: String) {
        viewModelScope.launch {
            val res = repository.resolveReportAdmin(reportId)
            res.onSuccess {
                showToast("تم إغلاق البلاغ بنجاح 👍")
                refreshAdminKPIs()
            }
        }
    }

    fun sendBroadcastNotificationAdmin(
        title: String,
        body: String,
        categoryId: String? = null,
        type: String = "UPDATE",
        businessName: String? = null,
        businessId: String? = null
    ) {
        viewModelScope.launch {
            repository.sendNotification(
                title = title,
                body = body,
                categoryId = categoryId,
                businessId = businessId,
                type = type,
                businessName = businessName
            )
            repository.logAuditEvent("SEND_BROADCAST_NOTIFICATION", "NOTIFICATION", type, "عنوان: $title")
            showToast("تم إرسال الإشعار والتنبيه بنجاح 🔔")
        }
    }

    fun sendPromotionalOfferAdmin(
        title: String,
        body: String,
        businessName: String? = null,
        businessId: String? = null
    ) {
        viewModelScope.launch {
            repository.sendNotification(
                title = title,
                body = body,
                categoryId = "cat_offers",
                businessId = businessId,
                type = "OFFER",
                businessName = businessName
            )
            repository.logAuditEvent("SEND_PROMOTIONAL_OFFER", "OFFER", businessId ?: "global", "عرض: $title")
            showToast("تم نشر العرض الترويجي وإشعار المستخدمين 🏷️")
        }
    }

    // ============================================================
    // PHASE 11: BACKUP, RESTORE & BULK DATA VIEWMODEL METHODS
    // ============================================================

    val adminBackupRecords: StateFlow<List<BackupRecordEntity>> =
        repository.getAllBackupRecords().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    private val _lastRestoreReport = MutableStateFlow<RestoreReportSummary?>(null)
    val lastRestoreReport: StateFlow<RestoreReportSummary?> = _lastRestoreReport.asStateFlow()

    private val _lastImportSummary = MutableStateFlow<ImportResultSummary?>(null)
    val lastImportSummary: StateFlow<ImportResultSummary?> = _lastImportSummary.asStateFlow()

    private val _isDataOperationLoading = MutableStateFlow(false)
    val isDataOperationLoading: StateFlow<Boolean> = _isDataOperationLoading.asStateFlow()

    fun createFullBackupAdmin(backupType: BackupType) {
        viewModelScope.launch {
            _isDataOperationLoading.value = true
            val res = repository.createFullBackendBackup(backupType)
            _isDataOperationLoading.value = false
            res.onSuccess { record ->
                showToast("تم إنشاء النسخة الاحتياطية (${record.fileName}) بنجاح 📦")
            }.onFailure {
                showToast(it.message ?: "فشل إنشاء النسخة الاحتياطية")
            }
        }
    }

    fun restoreFullBackupAdmin(backupRecord: BackupRecordEntity, word: String) {
        viewModelScope.launch {
            _isDataOperationLoading.value = true
            val res = repository.restoreFullBackendBackup(backupRecord, word)
            _isDataOperationLoading.value = false
            res.onSuccess { report ->
                _lastRestoreReport.value = report
                showToast("تمت استعادة البيانات بنجاح مع إنشاء نسخة سلامة تلقائية 🎉")
                refreshAdminKPIs()
            }.onFailure {
                showToast(it.message ?: "فشلت عملية استعادة النسخة الاحتياطية")
            }
        }
    }

    fun uploadBackupToGoogleDriveAdmin(backupRecord: BackupRecordEntity) {
        viewModelScope.launch {
            _isDataOperationLoading.value = true
            val res = repository.uploadBackupToGoogleDrive(backupRecord)
            _isDataOperationLoading.value = false
            res.onSuccess {
                showToast("تم رفع النسخة الاحتياطية إلى Google Drive بنجاح ☁️")
            }.onFailure {
                showToast("فشل رفع النسخة إلى Google Drive")
            }
        }
    }

    fun processBulkImportAdmin(
        importType: BulkImportType,
        rowsData: List<Map<String, String>>,
        mode: ImportMode,
        isDryRun: Boolean
    ) {
        viewModelScope.launch {
            _isDataOperationLoading.value = true
            val res = repository.processBulkImport(importType, rowsData, mode, isDryRun)
            _isDataOperationLoading.value = false
            res.onSuccess { summary ->
                _lastImportSummary.value = summary
                if (isDryRun) {
                    showToast("تمت معالجة الفحص التجريبي (Dry Run): ${summary.successCount} صالح، ${summary.failedCount} به أخطاء")
                } else {
                    showToast("تم الاستيراد الجماعي للبيانات بنجاح: ${summary.successCount} سجل مضاف/محدث 🎉")
                    refreshAdminKPIs()
                }
            }.onFailure {
                showToast(it.message ?: "فشلت عملية الاستيراد الجماعي")
            }
        }
    }

    fun getImportTemplateCsv(importType: BulkImportType): String {
        return repository.generateImportTemplateCsv(importType)
    }

    fun getImportTemplateExcel(): String {
        return repository.generateImportTemplateExcel()
    }

    fun clearLastImportSummary() {
        _lastImportSummary.value = null
    }

    fun clearLastRestoreReport() {
        _lastRestoreReport.value = null
    }

    // --- AI Assistant State & Services ---
    private val aiAssistantService = com.example.data.ai.AiAssistantService()

    private val _aiChatMessages = MutableStateFlow<List<com.example.data.model.AiChatMessage>>(
        listOf(
            com.example.data.model.AiChatMessage(
                sender = com.example.data.model.AiSender.AI,
                text = "أهلاً بك في مساعد ميت غمر الذكي! 👋🤖\nاسألني عن أي مكان، مطعم، طبيب، أو خدمة في دليل ميت غمر وسأجيبك ببيانات دقيقة ومحدثة."
            )
        )
    )
    val aiChatMessages: StateFlow<List<com.example.data.model.AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _isLocationPermissionGranted = MutableStateFlow(false)
    val isLocationPermissionGranted: StateFlow<Boolean> = _isLocationPermissionGranted.asStateFlow()

    private val _aiConfig = MutableStateFlow(com.example.data.model.AiAssistantConfig())
    val aiConfig: StateFlow<com.example.data.model.AiAssistantConfig> = _aiConfig.asStateFlow()

    private val _aiAnalytics = MutableStateFlow(com.example.data.model.AiAnalyticsSummary())
    val aiAnalytics: StateFlow<com.example.data.model.AiAnalyticsSummary> = _aiAnalytics.asStateFlow()

    fun updateLocationPermission(granted: Boolean, lat: Double? = null, lng: Double? = null) {
        _isLocationPermissionGranted.value = granted
        if (lat != null && lng != null) {
            _userLocation.value = Pair(lat, lng)
        } else if (granted) {
            _userLocation.value = Pair(
                com.example.data.ai.AiRecommendationEngine.MET_GHAMR_CENTER_LAT,
                com.example.data.ai.AiRecommendationEngine.MET_GHAMR_CENTER_LNG
            )
        }
    }

    fun sendAiMessage(userQuery: String) {
        if (userQuery.isBlank() || _isAiLoading.value) return

        val userMessage = com.example.data.model.AiChatMessage(
            sender = com.example.data.model.AiSender.USER,
            text = userQuery
        )

        _aiChatMessages.update { it + userMessage }
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val responseMessage = aiAssistantService.processUserQuery(
                    userQuery = userQuery,
                    candidatesProvider = { query -> repository.queryCandidatesForAi(query) },
                    userLat = _userLocation.value?.first,
                    userLng = _userLocation.value?.second,
                    isLocationPermissionGranted = _isLocationPermissionGranted.value
                )
                _aiChatMessages.update { it + responseMessage }
            } catch (e: Exception) {
                val errorMessage = com.example.data.model.AiChatMessage(
                    sender = com.example.data.model.AiSender.AI,
                    text = "عذراً، حدث خطأ مؤقت في الاتصال بالمساعد الذكي: ${e.localizedMessage ?: "حاول مجدداً"}\nيمكنك الانتقال إلى شاشة البحث التقليدية."
                )
                _aiChatMessages.update { it + errorMessage }
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiChat() {
        _aiChatMessages.value = listOf(
            com.example.data.model.AiChatMessage(
                sender = com.example.data.model.AiSender.AI,
                text = "تم البدء بمحادثة جديدة. 🤖\nاسألني عن أي نشاط أو خدمة بدليل ميت غمر!"
            )
        )
    }

    fun updateAiConfig(newConfig: com.example.data.model.AiAssistantConfig) {
        _aiConfig.value = newConfig
        showToast("تم تحديث إعدادات ونماذج الذكاء الاصطناعي بنجاح ✨")
    }

    // ============================================================
    // SMART DIRECTORY DATA ENGINE VIEWMODEL EXPOSURES
    // ============================================================

    val activeDiscoveryJob: StateFlow<DiscoveryJob?> = repository.activeDiscoveryJob
    val discoveredCandidates: StateFlow<List<CandidateBusiness>> = repository.discoveredCandidates
    val activeDataConflicts: StateFlow<List<DataConflictItem>> = repository.activeDataConflicts
    val suggestedCategories: StateFlow<List<SuggestedCategoryItem>> = repository.suggestedCategories
    val suggestedAreas: StateFlow<List<SuggestedAreaItem>> = repository.suggestedAreas
    val isEnginePaused: StateFlow<Boolean> = repository.isEnginePaused

    fun approveSuggestedArea(areaId: String) {
        repository.approveSuggestedArea(areaId)
        showToast("تم اعتماد وإضافة المنطقة الجديدة إلى النطاق الجغرافي بنجاح 🗺️")
    }

    fun rejectSuggestedArea(areaId: String) {
        repository.rejectSuggestedArea(areaId)
        showToast("تم استبعاد المنطقة المقترحة.")
    }

    fun computeAreaCoverageReport(): List<com.example.data.model.AreaCoverageStat> {
        return repository.computeAreaCoverageReport()
    }

    fun runExpandedRestaurantsDiscoveryTest(onResult: (String) -> Unit) {
        repository.runExpandedRestaurantsDiscoveryTest(onResult)
    }

    fun runExpandedDoctorsDiscoveryTest(onResult: (String) -> Unit) {
        repository.runExpandedDoctorsDiscoveryTest(onResult)
    }

    fun startDataDiscoveryJob(
        title: String,
        targetCategory: String,
        targetArea: String,
        sources: List<DiscoverySourceType>,
        targetCount: Int
    ) {
        repository.startDataDiscoveryJob(
            title = title,
            targetCategory = targetCategory,
            targetArea = targetArea,
            sources = sources,
            targetCount = targetCount
        )
        showToast("تم بدء مهمة جمع واكتشاف البيانات بنجاح 🚀")
    }

    fun pauseDataDiscoveryJob() {
        repository.pauseDataDiscoveryJob()
        showToast("تم إيقاف مهمة الجمع مؤقتاً ⏸️")
    }

    fun resumeDataDiscoveryJob() {
        repository.resumeDataDiscoveryJob()
        showToast("تم استئناف مهمة الجمع الذكي ▶️")
    }

    fun cancelDataDiscoveryJob() {
        repository.cancelDataDiscoveryJob()
        showToast("تم إلغاء مهمة الجمع ⏹️")
    }

    fun commitDiscoveredCandidatesBatch(candidatesToCommit: List<CandidateBusiness>) {
        viewModelScope.launch {
            val response = repository.commitDiscoveredCandidatesBatch(candidatesToCommit)
            if (response.success) {
                showToast("تم اعتماد وإضافة ${response.data} نشاط إلى قاعدة البيانات الرئيسية بنجاح 🎉")
            } else {
                showToast("خطأ: ${response.message}")
            }
        }
    }

    fun resolveDataConflict(conflictId: String, chosenValue: String, candidateId: String, fieldName: String) {
        viewModelScope.launch {
            val response = repository.resolveDataConflict(conflictId, chosenValue, candidateId, fieldName)
            if (response.success) {
                showToast("تم حل التعارض واعتماد القيمة بنجاح ✅")
            } else {
                showToast("خطأ: ${response.message}")
            }
        }
    }

    fun mergeDuplicateBusiness(masterBusinessId: String, candidate: CandidateBusiness) {
        viewModelScope.launch {
            val response = repository.mergeDuplicateBusiness(masterBusinessId, candidate)
            if (response.success) {
                showToast("تم دمج النشاط المكرر وإثراء الحقول بنجاح 🔄")
            } else {
                showToast("خطأ: ${response.message}")
            }
        }
    }

    fun approveSuggestedCategory(categoryId: String) {
        repository.approveSuggestedCategory(categoryId)
        showToast("تمت الموافقة على التصنيف المقترح وإضافته للقائمة ✅")
    }

    fun rejectSuggestedCategory(categoryId: String) {
        repository.rejectSuggestedCategory(categoryId)
        showToast("تم رفض التصنيف المقترح")
    }

    fun getEngineAnalytics(): EngineAnalyticsSummary {
        return repository.computeEngineAnalytics()
    }

    fun getCategoryCoverageReport(): List<CoverageReportCategory> {
        return repository.computeCategoryCoverageReport()
    }

    val mergeHistories: StateFlow<List<MergeHistoryEntity>> = repository.getAllMergeHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFieldAudits: StateFlow<List<FieldAuditHistoryEntity>> = repository.getRecentFieldAudits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSourcesForBusiness(businessId: String): Flow<List<BusinessSourceEntity>> {
        return repository.getSourcesForBusiness(businessId)
    }

    fun rollbackMerge(mergeHistoryId: String) {
        viewModelScope.launch {
            val response = repository.rollbackMerge(mergeHistoryId)
            if (response.success) {
                showToast("تم التراجع عن عملية الدمج واستعادة البيانات السابقة بنجاح ↩️")
            } else {
                showToast("فشل التراجع: ${response.message}")
            }
        }
    }

    fun runMultiSourceScenarioTest(scenarioId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.runMultiSourceScenarioTest(scenarioId)
            res.onSuccess { report ->
                showToast("تم تشغيل سيناريو التحقق بنجاح 🧪")
                onResult(report)
            }.onFailure { err ->
                val errorMsg = "فشل تشغيل السيناريو: ${err.localizedMessage}"
                showToast(errorMsg)
                onResult(errorMsg)
            }
        }
    }

    fun syncAllBusinessesToCloudServer(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.syncAllBusinessesToCloud()
            result.onSuccess { count ->
                showToast("تم تثبيت ومزامنة $count نشاط على السيرفر السحابي بنجاح ☁️🚀")
                onComplete(true, "تم تثبيت $count نشاط بنجاح")
            }.onFailure { err ->
                val msg = "فشلت المزامنة السحابية: ${err.localizedMessage}"
                showToast(msg)
                onComplete(false, msg)
            }
        }
    }

    fun pullBusinessesFromCloudServer(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.pullBusinessesFromCloud()
            result.onSuccess { count ->
                showToast("تم جلب وتحديث $count نشاط من السيرفر السحابي 🔄")
                onComplete(true, "تم جلب $count نشاط")
            }.onFailure { err ->
                val msg = "فشل جلب البيانات من السيرفر: ${err.localizedMessage}"
                showToast(msg)
                onComplete(false, msg)
            }
        }
    }

    fun cleanupCorruptedImportedBusinesses(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.cleanupCorruptedImportedBusinessesAdmin()
            result.onSuccess { count ->
                if (count > 0) {
                    showToast("تم تنظيف وحذف $count نشاط تجاري تالف/وهمي بنجاح 🧹✨")
                } else {
                    showToast("لم يتم العثور على أنشطة تالفة أو وهمية في قاعدة البيانات 👍")
                }
                onComplete(count)
            }.onFailure { err ->
                showToast("فشلت عملية التنظيف: ${err.localizedMessage}")
                onComplete(0)
            }
        }
    }
}

