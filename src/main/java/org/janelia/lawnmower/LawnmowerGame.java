package org.janelia.lawnmower;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.lawnmower.control.Controls;
import org.janelia.lawnmower.control.GameController;
import org.janelia.lawnmower.control.KeyboardControls;
import org.janelia.lawnmower.control.WanderingXTouchControls;
import org.janelia.lawnmower.control.XTouchControls;
import org.janelia.lawnmower.model.Game;
import org.janelia.lawnmower.view.GameView;

/**
 * Entry point: opens a window and starts a round.
 *
 * <p>Mow as much of the lawn as you can before the clock runs out. Hold the up arrow to
 * drive, and use the left and right arrows to turn while standing still. Every squirrel
 * you run over costs five percent of your score.
 *
 * <p>Pass a player name on the command line to have it shown on the scoreboard, and
 * {@code --device=} to pick what to play on:
 * {@code mvn compile exec:java -Dexec.args="--device=xtouch my name"}. The devices are
 * {@code keyboard}, {@code xtouch}, and {@code xtouch-hard}, which is the same board with
 * controls that will not stay put.
 */
public final class LawnmowerGame {

	private static final int LAWN_WIDTH = 1600;
	private static final int LAWN_HEIGHT = 800;

	/** Stands in for a name the player did not give. */
	private static final String ANONYMOUS = "Mowy McMowface";
	/** Room for a real name, but not enough to push the rest of the status bar off screen. */
	private static final int NAME_LIMIT = 20;

	/** The argument that picks the input device; everything else is the player's name. */
	private static final String DEVICE_FLAG = "--device=";
	private static final String KEYBOARD = "keyboard";
	private static final String XTOUCH = "xtouch";
	/** The X-Touch Mini with controls that move as you play; see {@link WanderingXTouchControls}. */
	private static final String XTOUCH_HARD = "xtouch-hard";
	/** What {@link #DEVICE_FLAG} accepts. */
	private static final List<String> DEVICES = List.of(KEYBOARD, XTOUCH, XTOUCH_HARD);

	private LawnmowerGame() {
	}

	public static void main(final String[] args) {
		final String device = deviceName(args);
		if (!DEVICES.contains(device)) {
			System.err.println("unknown device \"" + device + "\"; pick one of "
					+ String.join(", ", DEVICES));
			System.exit(2);
		}
		final String player = playerName(args);
		SwingUtilities.invokeLater(() -> startRound(device, player));
	}

	/**
	 * Reads the input device off the command line.
	 *
	 * @param args the command-line arguments, possibly empty
	 * @return the value of the last {@link #DEVICE_FLAG} argument, lower-cased, or
	 *     {@value #KEYBOARD} if there is none; not checked against the known devices
	 */
	static String deviceName(final String[] args) {
		String device = KEYBOARD;
		for (final String arg : args) {
			if (arg.startsWith(DEVICE_FLAG)) {
				device = arg.substring(DEVICE_FLAG.length()).trim().toLowerCase();
			}
		}
		return device;
	}

	/**
	 * Reads the player's name off the command line.
	 *
	 * <p>The arguments are joined with spaces, because {@code -Dexec.args} hands a name like
	 * "Ada Lovelace" over as two of them.
	 *
	 * @param args the command-line arguments, possibly empty
	 * @return the name to put on the scoreboard, never blank and never longer than
	 *     {@link #NAME_LIMIT} characters
	 */
	static String playerName(final String[] args) {
		final String given = Arrays.stream(args)
				.filter(arg -> !arg.startsWith(DEVICE_FLAG))
				.collect(Collectors.joining(" "))
				.trim();
		if (given.isEmpty()) {
			return ANONYMOUS;
		}
		return given.length() <= NAME_LIMIT ? given : given.substring(0, NAME_LIMIT).trim();
	}

	/**
	 * Opens the chosen input device, falling back to the keyboard if it cannot be had.
	 *
	 * <p>The keyboard is always listening, so a board that fails to open, or one control
	 * scheme that turns out to be awkward, never leaves the round unplayable.
	 *
	 * @param device one of {@link #DEVICES}
	 * @param view the component the keyboard listens on
	 * @return the controls to poll
	 */
	private static Controls openControls(final String device, final GameView view) {
		final KeyboardControls keyboard = new KeyboardControls();
		view.addKeyListener(keyboard);
		if (device.equals(KEYBOARD)) {
			return keyboard;
		}
		final boolean hard = device.equals(XTOUCH_HARD);
		try {
			final XTouchControls controls = hard
					? WanderingXTouchControls.open()
					: XTouchControls.open();
			Runtime.getRuntime().addShutdownHook(new Thread(controls::close));
			System.out.println(hard
					? "playing on the X-Touch Mini, the hard way: the lit button in the bottom "
							+ "row drives, the lit one in the top row sits under the encoder that "
							+ "steers, and both move as you play"
					: "playing on the X-Touch Mini: leftmost encoder steers, "
							+ "the button under it drives");
			return controls;
		} catch (final MidiUnavailableException e) {
			System.err.println("could not open the X-Touch Mini, using the keyboard: "
					+ e.getMessage());
			return keyboard;
		}
	}

	/**
	 * Picks the screen to play on: the biggest one there is, so the game does not open on a
	 * laptop's own display when a roomier screen is plugged in.
	 *
	 * @return the bounds of the chosen screen, in the virtual desktop's coordinates
	 */
	private static Rectangle preferredScreen() {
		final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		return largest(
				environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds(),
				Arrays.stream(environment.getScreenDevices())
						.map(screen -> screen.getDefaultConfiguration().getBounds())
						.toArray(Rectangle[]::new));
	}

	/**
	 * Picks the screen with the most room on it.
	 *
	 * @param primary the screen to fall back on, which also wins ties
	 * @param screens the bounds of every screen, possibly including {@code primary}
	 * @return the bounds of the largest screen by area, or {@code primary} if the screens
	 *     are all smaller or there are none
	 */
	static Rectangle largest(final Rectangle primary, final Rectangle... screens) {
		Rectangle chosen = primary;
		for (final Rectangle screen : screens) {
			if (area(screen) > area(chosen)) {
				chosen = screen;
			}
		}
		return chosen;
	}

	/** Screen area in pixels, as a long, because two 4K screens overflow an int. */
	private static long area(final Rectangle screen) {
		return (long) screen.width * screen.height;
	}

	/**
	 * Centres a window on a screen, keeping its title bar reachable.
	 *
	 * @param screen bounds of the screen, in the virtual desktop's coordinates
	 * @param window size of the window, in pixels
	 * @return where to put the window's top left corner; never above or left of the
	 *     screen's own corner, even if the window does not fit
	 */
	static Point topLeftOn(final Rectangle screen, final Dimension window) {
		return new Point(
				screen.x + Math.max(0, (screen.width - window.width) / 2),
				screen.y + Math.max(0, (screen.height - window.height) / 2));
	}

	private static void startRound(final String device, final String player) {
		final Game game = new Game(LAWN_WIDTH, LAWN_HEIGHT);
		final GameView view = new GameView(game, player);
		final Controls controls = openControls(device, view);

		final JFrame frame = new JFrame("Lawnmower");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(view);
		frame.setResizable(false);
		frame.pack();
		frame.setLocation(topLeftOn(preferredScreen(), frame.getSize()));
		frame.setVisible(true);
		view.requestFocusInWindow();

		new GameController(game, view, controls).start();
	}
}
