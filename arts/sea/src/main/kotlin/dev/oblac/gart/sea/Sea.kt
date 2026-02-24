package dev.oblac.gart.sea

import de.pirckheimer_gymnasium.jbox2d.collision.shapes.CircleShape
import de.pirckheimer_gymnasium.jbox2d.collision.shapes.PolygonShape
import de.pirckheimer_gymnasium.jbox2d.common.Vec2
import de.pirckheimer_gymnasium.jbox2d.dynamics.*
import de.pirckheimer_gymnasium.jbox2d.particle.ParticleGroupDef
import dev.obac.gart.box2d.createContainer
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.*
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeLinearGradient

const val waterRadius = 2f
const val ballRadius = 50f

val ppp = Palettes.cool7.expand(100)
val waterColors = ppp.map { fillOf(it) }
val waterColorsInt = ppp.map { it.color4f() }.toTypedArray()
val bbbColors = Palettes.cool36.map { fillOf(it) }

val topGradient = Paint().apply {
    this.isAntiAlias = true
    this.shader = makeLinearGradient(
        Point(0f, 0f),
        Point(0f, 800f),
        waterColorsInt,
        null, null, GradientStyle.DEFAULT
    )
}

val gart = Gart.of("sea", 600, 1024)
val d = gart.d
fun main() {
    val world = World(
        Vec2(0f, 9.81f) // gravity pointing down
    ).apply {
        particleDamping = 0.2f // Reduces velocity over time for stability
        particleRadius = waterRadius // Particle radius
    }

    createWaterParticles(world, 00f, 00f)
    // balls
    val balls = mutableListOf<Body>()
    balls.add(spawnBall(world, ballRadius, 100f, 300f))
    balls.add(spawnBall(world, ballRadius, 330f, 400f))
    balls.add(spawnBall(world, ballRadius, 500f, 300f))
    createContainer(world, d)

    val whites = 20
    val whitePoints = randomPoints(d, whites).map { Circle.of(it, rndi(20, 40)) }

    val s = gart.snapshot()
    gart.window().show { c, _, f ->
        c.clear(BgColors.bg03)
        if (f.new) {
            println(f.frame)
        }
        if (s.isCaptured()) {
            s.draw(c)
            return@show
        }

        // WATER ANIMATION that generates layers of water particles
        world.step(f.frameDurationSeconds, 4, 2)

        // DRAW SCENE
        c.drawRect(Rect(0f, 0f, d.wf, 200f), topGradient)

        val particleCount = world.particleCount
        val positions = world.particlePositionBuffer
        for (i in 0 until particleCount) {
            val x = (positions[i].x)
            val y = (positions[i].y)
            val radius = waterRadius
            if (y < 0) {
                continue
            }

            // find the color for the particle
            val index = y.toInt() / 8
            val waterColor = waterColors[index]

            c.drawCircle(x - radius, y - radius, radius * 2, waterColor)
        }

        whitePoints.forEach { circ ->
            c.drawCircle(circ, fillOfWhite().apply { alpha = 50 })
        }

        balls.forEach {
            val pos = it.position
            c.drawCircle(pos.x, pos.y, ballRadius, bbbColors[8])
        }

        // END OF ANIMATION
        if (f.frame == 133L) {
            repeat(500) { drawForceField(c) }
            balls.forEach {
                val pos = it.position
                c.drawCircle(pos.x, pos.y, ballRadius, bbbColors[8])
            }
            c.drawBorder(d, 20f, Color.WHITE)
            s.freeze(c)
            s.saveImage()
        }
    }
}

private fun createWaterParticles(world: World, posx: Float, posy: Float) {
    val pg = ParticleGroupDef().apply {
        position.set(posx, posy)    // position of water
        angularVelocity = 0f  // angular velocity of water
        linearVelocity.set(Vec2(0f, 100f))
        shape = PolygonShape().apply {
            setAsBox(
                600f,    // half-width
                200f,    // half-height
                Vec2(0f, 0f),   // The center position of the box in local coordinates. In this case, (0f, 0f) means the rectangle is centered at the origin of the particle group.
                0f  // the rotation of the box in local coordinates
            )
        }
    }
    world.createParticleGroup(pg)
}

private fun spawnBall(world: World, ballRadius: Float, x: Float, y: Float): Body {
    val ballDef = BodyDef().apply {
        type = BodyType.STATIC
        position.set(x, y) // random X position at top
    }
    val ballBody = world.createBody(ballDef)
    val circleShape = CircleShape().apply {
        radius = ballRadius
    }
    val fixtureDef = FixtureDef().apply {
        shape = circleShape
        density = 1f
        friction = 0.1f
    }
    ballBody.createFixture(fixtureDef)
    return ballBody
}

// FIELD
val fnz = ComplexFunctions.polesAndHoles(
    poles = Array(40) {
        val x = 0.1 * kotlin.math.sin((rndf(0, 360)).toRadian())
        val y = 0.1 * kotlin.math.cos((rndf(0, 360)).toRadian())
        Complex(x, y)
    },
    holes = Array(20) {
        val x = 0.2 * kotlin.math.sin((rndf(0, 360)).toRadian())
        val y = 0.2 * kotlin.math.cos((rndf(0, 360)).toRadian())
        Complex(x, y)
    },
)
val complexField = ComplexField.of(gart.d) { x, y ->
    fnz(Complex(x, y))
}
val ff = ForceField.from(gart.d) { x, y ->
    complexField[x, y].let { c -> Vector2(c.real, c.imag) }
}

var points = Array(1000) {
    Point(rndf(0f, gart.d.wf), rndf(0f, 200))
}.toList()

fun drawForceField(c: Canvas) {
    points = ff.apply(points) { old, p ->
        c.drawLine(old.x, old.y, p.x, p.y, strokeOfWhite(1f).also { it.alpha = 50 })
    }
}
