package dev.oblac.gart.presentation

import dev.oblac.gart.Draw
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.text.drawStringInRect

internal val slide01 = Draw { c, d ->
    c.clear(CssColors.darkGreen)
    c.drawCircle(d.center, 100f, fillOfRed())
    c.drawStringInRect("Hello world!", titleBox, fontTitle, CssColors.white.toFillPaint())
}
