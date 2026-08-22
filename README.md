# lawnmower-game

A simple game based on Janelia's lawnmower robot.

Mow as much of the lawn as you can before the clock runs out, and try not to run over the
squirrels.

## Playing

```bash
mvn compile exec:java
```

| Key | Action |
| --- | --- |
| Up arrow | drive forward |
| Left / right arrow | turn, only while standing still |

The mower drives in straight lines. It speeds up while you hold the up arrow, coasts to a
stop when you let go, and stops dead if it runs into the edge of the lawn. It can only turn
while standing still, and it will not pull away in the middle of a turn, so every run cuts
a straight swath.

A round lasts 90 seconds. For every 10% of the lawn you mow, a squirrel settles down in
your path a safe distance ahead; it wanders off after six seconds. Each squirrel you mow
over costs five percentage points of your final score.

## Building

Java 21 and Maven. The game itself has no dependencies beyond the JDK; JUnit is used for
the tests only.

```bash
mvn test
```

## Layout

The code follows a model-view-controller split:

- `model` — the rules: mower physics, mowed swaths and coverage, the clock, squirrels and
  scoring. Knows nothing about Swing.
- `view` — `GameView`, which only draws the model.
- `control` — the game loop, and `Controls`, the interface that hides the input device.
  `KeyboardControls` implements it today; a MIDI controller with a button and a rotary knob
  can replace it without touching the model or the view.
