package com.hexis.bi.ui.main.scan.results

import com.hexis.bi.ui.main.scan.results.content.ScanResultsActions

internal fun ResultsViewModel.resultsActions() = ScanResultsActions(
    onTabSelected = ::selectTab,
    onVisualBodyPartSelected = ::selectBodyPart,
    onCompareBodyPartSelected = ::selectCompareBodyPart,
    onModeSelected = ::selectMode,
    onVisualScanSelected = ::selectVisualScan,
    onCompareLeftScanSelected = ::selectCompareLeftScan,
    onCompareRightScanSelected = ::selectCompareRightScan,
    onModelCardMeasured = ::setModelCardHeight,
)
