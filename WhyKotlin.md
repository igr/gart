# Why Kotlin?

Since I'm getting this question frequently, here's my reasoning behind the choices.

## Just a Canvas: Skiko

My primary goal was to start with a **bare minimum canvas abstraction** — essentially a blank slate I could draw on. I
wanted only the fundamentals: drawing and filling lines, circles, paths, handling colors and pixels. Nothing beyond the
basics.

The plan was to build everything else from the ground up. I wanted to learn algorithms, experiment with mathematical
formulas, make mistakes, and simply have fun coding. Rather than being constrained by existing frameworks, I was
determined to understand how things work under the hood and grasp the mathematics behind the algorithms.

This led me to [Skiko](https://github.com/JetBrains/skiko), a library that provides a canvas abstraction
over [Skia](https://skia.org) — one of the industry's premier graphics libraries, powering Chrome, Android, and many
other applications. Skiko is essentially a thin wrapper around Skia, and importantly, it's actively maintained by
JetBrains.

## Rich Ecosystem

Kotlin's access to the entire JVM ecosystem was another significant advantage. There's an abundance of libraries
available, making it easy to find solutions when you hit roadblocks. Need GIF creation? There's a library for that.
FFmpeg wrapper? Covered.

From the 3rd party libraries, I use `box-2d` for physics simulation.

## Functionally Pragmatic

I also appreciate Kotlin as what I call a "Minimally Viable Functional" programming language. Having a functional
mindset, Kotlin feels natural to work with. Unlike stricter functional languages like Haskell, Kotlin offers the
pragmatism I need for a passion project with limited time constraints. The code remains type-safe while allowing me to
be productive and expressive.
