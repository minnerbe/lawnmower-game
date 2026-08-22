package org.janelia.lawnmower.model;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One round of lawnmowing: the clock, the squirrels and the score.
 *
 * <p>This is the whole model behind a single {@link #update} call per frame. It knows
 * nothing about Swing or about how the player's controls are read.
 */
public class Game {

	/** Whether the round is still being played. */
	public enum State { RUNNING, OVER }

	/** Length of a round, in seconds. */
	public static final double ROUND_SECONDS = 90.0;
	/** Mowed share between one squirrel and the next. */
	public static final double MOWED_PER_SQUIRREL = 0.10;
	/** How far ahead of the mower a squirrel appears, in units. */
	public static final double SAFE_DISTANCE = 200.0;
	/** How long a squirrel stays before wandering off, in seconds. */
	public static final double SQUIRREL_LIFETIME = 6.0;
	/** Share of the lawn deducted per squirrel run over. */
	public static final double PENALTY_PER_SQUIRREL = 0.05;
	/** How often the mowed fraction is re-measured while mowing, in seconds. */
	private static final double REFRESH_INTERVAL = 0.2;

	private final Lawn lawn;
	private final Mower mower;
	private final List<Squirrel> squirrels = new ArrayList<>();

	private State state = State.RUNNING;
	private double elapsed;
	private double lastRefresh;
	private int squirrelsEarned;
	private int squirrelsPlaced;
	private int hits;

	/**
	 * Starts a round with the mower parked in the middle of the lawn, facing right.
	 *
	 * @param lawnWidth width of the lawn, in units
	 * @param lawnHeight height of the lawn, in units
	 */
	public Game(final double lawnWidth, final double lawnHeight) {
		this.lawn = new Lawn(lawnWidth, lawnHeight);
		this.mower = new Mower(lawnWidth / 2, lawnHeight / 2, 0.0);
	}

	/**
	 * Advances the round by one time step and does nothing once the round is over.
	 *
	 * @param dt elapsed time, in seconds
	 * @param accelerate whether the forward control is held
	 * @param turn turn input in [-1, 1]; negative turns left, positive right
	 */
	public void update(final double dt, final boolean accelerate, final double turn) {
		if (state == State.OVER) {
			return;
		}

		elapsed += dt;
		mower.update(dt, accelerate, turn, lawn.width(), lawn.height());
		lawn.follow(mower);

		if (elapsed - lastRefresh >= REFRESH_INTERVAL) {
			lawn.refreshMowedFraction();
			lastRefresh = elapsed;
		}

		squirrelsEarned = (int) (lawn.mowedFraction() / MOWED_PER_SQUIRREL);
		placeDueSquirrels();
		squirrels.removeIf(squirrel -> elapsed - squirrel.spawnTime() > SQUIRREL_LIFETIME);
		checkForHits();

		if (elapsed >= ROUND_SECONDS) {
			lawn.commitSwath();
			state = State.OVER;
		}
	}

	/**
	 * Puts every squirrel the player has earned into the mower's path, a safe distance
	 * ahead. A squirrel that would land off the lawn is deferred rather than dropped: the
	 * attempt repeats every frame until the mower has room ahead of it, which in practice
	 * means on its next run away from the boundary.
	 */
	private void placeDueSquirrels() {
		// Squirrels deferred from earlier are spread out along the path so they do not stack.
		for (int placedNow = 0; squirrelsPlaced < squirrelsEarned; placedNow++) {
			final double distance = SAFE_DISTANCE + placedNow * 2 * Squirrel.SIZE;
			final Squirrel squirrel = new Squirrel(
					mower.x() + Math.cos(mower.heading()) * distance,
					mower.y() + Math.sin(mower.heading()) * distance,
					elapsed);
			if (!lawnContains(squirrel)) {
				return;
			}
			squirrels.add(squirrel);
			squirrelsPlaced++;
		}
	}

	private boolean lawnContains(final Squirrel squirrel) {
		return new Rectangle2D.Double(0, 0, lawn.width(), lawn.height())
				.contains(squirrel.bounds());
	}

	private void checkForHits() {
		if (!mower.isMoving()) {
			return;
		}
		final Shape body = mower.body();
		for (final var it = squirrels.iterator(); it.hasNext(); ) {
			if (body.intersects(it.next().bounds())) {
				it.remove();
				hits++;
			}
		}
	}

	/**
	 * Returns the final score: the mowed share of the lawn less the squirrel penalty,
	 * never below zero.
	 *
	 * @return the score as a share of the lawn, from 0 to 1
	 */
	public double score() {
		return Math.max(0.0, lawn.mowedFraction() - hits * PENALTY_PER_SQUIRREL);
	}

	/** Returns the time left in the round, in seconds, never below zero. */
	public double remainingSeconds() {
		return Math.max(0.0, ROUND_SECONDS - elapsed);
	}

	/** Returns the squirrels currently on the lawn. */
	public List<Squirrel> squirrels() {
		return Collections.unmodifiableList(squirrels);
	}

	public Lawn lawn() {
		return lawn;
	}

	public Mower mower() {
		return mower;
	}

	public State state() {
		return state;
	}

	public int hits() {
		return hits;
	}
}
