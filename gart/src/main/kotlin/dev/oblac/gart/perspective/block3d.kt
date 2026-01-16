package dev.oblac.gart.perspective

import dev.oblac.gart.gfx.Poly4
import org.jetbrains.skia.Point

/**
 * Represents a 3D block in two-point perspective.
 * The block consists of three visible faces: left, right, and either top or bottom
 * (depending on whether the viewer is looking down or up at the block).
 *
 * @property left The left face of the block (edges converge to left vanishing point)
 * @property right The right face of the block (edges converge to right vanishing point)
 * @property top The top face of the block (visible when horizon is above the block’s top), null otherwise
 * @property bottom The bottom face of the block (visible when horizon is below the block’s bottom), null otherwise
 */
data class Block3D(
    val left: Poly4,
    val right: Poly4,
    val top: Poly4?,
    val bottom: Poly4?,
) {
    /**
     * Returns all visible faces as a list.
     */
    fun faces(): List<Poly4> = listOfNotNull(left, right, top, bottom)

    /**
     * Returns the horizontal face (either top or bottom, whichever is visible).
     */
    fun horizontalFace(): Poly4? = top ?: bottom

    companion object {
        /**
         * Creates a 3D block in two-point perspective.
         * The horizontal face (top or bottom) is determined by the horizon (eye level) relative to
         * the block’s vertical span in screen coordinates:
         * - If horizon is above the block’s top (frontTop.y) → top face is visible
         * - If horizon is below the block’s bottom (frontBottom.y) → bottom face is visible
         * - If horizon crosses the block’s vertical span → neither top nor bottom is visible
         *
         * @param vpLeft Left vanishing point (perspective focus on left side)
         * @param vpRight Right vanishing point (perspective focus on right side)
         * @param frontBottom The front bottom corner of the block (closest point to viewer)
         * @param height The height of the front edge in pixels
         * @param leftWidth Width of the left face in pixels (distance along perspective line)
         * @param rightWidth Width of the right face in pixels (distance along perspective line)
         * @return A Block3D containing left, right, and either top or bottom face
         */
        fun of(
            vpLeft: Point,
            vpRight: Point,
            frontBottom: Point,
            height: Float,
            leftWidth: Float,
            rightWidth: Float
        ): Block3D {
            // Front top corner (directly above front bottom)
            val frontTop = Point(frontBottom.x, frontBottom.y - height)

            // Calculate horizon Y (average of vanishing points Y)
            val horizonY = (vpLeft.y + vpRight.y) / 2

            // In screen coordinates, smaller Y is "higher".
            // Top face is visible only when the horizon (eye level) is ABOVE the block's top.
            // Bottom face is visible only when the horizon is BELOW the block's bottom.
            // If the horizon passes through the block's vertical span, neither is visible.

            // Calculate points on the left face using pixel distance
            val leftBottomBack = pointAtDistance(frontBottom, vpLeft, leftWidth)
            val leftTopBack = pointAtDistance(frontTop, vpLeft, leftWidth)

            // Calculate points on the right face using pixel distance
            val rightBottomBack = pointAtDistance(frontBottom, vpRight, rightWidth)
            val rightTopBack = pointAtDistance(frontTop, vpRight, rightWidth)

            // Create the left and right faces
            val leftFace = Poly4(
                frontBottom,
                frontTop,
                leftTopBack,
                leftBottomBack
            )

            val rightFace = Poly4(
                frontBottom,
                rightBottomBack,
                rightTopBack,
                frontTop
            )

            // Create top, bottom, or neither face based on perspective
            // - horizon above the block’s top → top face visible
            // - horizon below the block’s bottom → bottom face visible
            // - horizon crossing the block’s vertical span → neither visible
            val topFace: Poly4?
            val bottomFace: Poly4?

            when {
                horizonY < frontTop.y -> {
                    // Block is below the horizon (eye level) -> top face visible
                    val topBackCorner = lineIntersection(
                        leftTopBack, vpRight,
                        rightTopBack, vpLeft
                    ) ?: Point(
                        (leftTopBack.x + rightTopBack.x) / 2,
                        (leftTopBack.y + rightTopBack.y) / 2
                    )

                    topFace = Poly4(
                        frontTop,
                        rightTopBack,
                        topBackCorner,
                        leftTopBack
                    )
                    bottomFace = null
                }
                horizonY > frontBottom.y -> {
                    // Block is above the horizon (eye level) -> bottom face visible
                    val bottomBackCorner = lineIntersection(
                        leftBottomBack, vpRight,
                        rightBottomBack, vpLeft
                    ) ?: Point(
                        (leftBottomBack.x + rightBottomBack.x) / 2,
                        (leftBottomBack.y + rightBottomBack.y) / 2
                    )

                    bottomFace = Poly4(
                        frontBottom,
                        rightBottomBack,
                        bottomBackCorner,
                        leftBottomBack
                    )
                    topFace = null
                }
                else -> {
                    // Horizon passes through the block's vertical span -> neither horizontal face visible
                    topFace = null
                    bottomFace = null
                }
            }

            return Block3D(leftFace, rightFace, topFace, bottomFace)
        }

        /**
         * Calculates a point at a specific pixel distance from start toward end.
         */
        private fun pointAtDistance(start: Point, end: Point, distance: Float): Point {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            if (length == 0f) return start
            val t = distance / length
            return Point(
                start.x + t * dx,
                start.y + t * dy
            )
        }

        /**
         * Calculates the intersection point of two lines defined by two points each.
         * Line 1: from p1 through p2
         * Line 2: from p3 through p4
         * Returns null if lines are parallel.
         */
        private fun lineIntersection(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
            val x1 = p1.x
            val y1 = p1.y
            val x2 = p2.x
            val y2 = p2.y
            val x3 = p3.x
            val y3 = p3.y
            val x4 = p4.x
            val y4 = p4.y

            val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            if (denom == 0f) return null // Lines are parallel

            val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom

            return Point(
                x1 + t * (x2 - x1),
                y1 + t * (y2 - y1)
            )
        }
    }
}
