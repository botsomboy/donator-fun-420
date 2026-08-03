package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Pins the rules that used to live in the order of four calls inside the
 * plugin: the wait only runs while the player is in the game and the quiz is
 * on, it starts over when a question is done rather than when one begins, and
 * no second question starts while one is up.
 */
public class QuizDirectorTest
{
	private static final Duration INTERVAL = Duration.ofMinutes(15);
	private static final Duration THINKING = Duration.ofSeconds(10);
	private static final Duration ANSWER = Duration.ofSeconds(10);
	private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 12, 0, 0);

	/**
	 * A whole run at these durations: the slide and the fade the state adds
	 * to them are 400 ms and 600 ms, so this is a minute over two minutes.
	 */
	private static final Duration LONG_THINKING = Duration.ofSeconds(60);
	private static final Duration LONG_ANSWER = Duration.ofSeconds(60);

	private static QuizDirector director()
	{
		// A real deck over the real questions; the seed only has to be fixed
		// so that a rerun deals the same order, not a particular one.
		return new QuizDirector(new QuizDeck(QuizQuestions.ALL, new Random(20L)));
	}

	private static void tick(QuizDirector director, LocalDateTime now)
	{
		director.onTick(now, true, true, INTERVAL, THINKING, ANSWER);
	}

	@Test
	public void theFirstTickInGameAsksNothing()
	{
		QuizDirector director = director();

		tick(director, START);

		assertFalse(director.getState().isActive(START));
	}

	@Test
	public void nothingIsAskedJustShortOfTheInterval()
	{
		QuizDirector director = director();

		tick(director, START);
		tick(director, START.plus(INTERVAL).minusSeconds(1));

		assertFalse(director.getState().isActive(START.plus(INTERVAL).minusSeconds(1)));
	}

	@Test
	public void aQuestionAppearsAfterAWholeInterval()
	{
		QuizDirector director = director();

		tick(director, START);
		tick(director, START.plus(INTERVAL));

		assertTrue(director.getState().isActive(START.plus(INTERVAL)));
		assertNotNull(director.getState().getQuestion());
	}

	@Test
	public void aNightLoggedOutCostsNothingAndEarnsNothing()
	{
		QuizDirector director = director();
		tick(director, START);
		director.reset();

		LocalDateTime back = START.plusHours(8);
		tick(director, back);
		assertFalse("logging back in must not be greeted with a question",
			director.getState().isActive(back));

		tick(director, back.plus(INTERVAL).minusSeconds(1));
		assertFalse("the wait starts over on arrival, so a whole one is owed",
			director.getState().isActive(back.plus(INTERVAL).minusSeconds(1)));

		tick(director, back.plus(INTERVAL));
		assertTrue(director.getState().isActive(back.plus(INTERVAL)));
	}

	@Test
	public void aNightLoggedOutIsNotPaidBackAsABurst()
	{
		QuizDirector director = director();
		tick(director, START);
		director.reset();

		LocalDateTime back = START.plusHours(8);
		tick(director, back);
		tick(director, back.plus(INTERVAL));
		QuizQuestion first = director.getState().getQuestion();

		// The eight hours away were worth about thirty questions to a timer
		// that kept running; the next tick must still be showing the first.
		tick(director, back.plus(INTERVAL).plusSeconds(1));

		assertSame(first, director.getState().getQuestion());
	}

	@Test
	public void theTimerDoesNotRunWhileThePlayerIsAway()
	{
		QuizDirector director = director();

		director.onTick(START, false, true, INTERVAL, THINKING, ANSWER);
		director.onTick(START.plusHours(8), false, true, INTERVAL, THINKING, ANSWER);

		assertFalse(director.getState().isActive(START.plusHours(8)));
	}

	@Test
	public void noSecondQuestionStartsWhileOneIsUp()
	{
		QuizDirector director = director();
		Duration interval = Duration.ofMinutes(1);

		director.onTick(START, true, true, interval, LONG_THINKING, LONG_ANSWER);
		director.onTick(START.plusMinutes(1), true, true, interval, LONG_THINKING, LONG_ANSWER);
		QuizQuestion first = director.getState().getQuestion();
		assertNotNull("precondition: a question must be up", first);

		// A whole interval on from the one that is on screen, and that run
		// lasts over two minutes, so the timer alone would deal a second one.
		director.onTick(START.plusMinutes(2), true, true, interval, LONG_THINKING, LONG_ANSWER);

		assertTrue("precondition: the first run must still be running",
			director.getState().isActive(START.plusMinutes(2)));
		assertSame(first, director.getState().getQuestion());
	}

	@Test
	public void theNextWaitRunsFromTheEndOfARunAndNotFromItsStart()
	{
		QuizDirector director = director();
		Duration interval = Duration.ofMinutes(1);

		director.onTick(START, true, true, interval, LONG_THINKING, LONG_ANSWER);
		director.onTick(START.plusMinutes(1), true, true, interval, LONG_THINKING, LONG_ANSWER);
		QuizQuestion first = director.getState().getQuestion();

		// The last tick that saw the box on screen; the run ends a little
		// after this, at two minutes and one second past the start.
		director.onTick(START.plusSeconds(150), true, true, interval, LONG_THINKING, LONG_ANSWER);

		// A whole interval past the moment the box appeared, but only forty
		// seconds past the moment it left.
		director.onTick(START.plusSeconds(190), true, true, interval, LONG_THINKING, LONG_ANSWER);
		assertFalse("the run is over", director.getState().isActive(START.plusSeconds(190)));
		assertSame("the wait runs from the end of a run, not from its start",
			first, director.getState().getQuestion());

		director.onTick(START.plusSeconds(210), true, true, interval, LONG_THINKING, LONG_ANSWER);
		assertNotSame(first, director.getState().getQuestion());
	}

	/**
	 * Switching the quiz off is switching it off: the box goes and the timer
	 * stops, so switching it back on costs a whole interval rather than
	 * putting the half-finished box back on screen.
	 */
	@Test
	public void switchingTheQuizOffClearsTheBoxAndTheTimer()
	{
		QuizDirector director = director();
		tick(director, START);
		tick(director, START.plus(INTERVAL));
		assertTrue("precondition: a question must be up",
			director.getState().isActive(START.plus(INTERVAL)));

		LocalDateTime off = START.plus(INTERVAL).plusSeconds(1);
		director.onTick(off, true, false, INTERVAL, THINKING, ANSWER);
		assertFalse(director.getState().isActive(off));

		LocalDateTime on = off.plusHours(2);
		tick(director, on);
		assertFalse("the box must not come back mid-run", director.getState().isActive(on));

		tick(director, on.plus(INTERVAL).minusSeconds(1));
		assertFalse("switching it back on owes a whole interval",
			director.getState().isActive(on.plus(INTERVAL).minusSeconds(1)));

		tick(director, on.plus(INTERVAL));
		assertTrue(director.getState().isActive(on.plus(INTERVAL)));
	}

	@Test
	public void leavingTheGameTakesTheBoxWithIt()
	{
		QuizDirector director = director();
		tick(director, START);
		tick(director, START.plus(INTERVAL));
		assertTrue("precondition: a question must be up",
			director.getState().isActive(START.plus(INTERVAL)));

		director.reset();

		assertFalse(director.getState().isActive(START.plus(INTERVAL)));
	}

	@Test
	public void showNowAsksAQuestionWhateverTheTimerSays()
	{
		QuizDirector director = director();

		director.showNow(START, THINKING, ANSWER);

		assertTrue(director.getState().isActive(START));
		assertNotNull(director.getState().getQuestion());
	}

	@Test
	public void showNowStartsTheWaitOverAsAnyQuestionWould()
	{
		QuizDirector director = director();
		tick(director, START);

		director.showNow(START.plusMinutes(5), THINKING, ANSWER);
		QuizQuestion shown = director.getState().getQuestion();

		// A whole interval past the first tick, so a timer that the button
		// left alone would deal here.
		tick(director, START.plus(INTERVAL));
		assertSame(shown, director.getState().getQuestion());

		tick(director, START.plusMinutes(20));
		assertNotSame(shown, director.getState().getQuestion());
	}

	/**
	 * The settings panel will not offer an interval below a minute, but the
	 * config file can be edited by hand, and an interval of nothing would put
	 * a question on screen on every tick for ever. Clamped the same way
	 * {@link QuizState} clamps a negative duration to none.
	 */
	@Test
	public void anIntervalOfNothingIsServedAsAMinute()
	{
		QuizDirector director = director();

		director.onTick(START, true, true, Duration.ZERO, THINKING, ANSWER);
		director.onTick(START.plusSeconds(1), true, true, Duration.ZERO, THINKING, ANSWER);
		assertFalse(director.getState().isActive(START.plusSeconds(1)));

		director.onTick(START.plusMinutes(1), true, true, Duration.ZERO, THINKING, ANSWER);
		assertTrue(director.getState().isActive(START.plusMinutes(1)));
	}

	@Test
	public void aNegativeIntervalIsServedAsAMinuteAsWell()
	{
		QuizDirector director = director();
		Duration negative = Duration.ofMinutes(-30);

		director.onTick(START, true, true, negative, THINKING, ANSWER);
		director.onTick(START.plusSeconds(30), true, true, negative, THINKING, ANSWER);
		assertFalse(director.getState().isActive(START.plusSeconds(30)));

		director.onTick(START.plusMinutes(1), true, true, negative, THINKING, ANSWER);
		assertTrue(director.getState().isActive(START.plusMinutes(1)));
	}
}
