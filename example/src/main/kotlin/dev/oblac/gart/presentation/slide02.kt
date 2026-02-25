package dev.oblac.gart.presentation

import dev.oblac.gart.Draw
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOfRed

internal val slide02 = Draw { c, d ->
    c.clear(CssColors.darkRed)
    c.drawCircle(d.center, 100f, fillOfRed())
}
