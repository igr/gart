package dev.oblac.gart.tri3d

import org.jetbrains.skia.Canvas

object Scene {

    /**
     * Rasterizes [mesh] into a fresh [ZBuffer] using a per-pixel z-buffer for
     * accurate depth testing. Returns the buffer so callers can run further
     * passes (e.g. [VolumetricLight]) before drawing it out.
     *
     * @param screenWidth  pixel width of the render target
     * @param screenHeight pixel height of the render target
     * @param background   background color (ARGB), default is transparent
     * @param shading      shading function for face colors, default is flat
     */
    fun rasterize(
        camera: Camera,
        mesh: Mesh,
        screenWidth: Int,
        screenHeight: Int,
        background: Int = 0,
        shading: Shading = Shading.flat,
    ): ZBuffer {
        val zBuffer = ZBuffer(screenWidth, screenHeight, shading)
        zBuffer.clear(background)
        zBuffer.rasterize(camera, mesh)
        return zBuffer
    }

    /**
     * Renders a mesh straight to [canvas]. Equivalent to
     * [rasterize] followed by [ZBuffer.drawTo].
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
        rasterize(camera, mesh, screenWidth, screenHeight, background, shading).drawTo(canvas)
    }

    /**
     * Renders [mesh] using [vl] as a standalone volumetric renderer (no
     * pre-rasterized z-buffer). Pure delegation to [VolumetricLight.render]
     * so call sites read parallel to [rasterize] / [render].
     */
    fun renderVolumetric(
        camera: Camera,
        mesh: Mesh,
        screenWidth: Int,
        screenHeight: Int,
        vl: VolumetricLight,
    ): dev.oblac.gart.Gartvas = vl.render(camera, mesh, screenWidth, screenHeight)
}
