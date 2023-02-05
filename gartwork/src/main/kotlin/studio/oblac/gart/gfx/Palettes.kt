package studio.oblac.gart.gfx

object Palettes {
    val cool1 = Palette(0xff001219, 0xff005f73, 0xff0a9396, 0xff94d2bd, 0xffe9d8a6, 0xffee9b00, 0xffca6702, 0xffbb3e03, 0xffae2012, 0xff9b2226)
    val cool2 = Palette(0xff264653, 0xff2a9d8f, 0xffe9c46a, 0xfff4a261, 0xffe76f51)
    val cool3 = Palette(0xffb7094c, 0xffa01a58, 0xff892b64, 0xff723c70, 0xff5c4d7d, 0xff455e89, 0xff2e6f95, 0xff1780a1, 0xff0091ad)
    val cool4 = Palette(0xfffabc2a, 0xffffcab1, 0xfff38d68, 0xffee6c4d, 0xfff76f8e, 0xfff2bac9, 0xff7fd8be, 0xffa1fcdf, 0xff3b5249, 0xff519872)
    val cool5 = Palette(0xffea698b, 0xffd55d92, 0xffc05299, 0xffac46a1, 0xff973aa8, 0xff822faf, 0xff6d23b6, 0xff6411ad, 0xff571089, 0xff47126b)
    val cool6 = Palette(0xff99e2b4, 0xff88d4ab, 0xff78c6a3, 0xff67b99a, 0xff56ab91, 0xff469d89, 0xff358f80, 0xff248277, 0xff14746f, 0xff036666)
    val cool7 = Palette(0xff012a4a, 0xff013a63, 0xff01497c, 0xff014f86, 0xff2a6f97, 0xff2c7da0, 0xff468faf, 0xff61a5c2, 0xff89c2d9, 0xffa9d6e5)
    val cool8 = Palette(0xff2d0d30, 0xff4b1650, 0xff702279, 0xff942d9f, 0xffb136bf, 0xffc150ce, 0xffd280db, 0xffe3afe9)
    val cool9 = Palette(0xffEF476F, 0xffF78C6B, 0xffFFD166, 0xff06D6A0, 0xff118AB2, 0xff073B4C)

    fun gradient(colorFrom: Long, colorTo: Long, steps: Int): Palette = gradient(colorFrom.toInt(), colorTo.toInt(), steps)
    fun gradient(colorFrom: Int, colorTo: Int, steps: Int): Palette {
        val colors = IntArray(steps)

        var a = alpha(colorFrom).toFloat()
        var r = red(colorFrom).toFloat()
        var g = green(colorFrom).toFloat()
        var b = blue(colorFrom).toFloat()
        val deltaA = (alpha(colorTo) - a) / steps
        val deltaR = (red(colorTo) - r) / steps
        val deltaG = (green(colorTo) - g) / steps
        val deltaB = (blue(colorTo) - b) / steps

        colors[0] = colorFrom
        var index = 1
        var i = steps - 2

        while (i > 0) {
            a += deltaA
            r += deltaR
            g += deltaG
            b += deltaB

            colors[index] = argb(
                (a + 0.5).toInt(),
                (r + 0.5).toInt(),
                (g + 0.5).toInt(),
                (b + 0.5).toInt()
            )
            index++
            i--
        }
        colors[steps - 1] = colorTo

        return Palette(colors)
    }
}
