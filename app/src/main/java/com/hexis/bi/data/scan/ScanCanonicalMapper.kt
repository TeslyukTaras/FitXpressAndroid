package com.hexis.bi.data.scan

import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.asInstant
import java.time.Instant

internal fun CanonicalBodyScanAggregate.toScanRecord(): ScanRecord = ScanRecord(
    id = documentId,
    measurementId = measurementId,
    timestamp = savedAt.asInstant().toEpochMilli(),
    model3dUrl = model3dUrl,
    measurements = (circumferenceParamsCm + frontLinearParamsCm + sideLinearParamsCm).mapValues { it.value.toFloat() },
    frontLinearParams = frontLinearParamsCm.mapValues { it.value.toFloat() },
    sideLinearParams = sideLinearParamsCm.mapValues { it.value.toFloat() },
    heightCm = heightCm?.toFloat(),
    weightKg = weightKg?.toFloat(),
    estimatedWeightKg = estimatedWeightKg?.toFloat(),
    bmi = bmi?.toFloat(),
    fatPercentage = fatPercentage?.toFloat(),
    leanBodyMassKg = leanBodyMassKg?.toFloat(),
    fatBodyMassKg = fatBodyMassKg?.toFloat(),
)

internal fun ScanRecord.toCanonicalAggregate(): CanonicalBodyScanAggregate {
    val frontKeys = frontLinearParams.keys
    val sideKeys = sideLinearParams.keys
    return CanonicalBodyScanAggregate(
        documentId = id,
        measurementId = measurementId,
        completedAt = Instant.ofEpochMilli(timestamp).toString(),
        savedAt = Instant.ofEpochMilli(timestamp).toString(),
        model3dUrl = model3dUrl,
        heightCm = heightCm?.toDouble(),
        weightKg = weightKg?.toDouble(),
        estimatedWeightKg = estimatedWeightKg?.toDouble(),
        bmi = bmi?.toDouble(),
        fatPercentage = fatPercentage?.toDouble(),
        leanBodyMassKg = leanBodyMassKg?.toDouble(),
        fatBodyMassKg = fatBodyMassKg?.toDouble(),
        circumferenceParamsCm = measurements
            .filterKeys { it !in frontKeys && it !in sideKeys }
            .mapValues { it.value.toDouble() },
        frontLinearParamsCm = frontLinearParams.mapValues { it.value.toDouble() },
        sideLinearParamsCm = sideLinearParams.mapValues { it.value.toDouble() },
    )
}
