package org.janelia.lawnmower;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.lawnmower.control.GameController;
import org.janelia.lawnmower.control.KeyboardControls;
import org.janelia.lawnmower.model.Game;
import org.janelia.lawnmower.view.GameView;

/**
 * Entry point: opens a window and starts a round.
 *
 * <p>Mow as much of the lawn as you can before the clock runs out. Hold the up arrow to
 * drive, and use the left and right arrows to turn while standing still. Every squirrel
 * you run over costs five percent of your score.
 *
 * <p>Pass a player name on the command line to have it shown on the scoreboard:
 * {@code mvn compile exec:java -Dexec.args="my name"}.
 */
public final class LawnmowerGame {

	private static final int LAWN_WIDTH = 1600;
	private static final int LAWN_HEIGHT = 800;

	/** Stands in for a name the player did not give. */
	private static final String ANONYMOUS = "player";
	/** Room for a real name, but not enough to push the rest of the status bar off screen. */
	private static final int NAME_LIMIT = 20;

	private LawnmowerGame() {
	}

	public static void main(final String[] args) {
		final String player = playerName(args);
		SwingUtilities.invokeLater(() -> startRound(player));
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
		final String given = String.join(" ", args).trim();
		if (given.isEmpty()) {
			return ANONYMOUS;
		}
		return given.length() <= NAME_LIMIT ? given : given.substring(0, NAME_LIMIT).trim();
	}

	private static void startRound(final String player) {
		final Game game = new Game(LAWN_WIDTH, LAWN_HEIGHT);
		final GameView view = new GameView(game, player);
		final KeyboardControls controls = new KeyboardControls();
		view.addKeyListener(controls);

		final JFrame frame = new JFrame("Lawnmower");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(view);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		view.requestFocusInWindow();

		new GameController(game, view, controls).start();
	}
}
