package com.hexis.bi.utils.constants

/**
 * Every UI animation duration in the app, in one place so pacing can be tuned without hunting
 * through individual screen/component files.
 */
internal object AnimationConstants {

    /** Loading scrim (BaseScreen) shown above screen content while a load is in flight. */
    const val SCRIM_FADE_IN_MS = 150
    const val SCRIM_FADE_OUT_MS = 250

    /** Selected-pill + label color crossfade on the custom tab selector (AppTabSelector). */
    const val TAB_SELECTION_MS = 250

    /** Crossfade when AnimatedContent swaps one tab's content for another's. */
    const val TAB_CONTENT_FADE_IN_MS = 200
    const val TAB_CONTENT_FADE_OUT_MS = 150

    /** Bars/progress fills growing from 0 to their value: mini bar charts, sleep progress bar,
     * activity bar charts, sleep structure bars. */
    const val BAR_GROWTH_MS = 450

    /** Circular gauges/rings sweeping in: Home intelligence gauges, Activity rings. */
    const val ARC_FILL_MS = 500

    /** Sheen sweeping across a glass card while its data loads. */
    const val GLASS_LOADING_SWEEP_MS = 1400
    const val GLASS_LOADING_BAND_FRACTION = 0.45f
    const val GLASS_LOADING_SHEEN_ALPHA = 0.2f
    const val GLASS_LOADING_FADE_IN_MS = 300
    const val GLASS_LOADING_FADE_OUT_MS = 1100


    /** Line charts wiping in left-to-right: scan sparkline, sleep HRV/RHR chart, hypnogram. */
    const val LINE_WIPE_MS = 600

    /** "Updating…" banner expanding above insight content while the engine re-runs. */
    const val UPDATING_BANNER_MS = 250
}
