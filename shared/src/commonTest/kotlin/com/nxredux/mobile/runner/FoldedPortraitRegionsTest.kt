package com.nxredux.mobile.runner

import com.nxredux.mobile.layout.DsKind
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.PxRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoldedPortraitRegionsTest {
    // The cover window 1080 x 2400 with the ~959 px portrait band: the game area is 1080 x 1441.

    @Test
    fun folded_ds_top_full_width_bottom_inset_in_the_panel() {
        val regions = foldedPortraitRegions(1080f, 1441f, DsKind.DS, NdsScreen.TOP)
        assertEquals(2, regions.size)
        // Top screen full window width on top, from the very top edge (under the camera hole).
        val top = regions[0]
        assertEquals(NdsScreen.TOP, top.screen)
        assertEquals(PxRect(0f, 0f, 1080f, 810f), top.dst)
        assertTrue(top.shaded)
        // Bottom screen inset in the panel below it, its top edge on the top screen's bottom edge;
        // it fills the panel height left above the band (631 px), not the width.
        val bottom = regions[1]
        assertEquals(NdsScreen.BOTTOM, bottom.screen)
        assertEquals(PxRect(119f, 810f, 841f, 630f), bottom.dst)
        assertTrue(bottom.touch)
        // The panel (everything below the top screen: inset screen + band) is 2400 - 810 = 1590 px,
        // 66% of the window height — more than half, as asked.
        assertTrue(2400f - 810f > 2400f / 2f)
        // The inset screen ends at the band's top (the game area's bottom), within the 1 px
        // whole-pixel flooring (630.75 floors to 630).
        assertTrue(1441f - (bottom.dst.y + bottom.dst.height) <= 1f)
    }

    @Test
    fun folded_3ds_top_5_to_3_bottom_4_to_3_inset() {
        val regions = foldedPortraitRegions(1080f, 1441f, DsKind.N3DS, NdsScreen.TOP)
        assertEquals(2, regions.size)
        assertEquals(PxRect(0f, 0f, 1080f, 648f), regions[0].dst)
        // The panel height left above the band is 793 px, so the 4:3 bottom screen is 1057 x 792 —
        // nearly full width, inset by 11 px a side.
        assertEquals(PxRect(11f, 648f, 1057f, 792f), regions[1].dst)
        assertTrue(regions[1].touch)
        assertTrue(2400f - 648f > 2400f / 2f)
    }

    @Test
    fun folded_ds_on_a_narrower_phone_window() {
        val regions = foldedPortraitRegions(720f, 1000f, DsKind.DS, NdsScreen.TOP)
        assertEquals(PxRect(0f, 0f, 720f, 540f), regions[0].dst)
        assertEquals(PxRect(53f, 540f, 613f, 459f), regions[1].dst)
    }

    @Test
    fun folded_big_bottom_swaps_which_screen_is_full_width() {
        val regions = foldedPortraitRegions(1080f, 1441f, DsKind.DS, NdsScreen.BOTTOM)
        // The bottom screen goes full width on top (and takes touches there); the top screen insets below.
        assertEquals(NdsScreen.BOTTOM, regions[0].screen)
        assertEquals(PxRect(0f, 0f, 1080f, 810f), regions[0].dst)
        assertTrue(regions[0].touch)
        assertEquals(NdsScreen.TOP, regions[1].screen)
        assertEquals(PxRect(119f, 810f, 841f, 630f), regions[1].dst)
        assertTrue(!regions[1].touch)
    }

    @Test
    fun folded_empty_when_no_top_over_inset_fits() {
        assertTrue(foldedPortraitRegions(0f, 1441f, DsKind.DS, NdsScreen.TOP).isEmpty())
        assertTrue(foldedPortraitRegions(1080f, 0f, DsKind.DS, NdsScreen.TOP).isEmpty())
        // A window shorter than the full-width top screen holds no split.
        assertTrue(foldedPortraitRegions(1080f, 500f, DsKind.DS, NdsScreen.TOP).isEmpty())
        // One pixel of panel height leaves no pixel of inset screen (1 x 0.75 floors to 0 tall).
        assertTrue(foldedPortraitRegions(1080f, 811f, DsKind.DS, NdsScreen.TOP).isEmpty())
    }
}
