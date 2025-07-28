package dev.oblac.gart.cellular

class BelousovZhabotinskyReaction1(
    val n: Int = 255,
    val k1: Int = 3,
    val k2: Int = 5,
    val g: Int = 50
) : CellularAutomataRules {
    
    init {
        require(n in 2..255) { "n must be in range 2-255" }
        require(k1 in 1..8) { "k1 must be in range 1-8" }
        require(k2 in 1..8) { "k2 must be in range 1-8" }
        require(g in 0..100) { "g must be in range 0-100" }
    }
    
    override fun validateState(state: Int): Int = state.coerceIn(0, n)
    
    override fun computeNextState(x: Int, y: Int, currentState: Int, neighbors: List<Int>): Int {
        return when (currentState) {
            0 -> { // healthy
                val a = neighbors.count { it in 1 until n } // infected neighbors
                val b = neighbors.count { it == n } // ill neighbors
                (a / k1) + (b / k2)
            }
            
            n -> 0 // ill becomes healthy
            
            else -> { // infected
                val a = neighbors.count { it in 1 until n } // infected neighbors
                val b = neighbors.count { it == n } // ill neighbors
                val s = currentState + neighbors.sum()
                val newState = (s / (a + b + 1)) + g
                minOf(newState, n)
            }
        }
    }
    
    override fun initialState(x: Int, y: Int, width: Int, height: Int): Int {
        val random = kotlin.random.Random.Default
        return when (random.nextFloat()) {
            in 0.0f..0.85f -> 0 // healthy
            in 0.85f..0.95f -> random.nextInt(1, n) // infected
            else -> n // ill
        }
    }
}
