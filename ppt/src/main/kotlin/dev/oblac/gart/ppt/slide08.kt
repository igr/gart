package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.shader.sksl
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

// Plasma - warm sunset palette (amber → magenta → deep purple)
//--- src: 1 Plasma
private val plasmaSksl = """
uniform float time;
uniform vec2 resolution;

half4 main(vec2 fragcoord) {
    vec2 uv = fragcoord / resolution;
    float v = sin(uv.x * 10.0 + time);
    v += sin(uv.y * 10.0 + time * 0.5);
    v += sin((uv.x + uv.y) * 10.0 + time * 0.7);
    v += sin(length(uv - 0.5) * 14.0 - time * 1.2);
    v *= 0.25;
    float t = v * 0.5 + 0.5;
    vec3 a = vec3(0.93, 0.55, 0.05);
    vec3 b = vec3(0.85, 0.15, 0.45);
    vec3 deep = vec3(0.25, 0.05, 0.35);
    vec3 col = mix(mix(deep, b, smoothstep(0.0, 0.5, t)),
                   mix(b, a, smoothstep(0.5, 1.0, t)), t);
    return half4(col, 1.0);
}
""".sksl()
//--- crs: 1

// Ripple - concentric animated rings
//--- src: 2 Ripple
private val rippleSksl = """
uniform float time;
uniform vec2 resolution;

half4 main(vec2 fragcoord) {
    vec2 uv = (fragcoord - resolution * 0.5) / resolution.y;
    float d = length(uv);
    float wave = sin(d * 30.0 - time * 4.0) * 0.5 + 0.5;
    float fade = smoothstep(0.5, 0.0, d);
    vec3 col = mix(vec3(0.05, 0.1, 0.3), vec3(0.2, 0.8, 1.0), wave * fade);
    return half4(col, 1.0);
}
""".sksl()
//--- crs: 2

// Warp - ocean-green palette (teal → emerald → deep navy)
//--- src: 3 Warp
private val warpSksl = """
uniform float time;
uniform vec2 resolution;

half4 main(vec2 fragcoord) {
    vec2 uv = fragcoord / resolution;
    vec2 p = uv * 4.0 - 2.0;
    float t = time * 0.5;
    p.x += sin(p.y * 2.0 + t) * 0.5;
    p.y += cos(p.x * 2.0 + t * 1.3) * 0.5;
    float v = sin(length(p) * 2.0 + t) * 0.5 + 0.5;
    vec3 deep = vec3(0.03, 0.05, 0.2);
    vec3 teal = vec3(0.0, 0.6, 0.55);
    vec3 bright = vec3(0.3, 0.95, 0.6);
    vec3 col = mix(mix(deep, teal, smoothstep(0.0, 0.45, v)),
                   mix(teal, bright, smoothstep(0.45, 1.0, v)), v);
    return half4(col, 1.0);
}
""".sksl()
//--- crs: 3

// Voronoi - cell pattern
private val voronoiSksl = """
uniform float time;
uniform vec2 resolution;

vec2 hash(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

half4 main(vec2 fragcoord) {
    vec2 uv = fragcoord / resolution * 5.0;
    vec2 ip = floor(uv);
    vec2 fp = fract(uv);
    float minDist = 1.0;
    vec2 minPoint = vec2(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = hash(ip + neighbor);
            point = 0.5 + 0.5 * sin(time * 0.7 + 6.28 * point);
            float d = length(neighbor + point - fp);
            if (d < minDist) {
                minDist = d;
                minPoint = point;
            }
        }
    }
    float v = sin(minPoint.x * 6.28) * 0.5 + 0.5;
    vec3 warm = vec3(0.95, 0.75, 0.2);
    vec3 rose = vec3(0.9, 0.35, 0.3);
    vec3 deep = vec3(0.3, 0.08, 0.15);
    vec3 col = mix(mix(deep, rose, smoothstep(0.0, 0.5, v)),
                   mix(rose, warm, smoothstep(0.5, 1.0, v)), v);
    col *= 1.0 - 0.4 * minDist;
    return half4(col, 1.0);
}
""".sksl()

val slide08 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    c.clear(CssColors.indigo)
    c.drawTitle("SkSL Shaders")

    val grid = contentBox.shrink(20f).splitToGrid(2, 2)
    val time = f.timeSeconds

    // 1. Plasma
    val g1 = grid[0].shrink(10f)
    plasmaSksl.uniform("time", time)
    plasmaSksl.uniform("resolution", g1.width, g1.height)
    c.save()
    c.clipRect(g1)
    c.translate(g1.left, g1.top)
    c.drawPaint(Paint().apply { shader = plasmaSksl.makeShader() })
    c.restore()
    c.drawLabel(g1, "Plasma")

    // 2. Ripple
    val g2 = grid[1].shrink(10f)
    rippleSksl.uniform("time", time)
    rippleSksl.uniform("resolution", g2.width, g2.height)
    c.save()
    c.clipRect(g2)
    c.translate(g2.left, g2.top)
    c.drawPaint(Paint().apply { shader = rippleSksl.makeShader() })
    c.restore()
    c.drawLabel(g2, "Ripple")

    // 3. Warp
    val g3 = grid[2].shrink(10f)
    warpSksl.uniform("time", time)
    warpSksl.uniform("resolution", g3.width, g3.height)
    c.save()
    c.clipRect(g3)
    c.translate(g3.left, g3.top)
    c.drawPaint(Paint().apply { shader = warpSksl.makeShader() })
    c.restore()
    c.drawLabel(g3, "Domain warp")

    // 4. Voronoi
    val g4 = grid[3].shrink(10f)
    voronoiSksl.uniform("time", time)
    voronoiSksl.uniform("resolution", g4.width, g4.height)
    c.save()
    c.clipRect(g4)
    c.translate(g4.left, g4.top)
    c.drawPaint(Paint().apply { shader = voronoiSksl.makeShader() })
    c.restore()
    c.drawLabel(g4, "Voronoi")
}
