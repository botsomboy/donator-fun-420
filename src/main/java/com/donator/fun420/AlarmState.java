package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks whether the alarm is running. The alarm always lasts 60 seconds from
 * the moment it started, even when that moment was halfway into the 420 minute.
 */
public class AlarmState
{
	private static final Duration ALARM_DURATION = Duration.ofSeconds(60);
	private static final long PULSE_PERIOD_MILLIS = 1000L;

	private volatile LocalDateTime startedAt;

	/**
	 * Starts the alarm when the clock enters the window while the player is
	 * logged in, or when the player logs in while the clock is already inside
	 * the window. Logging out clears the state, so logging back in within the
	 * same window yields a fresh minute.
	 */
	public void update(LocalDateTime now, boolean loggedIn)
	{
		if (!loggedIn)
		{
			reset();
			return;
		}

		if (Fun420Clock.isAlarmWindow(now) && !isActive(now))
		{
			start(now);
		}
	}

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
	 * Judges a start moment that the caller already read, so that a caller
	 * needing both the moment and the verdict reads the field only once.
	 */
	private static boolean isActive(LocalDateTime start, LocalDateTime now)
	{
		return start != null
			&& !now.isBefore(start)
			&& Duration.between(start, now).compareTo(ALARM_DURATION) < 0;
	}

	/**
	 * Value between 0 and 1 that rises and falls once per second; the overlay
	 * translates it into an alpha.
	 */
	public double pulse(LocalDateTime now)
	{
		LocalDateTime start = startedAt;
		if (!isActive(start, now))
		{
			return 0.0;
		}

		long millis = Duration.between(start, now).toMillis();
		double phase = (millis % PULSE_PERIOD_MILLIS) / (double) PULSE_PERIOD_MILLIS;
		return (1.0 - Math.cos(2.0 * Math.PI * phase)) / 2.0;
	}
}
