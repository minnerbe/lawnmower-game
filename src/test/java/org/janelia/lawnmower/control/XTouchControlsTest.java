package org.janelia.lawnmower.control;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XTouchControlsTest {

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
	private static final int STEER_ENCODER_CC = 0x10;
	private static final int DRIVE_BUTTON_NOTE = 0x59;

	private static void receive(final XTouchControls controls,
			final int status, final int data1, final int data2) throws Exception {
		controls.send(new ShortMessage(status, data1, data2), -1);
	}

	@Test
	void theEncoderTurnsBothWaysAndTheTurnLapses() throws Exception {
		final XTouchControls controls = new XTouchControls(DISCARD);
		assertEquals(0.0, controls.turn(), "an untouched board goes straight");

		receive(controls, CONTROL_CHANGE, STEER_ENCODER_CC, 0x02);
		final long detent = System.currentTimeMillis();
		assertEquals(1.0, controls.turnAt(detent), "values under 0x40 are clockwise detents");
		assertEquals(0.0, controls.turnAt(detent + XTouchControls.TURN_HOLD_MILLIS),
				"one detent must not turn the mower for ever");

		receive(controls, CONTROL_CHANGE, STEER_ENCODER_CC, 0x41);
		assertEquals(-1.0, controls.turnAt(System.currentTimeMillis()),
				"0x41 and up are anticlockwise detents");

		receive(controls, CONTROL_CHANGE, STEER_ENCODER_CC + 3, 0x02);
		assertEquals(-1.0, controls.turnAt(System.currentTimeMillis()),
				"the other encoders are not mapped to anything");
	}

	@Test
	void theDriveButtonIsHeldNotToggled() throws Exception {
		final XTouchControls controls = new XTouchControls(DISCARD);
		assertFalse(controls.accelerating());

		receive(controls, NOTE_ON, DRIVE_BUTTON_NOTE, 127);
		assertTrue(controls.accelerating());
		receive(controls, NOTE_ON, DRIVE_BUTTON_NOTE, 0);
		assertFalse(controls.accelerating(), "note-on with velocity 0 is the release");
	}

	@Test
	void theLedRingShowsAFanScaledToTheSegmentCount() {
		assertEquals(0x20, XTouchControls.ringFan(0.0), "mode 2, no segments lit");
		assertEquals(0x20 + 11, XTouchControls.ringFan(1.0), "mode 2, all eleven lit");
		assertEquals(0x20 + 6, XTouchControls.ringFan(0.5));
		assertEquals(0x20 + 11, XTouchControls.ringFan(1.5), "an out-of-range value is clamped");
	}
}
