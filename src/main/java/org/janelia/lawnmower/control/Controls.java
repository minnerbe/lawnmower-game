package org.janelia.lawnmower.control;

/**
 * The player's input, independent of the device it comes from.
 *
 * <p>This is the seam that keeps the rest of the game free of any particular hardware.
 * A keyboard implementation ships today; a MIDI controller with a button and a rotary
 * knob can be dropped in later without touching the model or the view.
 *
 * <p>Implementations are polled once per frame and must be cheap to call.
 */
public interface Controls {

	/**
	 * Reports whether the player is asking the mower to move forward.
	 *
	 * @return {@code true} while the forward control is held
	 */
	boolean accelerating();

	/**
	 * Reports how hard the player is turning.
	 *
	 * <p>A signed rate rather than an absolute heading, so a knob can report how far it
	 * was twisted since the last frame without the model knowing about knobs.
	 *
	 * @return turn input in [-1, 1]; negative turns left, positive right, 0 goes straight
	 */
	double turn();
}
