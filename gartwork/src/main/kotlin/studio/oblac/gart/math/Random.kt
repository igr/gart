package studio.oblac.gart.math

fun rnd(max: Int): Int {
    return (Math.random() * max).toInt()
}

fun rnd(min: Int, max: Int): Int {
    return (Math.random() * (max - min) + min).toInt()
}

fun rnd(min: Float, max: Float): Float {
    return (Math.random() * (max - min) + min).toFloat()
}
