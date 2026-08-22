package org.janelia.lawnmower.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LawnTest {

	private static final double LAWN_W = 1600.0;
	private static final double LAWN_H = 1200.0;

	private static Area union(final Rectangle2D... rectangles) {
		final Area area = new Area();
		for (final Rectangle2D rectangle : rectangles) {
			area.add(new Area(rectangle));
		}
		return area;
	}

	@Test
	void measuresARectangle() {
		assertEquals(200.0, Lawn.areaOf(new Rectangle2D.Double(5, 7, 10, 20)), 1e-9);
	}

	@Test
	void rotationDoesNotChangeTheMeasuredArea() {
		final Rectangle2D rectangle = new Rectangle2D.Double(5, 7, 10, 20);
		final var rotated = AffineTransform.getRotateInstance(Math.toRadians(30))
				.createTransformedShape(rectangle);
		assertEquals(200.0, Lawn.areaOf(rotated), 1e-6);
	}

	@Test
	void overlappingSwathsAreCountedOnce() {
		final Rectangle2D swath = new Rectangle2D.Double(0, 0, 100, 24);
		assertEquals(2400.0, Lawn.areaOf(union(swath, swath)), 1e-6);
	}

	@Test
	void disjointSwathsAddUp() {
		assertEquals(4800.0, Lawn.areaOf(union(
				new Rectangle2D.Double(0, 0, 100, 24),
				new Rectangle2D.Double(0, 100, 100, 24))), 1e-6);
	}

	@Test
	void aRunOpensASwathAndStoppingCommitsIt() {
		final Lawn lawn = new Lawn(LAWN_W, LAWN_H);
		final Mower mower = new Mower(200, 600, 0);

		mower.update(0.1, true, 0, LAWN_W, LAWN_H);
		lawn.follow(mower);
		assertNotNull(lawn.currentSwath());
		assertEquals(0, lawn.mowed().size());

		for (int i = 0; i < 100; i++) {
			mower.update(1.0 / 60, false, 0, LAWN_W, LAWN_H);
			lawn.follow(mower);
		}
		assertNull(lawn.currentSwath());
		assertEquals(1, lawn.mowed().size());
		assertEquals(Lawn.areaOf(lawn.mowed().iterator().next()) / (LAWN_W * LAWN_H),
				lawn.mowedFraction(), 1e-9);
	}

	@Test
	void mowingTheSameStripTwiceDoesNotDoubleCount() {
		final Lawn lawn = new Lawn(LAWN_W, LAWN_H);
		for (int run = 0; run < 2; run++) {
			final Mower mower = new Mower(200, 600, 0);
			for (int i = 0; i < 120; i++) {
				mower.update(1.0 / 60, i < 60, 0, LAWN_W, LAWN_H);
				lawn.follow(mower);
			}
		}
		assertEquals(2, lawn.mowed().size());
		final double single = Lawn.areaOf(lawn.mowed().iterator().next());
		assertEquals(single / (LAWN_W * LAWN_H), lawn.mowedFraction(), 1e-6);
	}

	@Test
	void theSwathInProgressCountsTowardsTheLiveFraction() {
		final Lawn lawn = new Lawn(LAWN_W, LAWN_H);
		final Mower mower = new Mower(200, 600, 0);
		for (int i = 0; i < 60; i++) {
			mower.update(1.0 / 60, true, 0, LAWN_W, LAWN_H);
			lawn.follow(mower);
		}
		assertEquals(0.0, lawn.mowedFraction());
		lawn.refreshMowedFraction();
		assertEquals(Lawn.areaOf(lawn.currentSwath()) / (LAWN_W * LAWN_H),
				lawn.mowedFraction(), 1e-6);
	}
}
