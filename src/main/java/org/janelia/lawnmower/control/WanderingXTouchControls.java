package org.janelia.lawnmower.control;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

/**
 * The X-Touch Mini controls, made harder: neither the drive button nor the steering encoder
 * stays where it is.
 *
 * <p>They start where {@link XTouchControls} keeps them, on the leftmost encoder and the
 * bottom right button. Then:
 *
 * <ul>
 * <li>pressing the drive button moves the steering encoder one to the right;
 * <li>releasing it moves the drive button itself one to the left;
 * <li>both wrap around at the end of their row.
 * </ul>
 *
 * <p>The lights are the same as ever, and are the only way to keep track: the lit button in
 * the bottom row drives, and the lit button in the top row sits under the encoder that
 * steers, with the speed fan on that encoder's ring.
 *
 * @see XTouchControls the plain version, where the controls stay put
 */
public class WanderingXTouchControls extends XTouchControls {

	// Written on the MIDI thread, read on the event dispatch thread.
	private volatile int steerEncoder = FIRST_ENCODER;
	private volatile int driveButton = FIRST_BUTTON;

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
	int steerEncoder() {
		return steerEncoder;
	}

	@Override
	int driveButton() {
		return driveButton;
	}

	/**
	 * Drives, and moves whichever control the player has just done with: the encoder on the
	 * way down, the button on the way up. Moving the button on the press instead would take
	 * it out from under the finger that is still holding it.
	 */
	@Override
	void drive(final boolean down) {
		super.drive(down);
		if (down) {
			steerEncoder = (steerEncoder + 1) % ENCODERS;
		} else {
			driveButton = (driveButton + BOTTOM_ROW.length - 1) % BOTTOM_ROW.length;
		}
	}
}
