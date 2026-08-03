package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks where a quiz box is in its run. A run has four phases in this order:
 * the box slides in, the bar drains through the thinking time, the answer is
 * shown for the answer time, and the whole thing fades out.
 * <p>
 * Both texts arrive as if blown in on a puff of smoke: one plume rides along
 * with the box as it slides in, and a second lands with the answer. How far
 * either has cleared is a number between 0 and 1 reckoned here, so the smoke
 * is as testable as the slide and the countdown; the overlay only draws it.
 * <p>
 * The slide, the fade and the plumes are presentation constants; the thinking
 * and answer times come from the config and are handed over at {@link #start}.
 * Same shape as {@link AlarmState} and {@link IntroState}, so all of it is
 * testable without rendering anything.
 * <p>
 * The start moment, the question and both durations live together in one
 * immutable {@link Run} behind a single volatile reference. A render pass can
 * therefore never catch a new start moment next to the durations of the run
 * before it, which several volatile fields would allow.
 */
public final class QuizState
{
	/** How long the box takes to slide in from the edge of the screen. */
	private static final long SLIDE_MILLIS = 400L;

	/** How long the box takes to fade out once the answer time is up. */
	private static final long FADE_MILLIS = 600L;

	/**
	 * How long a plume of smoke takes to swell, drift off and clear. Longer
	 * than the slide on purpose: the smoke has to still be hanging when the
	 * box lands, or the text was slid in rather than blown in.
	 */
	private static final long PLUME_MILLIS = 900L;

	/** What a plume reads when there is no smoke to draw. */
	private static final double CLEARED = 1.0;

	private volatile Run run;

	/**
	 * Anchors a fresh run, also while a previous one is still on screen.
	 *
	 * @param now      the moment the box starts sliding in
	 * @param question the pair to show; never null
	 * @param thinking how long the bar takes to drain; a negative value counts as none
	 * @param answer   how long the answer stays up; a negative value counts as none
	 */
	public void start(LocalDateTime now, QuizQuestion question, Duration thinking, Duration answer)
	{
		if (now == null || question == null || thinking == null || answer == null)
		{
			throw new IllegalArgumentException("a quiz needs a moment, a question and two durations");
		}

		run = new Run(now, question, millisOf(thinking), millisOf(answer));
	}

	/** Negative is not a length of time; it counts as no time at all. */
	private static long millisOf(Duration duration)
	{
		return Math.max(0L, duration.toMillis());
	}

	public void reset()
	{
		run = null;
	}

	/** Whether the box is on screen. False once the fade has finished. */
	public boolean isActive(LocalDateTime now)
	{
		Run current = run;
		return current != null && current.isActive(now);
	}

	/**
	 * The question of the run that was started last, or null when none was
	 * started or after a {@link #reset}. Deliberately not tied to the clock:
	 * an overlay that has just been told the box is up must not then be
	 * handed nothing to draw.
	 */
	public QuizQuestion getQuestion()
	{
		Run current = run;
		return current == null ? null : current.question;
	}

	/**
	 * How far the box has slid in, from 0 at the edge of the screen to 1 in
	 * place. Stays at 1 for the rest of the run, and reads 0 while nothing is
	 * on screen.
	 */
	public double slideProgress(LocalDateTime now)
	{
		Run current = run;
		if (current == null || !current.isActive(now))
		{
			return 0.0;
		}

		long millis = current.millisSince(now);
		if (millis >= SLIDE_MILLIS)
		{
			return 1.0;
		}
		return millis / (double) SLIDE_MILLIS;
	}

	/**
	 * How much of the thinking time is left, from 1 to 0; the overlay draws it
	 * as the width of the countdown bar. Full while the box is still sliding
	 * in, because the player cannot have read the question yet, and empty from
	 * the moment the answer appears until the end of the run.
	 */
	public double thinkingRemaining(LocalDateTime now)
	{
		Run current = run;
		if (current == null || !current.isActive(now))
		{
			return 0.0;
		}

		long millis = current.millisSince(now);
		if (millis >= current.thinkingEnds())
		{
			return 0.0;
		}
		if (millis <= SLIDE_MILLIS)
		{
			return 1.0;
		}

		// Past the slide and short of the end, so there is thinking time to
		// divide by.
		return (current.thinkingEnds() - millis) / (double) current.thinkingMillis;
	}

	/**
	 * Whether the answer is up. It appears the moment the bar is empty and
	 * stays up through the fade, so the box does not go blank on its way out.
	 */
	public boolean answerVisible(LocalDateTime now)
	{
		Run current = run;
		return current != null
			&& current.isActive(now)
			&& current.millisSince(now) >= current.thinkingEnds();
	}

	/**
	 * How far the plume that the question rides in on has cleared: 0 the
	 * moment it is blown, at the very start of the run, and 1 once it has
	 * drifted away, which is a little after the box has landed. Stays at 1
	 * for the rest of the run.
	 *
	 * @see #plumeProgress
	 */
	public double questionPlumeProgress(LocalDateTime now)
	{
		Run current = run;
		if (current == null || !current.isActive(now))
		{
			return CLEARED;
		}
		return plumeProgress(current.millisSince(now), 0L);
	}

	/**
	 * How far the plume that the answer lands on has cleared: 0 the moment
	 * the answer appears and 1 once it has drifted away. Reads 1 for the
	 * whole slide and thinking time before that, because a puff that has not
	 * been blown is nothing to draw, not a fresh cloud.
	 *
	 * @see #plumeProgress
	 */
	public double answerPlumeProgress(LocalDateTime now)
	{
		Run current = run;
		if (current == null || !current.isActive(now))
		{
			return CLEARED;
		}
		return plumeProgress(current.millisSince(now), current.thinkingEnds());
	}

	/**
	 * The shared reckoning behind both plumes. The overlay draws the smoke
	 * thicker the lower this reads, so 1 is the value that paints nothing,
	 * and it is therefore what a plume reads before it is blown, after it has
	 * cleared and whenever there is no box on screen at all.
	 *
	 * @param millis  how far the run has come
	 * @param blownAt how far into the run this plume is blown
	 */
	private static double plumeProgress(long millis, long blownAt)
	{
		if (millis < blownAt)
		{
			return CLEARED;
		}

		long since = millis - blownAt;
		if (since >= PLUME_MILLIS)
		{
			return CLEARED;
		}
		return since / (double) PLUME_MILLIS;
	}

	/**
	 * Value between 0 and 1 that the overlay translates into an alpha: full
	 * until the answer time is up, then falling linearly to zero. Zero
	 * whenever nothing is on screen.
	 */
	public double opacity(LocalDateTime now)
	{
		Run current = run;
		if (current == null || !current.isActive(now))
		{
			return 0.0;
		}

		long millis = current.millisSince(now);
		if (millis < current.answerEnds())
		{
			return 1.0;
		}
		return (current.total() - millis) / (double) FADE_MILLIS;
	}

	/**
	 * One run: everything that was fixed when the box started. Immutable, so
	 * that publishing it is a single write of one reference.
	 */
	private static final class Run
	{
		private final LocalDateTime startedAt;
		private final QuizQuestion question;
		private final long thinkingMillis;
		private final long answerMillis;

		private Run(LocalDateTime startedAt, QuizQuestion question,
			long thinkingMillis, long answerMillis)
		{
			this.startedAt = startedAt;
			this.question = question;
			this.thinkingMillis = thinkingMillis;
			this.answerMillis = answerMillis;
		}

		/**
		 * A clock that jumped back past the start moment counts as nothing on
		 * screen, the same way the alarm and the login message treat it.
		 */
		private boolean isActive(LocalDateTime now)
		{
			return !now.isBefore(startedAt) && millisSince(now) < total();
		}

		private long millisSince(LocalDateTime now)
		{
			return Duration.between(startedAt, now).toMillis();
		}

		/** The moment the bar is empty and the answer takes over. */
		private long thinkingEnds()
		{
			return SLIDE_MILLIS + thinkingMillis;
		}

		/** The moment the fade begins. */
		private long answerEnds()
		{
			return thinkingEnds() + answerMillis;
		}

		private long total()
		{
			return answerEnds() + FADE_MILLIS;
		}
	}
}
