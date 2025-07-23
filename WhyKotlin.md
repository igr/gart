# Why Kotlin?

Since I am getting this question a lot, here is my answer.

## Just a Canvas: Skiko

My first intention is to have bare minimum canvas abstraction - one that you can simply draw on. I wanted only basics:
to draw lines, circles, paths, colors, pixels, etc. My goal is to build everything else on top of it. I want to learn
about
algorithms, have fun with math formulas, experiment, make mistakes and just play with the code.

I didn't want to be bounded with any existing framework. Instead, I wanted to
learn how things work under the hood, and understand the math behind the algorithms. Finally, I wanted to have fun.

It happens that there is a [Skiko](https://github.com/JetBrains/skiko) library that provides a canvas abstraction on top
of [Skia](https://skia.org) - one of the best graphics libraries out there, used by Chrome, Android, etc. Skiko is a
very thin layer on top of Skia. It is actively maintained by JetBrains, which was important for my decision.

## Big Ecosystem

Another plus for Kotlin is a big JVM ecosystem. There are a lot of libraries available, and it is easy to find what you
need, if you get stuck. For example, there is a gif creation library, or a ffmpeg wrapper, etc.

From the 3rd party libraries, I use `box-2d` for physics simulation, `openrndr` for only a few math functions and
geometry.

## Functional Enough

I also consider Kotlin as a Minimal Valuable Functional programming language (the coinage is mine:) I used to think in
functional way, so Kotlin naturally fits my way of thinking. It is also not strict (lazy) as e.g. Haskell, and that is
pragmatic in this case as I don't have much time for this passion project. The code is type-safe (enough) and I can be
quite fluent with it.  
