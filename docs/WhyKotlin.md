# Why Kotlin?

Since I'm getting this question frequently, here's my reasoning behind the choices.

## Just a Canvas: Skiko

My primary goal was to start with a **bare minimum canvas abstraction**: essentially a blank slate I could draw on. I
wanted only the fundamentals: drawing and filling lines, circles, paths, handling colors and pixels. Nothing beyond the
basics.

The plan was to build everything else from the ground up. I wanted to learn algorithms, experiment with mathematical
formulas, make mistakes, and simply have fun coding. Rather than being constrained by existing frameworks, I was
determined to understand how things work under the hood and grasp the mathematics behind the algorithms.

This led me to [Skiko](https://github.com/JetBrains/skiko), a library that provides a canvas abstraction
over [Skia](https://skia.org); one of the industry's premier graphics libraries, powering Chrome, Android, and many
other applications. Skiko is essentially a thin wrapper around Skia, and importantly, it's actively maintained by
JetBrains.

## Rich Ecosystem

Kotlin's access to the entire JVM ecosystem was another significant advantage. There's an abundance of libraries
available, making it easy to find solutions when you hit roadblocks. GIF creation and FFmpeg wrapper are two such
examples used in this project.

From the 3rd party libraries, I only use `box-2d` for physics simulation.

## Functionally Pragmatic

I also appreciate Kotlin as what I call a "Minimally Viable Functional" programming language. Having a functional
mindset, Kotlin feels natural to work with. Unlike stricter functional languages like Haskell, Kotlin offers the
_pragmatism_ I need for a passion project with limited time constraints. The code remains type-safe while allowing me to
be productive and expressive. Granted, I am not writing in a purely functional style, and my graphics code is quite
imperative (_and dirty!_), but I can still leverage functional programming concepts when it suits the problem at hand.

Kotlin has "extended functions" that allow me to add functionality to existing Skiko classes without modifying their
source code.

In short: in Kotlin you may work just with functions, and that is all what I need.

Finally, I was able to create a hot-reloadable application, so I can see changes in real-time without restarting the
app.

## Alternatives

From the alternatives, I considered only Rust, but having JetBrains updating Skiko regularly was a decisive factor (as
they are using it for their own products). I also consider using C++ with Skia directly, but that was too much work for
a personal project.
