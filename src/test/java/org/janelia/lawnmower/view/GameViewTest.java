package org.janelia.lawnmower.view;

import java.awt.image.BufferedImage;

import org.janelia.lawnmower.model.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Paints the view off-screen. This catches the painting mistakes that only show up at
 * runtime, such as touching the swath in progress while the mower stands still.
 */
class GameViewTest {

	private static BufferedImage render(final Game game) {
		final GameView view = new GameView(game);
		view.setSize(view.getPreferredSize());
		final BufferedImage image = new BufferedImage(
				view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
		final var g = image.createGraphics();
		view.paint(g);
		g.dispose();
		return image;
	}

	@Test
	void paintsAnUntouchedLawn() {
		assertDoesNotThrow(() -> render(new Game(1600, 1200)));
	}

	/** Runs the opening countdown out, so the lawn is not behind the "get ready" overlay. */
	private static Game started(final Game game) {
		while (game.state() == Game.State.COUNTDOWN) {
			game.update(1.0 / 60, false, 0);
		}
		return game;
	}

	@Test
	void anArrivingSquirrelTintsTheLawn() {
		final Game game = started(new Game(2400, 120));
		final BufferedImage calm = render(game);

		for (int i = 0; i < 600 && game.squirrels().isEmpty(); i++) {
			game.update(1.0 / 60, true, 0);
		}
		assertFalse(game.squirrels().isEmpty(), "a squirrel should have appeared by now");
		final BufferedImage flash = render(game);

		// A strip of lawn the mower has not touched in either frame, so only the tint differs.
		final int middle = flash.getWidth() / 2;
		assertNotEquals(calm.getRGB(middle, 5), flash.getRGB(middle, 5),
				"the lawn should be tinted for a moment after a squirrel appears");
	}

	@Test
	void paintsWhileMowingAndAfterTheRoundEnds() {
		final Game game = started(new Game(2400, 120));
		for (int i = 0; i < 180; i++) {
			game.update(1.0 / 60, true, 0);
		}
		final BufferedImage mowing = render(game);

		while (game.state() == Game.State.RUNNING) {
			game.update(0.05, false, 0);
		}
		final BufferedImage finished = render(game);

		final int middle = mowing.getWidth() / 2;
		assertNotEquals(mowing.getRGB(middle, 10), mowing.getRGB(middle, 60),
				"the mowed swath should stand out against the lawn");
		assertNotEquals(mowing.getRGB(middle, 60), finished.getRGB(middle, 60),
				"the scoreboard should darken the lawn once the round is over");
	}
}
