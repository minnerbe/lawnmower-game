package org.janelia.lawnmower.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.janelia.lawnmower.model.Game;
import org.janelia.lawnmower.model.Lawn;
import org.janelia.lawnmower.model.Mower;
import org.janelia.lawnmower.model.Squirrel;

/**
 * Draws the lawn, the mower and the heads-up display.
 *
 * <p>The view only reads the model: it holds no game state of its own and never advances
 * the round. Lawn units map one to one onto pixels, with the status strip below the lawn.
 */
public class GameView extends JPanel {

	/** Height of the status strip below the lawn, in pixels. */
	private static final int HUD_HEIGHT = 80;

	private static final Color UNMOWED = new Color(28, 92, 38);
	private static final Color MOWED = new Color(126, 196, 84);
	private static final Color SQUIRREL = new Color(140, 96, 62);
	private static final Color ALERT = new Color(220, 30, 30);
	private static final Color MOWER = new Color(220, 60, 50);
	private static final Color HUD_BACKGROUND = new Color(32, 32, 32);
	private static final Color HUD_TEXT = new Color(238, 238, 238);
	private static final Color BAR_EMPTY = new Color(70, 70, 70);

	/** How long the lawn stays tinted after a squirrel appears, in seconds. */
	private static final double FLASH_SECONDS = 0.3;
	/** Opacity of the tint at the moment a squirrel appears, from 0 to 255. */
	private static final int FLASH_ALPHA = 90;

	/** Set between the HUD readings, which all share one line. */
	private static final String SEPARATOR = "   \u00b7   ";

	/** Speed bar colours, slowest segment first. */
	private static final Color[] BAR_COLORS = {
			new Color(80, 200, 80), new Color(160, 210, 60), new Color(230, 210, 60),
			new Color(240, 150, 50), new Color(220, 60, 50)};

	private final Game game;

	/**
	 * Creates a view of a round in progress.
	 *
	 * @param game the model to draw; the view never modifies it
	 */
	public GameView(final Game game) {
		this.game = game;
		final Lawn lawn = game.lawn();
		setPreferredSize(new Dimension((int) lawn.width(), (int) lawn.height() + HUD_HEIGHT));
		setFocusable(true);
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		final Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		paintLawn(g);
		paintSquirrelAlert(g);
		paintSquirrels(g);
		paintMower(g);
		paintHud(g);
		if (game.state() == Game.State.OVER) {
			paintResult(g);
		}
		g.dispose();
	}

	private void paintLawn(final Graphics2D g) {
		final Lawn lawn = game.lawn();
		g.setColor(UNMOWED);
		g.fillRect(0, 0, (int) lawn.width(), (int) lawn.height());

		g.setColor(MOWED);
		for (final Shape swath : lawn.mowed()) {
			g.fill(swath);
		}
		if (lawn.currentSwath() != null) {
			g.fill(lawn.currentSwath());
		}
	}

	/**
	 * Tints the lawn red as a squirrel appears and fades the tint out again, so the player
	 * notices the new arrival without having to spot it.
	 *
	 * <p>The tint is derived from the youngest squirrel's age rather than counted down in a
	 * field of its own, which keeps the view free of state that a dropped frame could
	 * desync. A squirrel mowed over mid-flash therefore takes its flash with it.
	 */
	private void paintSquirrelAlert(final Graphics2D g) {
		double youngest = FLASH_SECONDS;
		for (final Squirrel squirrel : game.squirrels()) {
			youngest = Math.min(youngest, game.elapsedSeconds() - squirrel.spawnTime());
		}
		final int alpha = (int) Math.round(FLASH_ALPHA * (1.0 - youngest / FLASH_SECONDS));
		if (alpha <= 0) {
			return;
		}

		final Lawn lawn = game.lawn();
		g.setColor(new Color(ALERT.getRed(), ALERT.getGreen(), ALERT.getBlue(), alpha));
		g.fillRect(0, 0, (int) lawn.width(), (int) lawn.height());
	}

	private void paintSquirrels(final Graphics2D g) {
		g.setColor(SQUIRREL);
		for (final Squirrel squirrel : game.squirrels()) {
			if (Sprites.SQUIRREL == null) {
				g.fill(squirrel.bounds());
			} else {
				drawSprite(g, Sprites.SQUIRREL, squirrel.x(), squirrel.y(), 0);
			}
		}
	}

	private void paintMower(final Graphics2D g) {
		final Mower mower = game.mower();
		if (Sprites.MOWER != null) {
			// The sprite faces north, a heading of -PI/2, so it trails the heading by a quarter turn.
			drawSprite(g, Sprites.MOWER, mower.x(), mower.y(), mower.heading() + Math.PI / 2);
			return;
		}

		g.setColor(MOWER);
		g.fill(mower.body());

		// A nose line, so the heading is readable while the mower stands still and turns.
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(3f));
		final double nose = Mower.LENGTH / 2;
		g.drawLine((int) mower.x(), (int) mower.y(),
				(int) (mower.x() + Math.cos(mower.heading()) * nose),
				(int) (mower.y() + Math.sin(mower.heading()) * nose));
	}

	/** Draws a sprite centred on a point in lawn units and turned clockwise by {@code rotation}. */
	private static void drawSprite(final Graphics2D g, final BufferedImage sprite,
			final double x, final double y, final double rotation) {
		final AffineTransform placement = AffineTransform.getRotateInstance(rotation, x, y);
		placement.translate(x - sprite.getWidth() / 2.0, y - sprite.getHeight() / 2.0);
		g.drawImage(sprite, placement, null);
	}

	private void paintHud(final Graphics2D g) {
		final Lawn lawn = game.lawn();
		final int top = (int) lawn.height();
		g.setColor(HUD_BACKGROUND);
		g.fillRect(0, top, getWidth(), HUD_HEIGHT);

		g.setColor(HUD_TEXT);
		g.setFont(getFont().deriveFont(Font.BOLD, 20f));
		final FontMetrics metrics = g.getFontMetrics();
		final int baseline = top + (HUD_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2;
		drawRow(g, baseline, 20,
				String.format("%04.1f s", game.remainingSeconds()),
				String.format("mowed %.1f%%", 100 * lawn.mowedFraction()),
				"squirrels hit: " + game.hits());

		paintSpeedBar(g, getWidth() - 60, top + 8);
	}

	/**
	 * Draws the readings left to right on one baseline, measuring each one so the gaps stay
	 * even however wide the numbers grow.
	 */
	private static void drawRow(final Graphics2D g, final int baseline, final int left,
			final String... readings) {
		final FontMetrics metrics = g.getFontMetrics();
		int x = left;
		for (final String reading : readings) {
			if (x > left) {
				g.drawString(SEPARATOR, x, baseline);
				x += metrics.stringWidth(SEPARATOR);
			}
			g.drawString(reading, x, baseline);
			x += metrics.stringWidth(reading);
		}
	}

	/** Draws five stacked segments, green at the bottom and red at the top. */
	private void paintSpeedBar(final Graphics2D g, final int x, final int y) {
		final int segments = BAR_COLORS.length;
		final int height = 12;
		final int gap = 3;
		final int lit = (int) Math.ceil(segments * game.mower().speed() / Mower.MAX_SPEED);

		for (int i = 0; i < segments; i++) {
			g.setColor(i < lit ? BAR_COLORS[i] : BAR_EMPTY);
			g.fillRect(x, y + (segments - 1 - i) * (height + gap), 40, height);
		}
	}

	private void paintResult(final Graphics2D g) {
		final Lawn lawn = game.lawn();
		g.setColor(new Color(0, 0, 0, 170));
		g.fillRect(0, 0, (int) lawn.width(), (int) lawn.height());

		g.setColor(Color.WHITE);
		g.setFont(getFont().deriveFont(Font.BOLD, 36f));
		drawCentred(g, "Time's up", lawn.height() / 2 - 60);
		g.setFont(getFont().deriveFont(Font.PLAIN, 20f));
		drawCentred(g, String.format("mowed %.1f%%", 100 * lawn.mowedFraction()),
				lawn.height() / 2 - 10);
		drawCentred(g, String.format("%d squirrel(s) run over: -%.0f%%",
				game.hits(), 100 * game.hits() * Game.PENALTY_PER_SQUIRREL), lawn.height() / 2 + 20);
		g.setFont(getFont().deriveFont(Font.BOLD, 28f));
		drawCentred(g, String.format("score %.1f%%", 100 * game.score()), lawn.height() / 2 + 70);
	}

	private void drawCentred(final Graphics2D g, final String text, final double baselineY) {
		final FontMetrics metrics = g.getFontMetrics();
		final int x = (int) ((game.lawn().width() - metrics.stringWidth(text)) / 2);
		g.drawString(text, x, (int) baselineY);
	}
}
