package org.janelia.lawnmower.model;

import java.awt.geom.Rectangle2D;

/**
 * A squirrel sitting on the lawn, waiting to be mowed over.
 *
 * @param x centre of the squirrel along the x axis, in units
 * @param y centre of the squirrel along the y axis, in units
 * @param spawnTime time on the round clock when it appeared, in seconds
 */
public record Squirrel(double x, double y, double spawnTime) {

	/** Edge length of a squirrel, in units. */
	public static final double SIZE = 60.0;

	/** Returns the square the squirrel occupies, in lawn coordinates. */
	public Rectangle2D bounds() {
		return new Rectangle2D.Double(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
	}
}
