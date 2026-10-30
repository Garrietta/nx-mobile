package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.nxredux.mobile.options.PxRect
import com.nxredux.mobile.settings.PadTheme

/**
 * The flat-unfolded virtual hinge's pad surface: the bottom half's background behind the touch
 * screen and the pad halves. The tabletop clusters draw no background of their own (they sit
 * translucent over the game, always Charcoal), so the split's bottom half was bare black where the
 * portrait pad draws its dark surface; this restores that surface, reusing the pre-built Charcoal
 * pad surface ([LocalPadColors.current.surface], the clusters' own theme) rather than a new colour.
 * The surface fills the bottom half from [top] to the window bottom, around [touch]'s rect, so the
 * touch screen sits drawn-in within the pad, framed by it; the touch rect itself stays transparent
 * so the game shows through, and the view takes no touches (they pass to the game and the halves
 * above). Virtual hinge only; the real half-folded layout and the flat layout are unchanged.
 * All rects are window px; the hosting view fills the window, so no density conversion applies.
 */
@Composable
fun TabletopPadSurface(top: Float, touch: PxRect, windowW: Float, windowH: Float) {
    PadThemeProvider(PadTheme.CHARCOAL) {
        val surface = LocalPadColors.current.surface
        Canvas(Modifier.fillMaxSize()) {
            val left = touch.x.coerceIn(0f, windowW)
            val right = (touch.x + touch.width).coerceIn(left, windowW)
            val midTop = touch.y.coerceIn(top, windowH)
            val midBottom = (touch.y + touch.height).coerceIn(midTop, windowH)
            // Beside the touch screen, full bottom-half height.
            drawRect(surface, Offset(0f, top), Size(left, windowH - top))
            drawRect(surface, Offset(right, top), Size(windowW - right, windowH - top))
            // Above and below the touch screen, between its sides.
            drawRect(surface, Offset(left, top), Size(right - left, midTop - top))
            drawRect(surface, Offset(left, midBottom), Size(right - left, windowH - midBottom))
        }
    }
}
