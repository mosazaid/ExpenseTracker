package com.mousa.mizani.presentation

import com.mousa.mizani.presentation.feature.overview.DueBannerReveal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DueBannerRevealTest {

    @Test
    fun `banner appearing while the list is at the top scrolls to it`() {
        assertTrue(DueBannerReveal.shouldScrollToBanner(previousCount = 0, newCount = 2, firstVisibleIndex = 0))
        // The list keeps the first card in view by key, so after the insert it is item 1.
        assertTrue(DueBannerReveal.shouldScrollToBanner(previousCount = 0, newCount = 1, firstVisibleIndex = 1))
    }

    @Test
    fun `banner appearing after the user scrolled down leaves the list alone`() {
        assertFalse(DueBannerReveal.shouldScrollToBanner(previousCount = 0, newCount = 1, firstVisibleIndex = 3))
    }

    @Test
    fun `count changing while the banner is already shown does not scroll`() {
        assertFalse(DueBannerReveal.shouldScrollToBanner(previousCount = 1, newCount = 2, firstVisibleIndex = 1))
    }

    @Test
    fun `no banner does not scroll`() {
        assertFalse(DueBannerReveal.shouldScrollToBanner(previousCount = 0, newCount = 0, firstVisibleIndex = 0))
        assertFalse(DueBannerReveal.shouldScrollToBanner(previousCount = 2, newCount = 0, firstVisibleIndex = 0))
    }
}
