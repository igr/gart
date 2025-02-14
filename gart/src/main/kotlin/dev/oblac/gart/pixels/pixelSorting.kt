package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels
import dev.oblac.gart.util.sortRange

fun pixelSorter(bitmap: Pixels, threshold: Int = 100, sortValueOf: (Int) -> Int) {
    val height = bitmap.d.h

    for (y in 0 until height) {
        pixelSorterRow(bitmap, y, sortValueOf, threshold)
    }
}

fun pixelSorterRow(
    bitmap: Pixels,
    row: Int,
    sortValueOf: (Int) -> Int,
    threshold: Int
) {
    val rowPixels = bitmap.row(row)

    var start = -1 // start index of a bright region
    for (x in rowPixels.indices) {
        val bright = sortValueOf(rowPixels[x]) > threshold

        if (bright) {
            if (start == -1) start = x // start a new region
        } else {
            if (start != -1) {
                // sort only the bright region
                rowPixels.sortRange(start, x) { color -> sortValueOf(color) }
                start = -1
            }
        }
    }

    // sort the last segment if it reaches the row's end
    if (start != -1) {
        rowPixels.sortRange(start, rowPixels.size) { color -> sortValueOf(color) }
    }

    bitmap.row(row, rowPixels)
}
