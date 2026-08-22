package org.janelia.lawnmower.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MowerTest {

	private static final double LAWN_W = 1600.0;
	private static final double LAWN_H = 1200.0;
	private static final double DT = 1.0 / 60;

	private static void drive(final Mower mower, final double seconds,
			final boolean accelerate, final double turn) {
		for (int i = 0; i < seconds / DT; i++) {
			mower.update(DT, accelerate, turn, LAWN_W, LAWN_H);
		}
	}

	@Test
	void acceleratesUpToMaxSpeedAndNoFurther() {
		final Mower mower = new Mower(60, 600, 0);
		drive(mower, 3, true, 0);
		assertEquals(Mower.MAX_SPEED, mower.speed(), 1e-9);
	}

	@Test
	void deceleratesToExactlyZero() {
		final Mower mower = new Mower(200, 600, 0);
		drive(mower, 1, true, 0);
		assertTrue(mower.isMoving());
		drive(mower, 5, false, 0);
		assertEquals(0.0, mower.speed());
		assertFalse(mower.isMoving());
	}

	@Test
	void stopsAtTheBoundaryInsteadOfLeavingTheLawn() {
		final Mower mower = new Mower(800, 600, 0);
		drive(mower, 20, true, 0);
		assertEquals(0.0, mower.speed());
		assertTrue(mower.body().getBounds2D().getMaxX() <= LAWN_W + 1e-9);
	}

	@Test
	void turnsOnlyWhileStandingStill() {
		final Mower mower = new Mower(800, 600, 0);
		drive(mower, 1, true, 0);
		final double headingWhileMoving = mower.heading();
		drive(mower, 0.5, true, 1);
		assertEquals(headingWhileMoving, mower.heading(), 1e-9);
	}

	@Test
	void cannotStartMovingWhileTurning() {
		final Mower mower = new Mower(800, 600, 0);
		drive(mower, 0.5, true, 1);
		assertEquals(0.0, mower.speed());
		assertTrue(mower.heading() > 0.0);
	}

	@Test
	void turningAgainstAWallDoesNotShoveTheMowerInwards() {
		// Parked flush against the right wall, so any rotation swings a corner over the edge.
		final Mower mower = new Mower(LAWN_W - Mower.LENGTH / 2, 600, 0);
		drive(mower, 0.5, false, 1);
		drive(mower, 0.5, false, 0);
		assertEquals(LAWN_W - Mower.LENGTH / 2, mower.x(), 1e-9);
		assertEquals(600.0, mower.y(), 1e-9);
	}

	@Test
	void bodyStaysTheSameSizeWhenRotated() {
		final Mower straight = new Mower(800, 600, 0);
		final Mower angled = new Mower(800, 600, Math.PI / 4);
		assertEquals(Mower.LENGTH, straight.body().getBounds2D().getWidth(), 1e-9);
		assertTrue(angled.body().getBounds2D().getWidth() > Mower.LENGTH);
	}
}
