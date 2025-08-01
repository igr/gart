package dev.oblac.gart.cellular

fun newBelousovZhabotinskyReaction2(
    q: Int = 255,
    k1: Int = 3,
    k2: Int = 5,
    g: Int = 50
): CellularAutomataRules {
    
    require(q in 2..255) { "q must be in range 2-255" }
    require(k1 in 1..8) { "k1 must be in range 1-8" }
    require(k2 in 1..8) { "k2 must be in range 1-8" }
    require(g in 0..100) { "g must be in range 0-100" }

    return CellularAutomataRules(
        validateState = fun(state: Int): Int = state.coerceIn(1, q),
        computeNextState = fun(x: Int, y: Int, currentState: Int, neighbors: List<Int>): Int {
            return when (currentState) {
                q -> 1

                1 -> {
                    val a = neighbors.count { it in 2 until q }
                    val b = neighbors.count { it == q }
                    val newState = a.toFloat() / k1 + b.toFloat() / k2 + 1
                    minOf(newState.toInt(), q)
                }

                else -> {
                    val s = currentState + neighbors.sum()
                    val c = neighbors.count { it == 1 }
                    val newState = s.toFloat() / (9 - c) + g
                    minOf(newState.toInt(), q)
                }
            }
        },
        initialState = fun(x: Int, y: Int, width: Int, height: Int): Int {
            val random = kotlin.random.Random.Default
            return when (random.nextFloat()) {
                in 0.0f..0.80f -> 1 // resting state
                else -> 255
            }
        }
    )
}
