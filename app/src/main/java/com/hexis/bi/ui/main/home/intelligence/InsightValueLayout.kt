package com.hexis.bi.ui.main.home.intelligence

internal data class InsightValueSize(val width: Int, val height: Int)

internal data class InsightValuePosition(
    val valueX: Int,
    val valueY: Int,
    val separatorX: Int?,
    val separatorY: Int?,
)

internal data class InsightValueLayoutResult(
    val positions: List<InsightValuePosition>,
    val height: Int,
)

internal fun calculateInsightValueLayout(
    values: List<InsightValueSize>,
    separator: InsightValueSize,
    maxWidth: Int,
    rowGap: Int,
): InsightValueLayoutResult {
    require(maxWidth >= 0)
    require(rowGap >= 0)

    val positions = ArrayList<InsightValuePosition>(values.size)
    var x = 0
    var y = 0
    var rowHeight = 0

    values.forEachIndexed { index, value ->
        val separatorWidth = if (index > 0) separator.width else 0
        if (x > 0 && x + separatorWidth + value.width > maxWidth) {
            y += rowHeight + rowGap
            x = 0
            rowHeight = 0
        }
        val separatorX = if (x > 0 && index > 0) x else null
        if (separatorX != null) x += separator.width
        positions += InsightValuePosition(
            valueX = x,
            valueY = y,
            separatorX = separatorX,
            separatorY = separatorX?.let { y },
        )
        x += value.width
        rowHeight = maxOf(rowHeight, value.height, if (separatorX != null) separator.height else 0)
    }
    return InsightValueLayoutResult(positions, y + rowHeight)
}
