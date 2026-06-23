package dev.oblac.gart.io

fun detectHeadlessFlags(args: Array<String>): Boolean =
    "--render" in args || System.getProperty("gart.headless") != null || System.getProperty("headless") != null
