# Force & Flow

`Force` is a force abstraction: applying _something_ on a `point` to produce a new, moved `point`.

`Flow` is a `Force` with magnitude and direction. It has _no starting point_, i.e., a `flow` may be applied to any
`point` to produce a new, moved `point`.

```kt
val flow = Flow(Radians.of(PI), 1.0)
val point = Point(0.0, 0.0)
val movedPoint = flow(point)
```
