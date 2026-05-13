package dev.oblac.gart.tri3d

import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.argb
import dev.oblac.gart.vector.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VolumetricLightTest {

    private fun emptyMesh() = Mesh(emptyList())

    private fun freshZBuffer(w: Int = 16, h: Int = 16, fill: Int = argb(255, 32, 32, 32)): ZBuffer {
        val zb = ZBuffer(w, h)
        zb.clear(fill)
        return zb
    }

    private fun sampleColor(zb: ZBuffer, x: Int, y: Int): Int {
        // gartmap is package-internal (see Task 4): same package, direct ARGB read.
        return zb.gartmap.pixels[y * zb.width + x]
    }

    @Test
    fun zeroSamplesIsNoOp() {
        val zb = freshZBuffer()
        val before = sampleColor(zb, 8, 8)

        VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 0,
        ).apply(zb, defaultCamera(), emptyMesh())

        assertEquals(before, sampleColor(zb, 8, 8))
    }

    @Test
    fun zeroStrengthIsNoOp() {
        val zb = freshZBuffer()
        val before = sampleColor(zb, 8, 8)

        VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 8,
            strength = 0f,
        ).apply(zb, defaultCamera(), emptyMesh())

        assertEquals(before, sampleColor(zb, 8, 8))
    }

    @Test
    fun emptyMeshWithReplaceProducesUniformHaze() {
        // No occluders + REPLACE + NONE falloff => every pixel gets the same color
        val zb = freshZBuffer()
        VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 4,
            strength = 1f,
            color = argb(255, 200, 200, 200),
            blendMode = VolumetricBlend.REPLACE,
            falloff = Falloff.NONE,
            maxDistance = 10f,
            seed = 42L,
        ).apply(zb, defaultCamera(), emptyMesh())

        val a = sampleColor(zb, 0, 0)
        val b = sampleColor(zb, 8, 8)
        val c = sampleColor(zb, 15, 15)
        assertEquals(a, b)
        assertEquals(b, c)
        assertNotEquals(argb(255, 32, 32, 32), a)
    }

    @Test
    fun addBlendBrightensPixels() {
        val baseFill = argb(255, 10, 10, 10)
        val zb = freshZBuffer(fill = baseFill)
        VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 4,
            strength = 1f,
            color = argb(255, 100, 100, 100),
            blendMode = VolumetricBlend.ADD,
            falloff = Falloff.NONE,
            maxDistance = 10f,
            seed = 7L,
        ).apply(zb, defaultCamera(), emptyMesh())

        val after = sampleColor(zb, 8, 8)
        assertTrue(red(after) >= red(baseFill))
        assertTrue(green(after) >= green(baseFill))
        assertTrue(blue(after) >= blue(baseFill))
        assertNotEquals(baseFill, after)
    }

    @Test
    fun deterministicWithSameSeed() {
        val zb1 = freshZBuffer()
        val zb2 = freshZBuffer()
        val vl = VolumetricLight(
            light = LightSource(Vec3(2f, -1f, -10f)),
            samples = 6,
            seed = 12345L,
        )
        val cam = defaultCamera()
        vl.apply(zb1, cam, emptyMesh())
        vl.apply(zb2, cam, emptyMesh())

        for (y in 0 until 16) for (x in 0 until 16) {
            assertEquals(sampleColor(zb1, x, y), sampleColor(zb2, x, y), "differ at ($x,$y)")
        }
    }

    @Test
    fun renderEmptyMeshSamplesZeroProducesBackgroundOnly() {
        val bg = argb(255, 50, 60, 70)
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 0,
            background = bg,
        )
        val gv = vl.render(defaultCamera(), emptyMesh(), width = 16, height = 16)

        val gm = Gartmap(gv)
        for (y in 0 until 16) for (x in 0 until 16) {
            assertEquals(bg, gm.pixels[y * 16 + x], "differ at ($x,$y)")
        }
    }

    @Test
    fun renderReturnsGartvasOfRequestedSize() {
        val vl = VolumetricLight(light = LightSource(Vec3(0f, 0f, -10f)), samples = 0)
        val gv = vl.render(defaultCamera(), emptyMesh(), width = 32, height = 24)
        assertEquals(32, gv.d.w)
        assertEquals(24, gv.d.h)
    }

    @Test
    fun renderAmbientFloorAppliesToHitFace() {
        // No volumetric (samples = 0), no direct light contribution (strength = 0)
        // => hit pixel = face.color * ambient.
        val faceColor = argb(255, 200, 100, 50)
        val mesh = Mesh(listOf(frontTriangle(faceColor)))
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -4f)),
            samples = 0,
            strength = 0f,
            ambient = 0.5f,
            background = argb(255, 0, 0, 0),
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        val center = gm.pixels[8 * 16 + 8]
        assertEquals(100, red(center))   // 200 * 0.5
        assertEquals(50, green(center))  // 100 * 0.5
        assertEquals(25, blue(center))   //  50 * 0.5
    }

    @Test
    fun renderDirectIlluminationHitsAtNormal() {
        // Light placed at the camera eye, in line with the surface normal.
        // ambient = 0, NONE falloff, strength = 1 => hit pixel ≈ face.color * dot(n, lightDir).
        // Surface at z=1, eye at z=-4 => P ≈ (0,0,1), toLight = (0,0,-5), lightDir = (0,0,-1).
        // Face normal normalized = (0,0,-1). Dot = 1. So pixel == face.color.
        val faceColor = argb(255, 120, 200, 80)
        val mesh = Mesh(listOf(frontTriangle(faceColor)))
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -4f)),
            samples = 0,
            strength = 1f,
            ambient = 0f,
            falloff = Falloff.NONE,
            background = argb(255, 0, 0, 0),
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        val center = gm.pixels[8 * 16 + 8]
        assertNear(120, red(center), 2)
        assertNear(200, green(center), 2)
        assertNear(80, blue(center), 2)
    }

    @Test
    fun renderShadowZeroesDirectLeavesAmbient() {
        // Two walls: near (z=1) and far (z=5). Light is *behind* the far wall (z=10),
        // so the shadow ray from the near hit toward the light is occluded by the far wall.
        // ambient = 0.4 => near-wall pixel should be face.color * 0.4 (direct zeroed).
        val nearColor = argb(255, 240, 240, 240)
        val farColor = argb(255, 10, 10, 10)
        val near = frontWall(z = 1f, color = nearColor)
        val far = frontWall(z = 5f, color = farColor)
        val mesh = Mesh(near.faces + far.faces)

        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, 10f)),
            samples = 0,
            strength = 5f,
            ambient = 0.4f,
            falloff = Falloff.NONE,
            background = argb(255, 0, 0, 0),
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        val center = gm.pixels[8 * 16 + 8]
        // 240 * 0.4 = 96
        assertNear(96, red(center), 2)
        assertNear(96, green(center), 2)
        assertNear(96, blue(center), 2)
    }

    @Test
    fun renderMissedPixelEqualsBackground() {
        val mesh = Mesh(listOf(frontTriangle(argb(255, 255, 255, 255))))
        val bg = argb(255, 7, 8, 9)
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -4f)),
            samples = 0,
            ambient = 1f,
            background = bg,
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        // Corners are well outside the small triangle's screen-space footprint.
        assertEquals(bg, gm.pixels[0])
        assertEquals(bg, gm.pixels[15])
        assertEquals(bg, gm.pixels[15 * 16])
        assertEquals(bg, gm.pixels[15 * 16 + 15])
    }

    @Test
    fun renderEmptyMeshUniformHazeMatchesApply() {
        // Empty mesh + REPLACE + NONE falloff: every pixel of render(...) gets the
        // same color, just like the existing zero-mesh apply test.
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -10f)),
            samples = 4,
            strength = 1f,
            color = argb(255, 200, 200, 200),
            blendMode = VolumetricBlend.REPLACE,
            falloff = Falloff.NONE,
            maxDistance = 10f,
            seed = 42L,
            background = argb(255, 32, 32, 32),
        )
        val gv = vl.render(defaultCamera(), emptyMesh(), 16, 16)
        val gm = Gartmap(gv)

        val a = gm.pixels[0]
        val b = gm.pixels[8 * 16 + 8]
        val c = gm.pixels[15 * 16 + 15]
        assertEquals(a, b)
        assertEquals(b, c)
        assertNotEquals(argb(255, 32, 32, 32), a)
    }

    @Test
    fun renderDeterministicWithSameSeed() {
        val vl = VolumetricLight(
            light = LightSource(Vec3(2f, -1f, -10f)),
            samples = 6,
            seed = 12345L,
            background = argb(255, 16, 16, 16),
        )
        val cam = defaultCamera()
        val gv1 = vl.render(cam, emptyMesh(), 16, 16)
        val gv2 = vl.render(cam, emptyMesh(), 16, 16)
        val gm1 = Gartmap(gv1)
        val gm2 = Gartmap(gv2)

        for (y in 0 until 16) for (x in 0 until 16) {
            assertEquals(gm1.pixels[y * 16 + x], gm2.pixels[y * 16 + x], "differ at ($x,$y)")
        }
    }

    @Test
    fun renderShowsGeometryFartherThanMaxDistance() {
        // A wall at z=15 → distance from eye (z=-4) is 19. maxDistance=5 sets
        // the volumetric march bound but must NOT clip primary-ray geometry.
        // With ambient=1, strength=0, the lit center pixel should equal the
        // wall color, not the background.
        val wallColor = argb(255, 240, 240, 240)
        val mesh = frontWall(z = 15f, color = wallColor)
        val bg = argb(255, 0, 0, 0)
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -4f)),
            samples = 0,
            strength = 0f,
            ambient = 1f,
            maxDistance = 5f,
            background = bg,
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        val center = gm.pixels[8 * 16 + 8]
        assertNear(240, red(center), 2)
        assertNear(240, green(center), 2)
        assertNear(240, blue(center), 2)
    }

    @Test
    fun renderAmbientOnlyUnaffectedByZeroVolumetric() {
        // With samples = 0 and strength = 0, the volumetric march contributes
        // nothing — Task 4's ambient-floor result must still hold after Task 5
        // wires the march in.
        val faceColor = argb(255, 200, 100, 50)
        val mesh = Mesh(listOf(frontTriangle(faceColor)))
        val vl = VolumetricLight(
            light = LightSource(Vec3(0f, 0f, -4f)),
            samples = 0,
            strength = 0f,
            ambient = 0.5f,
            background = argb(255, 0, 0, 0),
        )
        val gv = vl.render(defaultCamera(), mesh, 16, 16)
        val gm = Gartmap(gv)

        val center = gm.pixels[8 * 16 + 8]
        assertEquals(100, red(center))
        assertEquals(50, green(center))
        assertEquals(25, blue(center))
    }

    // --- helpers --------------------------------------------------------------

    private fun defaultCamera() = Camera(screenCx = 8f, screenCy = 8f, scale = 4f, distance = 4f)

    /**
     * A single triangle at z = 1 covering the screen center for the default
     * 16x16 camera. Winding chosen so the normal points -z (back toward the
     * camera eye at z = -4).
     *   a = (-1,-1,1), b = (0,1,1), c = (1,-1,1)
     *   edge1 = (1,2,0), edge2 = (2,0,0), normal = edge1 x edge2 = (0,0,-4)
     */
    private fun frontTriangle(color: Int): Face = Face(
        a = Vec3(-1f, -1f, 1f),
        b = Vec3(0f, 1f, 1f),
        c = Vec3(1f, -1f, 1f),
        color = color,
    )

    /**
     * An axis-aligned quad (two triangles) at z = z, big enough to fully
     * cover the camera's view. Winding gives normals pointing -z.
     */
    private fun frontWall(z: Float, color: Int): Mesh {
        val a = Vec3(-10f, -10f, z)
        val b = Vec3(10f, -10f, z)
        val c = Vec3(10f, 10f, z)
        val d = Vec3(-10f, 10f, z)
        return Mesh(listOf(
            Face(a, d, c, color),
            Face(a, c, b, color),
        ))
    }

    private fun assertNear(expected: Int, actual: Int, tolerance: Int) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "expected $expected ± $tolerance, got $actual",
        )
    }

    private fun red(c: Int): Int = (c shr 16) and 0xFF
    private fun green(c: Int): Int = (c shr 8) and 0xFF
    private fun blue(c: Int): Int = c and 0xFF
}
