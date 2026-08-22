package org.janelia.lawnmower.view;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.janelia.lawnmower.model.Mower;
import org.janelia.lawnmower.model.Squirrel;

/**
 * Loads the game's sprites from the classpath, scaled to the size they are drawn at.
 *
 * <p>Sprites are optional. A missing or unreadable image leaves its field {@code null} and
 * the view falls back to plain rectangles, so a broken asset never stops the game from
 * starting. Both sprites face north, matching a heading of {@code -PI/2}.
 */
final class Sprites {

	/** Passed as the target height to derive the height from the image's aspect ratio. */
	private static final double KEEP_ASPECT = -1.0;

	/** Channel value above which a pixel of an opaque sprite counts as background. */
	private static final int WHITE = 240;

	/** The mower, squeezed into its {@link Mower#WIDTH} by {@link Mower#LENGTH} body. */
	static final BufferedImage MOWER =
			load("/sprites/lawnmower.png", Mower.WIDTH, Mower.LENGTH);

	/** The squirrel, as wide as its hit box and as tall as its own proportions require. */
	static final BufferedImage SQUIRREL =
			load("/sprites/squirrel.png", Squirrel.SIZE, KEEP_ASPECT);

	private Sprites() {
	}

	/**
	 * Reads a sprite and scales it to its on-screen size.
	 *
	 * @param resource absolute classpath path of the image
	 * @param width target width in lawn units, which are pixels
	 * @param height target height in lawn units, or {@link #KEEP_ASPECT} to scale the
	 *     height in proportion to the width
	 * @return the scaled sprite, or {@code null} if the image is missing or unreadable
	 */
	private static BufferedImage load(final String resource, final double width, final double height) {
		final URL url = Sprites.class.getResource(resource);
		if (url == null) {
			return null;
		}
		try {
			final BufferedImage source = ImageIO.read(url);
			// Sprites saved without an alpha channel carry a white background instead.
			return scale(source.getColorModel().hasAlpha() ? source : keyOutWhite(source),
					width, height);
		} catch (final IOException e) {
			return null;
		}
	}

	/** Scales down once at startup, rather than on every frame. */
	private static BufferedImage scale(final BufferedImage source, final double width, final double height) {
		final int targetWidth = (int) Math.round(width);
		final int targetHeight = (int) Math.round(height == KEEP_ASPECT
				? width * source.getHeight() / source.getWidth()
				: height);
		final Image scaled = source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

		final BufferedImage sprite =
				new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		final var g = sprite.createGraphics();
		g.drawImage(scaled, 0, 0, null);
		g.dispose();
		return sprite;
	}

	/**
	 * Returns a copy of an opaque sprite with its near-white pixels made transparent.
	 *
	 * <p>ponytail: a flat per-channel threshold, so white highlights inside the subject are
	 * knocked out too. Export the sprite with an alpha channel and this never runs.
	 */
	private static BufferedImage keyOutWhite(final BufferedImage source) {
		final BufferedImage keyed = new BufferedImage(
				source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				final int rgb = source.getRGB(x, y);
				keyed.setRGB(x, y, isWhite(rgb) ? rgb & 0xffffff : rgb | 0xff000000);
			}
		}
		return keyed;
	}

	private static boolean isWhite(final int rgb) {
		return ((rgb >> 16) & 0xff) >= WHITE && ((rgb >> 8) & 0xff) >= WHITE && (rgb & 0xff) >= WHITE;
	}
}
