package com.donator.fun420;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the pulsing border and the alarm message. It decides nothing itself:
 * whether to draw comes from the config and from {@link AlarmState}, and how
 * bright the border is comes from {@link AlarmState#pulse(LocalDateTime)}.
 * The plugin is injected rather than the state, so that the overlay is
 * guaranteed to read the same instance the plugin updates.
 * <p>
 * Scoped to one instance per plugin injector, for the same reason as
 * {@link BannerOverlay}: unscoped, Guice hands out a fresh overlay per
 * injection point, so a second injection point would silently get an overlay
 * that is not the one the plugin registered and renders.
 */
@Singleton
public class AlarmOverlay extends Overlay
{
	private static final int BORDER_WIDTH = 12;
	private static final float TEXT_SIZE = 36f;
	private static final int SHADOW_OFFSET = 2;
	private static final int MAX_ALPHA = 255;

	/**
	 * Share of the configured alpha that survives the trough of the pulse, so
	 * that the border dims without disappearing. The pulse scales the alpha the
	 * player picked; at the default opaque colour this is a 64..255 swing.
	 */
	private static final double TROUGH_SHARE = 0.25;

	/** The width never varies and a stroke is immutable, so it is built once instead of per frame. */
	private static final Stroke BORDER_STROKE = new BasicStroke(BORDER_WIDTH);

	private final Client client;
	private final Fun420Config config;
	private final Fun420Plugin plugin;

	@Inject
	AlarmOverlay(Client client, Fun420Config config, Fun420Plugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Must follow setPosition, which assigns movable itself. Undraggable
		// keeps preferredLocation null, which is what keeps the graphics
		// untranslated and drawing at (0,0) on the canvas top left.
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.alarmEnabled())
		{
			return null;
		}

		LocalDateTime now = plugin.getClock().now();
		AlarmState state = plugin.getAlarmState();
		if (!state.isActive(now))
		{
			return null;
		}

		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		Color textColor = config.alarmColor();
		Color borderColor = withPulseAlpha(textColor, state.pulse(now));

		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(borderColor);
		graphics.setStroke(BORDER_STROKE);
		graphics.drawRect(
			BORDER_WIDTH / 2,
			BORDER_WIDTH / 2,
			width - BORDER_WIDTH,
			height - BORDER_WIDTH);
		graphics.setStroke(originalStroke);

		Font originalFont = graphics.getFont();
		graphics.setFont(originalFont.deriveFont(Font.BOLD, TEXT_SIZE));

		String text = config.alarmText();
		FontMetrics metrics = graphics.getFontMetrics();
		int textX = (width - metrics.stringWidth(text)) / 2;
		// drawString takes a baseline, so centring needs the ascent and descent.
		int textY = (height / 2) + ((metrics.getAscent() - metrics.getDescent()) / 2);

		// The message keeps the configured alpha instead of the pulsing one:
		// text that fades to the trough is unreadable half of every second.
		graphics.setColor(new Color(0, 0, 0, textColor.getAlpha()));
		graphics.drawString(text, textX + SHADOW_OFFSET, textY + SHADOW_OFFSET);
		graphics.setColor(textColor);
		graphics.drawString(text, textX, textY);

		graphics.setFont(originalFont);
		return null;
	}

	/**
	 * Scales the alpha the player configured by the pulse, so that a dialled
	 * down colour stays dialled down. Clamped because {@link Color} rejects an
	 * alpha outside 0..255.
	 */
	private static Color withPulseAlpha(Color base, double pulse)
	{
		double share = TROUGH_SHARE + ((1.0 - TROUGH_SHARE) * pulse);
		int alpha = (int) Math.round(base.getAlpha() * share);
		alpha = Math.max(0, Math.min(MAX_ALPHA, alpha));
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
