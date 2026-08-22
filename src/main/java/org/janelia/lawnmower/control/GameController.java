package org.janelia.lawnmower.control;

import javax.swing.Timer;

import org.janelia.lawnmower.model.Game;
import org.janelia.lawnmower.view.GameView;

/**
 * Drives the round: polls the controls, advances the model and repaints the view.
 *
 * <p>The loop is a Swing {@link Timer}, so updating and painting both happen on the event
 * dispatch thread and cannot race each other.
 */
public class GameController {

	/** Target frame interval, in milliseconds; roughly 60 frames per second. */
	private static final int FRAME_MILLIS = 16;
	/** Largest time step handed to the model, in seconds. */
	private static final double MAX_STEP = 0.05;

	private final Game game;
	private final GameView view;
	private final Controls controls;
	private final Timer timer;

	private long lastTickNanos;

	/**
	 * Wires a round together.
	 *
	 * @param game the model to advance
	 * @param view the view to repaint after each step
	 * @param controls the input device to poll once per frame
	 */
	public GameController(final Game game, final GameView view, final Controls controls) {
		this.game = game;
		this.view = view;
		this.controls = controls;
		this.timer = new Timer(FRAME_MILLIS, event -> tick());
	}

	/** Starts the clock. Call from the event dispatch thread. */
	public void start() {
		lastTickNanos = System.nanoTime();
		timer.start();
	}

	private void tick() {
		final long now = System.nanoTime();
		// Cap the step: after a stall, a huge dt would jump the mower straight over a squirrel.
		final double dt = Math.min((now - lastTickNanos) / 1e9, MAX_STEP);
		lastTickNanos = now;

		game.update(dt, controls.accelerating(), controls.turn());
		view.repaint();

		if (game.state() == Game.State.OVER) {
			timer.stop();
		}
	}
}
