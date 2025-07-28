package dev.oblac.gart.cellular

class CellularAutomata(
    val width: Int,
    val height: Int,
    private val ruleEngine: CellularAutomataRules
) {
    private var grid = Array(width) { x -> Array(height) { y -> ruleEngine.initialState(x, y, width, height) } }
    private var nextGrid = Array(width) { Array(height) { 1 } }

    operator fun set(x: Int, y: Int, state: Int) {
        if (x in 0 until width && y in 0 until height) {
            grid[x][y] = ruleEngine.validateState(state)
        }
    }

    operator fun get(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) grid[x][y] else 1
    }

    private fun neighbors(x: Int, y: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    neighbors.add(grid[nx][ny])
                }
            }
        }
        return neighbors
    }

    fun step() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val currentState = grid[x][y]
                val neighbors = neighbors(x, y)

                nextGrid[x][y] = ruleEngine.computeNextState(x, y, currentState, neighbors)
            }
        }

        // swap grids
        val temp = grid
        grid = nextGrid
        nextGrid = temp
    }

    fun forEach(action: (x: Int, y: Int, state: Int) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                action(x, y, grid[x][y])
            }
        }
    }
}

interface CellularAutomataRules {
    fun computeNextState(x: Int, y: Int, currentState: Int, neighbors: List<Int>): Int
    fun validateState(state: Int): Int
    fun initialState(x: Int, y: Int, width: Int, height: Int): Int
}
