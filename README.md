# lawnmower-game

A simple game based on Janelia's lawnmower robot.

Mow as much of the lawn as you can before the clock runs out, and try not to run over the
squirrels.

## Playing

```bash
mvn compile exec:java
mvn compile exec:java -Dexec.args="Ada Lovelace"                   # name on the scoreboard
mvn compile exec:java -Dexec.args="--device=xtouch Ada Lovelace"   # on an X-Touch Mini
```

Maven reads the arguments from `-Dexec.args`; a bare `mvn compile exec:java myname` will
not work, because Maven takes `myname` for a goal. `--device=` picks the input device,
either `keyboard` (the default) or `xtouch`. Everything else is the player's name; without
one the scoreboard just says `player`.

The window opens on an external screen if one is attached, whatever screen the shell that
started the game is on; with a single screen it opens there.

| Key | Action |
| --- | --- |
| Up arrow | drive forward |
| Left / right arrow | turn, only while standing still |

The mower drives in straight lines. It speeds up while you hold the up arrow, coasts to a
stop when you let go, and stops dead if it runs into the edge of the lawn. It can only turn
while standing still, and it will not pull away in the middle of a turn, so every run cuts
a straight swath.

A round opens with a five-second countdown and then lasts 90 seconds. For every 10% of the
lawn you mow, a squirrel settles down in your path a safe distance ahead and the lawn
flashes red; it wanders off after six seconds. Each squirrel you mow over costs five
percentage points of your final score.

When the clock runs out, the game writes a picture of the finished lawn to
`screenshots/lawnmower-<date>-<time>.png`, with a red ring around every spot where you ran
a squirrel over.

## Playing on an X-Touch Mini

Start the game with `--device=xtouch` to play on a Behringer X-Touch Mini. The keyboard
keeps working either way, and the game falls back to it if the board cannot be opened.
The board must be in **Mackie
Control mode** (hold the MC button while powering it on, or set the mode in the X-Touch
Editor); the game also asks for MC mode when it opens the port.

| Control | Action |
| --- | --- |
| Leftmost encoder | turn, one detent at a time, only while standing still |
| Button under it (top row, leftmost) | drive forward |
| That button's LED | lit while the mower is moving |
| Leftmost LED ring | speed, as a fan |
| Bottom row LEDs | mowed percentage, one LED per 12.5% |
| Top row LEDs, 2 to 8 | blink while a squirrel is on the lawn |

To see what the board sends before mapping anything to it, run the device class on its
own. It lights every LED in turn and then prints each message as `status data1 data2`:

```bash
mvn compile exec:java -Dexec.mainClass=org.janelia.lawnmower.control.XTouchControls
```

Unmapped and available: seven more encoders and their LED rings, the eight encoder push
switches (no LEDs), fifteen more buttons, the two layer buttons, and the fader.

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
