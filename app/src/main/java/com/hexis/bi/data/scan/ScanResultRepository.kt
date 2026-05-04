package com.hexis.bi.data.scan

import com.hexis.bi.data.scan.api.MeasurementResponse

/**
 * In-memory holder for passing scan results between StartScan and Results screens
 * without serializing through navigation arguments.
 */
class ScanResultRepository {
    var latestResult: ScanResult? = null
    var selectedScanId: String? = null
}

data class ScanResult(
    val measurementId: String,
    val response: MeasurementResponse,
)
