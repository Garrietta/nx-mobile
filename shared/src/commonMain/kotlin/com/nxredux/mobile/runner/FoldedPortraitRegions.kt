package com.nxredux.mobile.runner

import com.nxredux.mobile.layout.DsKind
import com.nxredux.mobile.layout.LayoutConstants
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.PxRect
import com.nxredux.mobile.ui.nx.PORTRAIT_EMBLEM_GAP
import com.nxredux.mobile.ui.nx.PORTRAIT_EMBLEM_H

/**
 * The folded-portrait DS / 3DS layout (v0.4.7): the phone-shaped portrait window's game area is
 * the window from its top edge (the container draws fullscreen, under the camera hole and the
 * hidden status bar area) down to the portrait band's top, and the screens split across it —
 * the [big] screen full window width on top, the other screen inset in the control panel below
 * it, centred across, its top edge on the big screen's bottom edge. The panel (everything below
 * the top screen: the inset screen plus the portrait band) is more than half the window height
 * on a tall phone window — on the 1080 x 2400 cover, 1590 px (66%) for DS, 1752 px (73%) for
 * 3DS — where the old layout stacked both screens above the band. A small inset screen is fine:
 * it fills the panel height left above the band (841 x 631 for DS on the cover, 1057 x 793 for
 * 3DS), not the width.
 *
 * [w] x [h] is the game area (window width, window height minus the band). The DS cuts its
 * 256 x 384 stack ([NdsLayout.src]); the 3DS its Stacked frame, the same 400 x 480 frame the
 * half-folded and Dual screen splits cut ([DualScreen.n3dsSrc]) — the caller pins the core to
 * that frame (default layout, equal screens, swap held) while this layout applies, as the
 * half-folded one does. The touch screen takes touches wherever it is drawn. Shaded regions
 * share one size (one preset chain): the full-width screen is shaded, the inset one only when
 * it is the same size (it never is here).
 *
 * v0.4.8 — the inset screen has a floor (the 2026-10-30 verdict: on the device's folded
 * window the full-width top screen plus the portrait band left only ~100-200 px of panel
 * height, so the inset rendered tiny, and a cutout-margin change later shrank it to a sliver).
 * The inset is now at least half the window width: on a tall window the full-width top screen
 * already leaves more than that, so those layouts are unchanged; on a short window the top
 * screen narrows (centred) just enough to guarantee the inset, instead of the inset taking
 * whatever crumbs remain. The inset's bottom edge is the game area's bottom at most (its height
 * never exceeds the space below the top screen), so it ends at or above the band's top and the
 * band's buttons cannot cover its corners. Empty when the area holds no usable split — a
 * window no taller than the minimum inset, or either screen narrower than half the window —
 * so the caller keeps the current frame instead of drawing a sliver.
 */
fun foldedPortraitRegions(w: Float, h: Float, kind: DsKind, big: NdsScreen): List<NdsRegion> {
    if (w <= 0f || h <= 0f) return emptyList()
    fun src(screen: NdsScreen) = if (kind == DsKind.DS) NdsLayout.src(screen) else DualScreen.n3dsSrc(screen)
    // The inset screen's floor: half the window width at 4:3, so the minimum inset height is 3/8 of the width.
    val minInsetH = w * 3f / 8f
    val fullTopH = NdsLayout.wholePx(w * if (kind == DsKind.DS) 3f / 4f else 3f / 5f)
    // Full-width top screen when it leaves the minimum inset; otherwise narrow it (centred) just enough.
    val topH = minOf(fullTopH, NdsLayout.wholePx(h - minInsetH))
    val spaceH = h - topH
    if (topH < 1f || spaceH < 1f) return emptyList()
    val topW = NdsLayout.wholePx(topH * if (kind == DsKind.DS) 4f / 3f else 5f / 3f)
    val bottomW = NdsLayout.wholePx(minOf(w, spaceH * 4f / 3f))
    val bottomH = NdsLayout.wholePx(bottomW * 3f / 4f)
    if (bottomW < 1f || bottomH < 1f) return emptyList()
    // A split with either screen below half the window width is a sliver, not a layout: keep the current frame.
    if (topW < w / 2f || bottomW < w / 2f) return emptyList()
    val topX = NdsLayout.wholePx((w - topW) / 2f)
    val bottomX = NdsLayout.wholePx((w - bottomW) / 2f)
    val other = big.other
    val sameSize = topW == bottomW && topH == bottomH
    return listOf(
        NdsRegion(big, src(big), PxRect(topX, 0f, topW, topH), shaded = true, touch = big == NdsScreen.BOTTOM),
        NdsRegion(other, src(other), PxRect(bottomX, topH, bottomW, bottomH), shaded = sameSize, touch = other == NdsScreen.BOTTOM),
    )
}

/**
 * The folded portrait panel's geometry (v0.4.10), whole px: the big screen on top, the inset
 * touch screen centred in the panel below it, and the panel's own rows — the pill row on top,
 * the clusters flanking the inset in the middle, the mic / MENU row at the bottom — all from
 * one unit, so the renderer (the screen rects) and the host (the panel view) cannot disagree.
 * [big] is the full-width screen (top or touch, by SWAP); [inset] the small touch screen in the
 * panel's middle; [panelTop] the panel's top edge (the big screen's bottom); [unitPx] the
 * panel's button unit; [pillBoxH] / [bottomBoxH] the panel rows' heights. The inset NEVER
 * shrinks: it is 0.40 of the window width (the folded mockup's share, ~40.5%), and when the
 * panel would crowd it, the big screen narrows and centres instead — the v0.4.8 floor
 * principle, with the floor at 0.40 (flanking clusters make 0.50 geometrically impossible on
 * a 1080 window: two 3-unit clusters + the inset + gaps exceed it). Null when no panel fits:
 * a degenerate window, or a big screen narrower than half the window after narrowing (the
 * v0.4.8 sliver rule) — the caller then falls back to the inset-screen + band layout.
 */
data class FoldedPanelGeometry(
    val big: PxRect,
    val inset: PxRect,
    val panelTop: Float,
    val unitPx: Float,
    val pillBoxH: Float,
    val bottomBoxH: Float,
)

fun foldedPanelGeometry(
    w: Int,
    h: Int,
    kind: DsKind,
    density: Float,
    faceWUnits: Float,
): FoldedPanelGeometry? {
    if (w < 1 || h < 1) return null
    val pad = LayoutConstants.CLUSTER_PADDING_DP * density
    val bigAspect = if (kind == DsKind.DS) 4f / 3f else 5f / 3f
    val insetW = NdsLayout.wholePx(0.40f * w)
    val insetH = NdsLayout.wholePx(insetW * 3f / 4f)
    val gapX = pad
    val unitPx = (w - insetW - 2f * gapX - 4f * pad) / (3f + faceWUnits)
    if (unitPx <= 0f) return null
    val pillBoxH = 1.0f * unitPx + 2f * pad
    val bottomBoxH = 0.9f * unitPx + 2f * pad
    val gap1 = 0.35f * unitPx
    val logoSpace = (PORTRAIT_EMBLEM_H + 2f * PORTRAIT_EMBLEM_GAP) * unitPx
    val needed = pillBoxH + gap1 + insetH + logoSpace + bottomBoxH
    var bigH = NdsLayout.wholePx(w / bigAspect)
    val panelH0 = h - bigH
    var bigX = 0f
    var bigW = w.toFloat()
    if (needed > panelH0) {
        // The panel crowds the inset: narrow the big screen (centred) so the panel grows to
        // exactly what its rows need — the inset keeps its 0.40 floor either way.
        bigH = NdsLayout.wholePx(h - needed)
        bigW = NdsLayout.wholePx(bigH * bigAspect)
        if (bigH < 1f || bigW < w / 2f) return null
        bigX = NdsLayout.wholePx((w - bigW) / 2f)
    }
    val insetX = NdsLayout.wholePx((w - insetW) / 2f)
    val insetY = NdsLayout.wholePx(bigH + pillBoxH + gap1)
    return FoldedPanelGeometry(
        big = PxRect(bigX, 0f, bigW, bigH),
        inset = PxRect(insetX, insetY, insetW, insetH),
        panelTop = bigH,
        unitPx = unitPx,
        pillBoxH = pillBoxH,
        bottomBoxH = bottomBoxH,
    )
}

/**
 * The folded portrait panel as game regions: [big] first, then the inset — the same convention
 * as [foldedPortraitRegions] (src by screen identity, the big screen shaded, the touch screen
 * touchable wherever it is), so a SWAP only swaps which screen is big. Null when the geometry
 * is null.
 */
fun foldedPanelRegions(
    w: Int,
    h: Int,
    kind: DsKind,
    big: NdsScreen,
    density: Float,
    faceWUnits: Float,
): List<NdsRegion>? {
    val g = foldedPanelGeometry(w, h, kind, density, faceWUnits) ?: return null
    fun region(screen: NdsScreen, dst: PxRect): NdsRegion {
        val src = when (kind) {
            DsKind.DS -> NdsLayout.src(screen)
            DsKind.N3DS -> DualScreen.n3dsSrc(screen)
        }
        return NdsRegion(screen, src, dst, shaded = screen == big, touch = screen == NdsScreen.BOTTOM)
    }
    return listOf(region(big, g.big), region(big.other, g.inset))
}
