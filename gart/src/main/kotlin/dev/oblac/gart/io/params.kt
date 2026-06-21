package dev.oblac.gart.io

// knobs (overridable via -Dkey=value for sweeping; defaults are the chosen values)
fun pi(k: String, d: Int) = System.getProperty(k)?.toInt() ?: d
fun pf(k: String, d: Float) = System.getProperty(k)?.toFloat() ?: d
fun ps(k: String, d: String) = System.getProperty(k) ?: d
