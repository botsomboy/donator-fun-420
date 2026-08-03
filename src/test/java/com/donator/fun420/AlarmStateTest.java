package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlarmStateTest
{
	private static LocalDateTime at(int hour, int minute, int second)
	{
		return LocalDateTime.of(2026, 8, 3, hour, minute, second);
	}

	@Test
	public void isInactiveInitially()
	{
		assertFalse(new AlarmState().isActive(at(16, 20, 0)));
	}

	@Test
	public void startsWhenTheWindowIsEnteredWhileLoggedIn()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 19, 59), true);
		assertFalse(state.isActive(at(16, 19, 59)));

		state.update(at(16, 20, 0), true);
		assertTrue(state.isActive(at(16, 20, 0)));
	}

	@Test
	public void doesNotStartWhileLoggedOut()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 20, 0), false);
		assertFalse(state.isActive(at(16, 20, 0)));
	}

	@Test
	public void runsForSixtySecondsFromTheTrigger()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 20, 40), true);

		assertTrue(state.isActive(at(16, 21, 39)));
		assertFalse(state.isActive(at(16, 21, 40)));
	}

	@Test
	public void keepsRunningAfterTheClockLeavesTheWindow()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 20, 40), true);
		state.update(at(16, 21, 10), true);

		assertTrue(state.isActive(at(16, 21, 39)));
		assertFalse(state.isActive(at(16, 21, 40)));
	}

	@Test
	public void startsAgainAtTheNextWindow()
	{
		AlarmState state = new AlarmState();
		state.update(at(4, 20, 0), true);
		state.update(at(4, 21, 30), true);

		state.update(at(16, 20, 0), true);
		assertTrue(state.isActive(at(16, 20, 30)));
	}

	@Test
	public void doesNotRestartWithinTheSameWindow()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 20, 0), true);
		state.update(at(16, 20, 30), true);

		assertFalse(state.isActive(at(16, 21, 0)));
	}

	@Test
	public void loggingOutResetsAndLoggingBackInStartsAFreshMinute()
	{
		AlarmState state = new AlarmState();
		state.update(at(16, 20, 0), true);
		state.update(at(16, 20, 10), false);
		assertFalse(state.isActive(at(16, 20, 10)));

		state.update(at(16, 20, 30), true);
		assertTrue(state.isActive(at(16, 21, 29)));
		assertFalse(state.isActive(at(16, 21, 30)));
	}

	@Test
	public void startingAgainRestartsTheMinute()
	{
		AlarmState state = new AlarmState();
		state.start(at(16, 20, 0));
		state.start(at(16, 20, 30));

		assertTrue(state.isActive(at(16, 21, 29)));
		assertFalse(state.isActive(at(16, 21, 30)));
	}

	@Test
	public void manualStartWorksOutsideTheWindow()
	{
		AlarmState state = new AlarmState();
		state.start(at(9, 0, 0));

		assertTrue(state.isActive(at(9, 0, 59)));
		assertFalse(state.isActive(at(9, 1, 0)));
	}

	@Test
	public void pulseStaysBetweenZeroAndOneForTheWholeMinute()
	{
		AlarmState state = new AlarmState();
		state.start(at(16, 20, 0));

		for (int step = 0; step < 600; step++)
		{
			double pulse = state.pulse(at(16, 20, 0).plus(Duration.ofMillis(step * 100L)));
			assertTrue("pulse too low at " + step, pulse >= 0.0);
			assertTrue("pulse too high at " + step, pulse <= 1.0);
		}
	}

	@Test
	public void pulseOscillatesWithinASecond()
	{
		AlarmState state = new AlarmState();
		LocalDateTime start = at(16, 20, 0);
		state.start(start);

		assertTrue("pulse should be dark at the start of a cycle",
			state.pulse(start) < 0.01);
		assertTrue("pulse should be bright halfway through a cycle",
			state.pulse(start.plus(Duration.ofMillis(500))) > 0.99);
	}

	@Test
	public void pulseIsZeroWhenInactive()
	{
		assertEquals(0.0, new AlarmState().pulse(at(16, 20, 0)), 0.0001);
	}

	@Test
	public void pulseIsZeroAfterTheAlarmExpired()
	{
		AlarmState state = new AlarmState();
		state.start(at(16, 20, 0));
		assertEquals(0.0, state.pulse(at(16, 21, 0).plus(Duration.ofMillis(500))), 0.0001);
	}

	@Test
	public void isInactiveWhenTheClockJumpsBackwards()
	{
		AlarmState state = new AlarmState();
		state.start(at(16, 20, 30));

		assertFalse(state.isActive(at(15, 20, 30)));
	}
}
