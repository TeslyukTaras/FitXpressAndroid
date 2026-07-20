package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.utils.constants.BodyVisualConstants

private enum class BodyTabSlot { CompactCard, Content }

/**
 * Model panel of constant height; the card below takes what is left down to the nav bar.
 *
 * [compactCard] is subcomposed and measured but never placed. Pass the tallest single-part variant:
 * the panel height then never depends on the selection, so picking a part leaves the GL surface
 * alone and the full-body card just grows past the fold. Below `body_model_min_height` the panel
 * stops shrinking and the page scrolls.
 *
 * @param reservedBelowModel Compare's part selector, which belongs to neither.
 * @param cardHeightFraction share of the measured card to reserve; My Body's card is never compact.
 */
@Composable
internal fun BodyTabLayout(
    navClearance: Dp,
    cardHorizontalPadding: Dp,
    compactCard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    reservedBelowModel: Dp = 0.dp,
    cardHeightFraction: Float = 1f,
    figureHeightFraction: Float = BodyVisualConstants.FULL_BODY_FIGURE_HEIGHT_FRACTION,
    content: @Composable (modelAreaHeight: Dp, compactCardHeight: Dp, fullBodyFigureHeight: Dp) -> Unit,
) {
    val minModelHeight = dimensionResource(R.dimen.body_model_min_height)
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val cardWidth = (constraints.maxWidth - cardHorizontalPadding.roundToPx() * 2)
            .coerceAtLeast(0)
        val referenceCardHeight = subcompose(BodyTabSlot.CompactCard, compactCard)
            .maxOfOrNull { it.measure(Constraints.fixedWidth(cardWidth)).height }
            ?: 0
        val compactCardHeight = (referenceCardHeight * cardHeightFraction).toInt()

        val available = constraints.maxHeight -
                navClearance.roundToPx() -
                reservedBelowModel.roundToPx() -
                compactCardHeight
        val modelAreaHeight = available.coerceAtLeast(minModelHeight.roundToPx())

        // Every tab occupies the same slot under the header, so maxHeight is identical across the three.
        val figureHeight = constraints.maxHeight * figureHeightFraction

        val placeables = subcompose(BodyTabSlot.Content) {
            content(modelAreaHeight.toDp(), compactCardHeight.toDp(), figureHeight.toDp())
        }.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}
