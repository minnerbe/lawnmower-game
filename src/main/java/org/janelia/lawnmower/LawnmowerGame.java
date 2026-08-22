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
 */
public final class LawnmowerGame {

	private static final int LAWN_WIDTH = 800;
	private static final int LAWN_HEIGHT = 600;

	private LawnmowerGame() {
	}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(LawnmowerGame::startRound);
	}

	private static void startRound() {
		final Game game = new Game(LAWN_WIDTH, LAWN_HEIGHT);
		final GameView view = new GameView(game);
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
