package org.janelia.lawnmower.control;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingXTouchControlsTest {

	/** Stands in for the board's LED port, which no test has. */
	private static final Receiver DISCARD = new Receiver() {

		@Override
		public void send(final MidiMessage message, final long timeStamp) {
		}

		@Override
		public void close() {
		}
	};

	private static final int CONTROL_CHANGE = 0xb0;
	private static final int NOTE_ON = 0x90;
	private static final int ENCODER_CC = 0x10;
	/** The bottom right button, where driving starts. */
	private static final int FIRST_BUTTON_NOTE = 0x5f;
	/** The button to its left, where driving goes next. */
	private static final int SECOND_BUTTON_NOTE = 0x5e;

	private static void receive(final WanderingXTouchControls controls,
			final int status, final int data1, final int data2) throws Exception {
		controls.send(new ShortMessage(status, data1, data2), -1);
	}

	private static void twist(final WanderingXTouchControls controls, final int encoder)
			throws Exception {
		receive(controls, CONTROL_CHANGE, ENCODER_CC + encoder, 0x02);
	}

	@Test
	void pressingTheDriveButtonMovesTheSteeringEncoderRight() throws Exception {
		final WanderingXTouchControls controls = new WanderingXTouchControls(DISCARD);
		twist(controls, 0);
		assertEquals(1.0, controls.turn(), "steering starts on the leftmost encoder");

		receive(controls, NOTE_ON, FIRST_BUTTON_NOTE, 127);
		assertTrue(controls.accelerating());
		assertEquals(1, controls.steerEncoder(), "the press moved steering one to the right");

		// Past the hold, so the twist above is no longer keeping the mower turning.
		final long lapsed = System.currentTimeMillis() + XTouchControls.TURN_HOLD_MILLIS;
		twist(controls, 0);
		assertEquals(0.0, controls.turnAt(lapsed), "the encoder that used to steer is dead");
		twist(controls, 1);
		assertEquals(1.0, controls.turn(), "its neighbour steers now");
	}

	@Test
	void releasingTheDriveButtonMovesTheDriveButtonLeft() throws Exception {
		final WanderingXTouchControls controls = new WanderingXTouchControls(DISCARD);
		receive(controls, NOTE_ON, FIRST_BUTTON_NOTE, 127);
		receive(controls, NOTE_ON, FIRST_BUTTON_NOTE, 0);
		assertFalse(controls.accelerating(), "note-on with velocity 0 is the release");

		receive(controls, NOTE_ON, FIRST_BUTTON_NOTE, 127);
		assertFalse(controls.accelerating(), "the button that used to drive is dead");
		receive(controls, NOTE_ON, SECOND_BUTTON_NOTE, 127);
		assertTrue(controls.accelerating(), "its neighbour to the left drives now");
	}

	@Test
	void bothControlsWrapAroundTheirRow() throws Exception {
		final WanderingXTouchControls controls = new WanderingXTouchControls(DISCARD);
		for (int i = 0; i < 8; i++) {
			final int note = note(controls.driveButton());
			receive(controls, NOTE_ON, note, 127);
			receive(controls, NOTE_ON, note, 0);
		}
		assertEquals(0, controls.steerEncoder(), "eight presses bring steering back to the left");
		assertEquals(7, controls.driveButton(), "eight releases bring driving back to the right");
	}

	private static int note(final int button) {
		return new int[] {0x57, 0x58, 0x5b, 0x5c, 0x56, 0x5d, 0x5e, 0x5f}[button];
	}
}
