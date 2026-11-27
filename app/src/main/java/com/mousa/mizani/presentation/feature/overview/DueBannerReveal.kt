package com.mousa.mizani.presentation.feature.overview

/** When the Overview list scrolls up to show a due banner that was just added above the first card. */
object DueBannerReveal {

    /** Items 0 and 1: the top of the list, before or after the banner is inserted above it. */
    private const val LAST_TOP_INDEX = 1

    fun shouldScrollToBanner(previousCount: Int, newCount: Int, firstVisibleIndex: Int): Boolean =
        previousCount == 0 && newCount > 0 && firstVisibleIndex <= LAST_TOP_INDEX
}
