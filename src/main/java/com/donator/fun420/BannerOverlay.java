package com.donator.fun420;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the April 20 banner with its close button. It decides nothing itself:
 * whether to draw comes from the config and from {@link Fun420Plugin#isBannerVisible()}.
 * The bounds of the close button are published so that the plugin can hit test
 * a click against them; they are empty whenever the banner is not on screen.
 */
public class BannerOverlay extends Overlay
{
	private static final int HEIGHT = 28;
	private static final int PADDING = 10;
	private static final int CLOSE_SIZE = 14;
	private static final int CLOSE_MARGIN = 8;
	private static final float TEXT_SIZE = 16f;

	/** Published while the banner is not drawn, so any hit test misses. */
	private static final Rectangle NO_CLOSE_BUTTON = new Rectangle();

	private final Client client;
	private final Fun420Config config;
	private final Fun420Plugin plugin;

	/**
	 * Snapshot of the close button, replaced rather than mutated: it is written
	 * while rendering and read from the thread that handles the click.
	 */
	private volatile Rectangle closeButton = NO_CLOSE_BUTTON;

	@Inject
	BannerOverlay(Client client, Fun420Config config, Fun420Plugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Must follow setPosition, which assigns movable itself. Undraggable
		// keeps preferredLocation null, which is what keeps the graphics
		// untranslated and these bounds in canvas coordinates.
		setMovable(false);
	}

	/**
	 * @return the close button in canvas coordinates, empty while the banner is
	 *         not drawn. Callers must not mutate the returned rectangle; it is
	 *         the live snapshot, not a copy.
	 */
	Rectangle getCloseButtonBounds()
	{
		return closeButton;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		closeButton = NO_CLOSE_BUTTON;

		if (!config.bannerEnabled() || !plugin.isBannerVisible())
		{
			return null;
		}

		Font originalFont = graphics.getFont();
		graphics.setFont(originalFont.deriveFont(Font.BOLD, TEXT_SIZE));

		String text = config.bannerText();
		FontMetrics metrics = graphics.getFontMetrics();

		int canvasWidth = client.getCanvasWidth();
		int bannerWidth = Math.min(
			canvasWidth,
			metrics.stringWidth(text) + (PADDING * 2) + CLOSE_SIZE + CLOSE_MARGIN);
		int x = (canvasWidth - bannerWidth) / 2;

		graphics.setColor(config.bannerColor());
		graphics.fillRect(x, 0, bannerWidth, HEIGHT);
		graphics.setColor(Color.BLACK);
		graphics.drawRect(x, 0, bannerWidth - 1, HEIGHT - 1);

		int textY = ((HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
		graphics.setColor(Color.WHITE);

		// A banner clamped to the canvas width has less room than the text
		// needs; clip it to its own area so it cannot run under the cross.
		int textWidth = bannerWidth - (PADDING * 2) - CLOSE_SIZE - CLOSE_MARGIN;
		Shape originalClip = graphics.getClip();
		graphics.clipRect(x + PADDING, 0, textWidth, HEIGHT);
		graphics.drawString(text, x + PADDING, textY);
		graphics.setClip(originalClip);

		int closeX = x + bannerWidth - CLOSE_SIZE - CLOSE_MARGIN;
		int closeY = (HEIGHT - CLOSE_SIZE) / 2;
		closeButton = new Rectangle(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
		// Still the text colour: the cross reads as part of the banner text.
		graphics.drawLine(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE);
		graphics.drawLine(closeX + CLOSE_SIZE, closeY, closeX, closeY + CLOSE_SIZE);

		graphics.setFont(originalFont);
		return null;
	}
}
