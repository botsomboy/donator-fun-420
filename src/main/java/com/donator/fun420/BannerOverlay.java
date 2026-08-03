package com.donator.fun420;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the April 20 banner with its close button. It decides nothing itself:
 * whether to draw comes from the config and from {@link Fun420Plugin#isBannerVisible()}.
 * The bounds of the close button are published so that the plugin can hit test
 * a click against them; they are empty whenever the banner is not on screen.
 * <p>
 * Scoped to one instance per plugin injector. Without that scope Guice hands
 * out a fresh overlay per injection point, so {@link BannerMouseListener} would
 * hit test against an overlay that is never rendered and whose close button is
 * therefore always empty.
 */
@Singleton
public class BannerOverlay extends Overlay
{
	private static final int HEIGHT = 72;
	private static final int PADDING = 20;
	private static final int CLOSE_SIZE = 30;
	private static final int CLOSE_MARGIN = 20;
	private static final float TEXT_SIZE = 38f;
	private static final int OUTLINE_OFFSET = 2;
	private static final int CLOSE_STROKE_WIDTH = 3;

	/**
	 * How far the drawn cross bleeds past its own endpoints: half the stroke,
	 * rounded up. The click target has to cover that bleed, or the outer pixels
	 * of the visible cross are not clickable.
	 */
	private static final int CLOSE_STROKE_BLEED = (CLOSE_STROKE_WIDTH + 1) / 2;

	/** The width never varies and a stroke is immutable, so it is built once instead of per frame. */
	private static final Stroke CLOSE_STROKE = new BasicStroke(CLOSE_STROKE_WIDTH);

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

		// Full width, so April 20 does not whisper.
		int bannerWidth = client.getCanvasWidth();

		graphics.setColor(config.bannerColor());
		graphics.fillRect(0, 0, bannerWidth, HEIGHT);
		graphics.setColor(Color.BLACK);
		graphics.drawRect(0, 0, bannerWidth - 1, HEIGHT - 1);

		int closeX = bannerWidth - CLOSE_SIZE - CLOSE_MARGIN;
		int closeY = (HEIGHT - CLOSE_SIZE) / 2;

		// Centred between the left padding and the cross rather than on the
		// canvas: a text that just fits the free room would otherwise still
		// end up half under the cross and be clipped away.
		int textAreaX = PADDING;
		int textAreaWidth = Math.max(0, closeX - PADDING - textAreaX);
		// Centred while it fits, and against the left edge once it does not, so
		// that an overlong text loses its tail instead of its opening words.
		int textX = textAreaX + Math.max(0, (textAreaWidth - metrics.stringWidth(text)) / 2);
		int textY = ((HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();

		// A configured text longer than the free room is clipped to that room,
		// so it can neither run under the cross nor off the canvas.
		Shape originalClip = graphics.getClip();
		graphics.clipRect(textAreaX, 0, textAreaWidth, HEIGHT);
		graphics.setColor(Color.BLACK);
		drawOutline(graphics, text, textX, textY);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, textX, textY);
		graphics.setClip(originalClip);

		// Grown by the bleed of the stroke, so that every pixel of the cross
		// the player can see is a pixel the player can click.
		closeButton = new Rectangle(
			closeX - CLOSE_STROKE_BLEED,
			closeY - CLOSE_STROKE_BLEED,
			CLOSE_SIZE + (CLOSE_STROKE_BLEED * 2),
			CLOSE_SIZE + (CLOSE_STROKE_BLEED * 2));
		Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(CLOSE_STROKE);
		// Still the text colour: the cross reads as part of the banner text.
		graphics.drawLine(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE);
		graphics.drawLine(closeX + CLOSE_SIZE, closeY, closeX, closeY + CLOSE_SIZE);
		graphics.setStroke(originalStroke);

		graphics.setFont(originalFont);
		return null;
	}

	/**
	 * Draws the text once at each of the eight surrounding offsets, which
	 * gives an outline all the way around. A single offset would only be a
	 * drop shadow, leaving the top left of the glyphs to blend into a light
	 * background. The caller sets the colour.
	 */
	private static void drawOutline(Graphics2D graphics, String text, int x, int y)
	{
		for (int dx = -OUTLINE_OFFSET; dx <= OUTLINE_OFFSET; dx += OUTLINE_OFFSET)
		{
			for (int dy = -OUTLINE_OFFSET; dy <= OUTLINE_OFFSET; dy += OUTLINE_OFFSET)
			{
				if (dx != 0 || dy != 0)
				{
					graphics.drawString(text, x + dx, y + dy);
				}
			}
		}
	}
}
