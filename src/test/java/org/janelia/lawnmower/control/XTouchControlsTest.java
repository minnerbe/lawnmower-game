package org.janelia.lawnmower.control;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.janelia.lawnmower.model.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XTouchControlsTest {

	/** Stands in for the board's LED port, which no test has, and remembers what it was sent. */
	private static final class Lights implements Receiver {

		private final List<String> sent = new ArrayList<>();

		@Override
		public void send(final MidiMessage message, final long timeStamp) {
			final ShortMessage m = (ShortMessage) message;
			sent.add(String.format("%02x %02x %02x", m.getStatus(), m.getData1(), m.getData2()));
		}

		@Override
		public void close() {
		}
	}

	private static final Receiver DISCARD = new Lights();

	private static final int CONTROL_CHANGE = 0xb0;
	private static final int NOTE_ON = 0x90;
	private static final int STEER_ENCODER_CC = 0x10;
	/** The bottom right button, which drives. */
	private static final int DRIVE_BUTTON_NOTE = 0x5f;
	/** The button below the steering encoder, which is only a light. */
	private static final int STEER_MARKER_NOTE = 0x59;

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
	void theLightsMarkTheControlsAndStayPut() throws Exception {
		final Lights lights = new Lights();
		final XTouchControls controls = new XTouchControls(lights);
		final Game game = new Game(1600, 800);

		controls.feedback(game);
		assertEquals(List.of(
						String.format("%02x %02x %02x", NOTE_ON, STEER_MARKER_NOTE, 127),
						String.format("%02x %02x %02x", CONTROL_CHANGE, 0x30, XTouchControls.ringFan(0)),
						String.format("%02x %02x %02x", NOTE_ON, DRIVE_BUTTON_NOTE, 127)),
				lights.sent,
				"the steering marker, the speed fan on that encoder's ring, and the drive button");

		lights.sent.clear();
		receive(controls, NOTE_ON, DRIVE_BUTTON_NOTE, 127);
		controls.feedback(game);
		controls.feedback(game);
		assertEquals(List.of(), lights.sent, "nothing moves, and a still board is not resent");
	}

	@Test
	void theLedRingShowsAFanScaledToTheSegmentCount() {
		assertEquals(0x20, XTouchControls.ringFan(0.0), "mode 2, no segments lit");
		assertEquals(0x20 + 11, XTouchControls.ringFan(1.0), "mode 2, all eleven lit");
		assertEquals(0x20 + 6, XTouchControls.ringFan(0.5));
		assertEquals(0x20 + 11, XTouchControls.ringFan(1.5), "an out-of-range value is clamped");
	}
}
