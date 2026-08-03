package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks whether the login message is on screen. It runs for five seconds from
 * the moment it started and fades out over the last of those seconds. Nothing
 * is remembered beyond that, so every login on April 20 shows it again.
 */
public class IntroState
{
	private static final Duration INTRO_DURATION = Duration.ofSeconds(5);
	private static final long FADE_MILLIS = 1000L;

	private volatile LocalDateTime startedAt;

	/** Anchors a fresh five seconds, also while a previous one is still fading. */
	public void start(LocalDateTime now)
	{
		startedAt = now;
	}

	public void reset()
	{
		startedAt = null;
	}

	public boolean isActive(LocalDateTime now)
	{
		return isActive(startedAt, now);
	}

	/**
	 * Judges a start moment the caller already read, so that {@link #opacity}
	 * measures against the very moment it just checked. Reading the field
	 * again there could pick up a restart in between and put the fade on a
	 * moment that was never judged.
	 */
	private static boolean isActive(LocalDateTime start, LocalDateTime now)
	{
		return start != null
			&& !now.isBefore(start)
			&& Duration.between(start, now).compareTo(INTRO_DURATION) < 0;
	}

	/**
	 * Value between 0 and 1 that the overlay translates into an alpha: full
	 * until the last second begins, then falling linearly to zero. Zero
	 * whenever the message is not up.
	 */
	public double opacity(LocalDateTime now)
	{
		LocalDateTime start = startedAt;
		if (!isActive(start, now))
		{
			return 0.0;
		}

		long remaining = INTRO_DURATION.toMillis() - Duration.between(start, now).toMillis();
		if (remaining >= FADE_MILLIS)
		{
			return 1.0;
		}

		return remaining / (double) FADE_MILLIS;
	}
}
