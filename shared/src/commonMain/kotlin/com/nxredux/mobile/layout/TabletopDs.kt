package com.nxredux.mobile.layout

import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.PxRect
import com.nxredux.mobile.runner.NdsLayout
import com.nxredux.mobile.ui.nx.VIRTUAL_HINGE_CLUSTER_INSET_UNITS
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Which two-screen console a half-folded layout is for. */
enum class DsKind { DS, N3DS }

/** SPLIT: top screen above the hinge, touch screen below it. STACKED_TOP: both screens stacked above the hinge. */
enum class TabletopDsMode { SPLIT, STACKED_TOP }

/**
 * Where the half-folded Fold puts a DS / 3DS. SPLIT: the [top] screen above the hinge, the [bottom] (touch) screen
 * below it, and the pad halves ([left], [right]) beside the bottom screen; both clusters are null with a controller.
 * STACKED_TOP: [top] and [bottom] are the two screens stacked in the area above the hinge and the clusters are null
 * (the normal portrait pad sits below the hinge).
 * The flat-unfolded virtual hinge's deck ([bottomCentred]) additionally carries [deckSpacerPx]: the spacer the halves
 * render between their shoulders and cluster rows, so the clusters sit below the touch screen (see [deckTouchSize]).
 */
data class TabletopDsPlacement(
    val mode: TabletopDsMode,
    val top: PxRect,
    val bottom: PxRect,
    val left: ClusterPlacement?,
    val right: ClusterPlacement?,
    val deckSpacerPx: Float? = null,
)

private const val DS_W = 256f
private const val DS_H = 192f
private const val N3DS_TOP_W = 400f
private const val N3DS_BOTTOM_W = 320f
private const val N3DS_H = 240f
private const val EPSILON = 0.001f

/** The centred (virtual-hinge) touch screen's gap below the hinge, dp: zero — the touch screen touches the
 * hinge line, so the two screens touch each other (v0.4.6; v0.4.5's 12 dp sliver is gone). */
private const val VIRTUAL_HINGE_GAP_DP = 0f

/**
 * The virtual-hinge deck's gaps, in units: touch screen bottom to cluster tops, and cluster bottoms
 * to the pill row top. The clusters sit strictly below the touch screen (v0.4.10: screens-stacked
 * + control deck, replacing the v0.4.3–v0.4.9 split whose clusters flanked the touch screen), so
 * the touch screen's corners can never meet a button — the clearances are vertical, not side margins.
 */
const val DECK_TOUCH_CLUSTER_GAP_UNITS = 0.35f
internal const val DECK_CLUSTER_PILL_GAP_UNITS = 0.35f

/** The deck's rows, in units: the shoulders row and the cluster row (the d-pad / face diamond, 3 units each). */
private const val DECK_SHOULDER_UNITS = 1f
private const val DECK_CLUSTER_UNITS = 3f

/** The deck's bottom pill row height, px — the same formula the host uses for its container (0.72 unit + one pad). */
fun deckPillRowHeightPx(unitPx: Float, density: Float): Float =
    0.72f * unitPx + LayoutConstants.CLUSTER_PADDING_DP * density

/**
 * How far the deck's shoulders reach in from each window edge, px: the pad + nudge + bumper + mic
 * gap + mic (left; the right, bumper only, is shorter). The touch screen's width cap, so it never
 * runs under L / mic / R where they flank its upper band.
 */
internal fun deckShoulderExtentPx(unitPx: Float, density: Float): Float {
    val pad = LayoutConstants.CLUSTER_PADDING_DP * density
    return pad + LayoutConstants.EDGE_NUDGE_UNITS * unitPx + DECK_SHOULDER_UNITS * unitPx +
        8f * density + 0.9f * unitPx
}

/**
 * The deck's touch screen size, whole px, or null when no usable touch fits: the largest 4:3
 * (DS, and a 3DS bottom screen) or top-screen-aspect (a swapped 3DS) screen whose height fits
 * the bottom half after the touch-cluster gap, the cluster row, the cluster-pill gap and the
 * pill row — capped by the width the shoulders leave. The height binds on phone windows, so
 * the touch lands near the unfolded mockup's share (~39% of the width on the test window's
 * device scale). The halves' shoulders-to-cluster spacer follows from the same fit (see
 * [deckSpacerPx]), so the cluster tops sit exactly one touch-cluster gap below the touch bottom.
 * Null when the fit goes degenerate (a touch under 1 px, or a negative spacer): the caller
 * falls back to the stack, as for any split that cannot fit.
 */
internal fun deckTouchSize(
    windowW: Float,
    bottomHalfH: Float,
    kind: DsKind,
    big: NdsScreen,
    unitPx: Float,
    density: Float,
): Pair<Float, Float>? {
    val pad = LayoutConstants.CLUSTER_PADDING_DP * density
    val maxH = bottomHalfH - DECK_TOUCH_CLUSTER_GAP_UNITS * unitPx - DECK_CLUSTER_UNITS * unitPx -
        DECK_CLUSTER_PILL_GAP_UNITS * unitPx - deckPillRowHeightPx(unitPx, density)
    val aspect = if (kind == DsKind.DS) DS_W / DS_H else n3dsAspect(big.other)
    var touchW = maxH * aspect
    val maxW = windowW - 2f * deckShoulderExtentPx(unitPx, density)
    if (touchW > maxW) touchW = maxW
    if (touchW < 1f) return null
    // The height follows the ROUNDED width (whole px): deriving it from the raw fit would
    // land a pixel taller (484, not 483, on the test window) and throw every downstream
    // number — the spacer, the box, the pill clearance — off by that pixel.
    val touchWR = NdsLayout.wholePx(touchW)
    val touchH = touchWR / aspect
    if (touchH < 1f) return null
    if (deckSpacerPx(touchH, unitPx, density) < 0f) return null
    return Pair(touchWR, NdsLayout.wholePx(touchH))
}

/**
 * The halves' shoulders-to-cluster spacer for a deck touch screen [touchH] tall, px: the spacer
 * puts the cluster tops exactly one touch-cluster gap below the touch bottom, given the halves'
 * box padding and the shoulders row above it.
 */
internal fun deckSpacerPx(touchH: Float, unitPx: Float, density: Float): Float {
    val pad = LayoutConstants.CLUSTER_PADDING_DP * density
    return touchH + DECK_TOUCH_CLUSTER_GAP_UNITS * unitPx - pad - DECK_SHOULDER_UNITS * unitPx
}

/** The deck's side boxes' width, px: the cluster row (nudge + inset + 3 units) inside the box padding. */
internal fun deckSideBoxWidthPx(unitPx: Float, density: Float): Float {
    val pad = LayoutConstants.CLUSTER_PADDING_DP * density
    return (LayoutConstants.EDGE_NUDGE_UNITS + VIRTUAL_HINGE_CLUSTER_INSET_UNITS + DECK_CLUSTER_UNITS) * unitPx + 2f * pad
}

/**
 * The top screen fills the area above the hinge (x 0..[windowW], [cutoutTop]..[hingeTop]); the bottom screen sits in
 * the area below it ([hingeBottom]..[windowH]) between the pad halves. DS screens are whole multiples of 256 x 192
 * (the bottom never below 1x); 3DS screens are fitted at their aspect (400 x 240 top, 320 x 240 bottom) in whole
 * pixels. The halves set aside [LayoutConstants.TABLETOP_DS_SET_ASIDE] of the [reach] each side (the touch screen
 * overlaps each by a quarter of it at most), nothing with a [controller]. The screens meet at the fold, as on a DS:
 * the top one's bottom edge on [hingeTop], the touch screen's top edge on [hingeBottom] (centred: a hinge gap
 * below it), each centred across.
 * A hinge reading that leaves no room for the split (a 1x DS touch screen, or any 3DS one, between the halves below
 * it, or the top screen above it) returns STACKED_TOP: on a tablet-class window that reading is not used for the DS
 * layout, and the game lays out flat instead ([usableTabletop]). Only a tablet-class [screen] has the width for the touch screen
 * between the pad halves; a phone-shaped foldable stacks both screens above the hinge instead. [secondRow]: the halves
 * draw their second row (STYLUS / SWAP), so their boxes are that row taller, as the landscape pad's. [big]: the screen
 * drawn first (above the hinge, or on top in the stack); after a 3DS SWAP the touch screen, so each rect is fitted to
 * the aspect of the screen it shows (the DS's two screens share one). [bottomCentred]: the flat-unfolded virtual hinge's
 * deck (v0.4.10: screens-stacked + control deck, per Garrett's confirmed unfolded mockup — replacing the v0.4.3–v0.4.9
 * split, whose clusters flanked the touch screen and kept overlapping its bottom corners): the top screen fills the top
 * half as before, the touch screen sits centred directly below it with its top edge ON the fold (zero gap — the two
 * screens touch each other) at the size [deckTouchSize] fits below it, L / mic / R flank the touch screen's upper band,
 * the d-pad and face cluster sit BELOW the touch screen (never beside it), the NX mark centres on the cluster strip, and
 * the six pill buttons move to a bottom pill row (VirtualHingePillRow in ui.nx). The halves' boxes are pinned at the
 * hinge and hold shoulders + cluster with the deck spacer between them ([deckSpacerPx], carried on the placement), so
 * the cluster tops sit one touch-cluster gap below the touch bottom. The half-folded layout keeps the quarter-reach
 * allowance and the centred fit.
 */
fun tabletopDs(
    windowW: Int,
    windowH: Int,
    screen: ScreenClass,
    cutoutTop: Int,
    hingeTop: Int,
    hingeBottom: Int,
    kind: DsKind,
    unitPx: Float,
    reach: Reach,
    density: Float,
    controller: Boolean,
    secondRow: Boolean = false,
    big: NdsScreen = NdsScreen.TOP,
    bottomCentred: Boolean = false,
): TabletopDsPlacement {
    val topArea = PxRect(0f, cutoutTop.toFloat(), windowW.toFloat(), (hingeTop - cutoutTop).toFloat())
    val bottomArea = PxRect(0f, hingeBottom.toFloat(), windowW.toFloat(), (windowH - hingeBottom).toFloat())
    if (!screen.isTablet) return stackedTop(topArea, kind, big)
    val aside = if (controller) 0f else LayoutConstants.TABLETOP_DS_SET_ASIDE * reach.total
    val bottomSpace = PxRect(aside, bottomArea.y, bottomArea.width - 2f * aside, bottomArea.height)
    val hingeGapPx = VIRTUAL_HINGE_GAP_DP * density

    val (topW, topH) = when (kind) {
        DsKind.DS -> wholeScale(topArea, DS_W, DS_H, atLeast1 = true)
        DsKind.N3DS -> fitted(topArea, n3dsAspect(big))
    }
    // The deck's touch screen is fitted to the bottom half ([deckTouchSize]), not to the drawn
    // box or the halves: the clusters sit below it, so they no longer bound its width, and the
    // reach no longer sizes it (the old estimate-to-measured resize step is gone). A controller
    // leaves no halves, so the same fit sizes the screen there too; the half-folded layout keeps
    // the quarter-reach set-aside. A null fit is the degenerate guard's trip (below).
    val (bottomW, bottomH) = if (bottomCentred) {
        deckTouchSize(windowW.toFloat(), bottomArea.height, kind, big, unitPx, density) ?: Pair(0f, 0f)
    } else when (kind) {
        DsKind.DS -> wholeScale(bottomSpace, DS_W, DS_H, atLeast1 = true)
        DsKind.N3DS -> fitted(bottomSpace, n3dsAspect(big.other))
    }
    // The touch screen must fit below the hinge, and the top screen above it (DS: 1x at least).
    // The fit itself keeps the non-centred screen inside its set-aside; this is the degenerate guard.
    // The deck's touch is exempt from the set-aside width (its fit already caps it by the shoulders),
    // and a null fit (0 x 0) trips the guard the same way.
    if (bottomW < 1f || bottomH < 1f || topW < 1f || topH < 1f || topH > topArea.height ||
        (!bottomCentred && (bottomW > bottomArea.width || bottomH > bottomSpace.height))
    ) return stackedTop(topArea, kind, big)
    val top = atFold(topArea, topW, topH, y = hingeTop - topH)
    // Deck: the touch screen's top edge sits ON the fold (zero gap), its x centred as before — not
    // vertically centred, which left a dead band above and below it. The top screen already sits on the
    // fold, so the two screens touch each other.
    val bottom = if (bottomCentred) PxRect(
        bottomArea.x + NdsLayout.wholePx((bottomArea.width - bottomW) / 2f),
        hingeBottom.toFloat() + hingeGapPx, bottomW, bottomH,
    ) else atFold(bottomArea, bottomW, bottomH, y = hingeBottom.toFloat())
    if (controller) return TabletopDsPlacement(TabletopDsMode.SPLIT, top, bottom, null, null)
    // One placement for both halves: the caller mirrors it to the right edge.
    // Deck: the halves' boxes are pinned at the fold and hold shoulders + cluster with the deck
    // spacer between them, so the shoulders flank the touch screen's upper band and the clusters
    // sit below it — the box is exactly the rows plus padding (a tall box centred on the bottom
    // half would otherwise put its top above the hinge). The spacer rides on the placement for
    // the halves to render.
    val placedHalf = if (bottomCentred) {
        val pad = LayoutConstants.CLUSTER_PADDING_DP * density
        val boxHeight = bottomH + DECK_TOUCH_CLUSTER_GAP_UNITS * unitPx + DECK_CLUSTER_UNITS * unitPx + pad
        ClusterPlacement(deckSideBoxWidthPx(unitPx, density), boxHeight, hingeBottom.toFloat(), 0f, true)
    } else clusterPlacement(bottomArea, bottom, unitPx, reach, density, toolRow = secondRow)
    val deckSpacer = if (bottomCentred) deckSpacerPx(bottomH, unitPx, density) else null
    return TabletopDsPlacement(TabletopDsMode.SPLIT, top, bottom, placedHalf, placedHalf, deckSpacer)
}

/** A 3DS screen's width over height: 400 x 240 on top, 320 x 240 the touch screen. */
private fun n3dsAspect(screen: NdsScreen): Float = (if (screen == NdsScreen.TOP) N3DS_TOP_W else N3DS_BOTTOM_W) / N3DS_H

/**
 * Both screens stacked in [area], [big] first: DS at one whole scale, 3DS as the 400 x 480 frame fitted, the touch
 * screen at its narrower width centred wherever it is.
 */
private fun stackedTop(area: PxRect, kind: DsKind, big: NdsScreen): TabletopDsPlacement {
    val (top, bottom) = when (kind) {
        DsKind.DS -> {
            val fit = floor(min(area.width / DS_W, area.height / (2f * DS_H)) + EPSILON).coerceAtLeast(1f)
            val block = centred(area, DS_W * fit, 2f * DS_H * fit)
            Pair(
                PxRect(block.x, block.y, block.width, DS_H * fit),
                PxRect(block.x, block.y + DS_H * fit, block.width, DS_H * fit),
            )
        }
        DsKind.N3DS -> {
            val (w, h) = fitted(area, N3DS_TOP_W / (2f * N3DS_H))
            val block = centred(area, w, h)
            val screenH = NdsLayout.wholePx(h / 2f)
            val bottomW = NdsLayout.wholePx(w * N3DS_BOTTOM_W / N3DS_TOP_W)
            fun widthOf(s: NdsScreen) = if (s == NdsScreen.TOP) w else bottomW
            fun at(s: NdsScreen, y: Float) = PxRect(block.x + NdsLayout.wholePx((w - widthOf(s)) / 2f), y, widthOf(s), screenH)
            Pair(at(big, block.y), at(big.other, block.y + screenH))
        }
    }
    return TabletopDsPlacement(TabletopDsMode.STACKED_TOP, top, bottom, null, null)
}

/** The largest whole multiple of [w] x [h] inside [space] (1x at least when [atLeast1]). */
private fun wholeScale(space: PxRect, w: Float, h: Float, atLeast1: Boolean): Pair<Float, Float> {
    val fit = floor(min(space.width / w, space.height / h) + EPSILON).coerceAtLeast(if (atLeast1) 1f else 0f)
    return Pair(w * fit, h * fit)
}

/** The biggest [aspect] (width / height) rectangle inside [space], whole pixels, by the repo's rounding. */
private fun fitted(space: PxRect, aspect: Float): Pair<Float, Float> =
    if (space.width / aspect <= space.height) {
        Pair(NdsLayout.wholePx(space.width), NdsLayout.wholePx(space.width / aspect))
    } else {
        Pair(NdsLayout.wholePx(space.height * aspect), NdsLayout.wholePx(space.height))
    }

/** A [w] x [h] screen centred across [space] at [y] (against the fold). */
private fun atFold(space: PxRect, w: Float, h: Float, y: Float) =
    PxRect(space.x + NdsLayout.wholePx((space.width - w) / 2f), y, w, h)

private fun centred(space: PxRect, w: Float, h: Float) =
    PxRect(
        space.x + NdsLayout.wholePx((space.width - w) / 2f),
        space.y + NdsLayout.wholePx((space.height - h) / 2f),
        w,
        h,
    )

/**
 * The half-folded DS / 3DS pad halves' boxes, px of a root that fills the window: each [widthPx] x [heightPx] at
 * [topMarginPx] from the top, against its own side edge (the right half mirrors the left); [insetPx] moves the buttons
 * in from that edge.
 */
data class TabletopDsPadBoxes(val widthPx: Int, val heightPx: Int, val topMarginPx: Int, val insetPx: Float)

/**
 * The pad halves' boxes for placement [p] (window px) in a root [rootH] tall, kept inside it; null when [p] has no
 * halves (a controller, or both screens stacked above the hinge with the portrait pad below).
 */
fun tabletopDsPadBoxes(p: TabletopDsPlacement, rootH: Int): TabletopDsPadBoxes? {
    val half = p.left ?: return null
    val h = half.boxHeightPx.roundToInt().coerceAtMost(rootH)
    val top = half.topMarginPx.roundToInt().coerceIn(0, rootH - h)
    return TabletopDsPadBoxes(half.boxWidthPx.roundToInt(), h, top, half.extraInsetPx)
}

/**
 * The Pad layout editor's half-folded preview on its window: the screens' [placement], the [hingeY] it splits at, the
 * pad's [unitPx] (the cover screen's button size) and the halves' [boxes].
 */
data class HalfFoldedPreview(val placement: TabletopDsPlacement, val hingeY: Float, val unitPx: Float, val boxes: TabletopDsPadBoxes)

/**
 * The half-folded DS / 3DS layout as the game draws it ([tabletopDs], [tabletopDsPadBoxes]) on a [windowW] x [windowH]
 * editor window folded at its middle, for the Pad layout editor, which runs no game: [kind]'s screens, the estimated
 * reach (nothing is measured there), no controller. A phone-shaped [screen] is laid out as a tablet-class one, so the
 * halves always show. Null when the window has no room for them. Arguments as [besideMeasures].
 */
fun halfFoldedPreview(
    windowW: Float,
    windowH: Float,
    screen: ScreenClass,
    cutoutTop: Float,
    ydpi: Float,
    density: Float,
    cutoutLeft: Float,
    cutoutRight: Float,
    secondRow: Boolean,
    faceUnits: Float,
    systemPills: Int,
    kind: DsKind,
): HalfFoldedPreview? {
    val unit = tabletopDsUnitPx(ydpi, density)
    val r = reach(unit, density, cutoutLeft, cutoutRight, null, faceUnits, systemPills)
    val hinge = (windowH / 2f).roundToInt()
    val tablet = if (screen.isTablet) screen else ScreenClass.NEAR_SQUARE_TABLET
    val p = tabletopDs(
        windowW.roundToInt(), windowH.roundToInt(), tablet, cutoutTop.roundToInt(), hinge, hinge, kind, unit, r, density,
        controller = false, secondRow = secondRow,
    )
    val boxes = tabletopDsPadBoxes(p, windowH.roundToInt()) ?: return null
    return HalfFoldedPreview(p, hinge.toFloat(), unit, boxes)
}

/**
 * Whether a half-folded DS / 3DS [placement] can be used on a window whose game view spans both halves ([spansHinge]:
 * the Fold's [com.nxredux.mobile.runner.TabletopPad.DS]): only as the split. A hinge reading the split cannot fit
 * (STACKED_TOP there) is not used: no pad would be drawn below and the stack could run past the top area, so the game
 * lays out flat, as when not half-folded. A phone-shaped window's stack (its game view above the hinge, the portrait
 * pad below) is always usable.
 */
fun usableTabletop(spansHinge: Boolean, placement: TabletopDsPlacement): Boolean =
    !spansHinge || placement.mode == TabletopDsMode.SPLIT
