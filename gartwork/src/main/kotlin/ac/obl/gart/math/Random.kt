package ac.obl.gart.math

fun rnd(max: Int): Int {
    return (Math.random() * max).toInt()
}

fun rnd(min: Int, max: Int): Int {
    return (Math.random() * (max - min) + min).toInt()
}
