package org.janelia.lawnmower;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LawnmowerGameTest {

	@Test
	void thePlayerNameComesOffTheCommandLine() {
		assertEquals("Ada Lovelace", LawnmowerGame.playerName(new String[] {"Ada", "Lovelace"}),
				"-Dexec.args splits a name on spaces, so the parts have to be rejoined");
		assertEquals("player", LawnmowerGame.playerName(new String[0]));
		assertEquals("player", LawnmowerGame.playerName(new String[] {"  "}),
				"a blank name would leave a gap in the status bar");
		assertEquals("aaaaaaaaaaaaaaaaaaaa",
				LawnmowerGame.playerName(new String[] {"a".repeat(50)}),
				"an overlong name is cut down to keep the status bar readable");
	}

	@Test
	void theDeviceComesOffTheCommandLineAndIsNotPartOfTheName() {
		assertEquals("xtouch", LawnmowerGame.deviceName(new String[] {"--device=XTouch"}),
				"the device is matched without regard to case");
		assertEquals("keyboard", LawnmowerGame.deviceName(new String[] {"Ada"}),
				"without the flag the game stays on the keyboard");
		assertEquals("Ada Lovelace",
				LawnmowerGame.playerName(new String[] {"Ada", "--device=xtouch", "Lovelace"}),
				"the flag must not turn up on the scoreboard");
		assertEquals("player", LawnmowerGame.playerName(new String[] {"--device=xtouch"}));
	}

	@Test
	void theWindowIsCentredOnTheScreenItIsSentTo() {
		final Rectangle external = new Rectangle(1800, 0, 1920, 1080);
		assertEquals(new Point(1960, 100), LawnmowerGame.topLeftOn(external, new Dimension(1600, 880)),
				"the offset of the second screen has to be kept");
		assertEquals(new Point(1800, 0), LawnmowerGame.topLeftOn(external, new Dimension(2400, 1600)),
				"a window too big for the screen still starts in its corner, not off it");
	}
}
