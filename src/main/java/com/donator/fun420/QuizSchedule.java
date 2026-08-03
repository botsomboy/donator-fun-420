package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Decides when the next question is due. It holds one moment: when the current
 * wait began. Whether enough of the interval has gone by is the only question
 * it answers, which keeps that decision out of the plugin and provable without
 * a client, the same way {@link AlarmState} and {@link QuizState} are.
 * <p>
 * The wait only runs while the player is in the game, and it starts over from
 * zero once a question is done. Both are why the moment is kept here rather
 * than reckoned from a fixed grid: a grid would greet a player who returns
 * after a night offline with a burst of questions to make up for the ones that
 * were "missed", instead of one question after a full interval.
 * <p>
 * Volatile for the same reason the sibling states are: the moment is written
 * from the thread that ticks the game and may be read from another.
 */
final class QuizSchedule
{
	/** When the current wait began, or null while the timer is not running. */
	private volatile LocalDateTime waitingSince;

	/**
	 * Starts the wait over from this moment. Used when a question is done, so
	 * that the next one is a full interval away from the end of the last, and
	 * held at the current moment for as long as a question is on screen.
	 */
	void restart(LocalDateTime now)
	{
		waitingSince = now;
	}

	/**
	 * Starts the wait unless it is already running, so that the plugin can
	 * call it on every tick the player is in the game without ever pushing the
	 * next question further away.
	 * <p>
	 * A wait running from a moment after {@code now} counts as not running: a
	 * clock that jumped backwards would otherwise leave the quiz suspended
	 * until it had caught up, which for an hour off the clock is an hour of
	 * silence. Starting over costs one interval instead.
	 */
	void startIfNotRunning(LocalDateTime now)
	{
		LocalDateTime since = waitingSince;
		if (since == null || since.isAfter(now))
		{
			waitingSince = now;
		}
	}

	/**
	 * Stops the timer and forgets it, for a player who is no longer in the
	 * game. Nothing is due until the wait is started again, which is what
	 * keeps the timer from running while logged out.
	 */
	void reset()
	{
		waitingSince = null;
	}

	/**
	 * Whether a new question is due.
	 *
	 * @param now      the moment on the real calendar; see {@link Fun420Clock#realNow()}
	 * @param interval how long a whole wait lasts
	 * @return whether at least the interval has passed since the wait began.
	 *         False while the timer is not running, and false for a moment
	 *         before the wait began, which is how the siblings read a clock
	 *         that has jumped back; the next tick starts the wait over.
	 */
	boolean isDue(LocalDateTime now, Duration interval)
	{
		LocalDateTime since = waitingSince;
		if (since == null || now.isBefore(since))
		{
			return false;
		}

		return Duration.between(since, now).compareTo(interval) >= 0;
	}
}
