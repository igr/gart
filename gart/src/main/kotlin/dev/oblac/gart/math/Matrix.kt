package dev.oblac.gart.math

class Matrix<D>(val rows: Int, val cols: Int, init: (Int, Int) -> D) {
    private val storage: MutableList<D> = MutableList(rows * cols) { init(it / cols, it % cols) }
    operator fun get(row: Int, col: Int): D {
        require(row in 0 until rows && col in 0 until cols) { "Index out of bounds" }
        return storage[row * cols + col]
    }

    operator fun set(row: Int, col: Int, value: D) {
        require(row in 0 until rows && col in 0 until cols) { "Index out of bounds" }
        storage[row * cols + col] = value
    }

    fun forEach(action: (Int, Int, D) -> Unit) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                action(row, col, storage[row * cols + col])
            }
        }
    }

    fun coordinates(): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                pairs.add(Pair(row, col))
            }
        }
        return pairs
    }
}
