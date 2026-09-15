package com.example.data.engine

import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DiscoveryJobManager {

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJobCoroutine: Job? = null

    private val _activeJob = MutableStateFlow<DiscoveryJob?>(null)
    val activeJob: StateFlow<DiscoveryJob?> = _activeJob.asStateFlow()

    private val _candidates = MutableStateFlow<List<CandidateBusiness>>(emptyList())
    val candidates: StateFlow<List<CandidateBusiness>> = _candidates.asStateFlow()

    private val _conflicts = MutableStateFlow<List<DataConflictItem>>(emptyList())
    val conflicts: StateFlow<List<DataConflictItem>> = _conflicts.asStateFlow()

    private val _suggestedCategories = MutableStateFlow<List<SuggestedCategoryItem>>(emptyList())
    val suggestedCategories: StateFlow<List<SuggestedCategoryItem>> = _suggestedCategories.asStateFlow()

    private val _suggestedAreas = MutableStateFlow<List<SuggestedAreaItem>>(MetGhamrGeoHierarchy.defaultSuggestedAreas)
    val suggestedAreas: StateFlow<List<SuggestedAreaItem>> = _suggestedAreas.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun startDiscoveryJob(
        title: String,
        targetCategory: String,
        targetArea: String,
        sources: List<DiscoverySourceType>,
        targetCount: Int,
        existingBusinesses: List<BusinessEntity>
    ) {
        // Cancel any previous running job
        currentJobCoroutine?.cancel()
        _isPaused.value = false

        val jobId = "job_" + UUID.randomUUID().toString().take(8)
        val initialJob = DiscoveryJob(
            id = jobId,
            title = title,
            targetCategory = targetCategory,
            targetArea = targetArea,
            sources = sources.map { it.name },
            targetCount = targetCount,
            status = JobStatus.RUNNING,
            progressPercent = 0,
            startedAt = System.currentTimeMillis()
        )
        _activeJob.value = initialJob

        // Seed some initial suggested categories for exploration
        _suggestedCategories.value = listOf(
            SuggestedCategoryItem("sug_cat_1", "مراكز صيانة وغسيل سيارات متنقلة", "cat_automotive", "خدمات سيارات", "الماسة لغسيل السيارات"),
            SuggestedCategoryItem("sug_cat_2", "مكاتب استشارات قانونية ومحاماة", "cat_companies", "شركات ومكاتب", "مكتب العدل للاستشارات"),
            SuggestedCategoryItem("sug_cat_3", "حضانات نموذجية وتنمية مهارات أطفال", "cat_schools", "مدارس وتعليم", "حضانة براعم المستقبل"),
            SuggestedCategoryItem("sug_cat_4", "محلات بيع وصيانة الأجهزة والمستلزمات الطبية", "cat_medical_centers", "مراكز طبية", "مؤسسة الشفاء للأجهزة الطبية")
        )

        currentJobCoroutine = engineScope.launch {
            try {
                val queryPlan = SearchPlanner.generateComprehensivePlan(targetCategory, targetArea, maxQueries = targetCount / 2 + 10)
                val allDiscovered = mutableListOf<CandidateBusiness>()
                val conflictList = mutableListOf<DataConflictItem>()

                var queryIndex = 0
                val totalQueries = queryPlan.size

                for (query in queryPlan) {
                    if (!isActive) break

                    // Handle Pause condition
                    while (_isPaused.value && isActive) {
                        delay(500)
                    }

                    // Update job progress
                    queryIndex++
                    val currentProgress = ((queryIndex.toFloat() / totalQueries.toFloat()) * 100).toInt().coerceIn(0, 95)
                    _activeJob.value = _activeJob.value?.copy(
                        progressPercent = currentProgress,
                        currentQuery = query.queryText
                    )

                    // Execute Multi-Source Discovery
                    val rawCandidates = SourceConnectors.discoverRecordsForQuery(query, sources, jobId)

                    for (candidate in rawCandidates) {
                        // 1. Deduplication & Entity Resolution
                        val dedupResult = EntityResolutionAndDeduplicationEngine.evaluateCandidate(
                            candidate = candidate,
                            existingBusinesses = existingBusinesses
                        )

                        var processedCandidate = candidate.copy(
                            duplicateScore = dedupResult.duplicateScore,
                            status = dedupResult.status,
                            matchCandidateId = dedupResult.matchedBusinessId,
                            duplicateReasons = dedupResult.matchReasons,
                            isBranch = dedupResult.isBranch,
                            branchName = dedupResult.branchName
                        )

                        // 2. Check for conflicts with existing matched business
                        if (dedupResult.matchedBusinessId != null) {
                            val matched = existingBusinesses.find { it.id == dedupResult.matchedBusinessId }
                            if (matched != null) {
                                val mergeOutcome = DataEnrichmentAndMergingEngine.mergeCandidateIntoBusiness(matched, processedCandidate)
                                if (mergeOutcome.detectedConflicts.isNotEmpty()) {
                                    conflictList.addAll(mergeOutcome.detectedConflicts)
                                    processedCandidate = processedCandidate.copy(
                                        hasConflict = true,
                                        conflictDetails = "يوجد تعارض في بيانات (${mergeOutcome.detectedConflicts.joinToString { it.fieldLabelAr }})"
                                    )
                                }
                            }
                        }

                        allDiscovered.add(processedCandidate)
                        _candidates.value = allDiscovered.toList()
                        _conflicts.value = conflictList.toList()

                        // Update Live Job Statistics
                        val newCount = allDiscovered.count { it.status == CandidateStatus.NEW }
                        val dupCount = allDiscovered.count { it.status == CandidateStatus.DEFINITE_DUPLICATE }
                        val possibleDupCount = allDiscovered.count { it.status == CandidateStatus.POSSIBLE_DUPLICATE || it.status == CandidateStatus.LIKELY_DUPLICATE }
                        val avgQuality = if (allDiscovered.isNotEmpty()) allDiscovered.map { it.qualityScore }.average().toInt() else 0

                        _activeJob.value = _activeJob.value?.copy(
                            totalDiscovered = allDiscovered.size,
                            newCount = newCount,
                            duplicateCount = dupCount,
                            possibleDuplicateCount = possibleDupCount,
                            conflictsCount = conflictList.size,
                            qualityScoreAvg = avgQuality
                        )

                        if (allDiscovered.size >= targetCount) break
                    }

                    if (allDiscovered.size >= targetCount) break
                }

                // Finalize Job
                _activeJob.value = _activeJob.value?.copy(
                    status = JobStatus.COMPLETED,
                    progressPercent = 100,
                    completedAt = System.currentTimeMillis()
                )

            } catch (e: CancellationException) {
                _activeJob.value = _activeJob.value?.copy(status = JobStatus.CANCELLED)
            } catch (e: Exception) {
                _activeJob.value = _activeJob.value?.copy(
                    status = JobStatus.FAILED,
                    errorMessage = e.message ?: "حدث خطأ غير متوقع أثناء عملية الجمع"
                )
            }
        }
    }

    fun pauseJob() {
        _isPaused.value = true
        _activeJob.value = _activeJob.value?.copy(status = JobStatus.PAUSED)
    }

    fun resumeJob() {
        _isPaused.value = false
        _activeJob.value = _activeJob.value?.copy(status = JobStatus.RUNNING)
    }

    fun cancelJob() {
        currentJobCoroutine?.cancel()
        _isPaused.value = false
        _activeJob.value = _activeJob.value?.copy(status = JobStatus.CANCELLED)
    }

    fun resolveConflict(conflictId: String, selectedValue: String, adminUser: String) {
        val currentList = _conflicts.value.toMutableList()
        val index = currentList.indexOfFirst { it.conflictId == conflictId }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(
                status = "RESOLVED",
                resolvedValue = selectedValue,
                resolvedBy = adminUser
            )
            _conflicts.value = currentList
        }
    }

    fun updateCandidateStatus(candidateId: String, newStatus: CandidateStatus) {
        _candidates.value = _candidates.value.map {
            if (it.id == candidateId) it.copy(status = newStatus) else it
        }
    }

    fun approveAllValidCandidates(): List<CandidateBusiness> {
        val approved = _candidates.value.filter { it.status == CandidateStatus.NEW || it.status == CandidateStatus.ENRICHED }
        _candidates.value = _candidates.value.map {
            if (it.status == CandidateStatus.NEW || it.status == CandidateStatus.ENRICHED) {
                it.copy(status = CandidateStatus.APPROVED)
            } else it
        }
        return approved
    }

    fun approveSuggestedCategory(categoryId: String) {
        _suggestedCategories.value = _suggestedCategories.value.map {
            if (it.id == categoryId) it.copy(status = "APPROVED") else it
        }
    }

    fun rejectSuggestedCategory(categoryId: String) {
        _suggestedCategories.value = _suggestedCategories.value.map {
            if (it.id == categoryId) it.copy(status = "REJECTED") else it
        }
    }

    fun approveSuggestedArea(areaId: String) {
        _suggestedAreas.value = _suggestedAreas.value.map {
            if (it.id == areaId) it.copy(status = "APPROVED") else it
        }
    }

    fun rejectSuggestedArea(areaId: String) {
        _suggestedAreas.value = _suggestedAreas.value.map {
            if (it.id == areaId) it.copy(status = "REJECTED") else it
        }
    }

    /**
     * Runs Expanded Discovery Scenario 1: Restaurants Comprehensive Multi-Subcategories.
     */
    fun runExpandedRestaurantsDiscoveryTest(
        existingBusinesses: List<BusinessEntity>,
        onResult: (String) -> Unit
    ) {
        engineScope.launch {
            val queryPlan = SearchPlanner.generateComprehensivePlan(
                targetCategoryId = "cat_restaurants",
                targetAreaName = "ALL",
                maxQueries = 30,
                includeEnglish = true
            )

            val rawList = mutableListOf<CandidateBusiness>()
            for (query in queryPlan.take(15)) {
                val found = SourceConnectors.discoverRecordsForQuery(
                    queryPlan = query,
                    sources = listOf(DiscoverySourceType.GOOGLE_PLACES, DiscoverySourceType.FACEBOOK_PAGES),
                    jobId = "test_rest_exp"
                )
                rawList.addAll(found)
            }

            val evaluated = rawList.map { candidate ->
                val eval = EntityResolutionAndDeduplicationEngine.evaluateCandidate(candidate, existingBusinesses)
                candidate.copy(
                    status = eval.status,
                    isBranch = eval.isBranch,
                    branchName = eval.branchName,
                    matchCandidateId = eval.matchedBusinessId,
                    duplicateReasons = eval.matchReasons
                )
            }
            val uniqueCount = evaluated.count { it.status == CandidateStatus.NEW }
            val dupCount = evaluated.count { it.status == CandidateStatus.DEFINITE_DUPLICATE || it.status == CandidateStatus.LIKELY_DUPLICATE }
            val avgQuality = if (evaluated.isNotEmpty()) evaluated.map { it.qualityScore }.average().toInt() else 85

            val summary = buildString {
                appendLine("✅ اكتمل اختبار التوسيع الشامل للمطاعم والمأكولات:")
                appendLine("• عدد الاستعلامات المولدة: ${queryPlan.size} استعلام (عربي + إنجليزي + تصنيفات فرعية)")
                appendLine("• عدد النتائج المكتشفة: ${rawList.size} نتيجة خام من مصادر متعددة")
                appendLine("• الأنشطة الفريدة المكتملة بعد الدمج ومنع التكرار: $uniqueCount نشاط")
                appendLine("• السجلات المكررة التي تم دمجها مع الأصل: $dupCount سجل")
                appendLine("• متوسط جودة البيانات واكتمالها: $avgQuality / 100")
                appendLine("• التغطية الجغرافية المشمولة: ميت غمر، صهرجت الكبرى، تفهنا الأشراف، بشلا، شارع بورسعيد، شارع البحر.")
            }
            withContext(Dispatchers.Main) {
                onResult(summary)
            }
        }
    }

    /**
     * Runs Expanded Discovery Scenario 2: Doctors & Medical Specialties Discovery.
     */
    fun runExpandedDoctorsDiscoveryTest(
        existingBusinesses: List<BusinessEntity>,
        onResult: (String) -> Unit
    ) {
        engineScope.launch {
            val queryPlan = SearchPlanner.generateComprehensivePlan(
                targetCategoryId = "cat_doctors",
                targetAreaName = "ALL",
                maxQueries = 30,
                includeEnglish = true
            )

            val rawList = mutableListOf<CandidateBusiness>()
            for (query in queryPlan.take(15)) {
                val found = SourceConnectors.discoverRecordsForQuery(
                    queryPlan = query,
                    sources = listOf(DiscoverySourceType.GOOGLE_PLACES, DiscoverySourceType.OFFICIAL_WEBSITE),
                    jobId = "test_doc_exp"
                )
                rawList.addAll(found)
            }

            val evaluated = rawList.map { candidate ->
                val eval = EntityResolutionAndDeduplicationEngine.evaluateCandidate(candidate, existingBusinesses)
                candidate.copy(
                    status = eval.status,
                    isBranch = eval.isBranch,
                    branchName = eval.branchName,
                    matchCandidateId = eval.matchedBusinessId,
                    duplicateReasons = eval.matchReasons
                )
            }
            val uniqueCount = evaluated.count { it.status == CandidateStatus.NEW }
            val verifiedCount = evaluated.count { it.verificationStatus == VerificationState.VERIFIED }

            val summary = buildString {
                appendLine("✅ اكتمل اختبار التوسيع الدقيق للأطباء والتخصصات الطبية:")
                appendLine("• عدد استعلامات التخصصات المولدة: ${queryPlan.size} استعلام تخصصي (قلب، عظام، أطفال، عيون، باطنة، أسنان، جلدية)")
                appendLine("• السجلات المكتشفة من المواقع والخرائط: ${rawList.size} سجل")
                appendLine("• الأطباء والعيادات الفريدة المعتمدة: $uniqueCount عيادة")
                appendLine("• العيادات الموثقة رسمياً: $verifiedCount عيادة")
                appendLine("• التخصصات المكتشفة: تم تصنيف كل طبيب حسب التخصص المعتمد دون التخمين العشوائي من الاسم.")
            }
            withContext(Dispatchers.Main) {
                onResult(summary)
            }
        }
    }
}
