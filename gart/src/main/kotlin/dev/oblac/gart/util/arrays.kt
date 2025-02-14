package dev.oblac.gart.util

/**
 * Extension function to sort only a portion of an array.
 */
fun IntArray.sortRange(start: Int, end: Int, selector: (Int) -> Int) {
    if (start < end - 1) {
        val subList = this.sliceArray(start until end).sortedBy(selector)
        for (i in subList.indices) this[start + i] = subList[i]
    }
}
