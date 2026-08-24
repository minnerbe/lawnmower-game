package org.janelia.lawnmower.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.janelia.lawnmower.model.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotsTest {

	@Test
	void writesATimestampedPngOfTheView(@TempDir final Path parent) throws IOException {
		final GameView view = new GameView(new Game(800, 600));
		view.setSize(view.getPreferredSize());

		// A directory that does not exist yet: the first round of the day has to create it.
		final Path file = Screenshots.save(view.snapshot(), parent.resolve("screenshots"));

		assertTrue(file.getFileName().toString().matches("lawnmower-\\d{8}-\\d{6}\\.png"),
				"unexpected file name: " + file.getFileName());
		final BufferedImage image = ImageIO.read(file.toFile());
		assertEquals(view.getWidth(), image.getWidth());
		assertEquals(view.getHeight(), image.getHeight());
	}
}
