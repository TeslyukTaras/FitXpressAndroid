package com.hexis.bi.ui.main.buysuit.suitsize

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.data.order.OrderDraftHolder
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.user.FirestoreSchema.UserFields
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.order.OrderSizing
import com.hexis.bi.domain.order.SuitSize
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.persistedUserMeasurements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SuitSizeResultsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val orderDraftHolder: OrderDraftHolder,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SuitSizeResultsState())
    val state: StateFlow<SuitSizeResultsState> = _state.asStateFlow()

    private var savedHeightCm: Int? = null
    private var savedWeightKg: Int? = null
    private var scan: ScanRecord? = null

    init {
        load()
    }

    private fun load() = launch(showLoading = false) {
        val profile = userRepository.getUser().getOrNull()
        savedHeightCm = profile?.heightCm
        savedWeightKg = profile?.weightKg
        val selectedScan = scanResultRepository.selectedScanId
            ?.let { scanHistoryRepository.getScanRecordById(it).getOrNull() }
        val latestScan = selectedScan ?: scanHistoryRepository.getLatestScan().getOrNull()
        scan = latestScan

        val heightCm = profile?.heightCm?.toFloat()
            ?: latestScan?.measurements?.get("height")
            ?: _state.value.heightCm
        val weightKg = profile?.weightKg?.toFloat()
            ?: latestScan?.weightKg
            ?: _state.value.weightKg

        if (latestScan == null) setError(R.string.suit_size_error_no_scan)

        _state.update {
            it.copy(
                isLoading = false,
                isMetric = profile?.unitSystem.isMetricUnitSystem(),
                heightCm = heightCm,
                weightKg = weightKg,
                suitSize = recommendSuitSize(heightCm, weightKg, latestScan),
                scanId = latestScan?.id,
            )
        }
    }

    fun proceedToOrder() {
        val s = _state.value
        val measurements = persistedUserMeasurements(s.heightCm, s.weightKg)
        if (measurements.heightCm == savedHeightCm && measurements.weightKg == savedWeightKg) {
            confirmSelection(s)
            return
        }
        launch {
            userRepository.updateFields(
                mapOf(
                    UserFields.HEIGHT_CM to measurements.heightCm,
                    UserFields.WEIGHT_KG to measurements.weightKg,
                    UserFields.HEIGHT_IN to measurements.heightIn,
                    UserFields.WEIGHT_LB to measurements.weightLb,
                ),
            )
                .onSuccess {
                    savedHeightCm = measurements.heightCm
                    savedWeightKg = measurements.weightKg
                    confirmSelection(s)
                }
                .onFailure { setError(it.message) }
        }
    }

    private fun confirmSelection(s: SuitSizeResultsState) {
        orderDraftHolder.sizing = OrderSizing(
            scanId = s.scanId,
            suitSize = s.suitSize.name,
            heightCm = s.heightCm,
            weightKg = s.weightKg,
        )
        emitEvent(SuitSizeResultsEvent.ProceedToOrder)
    }

    fun updateHeight(heightCm: Float) {
        _state.update {
            it.copy(
                heightCm = heightCm,
                suitSize = recommendSuitSize(heightCm, it.weightKg, scan),
            )
        }
    }

    fun updateWeight(weightKg: Float) {
        _state.update {
            it.copy(
                weightKg = weightKg,
                suitSize = recommendSuitSize(it.heightCm, weightKg, scan),
            )
        }
    }

    fun selectMetric() = _state.update { it.copy(isMetric = true) }

    fun selectImperial() = _state.update { it.copy(isMetric = false) }

    private fun recommendSuitSize(
        heightCm: Float,
        weightKg: Float,
        scan: ScanRecord?,
    ): SuitSize {
        val chest = scan?.measurements?.firstValue(BodyMeasurementKeys.Chest, "chestCircumference", "bust")
        val waist = scan?.measurements?.firstValue(BodyMeasurementKeys.Waist, "waistCircumference")
        val hips = scan?.measurements?.firstValue("hips", "hip", "hipCircumference")
        val torsoSignal = listOfNotNull(chest, waist, hips).averageOrNull()

        val score = when {
            torsoSignal != null -> (torsoSignal - 88f) / 9f
            else -> ((weightKg - 70f) / 10f) + ((heightCm - 175f) / 18f)
        }

        return when {
            score < -1.2f -> SuitSize.X_SMALL
            score < -0.35f -> SuitSize.SMALL
            score < 0.8f -> SuitSize.MEDIUM
            score < 1.8f -> SuitSize.LARGE
            score < 2.8f -> SuitSize.X_LARGE
            else -> SuitSize.XX_LARGE
        }
    }

    private fun Map<String, Float>.firstValue(vararg names: String): Float? {
        names.forEach { name ->
            this[name]?.let { return it }
        }
        val normalizedNames = names.map { it.lowercase() }
        return entries.firstOrNull { entry ->
            normalizedNames.any { normalized -> entry.key.lowercase().contains(normalized) }
        }?.value
    }

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else average().toFloat()
}
