package dev.oblac.gart.whfast

/**
 *   | File                          | Description                                            |
 *   |-------------------------------|--------------------------------------------------------|
 *   | whfast/Body2D.kt              | Celestial body with position, velocity, mass           |
 *   | whfast/OrbitalElements2D.kt   | Keplerian orbital elements (a, e, ω, M, μ)             |
 *   | whfast/KeplerSolver.kt        | Newton-Raphson solver for Kepler's equation            |
 *   | whfast/CoordinateTransform.kt | Cartesian ↔ orbital element conversions, Jacobi coords |
 *   | whfast/WHIntegrator2D.kt      | Core WHFAST integrator with DKD/KDK schemes            |
 *   | whfast/NBodySystem2D.kt       | High-level simulation manager                          |
 *
 *   Key Features
 *
 *   - Symplectic integration - preserves phase space volume, excellent long-term energy conservation
 *   - Two schemes: Drift-Kick-Drift (DKD) and Kick-Drift-Kick (KDK)
 *   - Analytical Kepler drift - solves 2-body problem exactly using orbital elements
 *   - Handles elliptic and hyperbolic orbits
 *   - Conservation tracking - energy, angular momentum, center of mass
 *
 */
