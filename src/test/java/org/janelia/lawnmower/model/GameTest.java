package org.janelia.lawnmower.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

	private static final double DT = 1.0 / 60;

	/**
	 * A long, thin lawn, so that a single straight run crosses the 10% mark. On a square
	 * lawn that would take several passes and say nothing extra about the rules.
	 */
	private static Game thinLawn() {
		return new Game(1200, 60);
	}

	private static void drive(final Game game, final double seconds,
			final boolean accelerate, final double turn) {
		for (int i = 0; i < seconds / DT; i++) {
			game.update(DT, accelerate, turn);
		}
	}

	@Test
	void aSquirrelAppearsAheadOfTheMowerAtTenPercentMowed() {
		final Game game = thinLawn();
		drive(game, 2.2, true, 0);

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
		final Game game = thinLawn();
		drive(game, 2.2, true, 0);
		assertEquals(1, game.squirrels().size());

		drive(game, Game.SQUIRREL_LIFETIME + 1, false, 0);
		assertTrue(game.squirrels().isEmpty());
		assertEquals(0, game.hits());
	}

	@Test
	void mowingOverASquirrelCostsFivePercent() {
		final Game game = thinLawn();
		drive(game, 2.2, true, 0);
		drive(game, 1.5, true, 0);

		assertEquals(1, game.hits());
		assertTrue(game.squirrels().isEmpty(), "the squirrel is gone once it is run over");
		assertEquals(game.lawn().mowedFraction() - Game.PENALTY_PER_SQUIRREL, game.score(), 1e-9);
	}

	@Test
	void aSquirrelThatWouldLandOffTheLawnIsDeferredNotSkipped() {
		// Tall enough that the 10% mark is only crossed once the mower is against the far wall.
		final Game game = new Game(1200, 100);
		drive(game, 6, true, 0);
		assertTrue(game.lawn().mowedFraction() >= 0.10, "the squirrel has been earned");
		assertTrue(game.squirrels().isEmpty(), "but there is no room ahead for it");

		drive(game, 1.6, false, -1);
		assertEquals(1, game.squirrels().size(), "it appears once the mower faces the lawn again");
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
