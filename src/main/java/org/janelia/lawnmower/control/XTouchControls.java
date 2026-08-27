package org.janelia.lawnmower.control;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.janelia.lawnmower.model.Game;
import org.janelia.lawnmower.model.Mower;

/**
 * Input and LED feedback for a Behringer X-Touch Mini, in Mackie Control mode.
 *
 * <p>The device is opened through {@link javax.sound.midi}, which is part of the JDK, so
 * this adds no dependency. In MC mode the board reports:
 *
 * <ul>
 * <li>eight <em>relative</em> rotary encoders on control change {@code 0x10}-{@code 0x17},
 *     sending {@code 1..7} for clockwise detents and {@code 0x41..0x47} for anticlockwise;
 * <li>the encoders' push switches as notes {@code 0x20}-{@code 0x27};
 * <li>sixteen buttons as the notes in {@link #TOP_ROW} and {@link #BOTTOM_ROW};
 * <li>the two layer buttons as notes {@code 0x54} and {@code 0x55};
 * <li>the fader as pitch bend on channel 9, {@code 0}-{@code 127}.
 * </ul>
 *
 * <p>Every LED is host-controlled in this mode: send the same note back with velocity
 * {@link #LED_ON}, {@link #LED_BLINK} or {@link #LED_OFF} to light a button, and a control
 * change on {@code 0x30}-{@code 0x37} to draw on an encoder's eleven-segment ring.
 *
 * <p>The leftmost encoder steers and the bottom right button drives; see
 * {@link #feedback} for what the lights say.
 *
 * <p>Run this class on its own to see what the board sends:
 * {@code mvn compile exec:java -Dexec.mainClass=org.janelia.lawnmower.control.XTouchControls}
 */
public class XTouchControls implements Controls, Receiver, AutoCloseable {

	/** Matched against the MIDI device name and description, case-insensitively. */
	private static final String DEVICE = "X-TOUCH MINI";

	static final int NOTE_ON = 0x90;
	static final int CONTROL_CHANGE = 0xb0;

	/** Control change of the leftmost encoder; the other seven follow it. */
	static final int ENCODER_CC = 0x10;
	/** Control change of the leftmost encoder's LED ring; the other seven follow it. */
	static final int RING_CC = 0x30;
	/** Notes of the eight buttons under the encoders, left to right. */
	static final int[] TOP_ROW = {0x59, 0x5a, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d};
	/** Notes of the eight buttons in the bottom row, left to right. */
	static final int[] BOTTOM_ROW = {0x57, 0x58, 0x5b, 0x5c, 0x56, 0x5d, 0x5e, 0x5f};

	static final int LED_OFF = 0;
	private static final int LED_BLINK = 1;
	static final int LED_ON = 127;

	/** Number of encoders, and therefore of LED rings. */
	static final int ENCODERS = 8;
	/** Segments in an encoder's LED ring. */
	private static final int RING_SEGMENTS = 11;
	/** Ring drawing mode: a fan growing clockwise from the left. */
	private static final int RING_FAN = 2;

	/** Where steering sits, and starts: the leftmost encoder. */
	static final int FIRST_ENCODER = 0;
	/** Where driving sits, and starts: the rightmost button of the bottom row. */
	static final int FIRST_BUTTON = BOTTOM_ROW.length - 1;

	/**
	 * How long one detent of the encoder keeps the mower turning, in milliseconds. The
	 * encoder reports detents, not a position, so a twist has to be stretched over a few
	 * frames to turn the mower by a noticeable angle.
	 */
	static final long TURN_HOLD_MILLIS = 150;

	private final Receiver out;
	private final MidiDevice[] devices;

	// Written on the MIDI thread, read on the event dispatch thread.
	volatile boolean driving;
	private volatile int turnDirection;
	private volatile long lastDetentMillis = Long.MIN_VALUE;

	/** Set by {@link #main} to print everything the board sends. */
	private volatile boolean dump;

	// Last state shown on the board, so a 60 Hz loop does not flood the MIDI port.
	private int sentRing = -1;
	private int litEncoder = -1;
	private int litButton = -1;

	/**
	 * Wires the controls to an already open MIDI port.
	 *
	 * @param out where LED messages go
	 * @param devices the devices to close with these controls, possibly none
	 */
	XTouchControls(final Receiver out, final MidiDevice... devices) {
		this.out = out;
		this.devices = devices;
	}

	/**
	 * Finds the board, opens it and switches it into Mackie Control mode.
	 *
	 * @return controls reading the board, with every LED dark
	 * @throws MidiUnavailableException if the board cannot be opened
	 */
	public static XTouchControls open() throws MidiUnavailableException {
		final MidiDevice[] ports = openPorts();
		return start(new XTouchControls(ports[1].getReceiver(), ports), ports[0]);
	}

	/**
	 * Finds the board and opens both of its ports.
	 *
	 * @return the input port first, the output port second
	 * @throws MidiUnavailableException if no X-Touch Mini is plugged in, or its ports are
	 *     already taken by another program
	 */
	static MidiDevice[] openPorts() throws MidiUnavailableException {
		MidiDevice input = null;
		MidiDevice output = null;
		for (final MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
			if (!matchesTheBoard(info)) {
				continue;
			}
			final MidiDevice device = MidiSystem.getMidiDevice(info);
			// The board shows up as two devices, one port each, in no particular order.
			if (input == null && device.getMaxTransmitters() != 0) {
				input = device;
			}
			if (output == null && device.getMaxReceivers() != 0) {
				output = device;
			}
		}
		if (input == null || output == null) {
			throw new MidiUnavailableException("no " + DEVICE + " with both an in and an out port");
		}
		input.open();
		output.open();
		return new MidiDevice[] {input, output};
	}

	/**
	 * Puts the board into Mackie Control mode with its LEDs dark, and has it report to the
	 * given controls.
	 *
	 * @param controls the controls to hand the board's messages to
	 * @param input the board's input port, already open
	 * @return {@code controls}
	 * @throws MidiUnavailableException if the input port has no transmitter to spare
	 */
	static <C extends XTouchControls> C start(final C controls, final MidiDevice input)
			throws MidiUnavailableException {
		input.getTransmitter().setReceiver(controls);
		// Ask for MC mode, in case the board was left in standard mode by another program.
		controls.send(CONTROL_CHANGE, 127, 1);
		controls.darken();
		return controls;
	}

	private static boolean matchesTheBoard(final MidiDevice.Info info) {
		return (info.getName() + " " + info.getDescription()).toUpperCase().contains(DEVICE);
	}

	@Override
	public boolean accelerating() {
		return driving;
	}

	@Override
	public double turn() {
		return turnAt(System.currentTimeMillis());
	}

	/**
	 * Reports the turn input as of a given moment.
	 *
	 * @param nowMillis the moment to report for, on the {@link System#currentTimeMillis} clock
	 * @return -1, 0 or 1, as {@link Controls#turn}
	 */
	double turnAt(final long nowMillis) {
		return nowMillis - lastDetentMillis < TURN_HOLD_MILLIS ? turnDirection : 0;
	}

	/**
	 * Mirrors the round onto the board: the lit button in the bottom row is the one that
	 * drives, the lit button in the top row sits under the encoder that steers, and that
	 * encoder's ring shows the mower's speed.
	 */
	@Override
	public void feedback(final Game game) {
		final int encoder = steerEncoder();
		if (encoder != litEncoder) {
			if (litEncoder >= 0) {
				send(CONTROL_CHANGE, RING_CC + litEncoder, 0);
				send(NOTE_ON, TOP_ROW[litEncoder], LED_OFF);
			}
			send(NOTE_ON, TOP_ROW[encoder], LED_ON);
			litEncoder = encoder;
			// The fan has to be drawn again, on the ring it has moved to.
			sentRing = -1;
		}
		final int fan = ringFan(game.mower().speed() / Mower.MAX_SPEED);
		if (fan != sentRing) {
			sentRing = fan;
			send(CONTROL_CHANGE, RING_CC + encoder, fan);
		}

		final int button = driveButton();
		if (button != litButton) {
			if (litButton >= 0) {
				send(NOTE_ON, BOTTOM_ROW[litButton], LED_OFF);
			}
			send(NOTE_ON, BOTTOM_ROW[button], LED_ON);
			litButton = button;
		}
		// ponytail: coverage and squirrel alerts are left off. Both rows of buttons are
		// needed to say where the controls are; the seven spare rings could take them.
	}

	/**
	 * Encodes a fraction of full scale as a fan of lit ring segments.
	 *
	 * @param fraction the fraction to show, clamped to [0, 1]
	 * @return the control change value to send to a ring
	 */
	static int ringFan(final double fraction) {
		return RING_FAN * 16 + (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * RING_SEGMENTS);
	}

	/**
	 * Which encoder steers. Fixed here; the wandering mode moves it.
	 *
	 * @return the index of the steering encoder, {@code 0} being the leftmost
	 */
	int steerEncoder() {
		return FIRST_ENCODER;
	}

	/**
	 * Which button drives. Fixed here; the wandering mode moves it.
	 *
	 * @return the index in {@link #BOTTOM_ROW} of the driving button
	 */
	int driveButton() {
		return FIRST_BUTTON;
	}

	/**
	 * Acts on the drive button. Called on a MIDI thread, not the EDT.
	 *
	 * @param down whether the button was pressed rather than released
	 */
	void drive(final boolean down) {
		driving = down;
	}

	/** Turns every LED off, so the board is not left lit after the game exits. */
	public void darken() {
		for (int i = 0; i < ENCODERS; i++) {
			send(CONTROL_CHANGE, RING_CC + i, 0);
		}
		for (final int note : TOP_ROW) {
			send(NOTE_ON, note, LED_OFF);
		}
		for (final int note : BOTTOM_ROW) {
			send(NOTE_ON, note, LED_OFF);
		}
		sentRing = litEncoder = litButton = -1;
	}

	/** Receives a message from the board. Called on a MIDI thread, not the EDT. */
	@Override
	public void send(final MidiMessage message, final long timeStamp) {
		if (!(message instanceof final ShortMessage m)) {
			return;
		}
		if (dump) {
			System.out.printf("status %02x  data %02x %02x%n",
					m.getStatus(), m.getData1(), m.getData2());
		}
		handle(m);
	}

	/**
	 * Acts on one message from the board. Called on a MIDI thread, not the EDT.
	 *
	 * @param m the message, already known to be a short one
	 */
	void handle(final ShortMessage m) {
		switch (m.getStatus()) {
			case CONTROL_CHANGE -> {
				if (m.getData1() == ENCODER_CC + steerEncoder()) {
					turnedBy(m.getData2());
				}
			}
			case NOTE_ON -> {
				if (m.getData1() == BOTTOM_ROW[driveButton()]) {
					drive(m.getData2() != 0);
				}
			}
			default -> { }
		}
	}

	/**
	 * Records a twist of the steering encoder.
	 *
	 * @param value the encoder's control change value: {@code 1..7} detents clockwise, or
	 *     {@code 0x41..0x47} for the same number anticlockwise
	 */
	void turnedBy(final int value) {
		turnDirection = value < 0x40 ? 1 : -1;
		lastDetentMillis = System.currentTimeMillis();
	}

	@Override
	public void close() {
		darken();
		for (final MidiDevice device : devices) {
			device.close();
		}
	}

	void send(final int status, final int data1, final int data2) {
		try {
			out.send(new ShortMessage(status, data1, data2), -1);
		} catch (final InvalidMidiDataException e) {
			throw new IllegalArgumentException("not a MIDI message: " + status, e);
		}
	}

	/**
	 * Lights the board up and prints every message it sends, so its controls can be
	 * identified before they are mapped to anything.
	 *
	 * @param args ignored
	 * @throws Exception if the board cannot be opened
	 */
	public static void main(final String... args) throws Exception {
		try (final XTouchControls controls = open()) {
			controls.dump = true;
			for (int i = 0; i < ENCODERS; i++) {
				controls.send(CONTROL_CHANGE, RING_CC + i, ringFan(1.0));
				controls.send(NOTE_ON, TOP_ROW[i], LED_ON);
				controls.send(NOTE_ON, BOTTOM_ROW[i], LED_BLINK);
				Thread.sleep(120);
			}
			System.out.println("every LED should be lit; turn and press things, then hit enter");
			System.in.read();
		}
	}
}
