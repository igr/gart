package dev.oblac.gart.io

/**
 * Appends `.ext` unless the name already ends with it (any case).
 * Meant for the `out` knob, so `-Dout=foo` and `-Dout=foo.png` both land on `foo.png`.
 */
fun String.ensureExtension(ext: String = "png"): String {
    val suffix = if (ext.startsWith(".")) ext else ".$ext"
    return if (endsWith(suffix, ignoreCase = true)) this else this + suffix
}
