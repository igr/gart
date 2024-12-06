package dev.obac.gart.box2d

import de.pirckheimer_gymnasium.jbox2d.collision.shapes.PolygonShape
import de.pirckheimer_gymnasium.jbox2d.common.Vec2
import de.pirckheimer_gymnasium.jbox2d.dynamics.BodyDef
import de.pirckheimer_gymnasium.jbox2d.dynamics.World
import dev.oblac.gart.Dimension

/**
 * Creates an open container around a given dimension.
 */
fun createContainer(world: World, d: Dimension) {
    val groundDef = BodyDef()
    val groundBody = world.createBody(groundDef)

    // Left wall
    val leftWallShape = PolygonShape().apply {
        setAsBox(0.1f, d.hf, Vec2(0.1f, d.hf / 2), 0f)
    }
    groundBody.createFixture(leftWallShape, 0f)

    // Right wall
    val rightWallShape = PolygonShape().apply {
        setAsBox(0.1f, d.hf, Vec2(d.wf - 0.1f, d.hf / 2), 0f)
    }
    groundBody.createFixture(rightWallShape, 0f)

    // Bottom
    val bottomShape = PolygonShape().apply {
        setAsBox(d.wf, 0.1f, Vec2(d.wf / 2, d.hf - 0.1f), 0f)
    }
    groundBody.createFixture(bottomShape, 0f)
}
