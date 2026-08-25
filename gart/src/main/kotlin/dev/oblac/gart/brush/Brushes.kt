package dev.oblac.gart.brush

import dev.oblac.gart.math.lerp

/**
 * Stock brushes. All tuned for size 1 on a canvas around 1000 px; go to
 * size 2..4 for sketch lines, more for a big pastel smear. Copy one to make your own:
 * `Brushes.pencil2B.copy(scatter = 1.2f)`.
 */
object Brushes {

    /** Ballpoint: fine, even, a little pooling at the ends. */
    val pen = Brush(
        weight = 0.3f, scatter = 0.15f, sharpness = 0.9f, grain = 0.7f, opacity = 0.59f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.2f, peak = 1f, drift = 0.15f, spread = 0.2f),
    )

    /** Technical pen: the thinnest and most regular line here. */
    val rotring = Brush(
        weight = 0.15f, scatter = 0.05f, sharpness = 0.7f, grain = 0.9f, opacity = 0.82f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.3f, peak = 1f, drift = 0.35f, spread = 0.2f),
    )

    /** Soft graphite: dark, fuzzy edges, skips a little. */
    val pencil2B = Brush(
        weight = 0.3f, scatter = 0.75f, sharpness = 0.45f, grain = 0.8f, opacity = 0.71f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.1f, peak = 0.9f, drift = 0.1f, spread = 0.3f),
    )

    /** Medium graphite. */
    val pencilHB = Brush(
        weight = 0.3f, scatter = 0.6f, sharpness = 0.3f, grain = 0.7f, opacity = 0.67f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.1f, peak = 0.9f, drift = 0.15f, spread = 0.2f),
    )

    /** Hard graphite: light and thin. */
    val pencil2H = Brush(
        weight = 0.2f, scatter = 0.6f, sharpness = 0.3f, grain = 0.75f, opacity = 0.47f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.1f, peak = 0.9f, drift = 0.15f, spread = 0.2f),
    )

    /** Coloured pencil: waxy, faint per stroke, builds up with layering. */
    val colorPencil = Brush(
        weight = 0.35f, scatter = 0.55f, sharpness = 0.8f, grain = 0.7f, opacity = 0.29f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 0.95f, peak = 1.1f, drift = 0.15f, spread = 0.2f),
    )

    /** Soft pastel: a wide dusty band, already fat at size 1. */
    val pastel = Brush(
        weight = 0.7f, scatter = 5f, sharpness = 0.91f, grain = 1f, opacity = 0.12f, spacing = 0.028f,
        pressure = Pressure.Bell(ends = 1.09f, peak = 0.93f, drift = 0.4f, spread = 0.05f),
    )

    /** Wax crayon: grainy, never skips, fades a little toward the end of the stroke. */
    val crayon = Brush(
        weight = 0.33f, scatter = 1.9f, sharpness = 0.75f, grain = 2f, opacity = 0.62f, spacing = 0.07f,
        pressure = Pressure.Curve { t -> lerp(1.1f, 0.9f, t) },
    )

    /** Charcoal: broad, dense, rough. */
    val charcoal = Brush(
        weight = 0.35f, scatter = 1.5f, sharpness = 0.68f, grain = 2f, opacity = 0.47f, spacing = 0.03f,
        pressure = Pressure.Bell(ends = 1.1f, peak = 0.95f, drift = 0.15f, spread = 0.4f),
    )

    /** Spray can: a cloud of specks that thins at the ends of the stroke. */
    val spray = Brush(
        tip = Tip.Spray(specks = 40),
        weight = 0.2f, scatter = 6f, opacity = 0.35f, spacing = 0.5f,
        pressure = Pressure.Bell(ends = 0.7f, peak = 1f, drift = 0.2f, spread = 0.35f),
    )

    /** Felt marker: one soft disc stacking up into a translucent band, darker where it lingers. */
    val marker = Brush(
        tip = Tip.Marker,
        weight = 2f, scatter = 0.2f, opacity = 0.013f, spacing = 0.1f,
        pressure = Pressure.Bell(ends = 1.2f, peak = 0.85f, drift = 0.35f, spread = 0.25f),
    )

    /** Thin dry line meant for hatching: never skips, presses harder in the middle. */
    val hatch = Brush(
        weight = 0.2f, scatter = 0.4f, sharpness = 0.3f, grain = 2f, opacity = 0.53f, spacing = 0.15f,
        pressure = Pressure.Bell(ends = 1f, peak = 1.5f, drift = 0.5f, spread = 0.7f),
    )

    /** Every stock brush by name, for knobs like `ps("brush", "pencil2B")`. */
    val all: Map<String, Brush> = linkedMapOf(
        "pen" to pen,
        "rotring" to rotring,
        "pencil2B" to pencil2B,
        "pencilHB" to pencilHB,
        "pencil2H" to pencil2H,
        "colorPencil" to colorPencil,
        "pastel" to pastel,
        "crayon" to crayon,
        "charcoal" to charcoal,
        "spray" to spray,
        "marker" to marker,
        "hatch" to hatch,
    )

    fun of(name: String): Brush = all[name]
        ?: throw IllegalArgumentException("no brush '$name', have: ${all.keys.joinToString(", ")}")
}
