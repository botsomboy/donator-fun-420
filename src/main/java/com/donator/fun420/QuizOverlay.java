package com.donator.fun420;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the quiz box: the question, the countdown bar under it, the answer
 * below that, and the two puffs of smoke the texts are blown in on. It decides
 * nothing itself. Whether there is a box, how far it has slid in, how full the
 * bar is, whether the answer is up, how far either plume has cleared and how
 * solid the whole thing is all come from {@link QuizState}; which corner it
 * sits in comes from the config. Everything left here is layout arithmetic.
 * <p>
 * The clock is read once per frame and that one moment is handed to every
 * state call: two reads can fall on either side of a phase boundary and paint
 * a frame in which the bar is not yet empty while the answer is already up.
 * <p>
 * Scoped to one instance per plugin injector, for the same reason as
 * {@link BannerOverlay}: unscoped, Guice hands out a fresh overlay per
 * injection point, so a second injection point would silently get an overlay
 * that is not the one the plugin registered and renders.
 */
@Singleton
public class QuizOverlay extends Overlay
{
	private static final int BOX_WIDTH = 280;

	/** Distance between the box and the two screen edges of its corner. */
	private static final int MARGIN = 16;

	/** Distance between the edge of the box and anything inside it. */
	private static final int PADDING = 12;

	/** Vertical room above and below the countdown bar. */
	private static final int GAP = 8;

	private static final int BAR_HEIGHT = 8;
	private static final int CORNER_ARC = 14;
	private static final float TEXT_SIZE = 14f;
	private static final int MAX_ALPHA = 255;

	private static final Color BACKGROUND = new Color(18, 22, 18, 235);
	private static final Color BORDER = new Color(0, 200, 60, 255);
	private static final Color TEXT = new Color(240, 240, 240, 255);
	private static final Color BAR_TRACK = new Color(60, 60, 60, 200);
	private static final Color BAR_FILL = new Color(0, 200, 60, 255);
	private static final Color SMOKE = new Color(215, 215, 215, 190);

	/**
	 * Where each puff of a plume sits, as a share of the width and the height
	 * of what the plume covers, and how large it is relative to the others.
	 * A fixed table rather than a random draw: a puff whose place is a pure
	 * function of its index and the progress needs nothing remembered between
	 * frames, so the same moment always paints the same plume.
	 */
	private static final double[] PUFF_X = {0.14, 0.33, 0.50, 0.68, 0.86, 0.28, 0.72};
	private static final double[] PUFF_Y = {0.58, 0.32, 0.62, 0.36, 0.55, 0.78, 0.74};
	private static final double[] PUFF_SIZE = {0.90, 1.25, 1.45, 1.15, 0.85, 1.00, 0.80};

	/** Radius of an average puff, as a share of the width of the box. */
	private static final double PUFF_RADIUS_SHARE = 0.16;

	/** How large a puff starts out and how much it swells while it clears. */
	private static final double PUFF_START_SWELL = 0.70;
	private static final double PUFF_END_SWELL = 1.80;

	/** How far a cleared puff has drifted up and outwards, in average radii. */
	private static final double PUFF_DRIFT_UP = 1.30;
	private static final double PUFF_DRIFT_OUT = 0.90;

	private final Client client;
	private final Fun420Config config;
	private final Fun420Plugin plugin;

	@Inject
	QuizOverlay(Client client, Fun420Config config, Fun420Plugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Must follow setPosition, which assigns movable itself. Undraggable
		// keeps preferredLocation null, which is what keeps the graphics
		// untranslated and these coordinates on the canvas.
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.quizEnabled())
		{
			return null;
		}

		QuizState state = plugin.getQuizState();
		// One reading of the clock for the whole frame; see the class comment.
		LocalDateTime now = plugin.getClock().realNow();
		if (!state.isActive(now))
		{
			return null;
		}

		QuizQuestion question = state.getQuestion();
		if (question == null)
		{
			// A run without a question cannot happen through start(), which
			// rejects a null one; this covers a reset landing between the two
			// reads above rather than drawing an empty box.
			return null;
		}

		double opacity = state.opacity(now);
		QuizCorner corner = config.quizCorner();

		Font originalFont = graphics.getFont();
		Font questionFont = originalFont.deriveFont(Font.BOLD, TEXT_SIZE);
		Font answerFont = originalFont.deriveFont(Font.PLAIN, TEXT_SIZE);
		FontMetrics questionMetrics = graphics.getFontMetrics(questionFont);
		FontMetrics answerMetrics = graphics.getFontMetrics(answerFont);

		int textWidth = BOX_WIDTH - (PADDING * 2);
		List<String> questionLines = wrap(question.getQuestion(), questionMetrics, textWidth);
		List<String> answerLines = wrap(question.getAnswer(), answerMetrics, textWidth);

		int questionHeight = questionLines.size() * questionMetrics.getHeight();
		int answerHeight = answerLines.size() * answerMetrics.getHeight();
		// The answer keeps its room from the first frame, whether it is up or
		// not: a box that grew when the answer landed would jump, and in a
		// bottom corner it would jump upwards under the player's cursor.
		int boxHeight = (PADDING * 2) + questionHeight + GAP + BAR_HEIGHT + GAP + answerHeight;

		int restX = corner.isLeft() ? MARGIN : client.getCanvasWidth() - MARGIN - BOX_WIDTH;
		// Just past the near edge, so that a box halfway in is halfway visible.
		int edgeX = corner.isLeft() ? -BOX_WIDTH : client.getCanvasWidth();
		int boxX = (int) Math.round(edgeX + ((restX - edgeX) * state.slideProgress(now)));
		int boxY = corner.isTop()
			? MARGIN
			: client.getCanvasHeight() - MARGIN - boxHeight;

		graphics.setColor(scaleAlpha(BACKGROUND, opacity));
		graphics.fillRoundRect(boxX, boxY, BOX_WIDTH, boxHeight, CORNER_ARC, CORNER_ARC);
		graphics.setColor(scaleAlpha(BORDER, opacity));
		graphics.drawRoundRect(boxX, boxY, BOX_WIDTH - 1, boxHeight - 1, CORNER_ARC, CORNER_ARC);

		int textX = boxX + PADDING;
		int answerY = boxY + PADDING + questionHeight + GAP + BAR_HEIGHT + GAP;

		// A word too long to wrap loses its tail instead of running out of the
		// box; the authored texts are far short of that, but a clip is cheaper
		// than trusting them.
		Shape originalClip = graphics.getClip();
		graphics.clipRect(textX, boxY, textWidth, boxHeight);
		graphics.setColor(scaleAlpha(TEXT, opacity));
		graphics.setFont(questionFont);
		drawLines(graphics, questionLines, textX, boxY + PADDING, questionMetrics);
		if (state.answerVisible(now))
		{
			graphics.setFont(answerFont);
			drawLines(graphics, answerLines, textX, answerY, answerMetrics);
		}
		graphics.setClip(originalClip);

		int barY = boxY + PADDING + questionHeight + GAP;
		int barFill = (int) Math.round(textWidth * state.thinkingRemaining(now));
		graphics.setColor(scaleAlpha(BAR_TRACK, opacity));
		graphics.fillRect(textX, barY, textWidth, BAR_HEIGHT);
		graphics.setColor(scaleAlpha(BAR_FILL, opacity));
		graphics.fillRect(textX, barY, barFill, BAR_HEIGHT);

		// After the text, because the smoke is what the text becomes readable
		// through. The first plume covers the whole box and travels with it,
		// the second only the answer, which by then is standing still.
		Object originalAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawPlume(graphics, boxX, boxY, BOX_WIDTH, boxHeight,
			state.questionPlumeProgress(now), opacity);
		drawPlume(graphics, boxX, answerY, BOX_WIDTH, Math.max(answerHeight, 1),
			state.answerPlumeProgress(now), opacity);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			originalAntialiasing == null
				? RenderingHints.VALUE_ANTIALIAS_DEFAULT
				: originalAntialiasing);

		graphics.setFont(originalFont);
		return null;
	}

	/**
	 * Draws a plume over the given area.
	 *
	 * @param cleared how far the plume has cleared, 0 for a fresh cloud and 1
	 *                for one that has gone; the density is what is left of it
	 */
	private static void drawPlume(Graphics2D graphics, int x, int y, int width, int height,
		double cleared, double opacity)
	{
		double density = 1.0 - cleared;
		if (density <= 0.0)
		{
			return;
		}

		// One colour for the whole plume: every puff of it is equally thick,
		// so this is a single allocation instead of one per puff.
		graphics.setColor(scaleAlpha(SMOKE, density * opacity));

		double baseRadius = BOX_WIDTH * PUFF_RADIUS_SHARE;
		double swell = PUFF_START_SWELL + ((PUFF_END_SWELL - PUFF_START_SWELL) * cleared);

		for (int i = 0; i < PUFF_X.length; i++)
		{
			double radius = baseRadius * PUFF_SIZE[i] * swell;
			// Outwards from the middle and upwards, so the plume opens up and
			// lifts off the box as it goes.
			double centreX = x + (PUFF_X[i] * width)
				+ ((PUFF_X[i] - 0.5) * cleared * baseRadius * PUFF_DRIFT_OUT * 2.0);
			double centreY = y + (PUFF_Y[i] * height)
				- (cleared * baseRadius * PUFF_DRIFT_UP * PUFF_SIZE[i]);

			int diameter = (int) Math.round(radius * 2.0);
			graphics.fillOval(
				(int) Math.round(centreX - radius),
				(int) Math.round(centreY - radius),
				diameter,
				diameter);
		}
	}

	/** Draws the lines top down; {@code top} is the top of the first line. */
	private static void drawLines(Graphics2D graphics, List<String> lines, int x, int top,
		FontMetrics metrics)
	{
		// drawString takes a baseline, which sits an ascent below the top.
		int baseline = top + metrics.getAscent();
		for (String line : lines)
		{
			graphics.drawString(line, x, baseline);
			baseline += metrics.getHeight();
		}
	}

	/**
	 * Breaks a text into lines that fit the given width, filling each line
	 * before starting the next. A word wider than the whole width gets a line
	 * of its own and is clipped by the caller; the authored questions have no
	 * such word.
	 *
	 * @return at least one line, so that a box always has a height
	 */
	private static List<String> wrap(String text, FontMetrics metrics, int width)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();

		for (String word : text.split(" "))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (line.length() == 0)
			{
				line.append(word);
				continue;
			}

			if (metrics.stringWidth(line + " " + word) <= width)
			{
				line.append(' ').append(word);
			}
			else
			{
				lines.add(line.toString());
				line.setLength(0);
				line.append(word);
			}
		}

		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		if (lines.isEmpty())
		{
			lines.add("");
		}
		return lines;
	}

	/**
	 * Scales the alpha of a colour, so that a colour drawn at half strength
	 * keeps its own translucency. Clamped because {@link Color} rejects an
	 * alpha outside 0..255.
	 */
	private static Color scaleAlpha(Color base, double share)
	{
		int alpha = (int) Math.round(base.getAlpha() * share);
		alpha = Math.max(0, Math.min(MAX_ALPHA, alpha));
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
