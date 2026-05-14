package dev.oblac.gart.tri3d

import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.argb
import dev.oblac.gart.vector.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneTest {

    @Test
    fun renderVolumetricDelegatesToVolumetricLight() {
        val cam = Camera(screenCx = 8f, screenCy = 8f, scale = 4f, distance = 4f)
        val mesh = Mesh(emptyList())
        val vl = VolumetricLight(
            lights = listOf(LightSource(Vec3(0f, 0f, -10f))),
            samples = 4,
            seed = 99L,
            background = argb(255, 11, 22, 33),
        )

        val direct = vl.render(cam, mesh, 16, 16)
        val viaScene = Scene.renderVolumetric(cam, mesh, 16, 16, vl)

        val gmA = Gartmap(direct)
        val gmB = Gartmap(viaScene)
        for (y in 0 until 16) for (x in 0 until 16) {
            assertEquals(gmA.pixels[y * 16 + x], gmB.pixels[y * 16 + x], "differ at ($x,$y)")
        }
    }
}
