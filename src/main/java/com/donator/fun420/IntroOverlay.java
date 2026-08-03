package com.donator.fun420;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the login message in the middle of the screen. It decides nothing
 * itself: whether to draw comes from the config and from {@link IntroState},
 * and how solid it is comes from {@link IntroState#opacity(LocalDateTime)}.
 * The plugin is injected rather than the state, so that the overlay is
 * guaranteed to read the same instance the plugin updates.
 * <p>
 * Deliberately independent of the banner bar: the two are separate settings,
 * so switching the bar off must leave this message alone.
 * <p>
 * Scoped to one instance per plugin injector, for the same reason as
 * {@link BannerOverlay}: unscoped, Guice hands out a fresh overlay per
 * injection point, so a second injection point would silently get an overlay
 * that is not the one the plugin registered and renders.
 */
@Singleton
public class IntroOverlay extends Overlay
{
	private static final float TEXT_SIZE = 40f;
	private static final int OUTLINE_OFFSET = 3;
	private static final int MAX_ALPHA = 255;

	private final Client client;
	private final Fun420Config config;
	private final Fun420Plugin plugin;

	@Inject
	IntroOverlay(Client client, Fun420Config config, Fun420Plugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Must follow setPosition, which assigns movable itself. Undraggable
		// keeps preferredLocation null, which is what keeps the graphics
		// untranslated and the message centred on the canvas.
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.bannerIntroEnabled())
		{
			return null;
		}

		LocalDateTime now = plugin.getClock().now();
		IntroState state = plugin.getIntroState();
		if (!state.isActive(now))
		{
			return null;
		}

		double opacity = state.opacity(now);
		Color base = config.bannerColor();

		Font originalFont = graphics.getFont();
		graphics.setFont(originalFont.deriveFont(Font.BOLD, TEXT_SIZE));

		String text = config.bannerText();
		FontMetrics metrics = graphics.getFontMetrics();
		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		int textX = (width - metrics.stringWidth(text)) / 2;
		// drawString takes a baseline, so centring needs the ascent and descent.
		int textY = (height / 2) + ((metrics.getAscent() - metrics.getDescent()) / 2);

		// Outline first, so the text sits on top of it. It carries the alpha of
		// the configured colour as well as the fade: given only the fade, a
		// colour dialled down to a quarter would leave a near solid black
		// outline around barely visible text, and the outline becomes the
		// message.
		graphics.setColor(withOpacity(new Color(0, 0, 0, base.getAlpha()), opacity));
		drawOutline(graphics, text, textX, textY);
		graphics.setColor(withOpacity(base, opacity));
		graphics.drawString(text, textX, textY);

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

	/**
	 * Scales the alpha of a colour by the fade, so that a colour the player
	 * dialled down stays dialled down. Clamped because {@link Color} rejects
	 * an alpha outside 0..255.
	 */
	private static Color withOpacity(Color base, double opacity)
	{
		int alpha = (int) Math.round(base.getAlpha() * opacity);
		alpha = Math.max(0, Math.min(MAX_ALPHA, alpha));
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
