package com.nxredux.mobile.layout

import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.PxRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabletopDsTest {
    private val reach = Reach(42f + 24f, 366f)

    private fun place(kind: DsKind, controller: Boolean = false, h: Int = 2448, hingeBottom: Int = 1236) =
        tabletopDs(1848, h, ScreenClass.NEAR_SQUARE_TABLET, 0, 1212, hingeBottom, kind, 122f, reach, 2.625f, controller)

    @Test
    fun fold_half_folded_ds_top_6x_bottom_4x_meeting_at_the_fold() {
        val p = place(DsKind.DS)
        assertEquals(TabletopDsMode.SPLIT, p.mode)
        // 3/4 of the 432 px reach set aside each side: 1200 px wide, 4x; the screens meet the hinge's two edges.
        assertEquals(PxRect(156f, 1212f - 1152f, 1536f, 1152f), p.top)
        assertEquals(PxRect(412f, 1236f, 1024f, 768f), p.bottom)
        assertNotNull(p.left)
        assertNotNull(p.right)
    }

    @Test
    fun fold_half_folded_3ds_fills() {
        val p = place(DsKind.N3DS)
        assertEquals(1848f, p.top.width)
        assertEquals(1108f, p.top.height)
        assertEquals(0f, p.top.x)
        assertEquals(1212f - 1108f, p.top.y)
        // bottom space is 1200 x 1212: width-limited 4:3
        assertEquals(1200f, p.bottom.width)
        assertEquals(900f, p.bottom.height)
        assertEquals(324f, p.bottom.x)
        assertEquals(1236f, p.bottom.y)
    }

    @Test
    fun controller_ds_bottom_grows() {
        val p = place(DsKind.DS, controller = true)
        assertNull(p.left)
        assertNull(p.right)
        // whole bottom area 1848 x 1212: 7x (1792 x 1344) is too tall, 6x fits
        assertEquals(PxRect(156f, 1236f, 1536f, 1152f), p.bottom)
        assertEquals(1212f, p.top.y + p.top.height)   // still meeting at the fold
    }

    @Test
    fun touch_screen_overlaps_each_half_by_a_quarter_of_its_reach_at_most() {
        for (kind in DsKind.entries) {
            val p = place(kind)
            assertTrue(reach.total - p.bottom.x <= reach.total / 4f, "$kind overlaps by ${reach.total - p.bottom.x}")
        }
    }

    @Test
    fun bottom_never_below_1x() {
        val p = place(DsKind.DS, h = 1436, hingeBottom = 1236)   // a 200 px bottom area: 1x, still split
        assertEquals(TabletopDsMode.SPLIT, p.mode)
        assertEquals(256f, p.bottom.width)
        assertEquals(192f, p.bottom.height)
    }

    @Test
    fun flip_half_folded_stacks_both_on_top() {
        val p = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.DS, 100f, reach, 2.625f, false)
        assertEquals(TabletopDsMode.STACKED_TOP, p.mode)
        assertNull(p.left)
        assertNull(p.right)
        // 1080 / 256 = 4.2 and 1308 / 384 = 3.4: 3x
        assertEquals(PxRect(156f, 78f, 768f, 576f), p.top)
        assertEquals(PxRect(156f, 654f, 768f, 576f), p.bottom)
        val n = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.N3DS, 100f, reach, 2.625f, false)
        assertEquals(TabletopDsMode.STACKED_TOP, n.mode)
        assertEquals(1080f, n.top.width)
        assertEquals(n.top.y + n.top.height, n.bottom.y)
    }

    @Test
    fun tabletop_unit_is_cover_size() {
        assertTrue(kotlin.math.abs(tabletopDsUnitPx(404.6f, 2.625f) - 122.7f) < 1f)
    }

    @Test
    fun n3ds_swap_fits_each_rect_to_the_screen_it_shows() {
        val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1212, 1236, DsKind.N3DS, 122f, reach, 2.625f, false, big = NdsScreen.BOTTOM)
        // Above the hinge the 4:3 touch screen: 1212 tall, 1616 wide, centred, meeting the fold.
        assertEquals(PxRect(116f, 0f, 1616f, 1212f), p.top)
        // Below it the 5:3 top screen in the 1200 x 1212 space: 1200 x 720, against the hinge.
        assertEquals(PxRect(324f, 1236f, 1200f, 720f), p.bottom)
    }

    @Test
    fun n3ds_stacked_swap_keeps_the_touch_screen_narrow() {
        val n = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.N3DS, 100f, reach, 2.625f, false)
        val s = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.N3DS, 100f, reach, 2.625f, false, big = NdsScreen.BOTTOM)
        assertEquals(n.bottom.width, s.top.width)
        assertEquals(n.top.width, s.bottom.width)
        assertEquals(n.top.y, s.top.y)
        assertEquals(s.top.y + s.top.height, s.bottom.y)
        assertEquals(n.bottom.x, s.top.x)
    }

    @Test
    fun ds_swap_changes_nothing() {
        assertEquals(place(DsKind.DS), tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1212, 1236, DsKind.DS, 122f, reach, 2.625f, false, big = NdsScreen.BOTTOM))
    }

    @Test
    fun bottom_area_too_short_for_1x_stacks_above_the_hinge() {
        // A 150 px bottom area: no 1x DS touch screen fits there, so both screens go above the hinge.
        val p = place(DsKind.DS, h = 1386, hingeBottom = 1236)
        assertEquals(TabletopDsMode.STACKED_TOP, p.mode)
        assertNull(p.left)
        assertTrue(p.bottom.y + p.bottom.height <= 1212f)
    }

    @Test
    fun top_area_too_short_for_1x_stacks() {
        // A 150 px top area cannot hold a 1x top screen: the stack's own 1x floor applies there instead.
        val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 150, 174, DsKind.DS, 122f, reach, 2.625f, false)
        assertEquals(TabletopDsMode.STACKED_TOP, p.mode)
    }

    @Test
    fun top_ds_screen_at_least_1x() {
        val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 200, 224, DsKind.DS, 122f, reach, 2.625f, false)
        assertEquals(TabletopDsMode.SPLIT, p.mode)
        assertEquals(PxRect(796f, 8f, 256f, 192f), p.top)
    }

    @Test
    fun controller_3ds_bottom_fills_the_bottom_area() {
        val p = place(DsKind.N3DS, controller = true)
        assertNull(p.left)
        // 1848 x 1212: height-limited 4:3, 1616 x 1212, centred, against the hinge.
        assertEquals(PxRect(116f, 1236f, 1616f, 1212f), p.bottom)
        assertEquals(1212f, p.top.y + p.top.height)
    }

    @Test
    fun wide_tablet_splits() {
        val p = tabletopDs(2560, 1600, ScreenClass.WIDE_TABLET, 0, 790, 810, DsKind.DS, 122f, reach, 2.625f, false)
        assertEquals(TabletopDsMode.SPLIT, p.mode)
        assertNotNull(p.left)
    }

    @Test
    fun flip_stacked_3ds_bottom_centred_under_the_top() {
        val n = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.N3DS, 100f, reach, 2.625f, false)
        // 1080 wide frame, 648 per screen: the touch screen 864 wide, centred under the top one.
        assertEquals(PxRect(0f, 6f, 1080f, 648f), n.top)
        assertEquals(PxRect(108f, 654f, 864f, 648f), n.bottom)
    }

    @Test
    fun touch_screen_stays_between_the_set_asides() {
        val aside = LayoutConstants.TABLETOP_DS_SET_ASIDE * reach.total
        for (kind in DsKind.entries) {
            val p = place(kind)
            assertTrue(p.bottom.x >= aside, "$kind left")
            assertTrue(p.bottom.x + p.bottom.width <= 1848f - aside, "$kind right")
        }
    }

    @Test
    fun editor_preview_draws_the_game_layout_at_the_fold() {
        val pv = assertNotNull(
            halfFoldedPreview(1848f, 2448f, ScreenClass.NEAR_SQUARE_TABLET, 0f, 404.6f, 2.625f, 0f, 0f, true, 3.3f, 2, DsKind.DS),
        )
        assertEquals(1224f, pv.hingeY)
        assertEquals(1224f, pv.placement.top.y + pv.placement.top.height)
        assertEquals(1224f, pv.placement.bottom.y)
        assertTrue(pv.boxes.topMarginPx >= 1224 && pv.boxes.topMarginPx + pv.boxes.heightPx <= 2448)
        assertEquals(tabletopDsUnitPx(404.6f, 2.625f), pv.unitPx)
        // A phone-shaped window still gets the halves (laid out as a tablet-class one).
        assertNotNull(halfFoldedPreview(1080f, 2400f, ScreenClass.REGULAR_PHONE, 0f, 400f, 2.625f, 0f, 0f, true, 3.3f, 2, DsKind.N3DS))
    }

    /** The 1.1-scale virtual pad's unit and estimate reach, scaled from the 0.85-scale inputs above. */
    private val unit11 = 122f * 1.1f / 0.85f
    private val reach11 = Reach(42f + 0.2f * unit11, 366f * unit11 / 122f)

    /** The 1.1-scale virtual pad with measured (drawn) buttons: the device-measured extent at 0.85 was ~335 px
     * from each edge, so the groups measure ~269 px there and ~348 px at 1.1 — well under the 474 px estimate. */
    private val reach11Measured = Reach(42f + 0.2f * unit11, 348f)

    @Test
    fun deck_touch_screen_sits_on_the_hinge_below_the_top_screen() {
        val ds = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, DsKind.DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        assertEquals(TabletopDsMode.SPLIT, ds.mode)
        // v0.4.10: the deck fit — the bottom half (1224) less the touch-cluster gap, the 3-unit
        // cluster row, the cluster-pill gap and the pill row leaves 484 px, so a 4:3 screen is
        // 645 x 483, centred, its top edge ON the fold. Narrower than v0.4.7's 1018 px drawn box:
        // the old width belonged to the flanking split, not the deck.
        assertEquals(PxRect(601f, 1224f, 645f, 483f), ds.bottom)
        // Top screen unchanged: 6x with its bottom edge on the 1224 fold.
        assertEquals(PxRect(156f, 72f, 1536f, 1152f), ds.top)
        assertNotNull(ds.left)
        assertNotNull(ds.right)
        val n3ds = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, DsKind.N3DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        assertEquals(TabletopDsMode.SPLIT, n3ds.mode)
        // Top screen unchanged: full window width, its bottom edge on the 1224 fold.
        assertEquals(PxRect(0f, 116f, 1848f, 1108f), n3ds.top)
        // The touch screen shows the bottom screen (4:3), so the same deck fit as DS.
        assertEquals(PxRect(601f, 1224f, 645f, 483f), n3ds.bottom)
        assertNotNull(n3ds.left)
        assertNotNull(n3ds.right)
    }

    @Test
    fun deck_touch_size_is_reach_independent() {
        // The deck fit sizes the touch screen, not the reach — the old estimate-to-measured
        // resize step is gone: estimate and measured reaches give the identical rect, both kinds.
        for (kind in DsKind.entries) {
            val est = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, kind, unit11, reach11, 2.625f, false, bottomCentred = true)
            val measured = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, kind, unit11, reach11Measured, 2.625f, false, bottomCentred = true)
            assertEquals(est.bottom, measured.bottom, "$kind touch is reach-independent")
        }
    }

    @Test
    fun deck_halves_pinned_at_the_hinge_with_the_computed_spacer() {
        val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, DsKind.N3DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        // The grown boxes are pinned at the hinge (their tops on the fold).
        assertEquals(1224f, p.left!!.topMarginPx)
        val box = tabletopDsPadBoxes(p, 2448)!!
        assertEquals(1224, box.topMarginPx)
        // The box holds shoulders + the deck spacer + cluster, plus one pad: 483 + 55.26 + 473.64
        // + 42 = 1054 px, 605 px wide (3.3 units + 2 pads), still inside the window.
        assertEquals(605, box.widthPx)
        assertEquals(1054, box.heightPx)
        assertTrue(box.topMarginPx + box.heightPx <= 2448)
        // The spacer itself, computed from the touch fit: 483 + 55.26 - 42 - 157.88 = 338.38 px.
        assertEquals(338.38f, p.deckSpacerPx!!, 0.5f)
    }

    @Test
    fun deck_clusters_clear_the_touch_screen_and_the_pill_row() {
        val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, DsKind.DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        // The clusters sit strictly below the touch screen (never beside it): their tops one
        // touch-cluster gap (0.35 units) below its bottom edge, their bottoms one cluster-pill
        // gap above the pill row's top — the v0.4.9 corner overlap cannot recur in this geometry.
        val clusterTop = p.left!!.topMarginPx + 42f + unit11 + p.deckSpacerPx!!
        val clusterBottom = clusterTop + 3f * unit11
        val pillTop = 2448f - deckPillRowHeightPx(unit11, 2.625f)
        assertEquals(55.26f, clusterTop - (p.bottom.y + p.bottom.height), 0.5f)
        assertEquals(56.42f, pillTop - clusterBottom, 0.5f)
        assertTrue(clusterTop > p.bottom.y + p.bottom.height)
        assertTrue(clusterBottom < pillTop)
    }

    @Test
    fun deck_width_cap_binds_on_a_narrow_tablet() {
        // A 1000 px window: the height fit (645 px) would run under L / mic / R, so the
        // shoulders' extent caps the touch at 210 px wide — still a usable 210 x 157 screen
        // with a non-negative spacer (12.38 px), not a degenerate stack.
        val p = tabletopDs(1000, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, DsKind.DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        assertEquals(TabletopDsMode.SPLIT, p.mode)
        assertEquals(PxRect(395f, 1224f, 210f, 157f), p.bottom)
        assertTrue(p.deckSpacerPx!! >= 0f)
    }

    @Test
    fun deck_degenerate_short_window_stacks() {
        // A bottom half too short for the deck's rows (700 px: the fit goes negative) falls
        // back to the stack, as for any split that cannot fit.
        val p = tabletopDs(1848, 1400, ScreenClass.NEAR_SQUARE_TABLET, 0, 700, 700, DsKind.DS, unit11, reach11, 2.625f, false, bottomCentred = true)
        assertEquals(TabletopDsMode.STACKED_TOP, p.mode)
    }

    @Test
    fun deck_screens_touch_at_the_hinge() {
        // The touch screen's top edge is ON the fold, so the two screens touch each other.
        for (kind in DsKind.entries) {
            val p = tabletopDs(1848, 2448, ScreenClass.NEAR_SQUARE_TABLET, 0, 1224, 1224, kind, unit11, reach11Measured, 2.625f, false, bottomCentred = true)
            assertEquals(1224f, p.top.y + p.top.height, "$kind top meets the fold")
            assertEquals(1224f, p.bottom.y, "$kind touch sits on the fold")
        }
    }

    @Test
    fun a_reading_the_split_cannot_fit_is_not_used_on_the_fold() {
        val bad = place(DsKind.DS, h = 1386, hingeBottom = 1236)   // 150 px below the hinge
        assertEquals(TabletopDsMode.STACKED_TOP, bad.mode)
        assertFalse(usableTabletop(spansHinge = true, bad))
        assertTrue(usableTabletop(spansHinge = true, place(DsKind.DS)))
        assertTrue(usableTabletop(spansHinge = true, place(DsKind.N3DS, controller = true)))
        // The Flip's stack (its game view above the hinge, the portrait pad below) is the layout itself.
        val flip = tabletopDs(1080, 2640, ScreenClass.REGULAR_PHONE, 0, 1308, 1332, DsKind.DS, 100f, reach, 2.625f, false)
        assertTrue(usableTabletop(spansHinge = false, flip))
    }
}
