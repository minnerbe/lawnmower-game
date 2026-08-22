package org.janelia.lawnmower.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The lawn and the swaths mowed out of it.
 *
 * <p>Because the mower cannot turn while moving, everything it cuts between one start and
 * the next stop is a single rotated rectangle. That rectangle grows while the mower runs
 * and joins the set of committed swaths once the mower stops.
 *
 * <p>Passes may overlap, so the mowed fraction comes from the geometric union of the
 * swaths rather than from the sum of their areas.
 */
public class Lawn {

	private final double width;
	private final double height;
	private final Set<Shape> mowed = new LinkedHashSet<>();
	private final Area coverage = new Area();

	private Shape currentSwath;
	private double startX;
	private double startY;
	private double startHeading;
	private double mowedFraction;

	/**
	 * Creates an unmowed lawn.
	 *
	 * @param width width of the lawn, in units
	 * @param height height of the lawn, in units
	 */
	public Lawn(final double width, final double height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Tracks the mower for one time step, opening, extending or closing a swath as needed.
	 *
	 * @param mower the mower to follow
	 */
	public void follow(final Mower mower) {
		if (!mower.isMoving()) {
			commitSwath();
			return;
		}
		if (currentSwath == null) {
			startX = mower.x();
			startY = mower.y();
			startHeading = mower.heading();
		}
		final double distance = Math.hypot(mower.x() - startX, mower.y() - startY);
		final Rectangle2D unrotated = new Rectangle2D.Double(
				startX - Mower.LENGTH / 2, startY - Mower.WIDTH / 2,
				distance + Mower.LENGTH, Mower.WIDTH);
		currentSwath = AffineTransform.getRotateInstance(startHeading, startX, startY)
				.createTransformedShape(unrotated);
	}

	/**
	 * Adds the swath in progress, if any, to the mowed set and updates the mowed fraction.
	 * Safe to call when no swath is open, which is what ends a round cleanly.
	 */
	public void commitSwath() {
		if (currentSwath == null) {
			return;
		}
		mowed.add(currentSwath);
		coverage.add(new Area(currentSwath));
		currentSwath = null;
		mowedFraction = areaOf(coverage) / (width * height);
	}

	/**
	 * Recomputes the mowed fraction including the swath in progress.
	 *
	 * <p>This unions and re-measures the whole coverage, so call it a few times per second
	 * at most, not once per frame.
	 */
	public void refreshMowedFraction() {
		if (currentSwath == null) {
			return;
		}
		// ponytail: re-unions and re-measures the whole coverage from scratch. Fine for the
		// ~100 swaths of a 90 s round; if it ever stutters, back the fraction with a coarse
		// boolean occupancy grid and keep Area only for drawing.
		final Area live = (Area) coverage.clone();
		live.add(new Area(currentSwath));
		mowedFraction = areaOf(live) / (width * height);
	}

	/**
	 * Computes the enclosed area of a shape by the shoelace formula over its flattened
	 * outline. Correct for the multi-part, holed outlines an {@link Area} union produces.
	 *
	 * @param shape the shape to measure
	 * @return the enclosed area, in square units
	 */
	public static double areaOf(final Shape shape) {
		double twiceArea = 0.0;
		double startPointX = 0.0;
		double startPointY = 0.0;
		double previousX = 0.0;
		double previousY = 0.0;
		final double[] point = new double[6];

		for (final PathIterator it = shape.getPathIterator(null, 1.0); !it.isDone(); it.next()) {
			switch (it.currentSegment(point)) {
				case PathIterator.SEG_MOVETO -> {
					startPointX = point[0];
					startPointY = point[1];
					previousX = point[0];
					previousY = point[1];
				}
				case PathIterator.SEG_LINETO -> {
					twiceArea += previousX * point[1] - point[0] * previousY;
					previousX = point[0];
					previousY = point[1];
				}
				case PathIterator.SEG_CLOSE -> {
					twiceArea += previousX * startPointY - startPointX * previousY;
					previousX = startPointX;
					previousY = startPointY;
				}
				default -> throw new IllegalStateException("flattened path has curves");
			}
		}
		return Math.abs(twiceArea) / 2;
	}

	/** Returns the committed swaths, oldest first. */
	public Collection<Shape> mowed() {
		return Collections.unmodifiableCollection(mowed);
	}

	/** Returns the swath being mowed right now, or {@code null} if the mower is stopped. */
	public Shape currentSwath() {
		return currentSwath;
	}

	/** Returns the mowed share of the lawn, from 0 to 1. */
	public double mowedFraction() {
		return mowedFraction;
	}

	public double width() {
		return width;
	}

	public double height() {
		return height;
	}
}
