package com.nxredux.mobile.runner

import com.nxredux.mobile.layout.DsKind
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.PxRect

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
 * it is the same size (it never is here). Empty when the area holds no top screen over an
 * inset one (a window shorter than the top screen, or no room for a pixel of the inset).
 */
fun foldedPortraitRegions(w: Float, h: Float, kind: DsKind, big: NdsScreen): List<NdsRegion> {
    if (w <= 0f || h <= 0f) return emptyList()
    fun src(screen: NdsScreen) = if (kind == DsKind.DS) NdsLayout.src(screen) else DualScreen.n3dsSrc(screen)
    val topH = NdsLayout.wholePx(w * if (kind == DsKind.DS) 3f / 4f else 3f / 5f)
    val spaceH = h - topH
    if (topH < 1f || spaceH < 1f) return emptyList()
    val bottomW = NdsLayout.wholePx(minOf(w, spaceH * 4f / 3f))
    val bottomH = NdsLayout.wholePx(bottomW * 3f / 4f)
    if (bottomW < 1f || bottomH < 1f) return emptyList()
    val bottomX = NdsLayout.wholePx((w - bottomW) / 2f)
    val other = big.other
    val sameSize = w == bottomW && topH == bottomH
    return listOf(
        NdsRegion(big, src(big), PxRect(0f, 0f, w, topH), shaded = true, touch = big == NdsScreen.BOTTOM),
        NdsRegion(other, src(other), PxRect(bottomX, topH, bottomW, bottomH), shaded = sameSize, touch = other == NdsScreen.BOTTOM),
    )
}
