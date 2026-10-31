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
    fun folded_ds_rebalanced_on_the_verdict_window() {
        // The 2026-10-30 verdict window (1248 wide, game area 1144 tall): the full-width top
        // screen (936 tall) would leave 208 px for the inset. v0.4.8 narrows the top screen
        // (centred) just enough to guarantee the inset half the window width instead.
        val regions = foldedPortraitRegions(1248f, 1144f, DsKind.DS, NdsScreen.TOP)
        assertEquals(2, regions.size)
        assertEquals(PxRect(173f, 0f, 901f, 676f), regions[0].dst)
        assertEquals(PxRect(312f, 676f, 624f, 468f), regions[1].dst)
        assertTrue(regions[1].touch)
        // The inset ends exactly at the game area's bottom (the band's top): the band's buttons
        // cannot cover its corners, and the panel (1972 - 676 = 1296 px) stays over half the window.
        assertEquals(1144f, regions[1].dst.y + regions[1].dst.height)
        assertTrue(1972f - 676f > 1972f / 2f)
    }

    @Test
    fun folded_3ds_rebalanced_on_the_verdict_window() {
        val regions = foldedPortraitRegions(1248f, 1144f, DsKind.N3DS, NdsScreen.TOP)
        assertEquals(2, regions.size)
        assertEquals(PxRect(61f, 0f, 1126f, 676f), regions[0].dst)
        assertEquals(PxRect(312f, 676f, 624f, 468f), regions[1].dst)
        assertTrue(regions[1].touch)
        assertEquals(1144f, regions[1].dst.y + regions[1].dst.height)
    }

    @Test
    fun folded_short_window_rebalances_instead_of_collapsing() {
        // One pixel over the full-width top screen used to mean an empty layout; v0.4.8
        // rebalances (narrower top, minimum inset) instead of leaving the caller with no split.
        val regions = foldedPortraitRegions(1080f, 811f, DsKind.DS, NdsScreen.TOP)
        assertEquals(2, regions.size)
        assertEquals(PxRect(269f, 0f, 541f, 406f), regions[0].dst)
        assertEquals(PxRect(270f, 406f, 540f, 405f), regions[1].dst)
    }

    @Test
    fun folded_empty_when_no_usable_split_fits() {
        assertTrue(foldedPortraitRegions(0f, 1441f, DsKind.DS, NdsScreen.TOP).isEmpty())
        assertTrue(foldedPortraitRegions(1080f, 0f, DsKind.DS, NdsScreen.TOP).isEmpty())
        // A window this short would need a sliver top screen (126 px wide on a 1080 window):
        // keep the current frame instead of drawing it.
        assertTrue(foldedPortraitRegions(1080f, 500f, DsKind.DS, NdsScreen.TOP).isEmpty())
        // A window no taller than the minimum inset (3/8 of the width) holds no split at all.
        assertTrue(foldedPortraitRegions(1080f, 405f, DsKind.DS, NdsScreen.TOP).isEmpty())
        assertTrue(foldedPortraitRegions(1080f, 400f, DsKind.DS, NdsScreen.TOP).isEmpty())
    }

    // The folded portrait panel (v0.4.10): one panel holding the whole pad around a small inset
    // touch screen, replacing the inset-screen + band layout above. Window dims are Ints here
    // (the panel's game area is the full window); the unit is the panel's own (66 px on the cover).

    @Test
    fun panel_ds_big_full_width_inset_centred_in_the_panel() {
        val g = foldedPanelGeometry(1080, 2400, DsKind.DS, 2.625f, 3f)!!
        assertEquals(PxRect(0f, 0f, 1080f, 810f), g.big)
        // The inset is 0.40 of the window width (the folded mockup's share), centred, its top
        // below the pill row (150) and its gap (23.1) — the clusters flank it, never cover it.
        assertEquals(PxRect(324f, 983f, 432f, 324f), g.inset)
        assertEquals(810f, g.panelTop)
        assertEquals(66f, g.unitPx)
        assertEquals(150f, g.pillBoxH)
        assertEquals(143.4f, g.bottomBoxH, 0.01f)
        // The panel (everything below the big screen) is 1590 px, 66% of the window — more than half.
        assertTrue(2400f - g.panelTop > 2400f / 2f)
        // The d-pad's right edge (pad + nudge + 3 units = 253.2) clears the inset's left edge (324).
        assertTrue(42f + 0.2f * 66f + 3f * 66f < g.inset.x)
        // The face diamond's left edge mirrors it against the inset's right edge (756).
        assertTrue(1080f - 42f - 0.2f * 66f - 3f * 66f > g.inset.x + g.inset.width)
    }

    @Test
    fun panel_3ds_big_5_to_3_inset_4_to_3() {
        val g = foldedPanelGeometry(1080, 2400, DsKind.N3DS, 2.625f, 3f)!!
        assertEquals(PxRect(0f, 0f, 1080f, 648f), g.big)
        assertEquals(PxRect(324f, 821f, 432f, 324f), g.inset)
        assertEquals(648f, g.panelTop)
        assertTrue(2400f - g.panelTop > 2400f / 2f)
    }

    @Test
    fun panel_narrows_the_big_screen_before_shrinking_the_inset() {
        // The 1080 x 1441 game area: the panel's rows need 752.7 px, the full-width big screen
        // leaves only 631 — so the big screen narrows (centred) and the inset keeps its 0.40 floor.
        val g = foldedPanelGeometry(1080, 1441, DsKind.DS, 2.625f, 3f)!!
        assertEquals(PxRect(81f, 0f, 917f, 688f), g.big)
        assertEquals(PxRect(324f, 861f, 432f, 324f), g.inset)
    }

    @Test
    fun panel_regions_carry_touch_and_shade_flags_including_swaps() {
        val regions = foldedPanelRegions(1080, 2400, DsKind.DS, NdsScreen.TOP, 2.625f, 3f)!!
        assertEquals(2, regions.size)
        assertEquals(NdsScreen.TOP, regions[0].screen)
        assertEquals(PxRect(0f, 0f, 1080f, 810f), regions[0].dst)
        assertTrue(regions[0].shaded)
        assertTrue(!regions[0].touch)
        assertEquals(NdsScreen.BOTTOM, regions[1].screen)
        assertEquals(PxRect(324f, 983f, 432f, 324f), regions[1].dst)
        assertTrue(regions[1].touch)
        // SWAP: the touch screen goes full width on top (and takes touches there).
        val swapped = foldedPanelRegions(1080, 2400, DsKind.DS, NdsScreen.BOTTOM, 2.625f, 3f)!!
        assertEquals(NdsScreen.BOTTOM, swapped[0].screen)
        assertEquals(PxRect(0f, 0f, 1080f, 810f), swapped[0].dst)
        assertTrue(swapped[0].touch)
        assertEquals(NdsScreen.TOP, swapped[1].screen)
        assertEquals(PxRect(324f, 983f, 432f, 324f), swapped[1].dst)
        assertTrue(!swapped[1].touch)
    }

    @Test
    fun panel_empty_when_no_usable_panel_fits() {
        assertTrue(foldedPanelGeometry(0, 2400, DsKind.DS, 2.625f, 3f) == null)
        assertTrue(foldedPanelGeometry(1080, 0, DsKind.DS, 2.625f, 3f) == null)
        // A window this short would need a sliver big screen (329 px wide on a 1080 window):
        // keep the current frame instead of drawing it — the inset never collapses to a sliver.
        assertTrue(foldedPanelGeometry(1080, 1000, DsKind.DS, 2.625f, 3f) == null)
        assertTrue(foldedPanelRegions(1080, 1000, DsKind.DS, NdsScreen.TOP, 2.625f, 3f) == null)
    }
}
