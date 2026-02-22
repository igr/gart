package dev.oblac.gart

import de.pirckheimer_gymnasium.jbox2d.collision.shapes.CircleShape
import de.pirckheimer_gymnasium.jbox2d.collision.shapes.PolygonShape
import de.pirckheimer_gymnasium.jbox2d.common.Vec2
import de.pirckheimer_gymnasium.jbox2d.dynamics.*
import de.pirckheimer_gymnasium.jbox2d.particle.ParticleGroupDef
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.rndf

fun main() {
    val gart = Gart.of("exampleFluid", 1024, 1024)
    val d = gart.d

    val world = World(
        Vec2(0f, 9.81f) // gravity pointing down
    ).apply {
        particleDamping = 0.2f // Reduces velocity over time for stability
        particleRadius = 1f // Particle radius
    }

    createWaterParticles(world)

    // balls
    val balls = mutableListOf<Body>()
    repeat(8) { balls.add(spawnBall(world, d, 50f)) }

    createContainer(world, d)

    val ballColor = fillOf(Colors.coral)
    val waterColor = fillOf(Colors.cyanColor)

    gart.window().show { c, _, f ->
        world.step(f.frameDurationSeconds, 4, 2)

        c.clear(Colors.gray)
        balls.forEach {
            val pos = it.position
            c.drawCircle(pos.x, pos.y, 50f, ballColor)
        }

        val particleCount = world.particleCount
        val positions = world.particlePositionBuffer
        for (i in 0 until particleCount) {
            val x = (positions[i].x)
            val y = (positions[i].y)
            val radius = 10f
            c.drawCircle(x - radius, y - radius, radius * 2, waterColor)
        }
    }
}

private fun createContainer(world: World, d: Dimension) {
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

private fun spawnBall(world: World, d: Dimension, ballRadius: Float): Body {
    val ballDef = BodyDef().apply {
        type = BodyType.DYNAMIC
        position.set(rndf(0, d.wf), rndf(250f)) // random X position at top
    }
    val ballBody = world.createBody(ballDef)
    val circleShape = CircleShape().apply {
        radius = ballRadius
    }
    val fixtureDef = FixtureDef().apply {
        shape = circleShape
        density = 1f
        friction = 0.3f
        restitution = 0.8f // Make it bouncy
    }
    ballBody.createFixture(fixtureDef)
    return ballBody
}

private fun createWaterParticles(world: World) {
    val initialVelocity = Vec2(30f, 30f)

    val pg = ParticleGroupDef().apply {
        position.set(440f, 400f)    // position of water
        angularVelocity = 0.1f  // angular velocity of water
        linearVelocity.set(initialVelocity)
        //initialVelocity.set(initialVelocity)   // initial velocity of water
        shape = PolygonShape().apply {
            setAsBox(
                400f,   // half-width
                10f,    // half-height
                Vec2(0f, 0f),   // The center position of the box in local coordinates. In this case, (0f, 0f) means the rectangle is centered at the origin of the particle group.
                10f  // the rotation of the box in local coordinates
            )
        }
    }
    world.createParticleGroup(pg)
}
