package org.janelia.lawnmower.view;

import org.janelia.lawnmower.model.Mower;
import org.janelia.lawnmower.model.Squirrel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the sprites ship with the build. The view silently falls back to rectangles
 * when a sprite is missing, so without this test a renamed asset would go unnoticed.
 */
class SpritesTest {

	@Test
	void loadsTheMowerScaledToItsBody() {
		assertNotNull(Sprites.MOWER, "/sprites/lawnmower.png should be on the classpath");
		assertEquals((int) Mower.WIDTH, Sprites.MOWER.getWidth());
		assertEquals((int) Mower.LENGTH, Sprites.MOWER.getHeight());
	}

	@Test
	void loadsTheSquirrelWithATransparentBackground() {
		assertNotNull(Sprites.SQUIRREL, "/sprites/squirrel.png should be on the classpath");
		assertEquals((int) Squirrel.SIZE, Sprites.SQUIRREL.getWidth());
		assertTrue(Sprites.SQUIRREL.getHeight() > Sprites.SQUIRREL.getWidth(),
				"the squirrel stands upright, so the sprite keeps its tall aspect ratio");
		assertEquals(0, Sprites.SQUIRREL.getRGB(0, 0) >>> 24,
				"the white background should have been keyed out");
	}
}
