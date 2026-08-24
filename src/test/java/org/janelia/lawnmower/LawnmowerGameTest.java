package org.janelia.lawnmower;

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
}
