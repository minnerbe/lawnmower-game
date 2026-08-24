package org.janelia.lawnmower.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

	private static final double DT = 1.0 / 60;

	/**
	 * A long, thin lawn, so that a single straight run crosses the 10% mark. On a square
	 * lawn that would take several passes and say nothing extra about the rules. The height
	 * follows the mower, so that one pass always cuts the same share of the lawn.
	 */
	private static Game thinLawn() {
		return started(new Game(2400, 2.5 * Mower.WIDTH));
	}

	/** Runs the opening countdown out, so a test can get straight to the mowing. */
	private static Game started(final Game game) {
		drive(game, Game.COUNTDOWN_SECONDS + DT, false, 0);
		return game;
	}

	/**
	 * Mows straight ahead until the first squirrel turns up, so the test does not have to
	 * know how long the mower takes to cover a tenth of the lawn.
	 */
	private static Game upToTheFirstSquirrel(final Game game) {
		for (int i = 0; i < 30 / DT && game.squirrels().isEmpty(); i++) {
			game.update(DT, true, 0);
		}
		assertFalse(game.squirrels().isEmpty(), "no squirrel appeared in half a minute of mowing");
		return game;
	}

	/** Long enough to drive past where a squirrel was put, whatever the mower's top speed. */
	private static double timeToClearTheSafeDistance() {
		return Game.SAFE_DISTANCE / Mower.MAX_SPEED + 0.5;
	}

	private static void drive(final Game game, final double seconds,
			final boolean accelerate, final double turn) {
		for (int i = 0; i < seconds / DT; i++) {
			game.update(DT, accelerate, turn);
		}
	}

	@Test
	void theRoundOnlyStartsOnceTheCountdownIsOver() {
		final Game game = new Game(2400, 340);
		drive(game, Game.COUNTDOWN_SECONDS - 1, true, 0);

		assertEquals(Game.State.COUNTDOWN, game.state());
		assertEquals(0.0, game.mower().speed(), "the controls are dead until the round starts");
		assertEquals(Game.ROUND_SECONDS, game.remainingSeconds(), "and the clock has not started");

		drive(game, 1.1, true, 0);
		assertEquals(Game.State.RUNNING, game.state());
		assertEquals(0.0, game.countdownSeconds());
		assertTrue(game.mower().isMoving());
	}

	@Test
	void aSquirrelAppearsAheadOfTheMowerAtTenPercentMowed() {
		final Game game = upToTheFirstSquirrel(thinLawn());

		assertTrue(game.lawn().mowedFraction() >= 0.10);
		assertEquals(1, game.squirrels().size());
		final Squirrel squirrel = game.squirrels().getFirst();
		final double gap = squirrel.x() - game.mower().x();
		assertTrue(gap > 0, "the squirrel sits in the mower's path");
		assertTrue(gap <= Game.SAFE_DISTANCE, "and never appears further out than the safe distance");
		assertEquals(game.mower().y(), squirrel.y(), 1e-9);
	}

	@Test
	void aSquirrelWandersOffIfItIsNotHit() {
		final Game game = upToTheFirstSquirrel(thinLawn());
		assertEquals(1, game.squirrels().size());

		drive(game, Game.SQUIRREL_LIFETIME + 1, false, 0);
		assertTrue(game.squirrels().isEmpty());
		assertEquals(0, game.hits());
	}

	@Test
	void mowingOverASquirrelCostsFivePercent() {
		final Game game = upToTheFirstSquirrel(thinLawn());
		final Squirrel doomed = game.squirrels().getFirst();
		drive(game, timeToClearTheSafeDistance(), true, 0);

		assertEquals(1, game.hits());
		assertEquals(List.of(doomed), game.squirrelsRunOver(),
				"the spot is kept, so the end-of-round picture can mark it");
		assertTrue(game.squirrels().isEmpty(), "the squirrel is gone once it is run over");
		assertEquals(game.lawn().mowedFraction() - Game.PENALTY_PER_SQUIRREL, game.score(), 1e-9);
	}

	@Test
	void aSquirrelThatWouldLandOffTheLawnIsDeferredNotSkipped() {
		// Tall enough that the 10% mark is only crossed once the mower is against the far wall.
		final Game game = started(new Game(2400, 340));
		drive(game, 6, true, 0);
		assertTrue(game.lawn().mowedFraction() >= 0.10, "the squirrel has been earned");
		assertTrue(game.squirrels().isEmpty(), "but there is no room ahead for it");

		drive(game, 1.6, false, -1);
		assertTrue(game.squirrels().isEmpty(), "turning alone is not yet a settled heading");

		drive(game, 1, true, 0);
		assertEquals(1, game.squirrels().size(), "it appears once the mower is under way again");
	}

	@Test
	void theRoundEndsWhenTimeRunsOutAndTheLastSwathIsKept() {
		final Game game = thinLawn();
		drive(game, Game.ROUND_SECONDS + 1, true, 0);

		assertEquals(Game.State.OVER, game.state());
		assertEquals(0.0, game.remainingSeconds());
		assertNull(game.lawn().currentSwath(), "the swath in progress is committed");
		assertTrue(game.lawn().mowedFraction() > 0);

		final double finalScore = game.score();
		drive(game, 5, true, 0);
		assertEquals(finalScore, game.score(), "the round is frozen once it is over");
	}
}
