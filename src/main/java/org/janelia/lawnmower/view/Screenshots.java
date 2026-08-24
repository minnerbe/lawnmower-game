package org.janelia.lawnmower.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

/**
 * Keeps a picture of a finished round.
 *
 * <p>The picture is painted from the live view rather than grabbed off the screen, so it
 * comes out at the lawn's own size and is not spoiled by an overlapping window.
 */
public final class Screenshots {

	/** Sortable, and legible without a lookup: 20260822-143012. */
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private Screenshots() {
	}

	/**
	 * Writes a picture to a PNG named after the local time it was taken.
	 *
	 * @param image the picture to write, typically from {@link GameView#snapshot()}
	 * @param directory the directory to write to, created if it does not exist yet
	 * @return the file written
	 * @throws IOException if the directory or the file cannot be written
	 */
	public static Path save(final BufferedImage image, final Path directory) throws IOException {
		Files.createDirectories(directory);
		final Path file = directory.resolve(
				"lawnmower-" + STAMP.format(LocalDateTime.now()) + ".png");
		ImageIO.write(image, "png", file.toFile());
		return file;
	}
}
