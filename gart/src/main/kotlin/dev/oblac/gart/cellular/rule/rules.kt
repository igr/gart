package dev.oblac.gart.cellular.rule

import kotlin.math.pow

/**
 * Generates cellular automaton rows based on Wolfram's elementary cellular automaton rules.
 * https://www.wolframscience.com/nks/notes-12-10--minimal-cellular-automata-for-sequences/
 *
 * @param rule Integer representing the rule (e.g., 30 for Rule 30, 110 for Rule 110)
 * @param neighborsAside Number of neighbors on each side (e.g., 1 means 3 total cells: left, center, right)
 * @param initialRow Initial configuration/seed row
 * @param generations Number of generations/rows to generate (including the initial row)
 * @param wrapAround If true, boundaries wrap around; if false, assume 0s at boundaries
 * @return List of rows, where each row is a list of cell states
 */
fun generateRuleCellularAutomaton(
    rule: Int,
    neighborsAside: Int,
    initialRow: List<Boolean> = List(256) { it == 128 },
    generations: Int = 256,
    wrapAround: Boolean = true
): List<List<Boolean>> {
    require(rule >= 0) { "Rule must be non-negative" }
    require(generations > 0) { "Generations must be positive" }
    require(neighborsAside >= 1) { "Number of neighbours must be 1 or higher" }

    // Build lookup table for the rule
    val maxNeighborhoodValue = (2.0.pow(2 * neighborsAside + 1) - 1).toInt()

    val ruleLookup = buildRuleLookup(rule, maxNeighborhoodValue)

    // Convert initial boolean row to int row
    var currentRow = initialRow

    // Generate all rows
    return buildList {
        add(currentRow)

        repeat(generations - 1) {
            currentRow = generateNextRow(currentRow, neighborsAside, ruleLookup, wrapAround)
            add(currentRow)
        }
    }
}

/**
 * Builds a lookup table mapping neighborhood patterns to next states.
 *
 * @param rule The rule number
 * @param maxValue Maximum neighborhood value to consider
 * @return Map from neighborhood value to next state
 */
private fun buildRuleLookup(rule: Int, maxValue: Int): Map<Int, Boolean> {
    return (0..maxValue).associateWith { neighborhoodValue ->
        // Extract the bit at position 'neighborhoodValue' from the rule
        (rule shr neighborhoodValue) and 1 == 1
    }
}

/**
 * Generates the next row based on the current row and rule.
 *
 * @param currentRow Current row of cell states
 * @param neighborsAside Number of neighbors on each side
 * @param ruleLookup Lookup table for rule transitions
 * @param wrapAround Whether to wrap around at boundaries
 * @return Next row of cell states
 */
private fun generateNextRow(
    currentRow: List<Boolean>,
    neighborsAside: Int,
    ruleLookup: Map<Int, Boolean>,
    wrapAround: Boolean
): List<Boolean> {
    return currentRow.indices.map { index ->
        // Collect neighborhood cells (neighbors on left, center, neighbors on right)
        val neighborhood = (-neighborsAside..neighborsAside).map { offset ->
            cellAt(currentRow, index + offset, wrapAround)
        }

        // Calculate neighborhood value as a binary number
        val neighborhoodValue = neighborhood.fold(0) { acc, cell ->
            (acc shl 1) or (if (cell) 1 else 0)
        }

        // Lookup next state
        ruleLookup[neighborhoodValue] ?: false
    }
}

/**
 * Gets a cell value with boundary handling.
 *
 * @param row Current row
 * @param index Index to retrieve
 * @param wrapAround Whether to wrap around at boundaries
 * @return Cell value at the index, or 0 if out of bounds and not wrapping
 */
private fun cellAt(row: List<Boolean>, index: Int, wrapAround: Boolean): Boolean {
    return when {
        index in row.indices -> row[index]
        wrapAround -> row[index.mod(row.size)]
        else -> false
    }
}
