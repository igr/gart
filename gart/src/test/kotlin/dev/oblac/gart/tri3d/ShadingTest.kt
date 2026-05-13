package dev.oblac.gart.tri3d

import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.vector.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShadingTest {

    /**
     * Triangle in the x/y plane at z = 0 with **centroid at the origin**,
     * winding chosen so the normal points -z. The centroid-at-origin
     * property makes light-direction math exact for the tests below.
     *
     *   a = (-1,-1,0), b = (0,2,0), c = (1,-1,0)
     *   centroid = (0, 0, 0)
     *   edge1 = b-a = (1,3,0), edge2 = c-a = (2,0,0)
     *   normal = edge1 x edge2 = (0,0,-6)
     */
    private fun faceFacingMinusZ(color: Int): Face = Face(
        a = Vec3(-1f, -1f, 0f),
        b = Vec3(0f, 2f, 0f),
        c = Vec3(1f, -1f, 0f),
        color = color,
    )

    /**
     * Same vertices, opposite winding — normal points +z, centroid at origin.
     *
     *   a = (-1,-1,0), b = (1,-1,0), c = (0,2,0)
     *   edge1 = b-a = (2,0,0), edge2 = c-a = (1,3,0)
     *   normal = edge1 x edge2 = (0,0,6)
     */
    private fun faceFacingPlusZ(color: Int): Face = Face(
        a = Vec3(-1f, -1f, 0f),
        b = Vec3(1f, -1f, 0f),
        c = Vec3(0f, 2f, 0f),
        color = color,
    )

    private fun assertNear(expected: Int, actual: Int, tolerance: Int = 2) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "expected $expected ± $tolerance, got $actual",
        )
    }

    @Test
    fun diffuseFullyLitFaceEqualsFaceColor() {
        val faceColor = argb(255, 120, 200, 80)
        val face = faceFacingMinusZ(faceColor)
        val shading = Shading.diffuse(
            light = LightSource(Vec3(0f, 0f, -5f)),
            ambient = 0f,
            strength = 1f,
            falloff = Falloff.NONE,
        )

        val out = shading.color(face, face.normal())

        assertNear(120, red(out))
        assertNear(200, green(out))
        assertNear(80, blue(out))
    }

    @Test
    fun diffuseBackFacingIsPureAmbient() {
        val faceColor = argb(255, 200, 100, 50)
        val face = faceFacingPlusZ(faceColor)
        val shading = Shading.diffuse(
            light = LightSource(Vec3(0f, 0f, -5f)),
            ambient = 0f,
        )

        val out = shading.color(face, face.normal())

        assertEquals(0, red(out))
        assertEquals(0, green(out))
        assertEquals(0, blue(out))
    }

    @Test
    fun diffuseAmbientFloorAppliesWithZeroStrength() {
        val faceColor = argb(255, 200, 100, 50)
        val face = faceFacingMinusZ(faceColor)
        val shading = Shading.diffuse(
            light = LightSource(Vec3(0f, 0f, -5f)),
            ambient = 0.3f,
            strength = 0f,
        )

        val out = shading.color(face, face.normal())

        // 200 * 0.3 = 60, 100 * 0.3 = 30, 50 * 0.3 = 15
        assertNear(60, red(out))
        assertNear(30, green(out))
        assertNear(15, blue(out))
    }

    @Test
    fun diffuseLightAtSurfaceFallsBackToAmbient() {
        // Light positioned exactly at the face centroid. distToLight ≈ 0;
        // INVERSE_SQUARE would otherwise produce 0 * Infinity = NaN and black
        // out the pixel. Expect pure-ambient color instead.
        val faceColor = argb(255, 200, 100, 50)
        val face = faceFacingMinusZ(faceColor)
        val centroid = (face.a + face.b + face.c) / 3f
        val shading = Shading.diffuse(
            light = LightSource(centroid),
            ambient = 0.4f,
            strength = 1f,
            falloff = Falloff.INVERSE_SQUARE,
        )

        val out = shading.color(face, face.normal())

        // 200 * 0.4 = 80, 100 * 0.4 = 40, 50 * 0.4 = 20
        assertNear(80, red(out))
        assertNear(40, green(out))
        assertNear(20, blue(out))
    }

    @Test
    fun diffuseInverseSquareFalloffScalesWithDistance() {
        // Near light at distance d=5, far light at distance 2d=10. With
        // INVERSE_SQUARE, direct(2d) should be ~1/4 of direct(d).
        //
        // Math: lambert=1 (face faces the light), strength=20.
        //   near: direct = 1 * 20 / 25 = 0.8  → green = 255 * 0.8 = 204
        //   far:  direct = 1 * 20 / 100 = 0.2 → green = 255 * 0.2 = 51
        //   ratio = 51 / 204 = 0.25 exactly.
        val faceColor = argb(255, 0, 255, 0)
        val face = faceFacingMinusZ(faceColor)

        val near = Shading.diffuse(
            light = LightSource(Vec3(0f, 0f, -5f)),
            ambient = 0f,
            strength = 20f,
            falloff = Falloff.INVERSE_SQUARE,
        )
        val far = Shading.diffuse(
            light = LightSource(Vec3(0f, 0f, -10f)),
            ambient = 0f,
            strength = 20f,
            falloff = Falloff.INVERSE_SQUARE,
        )

        val gNear = green(near.color(face, face.normal()))
        val gFar = green(far.color(face, face.normal()))

        assertTrue(gNear > 0, "near must be non-black, got $gNear")
        assertTrue(gFar > 0, "far must be non-black, got $gFar")
        val ratio = gFar.toFloat() / gNear.toFloat()
        assertTrue(
            kotlin.math.abs(ratio - 0.25f) < 0.02f,
            "expected ratio ≈ 0.25 ± 0.02, got $ratio (gNear=$gNear gFar=$gFar)",
        )
    }
}
