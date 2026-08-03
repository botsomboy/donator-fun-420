package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the quiz: it owns the deck, the timer and the box, and decides on each
 * tick whether a question is due. Deliberately free of RuneLite types, so that
 * the rules can be proven without a client. They are rules that live in the
 * order of a handful of calls, and an order is exactly what a reader cannot
 * check by eye:
 * <ul>
 * <li>the wait only runs while the player is in the game and the quiz is on;
 * <li>it starts over when a question is done, not when one begins;
 * <li>no second question starts while one is on screen.
 * </ul>
 * <p>
 * The plugin hands over the moment, the two facts it knows about the client
 * and the four settings, and keeps nothing of its own.
 * <p>
 * Client thread only, because {@link QuizDeck#draw()} is. The state it
 * publishes is read by the render thread, which is safe: every field of it is
 * volatile and the questions are immutable.
 */
@Slf4j
final class QuizDirector
{
	/**
	 * The shortest wait that is served. The settings panel will not offer
	 * less, but the config file can be edited by hand, and an interval of
	 * nothing would put a question on screen on every tick for ever. Clamped
	 * rather than rejected, in the same spirit as {@link QuizState}, which
	 * serves a negative duration as no time at all.
	 */
	private static final Duration MINIMUM_INTERVAL = Duration.ofMinutes(1);

	private final QuizDeck deck;
	private final QuizSchedule schedule = new QuizSchedule();
	private final QuizState state = new QuizState();

	QuizDirector(QuizDeck deck)
	{
		if (deck == null)
		{
			throw new IllegalArgumentException("the quiz needs a deck to deal from");
		}
		this.deck = deck;
	}

	/** The box the overlay draws. */
	QuizState getState()
	{
		return state;
	}

	/**
	 * Judges one tick and puts a question on screen if one is due.
	 *
	 * @param now      the moment on the real calendar; see {@link Fun420Clock#realNow()}
	 * @param inGame   whether the player is in the game
	 * @param enabled  whether the quiz is switched on
	 * @param interval how long a whole wait lasts; less than a minute is
	 *                 served as a minute
	 * @param thinking how long the countdown bar takes to drain
	 * @param answer   how long the answer stays up
	 */
	void onTick(LocalDateTime now, boolean inGame, boolean enabled, Duration interval,
		Duration thinking, Duration answer)
	{
		if (!inGame || !enabled)
		{
			// Both are the quiz not running at all, so both take the box with
			// them as well as the timer. Coming back therefore costs a whole
			// interval: a timer that kept running while the player was away
			// would greet the next login with a question, and after a night
			// away with every question it had "missed".
			reset();
			return;
		}

		if (state.isActive(now))
		{
			// The wait for the next question begins when this one is gone, so
			// the timer is held at the current moment for as long as a box is
			// on screen. It is also what makes a second run impossible: this
			// returns before anything can be dealt.
			schedule.restart(now);
			return;
		}

		// Starts the wait on the first tick in the game and leaves a running
		// one alone, so that the first question of a session is a whole
		// interval away rather than immediate.
		schedule.startIfNotRunning(now);

		if (schedule.isDue(now, atLeastAMinute(interval)))
		{
			show(now, thinking, answer);
		}
	}

	/**
	 * Puts a question on screen whatever the timer says, for the test switch
	 * in the settings. The wait starts over from here, exactly as it would
	 * have for a question the timer dealt itself.
	 */
	void showNow(LocalDateTime now, Duration thinking, Duration answer)
	{
		show(now, thinking, answer);
	}

	/**
	 * Clears the box and stops the timer, for a player who has left the game
	 * and for a plugin that is shutting down. Nothing is due until the wait is
	 * started again by a tick in the game.
	 */
	void reset()
	{
		state.reset();
		schedule.reset();
	}

	private void show(LocalDateTime now, Duration thinking, Duration answer)
	{
		state.start(now, deck.draw(), thinking, answer);
		schedule.restart(now);
		log.debug("420 quiz question shown at {}", now);
	}

	private static Duration atLeastAMinute(Duration interval)
	{
		return interval.compareTo(MINIMUM_INTERVAL) < 0 ? MINIMUM_INTERVAL : interval;
	}
}
