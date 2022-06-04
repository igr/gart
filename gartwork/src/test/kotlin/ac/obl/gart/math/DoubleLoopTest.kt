package ac.obl.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DoubleLoopTest {

    @Test
    fun doubleLoop_ints() {
        var results = ""
        doubleLoop(5, 3) { (i, j) ->
            results += "$i,$j,"
        }
        assertEquals("0,0,1,0,2,0,3,0,4,0,0,1,1,1,2,1,3,1,4,1,0,2,1,2,2,2,3,2,4,2,", results)
    }

    @Test
    fun doubleLoop_floats() {
        var results = ""
        doubleLoop(0f to 0f, 5f, 3f, 1f to 1f) { (i, j) ->
            results += "${i.toInt()},${j.toInt()},"
        }
        assertEquals("0,0,1,0,2,0,3,0,4,0,0,1,1,1,2,1,3,1,4,1,0,2,1,2,2,2,3,2,4,2,", results)
    }
}
