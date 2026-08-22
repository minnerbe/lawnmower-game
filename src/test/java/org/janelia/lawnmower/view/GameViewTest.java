package org.janelia.lawnmower.view;

import java.awt.image.BufferedImage;

import org.janelia.lawnmower.model.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
		assertDoesNotThrow(() -> render(new Game(800, 600)));
	}

	@Test
	void paintsWhileMowingAndAfterTheRoundEnds() {
		final Game game = new Game(1200, 60);
		for (int i = 0; i < 180; i++) {
			game.update(1.0 / 60, true, 0);
		}
		final BufferedImage mowing = render(game);

		while (game.state() == Game.State.RUNNING) {
			game.update(0.05, false, 0);
		}
		final BufferedImage finished = render(game);

		final int middle = mowing.getWidth() / 2;
		assertNotEquals(mowing.getRGB(middle, 5), mowing.getRGB(middle, 30),
				"the mowed swath should stand out against the lawn");
		assertNotEquals(mowing.getRGB(middle, 30), finished.getRGB(middle, 30),
				"the scoreboard should darken the lawn once the round is over");
	}
}
