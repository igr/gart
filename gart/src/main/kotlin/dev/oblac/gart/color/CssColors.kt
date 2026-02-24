package dev.oblac.gart.color

import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f

/**
 * CSS color names.
 * https://www.w3.org/TR/css-color-3/#svg-color
 */
object CssColors {
    const val aliceBlue = 0xFFF0F8FF.toInt()
    const val antiqueWhite = 0xFFFAEBD7.toInt()
    const val aqua = 0xFF00FFFF.toInt()
    const val aquamarine = 0xFF7FFFD4.toInt()
    const val azure = 0xFFF0FFFF.toInt()
    const val beige = 0xFFF5F5DC.toInt()
    const val bisque = 0xFFFFE4C4.toInt()
    const val black = 0xFF000000.toInt()
    const val blanchedAlmond = 0xFFFFEBCD.toInt()
    const val blue = 0xFF0000FF.toInt()
    const val blueViolet = 0xFF8A2BE2.toInt()
    const val brown = 0xFFA52A2A.toInt()
    const val burlyWood = 0xFFDEB887.toInt()
    const val cadetBlue = 0xFF5F9EA0.toInt()
    const val chartreuse = 0xFF7FFF00.toInt()
    const val chocolate = 0xFFD2691E.toInt()
    const val coral = 0xFFFF7F50.toInt()
    const val cornflowerBlue = 0xFF6495ED.toInt()
    const val cornsilk = 0xFFFFF8DC.toInt()
    const val crimson = 0xFFDC143C.toInt()
    const val cyan = 0xFF00FFFF.toInt()
    const val darkBlue = 0xFF00008B.toInt()
    const val darkCyan = 0xFF008B8B.toInt()
    const val darkGoldenrod = 0xFFB8860B.toInt()
    const val darkGray = 0xFFA9A9A9.toInt()
    const val darkGreen = 0xFF006400.toInt()
    const val darkKhaki = 0xFFBDB76B.toInt()
    const val darkMagenta = 0xFF8B008B.toInt()
    const val darkOliveGreen = 0xFF556B2F.toInt()
    const val darkOrange = 0xFFFF8C00.toInt()
    const val darkOrchid = 0xFF9932CC.toInt()
    const val darkRed = 0xFF8B0000.toInt()
    const val darkSalmon = 0xFFE9967A.toInt()
    const val darkSeaGreen = 0xFF8FBC8F.toInt()
    const val darkSlateBlue = 0xFF483D8B.toInt()
    const val darkSlateGray = 0xFF2F4F4F.toInt()
    const val darkTurquoise = 0xFF00CED1.toInt()
    const val darkViolet = 0xFF9400D3.toInt()
    const val deepPink = 0xFFFF1493.toInt()
    const val deepSkyBlue = 0xFF00BFFF.toInt()
    const val dimGray = 0xFF696969.toInt()
    const val dodgerBlue = 0xFF1E90FF.toInt()
    const val firebrick = 0xFFB22222.toInt()
    const val floralWhite = 0xFFFFFAF0.toInt()
    const val forestGreen = 0xFF228B22.toInt()
    const val fuchsia = 0xFFFF00FF.toInt()
    const val gainsboro = 0xFFDCDCDC.toInt()
    const val ghostWhite = 0xFFF8F8FF.toInt()
    const val gold = 0xFFFFD700.toInt()
    const val goldenrod = 0xFFDAA520.toInt()
    const val gray = 0xFF808080.toInt()
    const val green = 0xFF008000.toInt()
    const val greenYellow = 0xFFADFF2F.toInt()
    const val honeydew = 0xFFF0FFF0.toInt()
    const val hotPink = 0xFFFF69B4.toInt()
    const val indianRed = 0xFFCD5C5C.toInt()
    const val indigo = 0xFF4B0082.toInt()
    const val ivory = 0xFFFFFFF0.toInt()
    const val khaki = 0xFFF0E68C.toInt()
    const val lavender = 0xFFE6E6FA.toInt()
    const val lavenderBlush = 0xFFFFF0F5.toInt()
    const val lawnGreen = 0xFF7CFC00.toInt()
    const val lemonChiffon = 0xFFFFFACD.toInt()
    const val lightBlue = 0xFFADD8E6.toInt()
    const val lightCoral = 0xFFF08080.toInt()
    const val lightCyan = 0xFFE0FFFF.toInt()
    const val lightGoldenrodYellow = 0xFFFAFAD2.toInt()
    const val lightGray = 0xFFD3D3D3.toInt()
    const val lightGreen = 0xFF90EE90.toInt()
    const val lightPink = 0xFFFFB6C1.toInt()
    const val lightSalmon = 0xFFFFA07A.toInt()
    const val lightSeaGreen = 0xFF20B2AA.toInt()
    const val lightSkyBlue = 0xFF87CEFA.toInt()
    const val lightSlateGray = 0xFF778899.toInt()
    const val lightSteelBlue = 0xFFB0C4DE.toInt()
    const val lightYellow = 0xFFFFFFE0.toInt()
    const val lime = 0xFF00FF00.toInt()
    const val limeGreen = 0xFF32CD32.toInt()
    const val linen = 0xFFFAF0E6.toInt()
    const val magenta = 0xFFFF00FF.toInt()
    const val maroon = 0xFF800000.toInt()
    const val mediumAquamarine = 0xFF66CDAA.toInt()
    const val mediumBlue = 0xFF0000CD.toInt()
    const val mediumOrchid = 0xFFBA55D3.toInt()
    const val mediumPurple = 0xFF9370DB.toInt()
    const val mediumSeaGreen = 0xFF3CB371.toInt()
    const val mediumSlateBlue = 0xFF7B68EE.toInt()
    const val mediumSpringGreen = 0xFF00FA9A.toInt()
    const val mediumTurquoise = 0xFF48D1CC.toInt()
    const val mediumVioletRed = 0xFFC71585.toInt()
    const val midnightBlue = 0xFF191970.toInt()
    const val mintCream = 0xFFF5FFFA.toInt()
    const val mistyRose = 0xFFFFE4E1.toInt()
    const val moccasin = 0xFFFFE4B5.toInt()
    const val navajoWhite = 0xFFFFDEAD.toInt()
    const val navy = 0xFF000080.toInt()
    const val oldLace = 0xFFFDF5E6.toInt()
    const val olive = 0xFF808000.toInt()
    const val oliveDrab = 0xFF6B8E23.toInt()
    const val orange = 0xFFFFA500.toInt()
    const val orangeRed = 0xFFFF4500.toInt()
    const val orchid = 0xFFDA70D6.toInt()
    const val paleGoldenrod = 0xFFEEE8AA.toInt()
    const val paleGreen = 0xFF98FB98.toInt()
    const val paleTurquoise = 0xFFAFEEEE.toInt()
    const val paleVioletRed = 0xFFDB7093.toInt()
    const val papayaWhip = 0xFFFFEFD5.toInt()
    const val peachPuff = 0xFFFFDAB9.toInt()
    const val peru = 0xFFCD853F.toInt()
    const val pink = 0xFFFFC0CB.toInt()
    const val plum = 0xFFDDA0DD.toInt()
    const val powderBlue = 0xFFB0E0E6.toInt()
    const val purple = 0xFF800080.toInt()
    const val red = 0xFFFF0000.toInt()
    const val rosyBrown = 0xFFBC8F8F.toInt()
    const val royalBlue = 0xFF4169E1.toInt()
    const val saddleBrown = 0xFF8B4513.toInt()
    const val salmon = 0xFFFA8072.toInt()
    const val sandyBrown = 0xFFF4A460.toInt()
    const val seaGreen = 0xFF2E8B57.toInt()
    const val seaShell = 0xFFFFF5EE.toInt()
    const val sienna = 0xFFA0522D.toInt()
    const val silver = 0xFFC0C0C0.toInt()
    const val skyBlue = 0xFF87CEEB.toInt()
    const val slateBlue = 0xFF6A5ACD.toInt()
    const val slateGray = 0xFF708090.toInt()
    const val snow = 0xFFFFFAFA.toInt()
    const val springGreen = 0xFF00FF7F.toInt()
    const val steelBlue = 0xFF4682B4.toInt()
    const val tan = 0xFFD2B48C.toInt()
    const val teal = 0xFF008080.toInt()
    const val thistle = 0xFFD8BFD8.toInt()
    const val tomato = 0xFFFF6347.toInt()
    const val turquoise = 0xFF40E0D0.toInt()
    const val violet = 0xFFEE82EE.toInt()
    const val wheat = 0xFFF5DEB3.toInt()
    const val white = 0xFFFFFFFF.toInt()

    const val whiteSmoke = 0xFFF5F5F5.toInt()
    const val yellow = 0xFFFFFF00.toInt()
    const val yellowGreen = 0xFF9ACD32.toInt()
    const val transparent = 0x00000000

    private val cssColors = mapOf(
        "aliceblue" to aliceBlue,
        "antiquewhite" to antiqueWhite,
        "aqua" to aqua,
        "aquamarine" to aquamarine,
        "azure" to azure,
        "beige" to beige,
        "bisque" to bisque,
        "black" to black,
        "blanchedalmond" to blanchedAlmond,
        "blue" to blue,
        "blueviolet" to blueViolet,
        "brown" to brown,
        "burlywood" to burlyWood,
        "cadetblue" to cadetBlue,
        "chartreuse" to chartreuse,
        "chocolate" to chocolate,
        "coral" to coral,
        "cornflowerblue" to cornflowerBlue,
        "cornsilk" to cornsilk,
        "crimson" to crimson,
        "cyan" to cyan,
        "darkblue" to darkBlue,
        "darkcyan" to darkCyan,
        "darkgoldenrod" to darkGoldenrod,
        "darkgray" to darkGray,
        "darkgreen" to darkGreen,
        "darkgrey" to darkGray,
        "darkkhaki" to darkKhaki,
        "darkmagenta" to darkMagenta,
        "darkolivegreen" to darkOliveGreen,
        "darkorange" to darkOrange,
        "darkorchid" to darkOrchid,
        "darkred" to darkRed,
        "darksalmon" to darkSalmon,
        "darkseagreen" to darkSeaGreen,
        "darkslateblue" to darkSlateBlue,
        "darkslategray" to darkSlateGray,
        "darkslategrey" to darkSlateGray,
        "darkturquoise" to darkTurquoise,
        "darkviolet" to darkViolet,
        "deeppink" to deepPink,
        "deepskyblue" to deepSkyBlue,
        "dimgray" to dimGray,
        "dimgrey" to dimGray,
        "dodgerblue" to dodgerBlue,
        "firebrick" to firebrick,
        "floralwhite" to floralWhite,
        "forestgreen" to forestGreen,
        "fuchsia" to fuchsia,
        "gainsboro" to gainsboro,
        "ghostwhite" to ghostWhite,
        "gold" to gold,
        "goldenrod" to goldenrod,
        "gray" to gray,
        "green" to green,
        "greenyellow" to greenYellow,
        "grey" to gray,
        "honeydew" to honeydew,
        "hotpink" to hotPink,
        "indianred" to indianRed,
        "indigo" to indigo,
        "ivory" to ivory,
        "khaki" to khaki,
        "lavender" to lavender,
        "lavenderblush" to lavenderBlush,
        "lawngreen" to lawnGreen,
        "lemonchiffon" to lemonChiffon,
        "lightblue" to lightBlue,
        "lightcoral" to lightCoral,
        "lightcyan" to lightCyan,
        "lightgoldenrodyellow" to lightGoldenrodYellow,
        "lightgray" to lightGray,
        "lightgreen" to lightGreen,
        "lightgrey" to lightGray,
        "lightpink" to lightPink,
        "lightsalmon" to lightSalmon,
        "lightseagreen" to lightSeaGreen,
        "lightskyblue" to lightSkyBlue,
        "lightslategray" to lightSlateGray,
        "lightslategrey" to lightSlateGray,
        "lightsteelblue" to lightSteelBlue,
        "lightyellow" to lightYellow,
        "lime" to lime,
        "limegreen" to limeGreen,
        "linen" to linen,
        "magenta" to magenta,
        "maroon" to maroon,
        "mediumaquamarine" to mediumAquamarine,
        "mediumblue" to mediumBlue,
        "mediumorchid" to mediumOrchid,
        "mediumpurple" to mediumPurple,
        "mediumseagreen" to mediumSeaGreen,
        "mediumslateblue" to mediumSlateBlue,
        "mediumspringgreen" to mediumSpringGreen,
        "mediumturquoise" to mediumTurquoise,
        "mediumvioletred" to mediumVioletRed,
        "midnightblue" to midnightBlue,
        "mintcream" to mintCream,
        "mistyrose" to mistyRose,
        "moccasin" to moccasin,
        "navajowhite" to navajoWhite,
        "navy" to navy,
        "oldlace" to oldLace,
        "olive" to olive,
        "olivedrab" to oliveDrab,
        "orange" to orange,
        "orangered" to orangeRed,
        "orchid" to orchid,
        "palegoldenrod" to paleGoldenrod,
        "palegreen" to paleGreen,
        "paleturquoise" to paleTurquoise,
        "palevioletred" to paleVioletRed,
        "papayawhip" to papayaWhip,
        "peachpuff" to peachPuff,
        "peru" to peru,
        "pink" to pink,
        "plum" to plum,
        "powderblue" to powderBlue,
        "purple" to purple,
        "red" to red,
        "rosybrown" to rosyBrown,
        "royalblue" to royalBlue,
        "saddlebrown" to saddleBrown,
        "salmon" to salmon,
        "sandybrown" to sandyBrown,
        "seagreen" to seaGreen,
        "seashell" to seaShell,
        "sienna" to sienna,
        "silver" to silver,
        "skyblue" to skyBlue,
        "slateblue" to slateBlue,
        "slategray" to slateGray,
        "slategrey" to slateGray,
        "snow" to snow,
        "springgreen" to springGreen,
        "steelblue" to steelBlue,
        "tan" to tan,
        "teal" to teal,
        "thistle" to thistle,
        "tomato" to tomato,
        "turquoise" to turquoise,
        "violet" to violet,
        "wheat" to wheat,
        "white" to white,
        "whitesmoke" to whiteSmoke,
        "yellow" to yellow,
        "yellowgreen" to yellowGreen,
        "transparent" to transparent
    )

    fun color4f(name: String): Color4f {
        val rgb = color(name)
        return Color4f.of(rgb)
    }

    fun color(name: String): Int {
        return cssColors[name.lowercase()] ?: throw IllegalArgumentException("Unknown color name: $name")
    }
}
