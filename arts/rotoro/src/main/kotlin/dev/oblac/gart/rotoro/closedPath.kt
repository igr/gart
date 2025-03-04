package dev.oblac.gart.rotoro

import kotlin.random.Random

typealias Cell = Pair<Int, Int>

fun generateClosedPath(): List<Cell> {
    val quadrants = listOf(
        listOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(1, 1)), // Top-left
        listOf(Cell(0, 2), Cell(0, 3), Cell(1, 2), Cell(1, 3)), // Top-right
        listOf(Cell(2, 0), Cell(2, 1), Cell(3, 0), Cell(3, 1)), // Bottom-left
        listOf(Cell(2, 2), Cell(2, 3), Cell(3, 2), Cell(3, 3))  // Bottom-right
    )

    // Step 1: Pick random cells from each quadrant
    val selectedCells = mutableSetOf<Cell>()
    for (quadrant in quadrants) {
        selectedCells.addAll(quadrant.shuffled().take(Random.nextInt(1, 4)))
    }

    // Step 2: Ensure all cells are connected
    val path = generateClosedPathFromCells(selectedCells)
    return path ?: generateClosedPath() // Retry if failed
}

// Uses DFS to find a valid closed path
private fun generateClosedPathFromCells(cells: Set<Cell>): List<Cell>? {
    val adjacency = buildAdjacencyMap(cells)
    val start = cells.first()
    val path = mutableListOf<Cell>()

    if (dfs(start, start, cells, path, mutableSetOf())) {
        return path
    }
    return null // No closed path found
}

// Build adjacency list of neighboring selected cells
private fun buildAdjacencyMap(cells: Set<Cell>): Map<Cell, List<Cell>> {
    val directions = listOf(Cell(0, 1), Cell(1, 0), Cell(0, -1), Cell(-1, 0))
    return cells.associateWith { cell ->
        directions.map { (dx, dy) -> Cell(cell.first + dx, cell.second + dy) }
            .filter { it in cells }
    }
}

// DFS to find a closed path
private fun dfs(
    current: Cell,
    start: Cell,
    cells: Set<Cell>,
    path: MutableList<Cell>,
    visited: MutableSet<Cell>
): Boolean {
    path.add(current)
    visited.add(current)

    // If all cells are visited and we return to start, we have a closed path
    if (visited.size == cells.size && start in buildAdjacencyMap(cells)[current].orEmpty()) {
        path.add(start) // Complete the cycle
        return true
    }

    // Explore neighbors
    for (neighbor in buildAdjacencyMap(cells)[current].orEmpty()) {
        if (neighbor !in visited || (neighbor == start && visited.size == cells.size)) {
            if (dfs(neighbor, start, cells, path, visited)) return true
        }
    }

    // Backtrack
    path.removeLast()
    visited.remove(current)
    return false
}
