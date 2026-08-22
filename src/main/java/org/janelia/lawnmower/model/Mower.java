package org.janelia.lawnmower.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The mower's pose and driving physics.
 *
 * <p>The mower drives straight along its current heading. It can only turn while standing
 * still, and it cannot start moving in the same instant it turns, so a completed run is
 * always a straight line. Running into the lawn boundary stops it.
 *
 * <p>All lengths are in lawn units (pixels), angles in radians, and times in seconds.
 * A heading of {@code 0} points in the direction of increasing x.
 */
public class Mower {

	/** Top speed, in units per second. */
	public static final double MAX_SPEED = 360.0;
	/** Acceleration while the forward control is held, in units per second squared. */
	public static final double ACCELERATION = 440.0;
	/** Deceleration while the forward control is released, in units per second squared. */
	public static final double DECELERATION = 520.0;
	/** Turn rate at full turn input, in radians per second. */
	public static final double TURN_RATE = 2.0;
	/** Width of the mower, and therefore of the swath it cuts, in units. */
	public static final double WIDTH = 72.0;
	/** Length of the mower along its heading, in units. */
	public static final double LENGTH = 108.0;

	private double x;
	private double y;
	private double heading;
	private double speed;

	/**
	 * Creates a mower standing still at the given pose.
	 *
	 * @param x centre of the mower along the x axis, in units
	 * @param y centre of the mower along the y axis, in units
	 * @param heading direction of travel, in radians
	 */
	public Mower(final double x, final double y, final double heading) {
		this.x = x;
		this.y = y;
		this.heading = heading;
		this.speed = 0.0;
	}

	/**
	 * Advances the mower by one time step.
	 *
	 * <p>Takes plain control values rather than an input device, so the model stays
	 * independent of how the player's hardware is read.
	 *
	 * @param dt elapsed time, in seconds
	 * @param accelerate whether the forward control is held
	 * @param turn turn input, clamped to [-1, 1]; negative turns left, positive right
	 * @param lawnWidth width of the lawn, in units
	 * @param lawnHeight height of the lawn, in units
	 */
	public void update(final double dt, final boolean accelerate, final double turn,
			final double lawnWidth, final double lawnHeight) {

		if (speed == 0.0 && turn != 0.0) {
			// Turning locks out acceleration: the mower cannot pull away mid-turn.
			heading += Math.clamp(turn, -1.0, 1.0) * TURN_RATE * dt;
			return;
		}

		speed = accelerate
				? Math.min(MAX_SPEED, speed + ACCELERATION * dt)
				: Math.max(0.0, speed - DECELERATION * dt);

		x += Math.cos(heading) * speed * dt;
		y += Math.sin(heading) * speed * dt;
		stopAtBoundary(lawnWidth, lawnHeight);
	}

	/** Pushes the body back inside the lawn and kills the speed if it stuck out. */
	private void stopAtBoundary(final double lawnWidth, final double lawnHeight) {
		final Rectangle2D bounds = body().getBounds2D();
		final double dx = overshoot(bounds.getMinX(), bounds.getMaxX(), lawnWidth);
		final double dy = overshoot(bounds.getMinY(), bounds.getMaxY(), lawnHeight);
		if (dx != 0.0 || dy != 0.0) {
			x += dx;
			y += dy;
			speed = 0.0;
		}
	}

	/** Returns the shift needed to bring the interval [min, max] back inside [0, limit]. */
	private static double overshoot(final double min, final double max, final double limit) {
		if (min < 0.0) {
			return -min;
		}
		return max > limit ? limit - max : 0.0;
	}

	/**
	 * Returns the mower's footprint, a rectangle of {@link #LENGTH} by {@link #WIDTH}
	 * centred on its position and rotated to its heading.
	 *
	 * @return the footprint in lawn coordinates
	 */
	public Shape body() {
		final Rectangle2D unrotated = new Rectangle2D.Double(
				x - LENGTH / 2, y - WIDTH / 2, LENGTH, WIDTH);
		return AffineTransform.getRotateInstance(heading, x, y).createTransformedShape(unrotated);
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double heading() {
		return heading;
	}

	public double speed() {
		return speed;
	}

	public boolean isMoving() {
		return speed > 0.0;
	}
}
