package org.janelia.lawnmower.control;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard input: the up arrow drives, the left and right arrows turn.
 *
 * <p>Tracks which keys are down rather than reacting to key events, because the mower
 * needs to know whether a control is held right now, not how often the platform chose to
 * repeat it. Register this on the focused component with
 * {@code component.addKeyListener(controls)}.
 */
public class KeyboardControls extends KeyAdapter implements Controls {

	private final Set<Integer> pressedKeys = new HashSet<>();

	@Override
	public void keyPressed(final KeyEvent event) {
		pressedKeys.add(event.getKeyCode());
	}

	@Override
	public void keyReleased(final KeyEvent event) {
		pressedKeys.remove(event.getKeyCode());
	}

	@Override
	public boolean accelerating() {
		return pressedKeys.contains(KeyEvent.VK_UP);
	}

	@Override
	public double turn() {
		final int left = pressedKeys.contains(KeyEvent.VK_LEFT) ? 1 : 0;
		final int right = pressedKeys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
		return right - left;
	}
}
