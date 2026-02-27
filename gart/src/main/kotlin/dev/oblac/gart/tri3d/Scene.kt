package dev.oblac.gart.tri3d

import org.jetbrains.skia.Canvas

object Scene {

    /**
     * Renders a mesh using a per-pixel z-buffer for accurate depth testing.
     * Correctly handles intersecting triangles and complex depth orderings.
     *
     * @param screenWidth  pixel width of the render target
     * @param screenHeight pixel height of the render target
     * @param background   background color (ARGB), default is transparent
     * @param shading      shading function for face colors, default is flat
     */
    fun render(
        canvas: Canvas,
        camera: Camera,
        mesh: Mesh,
        screenWidth: Int,
        screenHeight: Int,
        background: Int = 0,
        shading: Shading = Shading.flat,
    ) {
        val zBuffer = ZBuffer(screenWidth, screenHeight, shading)
        zBuffer.clear(background)
        zBuffer.rasterize(camera, mesh)
        zBuffer.drawTo(canvas)
    }
}
