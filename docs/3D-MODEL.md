# The 3D body map

The Body tab draws a rotatable human figure with eleven tappable muscle
groups, lit by how much volume each one has taken. There is no 3D engine in
this app and no model file: the figure is generated from primitives at startup
and drawn by about 300 lines of software renderer.

This document is for whoever has to change it — most likely for ticket A-4, or
when a marker asks how it works.

## Why no engine

Filament (via SceneView) is the normal answer on Android, and it would have
brought several megabytes of native libraries plus a `.glb` asset pipeline, for
a figure that is thirty boxes and capsules in a trench coat. The whole thing is
parametric, so the maths is smaller than the dependency, builds offline, and
has no asset to keep in sync with the website's version of the same figure.

It is also a better answer for a MAD project: everything on screen is code you
can read.

## The three files

| File | Job |
| --- | --- |
| `anatomy/Geometry.kt` | `Vec3`, `Mesh`, and the primitive builders: `box`, `sphere`, `cylinder`, `capsule`, `icosahedron`. |
| `anatomy/AnatomyRig.kt` | The body: which primitive goes where, which parts belong to which muscle group, and the camera angle for each group. |
| `anatomy/Renderer.kt` | Turning that into pixels, and answering "which muscle is under this finger". |

`feature/body/BodyScreen.kt` is the Compose screen on top; `OrbitCamera.kt`
holds the camera state and the gesture handling.

## The rig

The figure is about 1.9 units tall with the feet at y = 0, ported part for part
from the website's `anatomy-config.ts`, so both show the same body.

Two kinds of part:

- **Chassis** — head, neck, torso, pelvis, arms, hands, shins, feet. Scenery.
  Never responds to touch, drawn in a neutral colour.
- **Zones** — the eleven muscle groups from `ExerciseCatalog`. Each is one or
  more primitives, and each can be lit, tinted and tapped.

A part flagged `mirror = true` is built twice, with x negated and the y/z
rotations negated with it. That is why the rig describes half a body:
symmetrical by construction, so a tweak to the left pec cannot drift from the
right one.

Mirroring is applied to the **transform**, never by scaling the mesh by -1.
A negative scale flips triangle winding, and winding is what the renderer uses
to decide which faces to draw (below).

Everything is baked into world space once, at startup, into three flat arrays:
vertex positions, triangle indices, and the zone index of each triangle (-1 for
chassis). Nothing is rebuilt per frame — the figure never animates, the camera
moves around it.

## The winding rule

**Every triangle is wound counter-clockwise as seen from outside the solid**,
so `cross(b - a, c - a)` points out of the surface.

This is the one rule that will bite you. The renderer drops triangles whose
normal faces away from the camera, so a triangle wound the wrong way becomes a
hole in the body, visible only from one angle, usually not the one you are
looking from.

`GeometryTest` checks it for every builder: each primitive is a convex solid
centred on the origin, so every face normal must point away from the centre.
If you add a primitive, add it to that test.

## The frame

`AnatomyRenderer.computeFrame` then `render`, every frame:

1. **Transform.** Build a look-at basis from the camera's orbit position
   (`yaw`, `pitch`, `distance` around a `target`) and put every vertex into
   view space, where the camera is at the origin looking down -Z.
2. **Project.** Pinhole camera: `screenX = centre + focal * x / depth`, with
   `focal` from the vertical field of view. Y is flipped, because screen Y
   grows downward.
3. **Cull.** Compute each triangle's view-space normal. Keep it only if
   `normal.z > 0` — facing the camera. That drops roughly half the figure.
   Triangles with a vertex behind the lens are dropped whole; proper clipping
   is a lot of code for a camera that never gets that close.
4. **Shade.** One directional light fixed *in view space*, so the figure never
   spins into darkness. Flat shading — one colour per triangle, which is the
   look. Then two additions: the muscle group's accent mixed in proportion to
   its glow, and a rim term `(1 - normal.z)³` that brightens edge-on faces,
   which is what gives the figure a neon outline without drawing one.
5. **Sort and draw.** Sort back to front by average depth and fill each
   triangle with `Canvas.drawPath`. This is the painter's algorithm: it gets
   the order wrong for interpenetrating surfaces, which on separated convex
   limbs never shows, and it costs one sort instead of a depth buffer that
   would have to be allocated and cleared every frame.

Everything the loop touches is allocated once in the constructor, including the
`Path` and `Paint`. A renderer that allocates per frame stutters when the
garbage collector runs, and this one redraws at 60fps while a finger drags it.

The depth sort is a hand-rolled quicksort over an index array keyed on a
`FloatArray`, because the library sorts want boxed keys — 1,800 `Integer`
objects per frame is 1,800 objects for the collector to find again sixteen
milliseconds later.

## Picking

Tapping walks the visible triangles **front to back** and takes the first one
whose projected triangle contains the point.

The chassis blocks a pick even though it is not selectable. That matters more
than it sounds: the back slab sits inside the torso, and its front face
survives culling even when you are looking at the chest. Without the chassis
blocking, a tap on the sternum would select the lats behind it.

A tap that lands on chassis gets a second chance at the nearest zone within
26px — the calves on a phone-sized figure are a target about four millimetres
across, and a fingertip is wider than that. The fallback only considers zones
at least as near as the chassis that blocked the tap, so generosity can never
reach through the body.

`RendererTest` pins all of this down on the JVM: `computeFrame` stops before
the first drawing call, so the chest being tappable from the front and not from
behind is a unit test rather than something you check by hand on a phone.

## Glow, and why it is data

`render` takes a `FloatArray` of per-zone glow, and `BodyScreen` fills it each
frame:

- idle: `0.07`
- trained: `+ 0.6 × (volume / the hardest-worked group's volume)`, over a
  28-day window from `AnalyticsRepository.muscleVolume`
- selected: `0.95` plus a pulse

So brightness means volume. A group with nothing logged sits dark, and the only
way to light it is to train it. Do not add a decorative floor to make the
figure look better on a fresh install — an empty body map is the honest picture
of an empty history, and it is a better advert for logging a session than a
glowing one.

## Performance notes

- ~1,800 triangles, ~900 after culling. That is the budget. Doubling the
  segment counts in `AnatomyRig.kt` doubles the draw calls, and `drawPath` per
  triangle is the floor on cost.
- The selection pulse is only subscribed to while something is selected, so an
  idle screen is a still screen rather than a 60fps battery drain. Keep it that
  way: reading an animated value unconditionally in a draw block invalidates
  every frame forever.
- Triangles smaller than a third of a pixel are skipped — all cost, no pixels.

## If you want to extend it

- **A new muscle group** — add it to the catalog on both the website and here,
  then add a `ZoneDef` in `AnatomyRig.kt`. `CatalogAndRigTest` fails until the
  zone exists, which is the point.
- **A better figure** — more primitives per zone is fine; keep the winding rule
  and watch the triangle budget.
- **Front/back toggle** — the camera poses are already in the rig, one per
  group; `OrbitCamera.flyTo` takes any of them.
- **Textures, shadows, a depth buffer** — out of scope. If the figure needs
  that much more, the honest answer is to bring in Filament and a real model,
  and that is a conversation, not a ticket.
