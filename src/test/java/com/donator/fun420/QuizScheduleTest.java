package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuizScheduleTest
{
	private static final Duration INTERVAL = Duration.ofMinutes(15);
	private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 12, 0, 0);

	@Test
	public void aTimerThatWasNeverStartedIsNeverDue()
	{
		QuizSchedule schedule = new QuizSchedule();

		assertFalse(schedule.isDue(START.plusHours(8), INTERVAL));
	}

	@Test
	public void nothingIsDueAtTheMomentTheWaitStarts()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertFalse(schedule.isDue(START, INTERVAL));
	}

	@Test
	public void notDueOneMillisecondShortOfTheInterval()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertFalse(schedule.isDue(START.plus(INTERVAL).minusNanos(1_000_000L), INTERVAL));
	}

	@Test
	public void dueTheMomentTheWholeIntervalHasPassed()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertTrue(schedule.isDue(START.plus(INTERVAL), INTERVAL));
	}

	@Test
	public void stillDueLongAfterTheInterval()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertTrue(schedule.isDue(START.plusHours(3), INTERVAL));
	}

	@Test
	public void anIntervalOfNothingIsDueStraightAway()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertTrue(schedule.isDue(START, Duration.ZERO));
	}

	@Test
	public void aFinishedRunPushesTheWaitForward()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);
		schedule.restart(START.plusMinutes(10));

		assertFalse("the wait runs from the finish, not from the first start",
			schedule.isDue(START.plus(INTERVAL), INTERVAL));
		assertTrue(schedule.isDue(START.plusMinutes(25), INTERVAL));
	}

	@Test
	public void aTimerThatIsAlreadyRunningIsLeftAlone()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);
		schedule.startIfNotRunning(START.plusMinutes(10));

		assertTrue("a tick must not push the wait back", schedule.isDue(START.plus(INTERVAL), INTERVAL));
	}

	@Test
	public void theTimerDoesNotRunWhileThePlayerIsAway()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);
		schedule.reset();

		assertFalse("a night offline must not pile up questions",
			schedule.isDue(START.plusHours(8), INTERVAL));
	}

	@Test
	public void comingBackStartsAWholeIntervalOver()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);
		schedule.reset();

		LocalDateTime backInGame = START.plusHours(8);
		schedule.startIfNotRunning(backInGame);

		assertFalse(schedule.isDue(backInGame.plus(INTERVAL).minusNanos(1_000_000L), INTERVAL));
		assertTrue(schedule.isDue(backInGame.plus(INTERVAL), INTERVAL));
	}

	@Test
	public void aClockThatJumpedBackPastTheWaitIsNotDue()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		assertFalse(schedule.isDue(START.minusMinutes(30), INTERVAL));
	}

	@Test
	public void aClockThatJumpedBackStartsTheWaitOverOnTheNextTick()
	{
		QuizSchedule schedule = new QuizSchedule();
		schedule.restart(START);

		// A timer running from a moment in the future is no timer at all, so
		// the next tick takes it over rather than leaving the quiz suspended
		// until the clock has caught up.
		LocalDateTime afterJump = START.minusHours(1);
		schedule.startIfNotRunning(afterJump);

		assertFalse(schedule.isDue(afterJump.plus(INTERVAL).minusNanos(1_000_000L), INTERVAL));
		assertTrue(schedule.isDue(afterJump.plus(INTERVAL), INTERVAL));
	}
}
