package studio.oblac.gart.util

import studio.oblac.gart.Dimension

data class Force(val direction: Float, val strength: Float = 1f) {
    
}

class ForceField(val w: Int, val h: Int, field: Array<Array<Force>>) {

    companion object {
        /**
         * Creates a force field from the dimension, but scaled by the factor.
         * Usually we want to create a force field that is bigger than the image.
         */
        fun of(d: Dimension, f: Float = 1.5f, supplier: (Int, Int) -> Force) =
            of((d.w * f).toInt(), (d.h * f).toInt(), supplier)

        fun of(width: Int, height: Int, supplier: (Int, Int) -> Force): ForceField {
            val field = Array(width) { x ->
                Array(height) { y ->
                    supplier(x, y)
                }
            }
            return ForceField(width, height, field)
        }
    }
}
