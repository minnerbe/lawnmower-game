package org.janelia.lawnmower.control;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.janelia.lawnmower.model.Game;

/**
 * The X-Touch Mini controls, made harder: neither the drive button nor the steering encoder
 * stays where it is.
 *
 * <p>Steering starts on the leftmost encoder and driving on the bottom right button. Then:
 *
 * <ul>
 * <li>pressing the drive button moves the steering encoder one to the right;
 * <li>releasing it moves the drive button itself one to the left;
 * <li>both wrap around at the end of their row.
 * </ul>
 *
 * <p>The lights say where the controls have got to. The lit button in the bottom row is the
 * one that drives; the lit button in the top row is the one directly below the encoder that
 * steers. Nothing else on the board does anything.
 *
 * @see XTouchControls the plain version, where the controls stay put
 */
public class WanderingXTouchControls extends XTouchControls {

	/** Where driving starts: the rightmost button of the bottom row. */
	private static final int FIRST_BUTTON = BOTTOM_ROW.length - 1;
	/** Where steering starts: the leftmost encoder. */
	private static final int FIRST_ENCODER = 0;

	// Written on the MIDI thread, read on the event dispatch thread.
	private volatile int steerEncoder = FIRST_ENCODER;
	private volatile int driveButton = FIRST_BUTTON;

	// Last positions shown on the board, so a 60 Hz loop does not flood the MIDI port.
	private int litEncoder = -1;
	private int litButton = -1;

	WanderingXTouchControls(final Receiver out, final MidiDevice... devices) {
		super(out, devices);
	}

	/**
	 * Finds the board, opens it and switches it into Mackie Control mode.
	 *
	 * @return controls reading the board, with every LED dark
	 * @throws MidiUnavailableException if the board cannot be opened
	 */
	public static WanderingXTouchControls open() throws MidiUnavailableException {
		final MidiDevice[] ports = openPorts();
		return start(new WanderingXTouchControls(ports[1].getReceiver(), ports), ports[0]);
	}

	@Override
	void handle(final ShortMessage m) {
		switch (m.getStatus()) {
			case CONTROL_CHANGE -> {
				if (m.getData1() == ENCODER_CC + steerEncoder) {
					turnedBy(m.getData2());
				}
			}
			case NOTE_ON -> {
				if (m.getData1() == BOTTOM_ROW[driveButton]) {
					drive(m.getData2() != 0);
				}
			}
			default -> { }
		}
	}

	/**
	 * Drives, and moves whichever control the player has just done with: the encoder on the
	 * way down, the button on the way up. Moving the button on the press instead would take
	 * it out from under the finger that is still holding it.
	 *
	 * @param down whether the drive button was pressed rather than released
	 */
	private void drive(final boolean down) {
		driving = down;
		if (down) {
			steerEncoder = (steerEncoder + 1) % ENCODERS;
		} else {
			driveButton = (driveButton + BOTTOM_ROW.length - 1) % BOTTOM_ROW.length;
		}
	}

	/** Shows where the two controls are; the round itself is not mirrored onto the board. */
	@Override
	public void feedback(final Game game) {
		final int encoder = steerEncoder;
		if (encoder != litEncoder) {
			if (litEncoder >= 0) {
				send(NOTE_ON, TOP_ROW[litEncoder], LED_OFF);
			}
			send(NOTE_ON, TOP_ROW[encoder], LED_ON);
			litEncoder = encoder;
		}
		final int button = driveButton;
		if (button != litButton) {
			if (litButton >= 0) {
				send(NOTE_ON, BOTTOM_ROW[litButton], LED_OFF);
			}
			send(NOTE_ON, BOTTOM_ROW[button], LED_ON);
			litButton = button;
		}
		// ponytail: speed, coverage and squirrel alerts are left off. Both rows are needed to
		// say where the controls are, and hunting for them is the point of this mode.
	}

	@Override
	public void darken() {
		super.darken();
		litEncoder = litButton = -1;
	}

	/**
	 * Steering position, for tests.
	 *
	 * @return the index of the encoder that currently steers, {@code 0} being the leftmost
	 */
	int steerEncoder() {
		return steerEncoder;
	}

	/**
	 * Driving position, for tests.
	 *
	 * @return the index in {@link #BOTTOM_ROW} of the button that currently drives
	 */
	int driveButton() {
		return driveButton;
	}
}
