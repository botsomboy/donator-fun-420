package com.donator.fun420;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntroStateTest
{
	private static final LocalDateTime START = LocalDateTime.of(2026, 4, 20, 10, 0, 0);

	private static LocalDateTime afterStart(long millis)
	{
		return START.plus(Duration.ofMillis(millis));
	}

	@Test
	public void isInactiveInitially()
	{
		assertFalse(new IntroState().isActive(START));
	}

	@Test
	public void isActiveFromTheStartMoment()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertTrue(state.isActive(START));
	}

	@Test
	public void runsForFiveSecondsFromTheStart()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertTrue("still visible just before the fifth second ends",
			state.isActive(afterStart(4999)));
		assertFalse("gone the moment the fifth second is up",
			state.isActive(afterStart(5000)));
	}

	@Test
	public void staysGoneLongAfterTheFiveSeconds()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertFalse(state.isActive(afterStart(60_000)));
	}

	@Test
	public void resetHidesItImmediately()
	{
		IntroState state = new IntroState();
		state.start(START);
		state.reset();

		assertFalse(state.isActive(afterStart(1000)));
		assertEquals(0.0, state.opacity(afterStart(1000)), 0.0001);
	}

	@Test
	public void startingAgainRestartsTheFiveSeconds()
	{
		IntroState state = new IntroState();
		state.start(START);
		state.start(afterStart(3000));

		assertTrue("the second start anchors a fresh five seconds",
			state.isActive(afterStart(7999)));
		assertFalse(state.isActive(afterStart(8000)));
	}

	@Test
	public void startingAgainDuringTheFadeRestoresFullOpacity()
	{
		IntroState state = new IntroState();
		state.start(START);
		assertTrue("half faded before the restart",
			state.opacity(afterStart(4500)) < 0.6);

		state.start(afterStart(4500));
		assertEquals(1.0, state.opacity(afterStart(4500)), 0.0001);
	}

	@Test
	public void isInactiveWhenTheClockJumpsBackwards()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertFalse(state.isActive(START.minusHours(1)));
	}

	@Test
	public void opacityIsZeroWhenTheClockJumpsBackwards()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertEquals(0.0, state.opacity(START.minusHours(1)), 0.0001);
	}

	@Test
	public void opacityIsZeroWhenInactive()
	{
		assertEquals(0.0, new IntroState().opacity(START), 0.0001);
	}

	@Test
	public void opacityIsZeroAfterTheFiveSeconds()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertEquals(0.0, state.opacity(afterStart(5000)), 0.0001);
		assertEquals(0.0, state.opacity(afterStart(9000)), 0.0001);
	}

	@Test
	public void opacityIsFullUntilTheLastSecondBegins()
	{
		IntroState state = new IntroState();
		state.start(START);

		// Sampled off the whole seconds as well: a fade that starts too early
		// only shows up between the ticks.
		for (long millis = 0; millis <= 4000; millis += 125)
		{
			assertEquals("opacity should still be full at " + millis + "ms",
				1.0, state.opacity(afterStart(millis)), 0.0001);
		}
	}

	@Test
	public void opacityFallsLinearlyThroughTheLastSecond()
	{
		IntroState state = new IntroState();
		state.start(START);

		assertEquals(1.0, state.opacity(afterStart(4000)), 0.0001);
		assertEquals(0.75, state.opacity(afterStart(4250)), 0.0001);
		assertEquals(0.5, state.opacity(afterStart(4500)), 0.0001);
		assertEquals(0.25, state.opacity(afterStart(4750)), 0.0001);
		assertEquals(0.1, state.opacity(afterStart(4900)), 0.0001);
	}

	@Test
	public void opacityKeepsFallingWithinTheLastSecond()
	{
		IntroState state = new IntroState();
		state.start(START);

		// Kills a constant opacity that only drops at the very end: every
		// sample in the fade must be strictly below the one before it.
		double previous = state.opacity(afterStart(4000));
		for (long millis = 4050; millis < 5000; millis += 50)
		{
			double current = state.opacity(afterStart(millis));
			assertTrue("opacity should keep falling at " + millis + "ms,"
					+ " was " + previous + " and is " + current,
				current < previous);
			previous = current;
		}
	}

	@Test
	public void opacityStaysBetweenZeroAndOneForTheWholeFiveSeconds()
	{
		IntroState state = new IntroState();
		state.start(START);

		for (long millis = 0; millis <= 6000; millis += 50)
		{
			double opacity = state.opacity(afterStart(millis));
			assertTrue("opacity too low at " + millis + "ms", opacity >= 0.0);
			assertTrue("opacity too high at " + millis + "ms", opacity <= 1.0);
		}
	}

	/**
	 * The preview does not need switching off by hand. A moment taken while
	 * the clock simulated April 20 is anchored on that date, so once the clock
	 * reports the real calendar again the message is out of its five seconds
	 * and stops on the spot. This is the day after April 20 and later.
	 */
	@Test
	public void aMomentFromTheSimulatedClockIsInactiveOnceTheClockReverts()
	{
		Fun420Clock clock = new Fun420Clock(
			Clock.fixed(Instant.parse("2026-08-03T14:05:00Z"), ZoneOffset.UTC));
		IntroState state = new IntroState();

		clock.simulateApril20();
		state.start(clock.now());
		assertTrue("precondition: the preview runs while the clock simulates",
			state.isActive(clock.now()));

		clock.useRealDate();
		assertFalse(state.isActive(clock.now()));
		assertEquals(0.0, state.opacity(clock.now()), 0.0001);
	}

	/**
	 * The same, before April 20: the anchored moment then lies in the future,
	 * which the backwards clock guard catches instead of the five seconds.
	 */
	@Test
	public void aMomentFromTheSimulatedClockIsAlsoInactiveWhenAprilIsStillAhead()
	{
		Fun420Clock clock = new Fun420Clock(
			Clock.fixed(Instant.parse("2026-01-31T09:00:00Z"), ZoneOffset.UTC));
		IntroState state = new IntroState();

		clock.simulateApril20();
		state.start(clock.now());

		clock.useRealDate();
		assertFalse(state.isActive(clock.now()));
		assertEquals(0.0, state.opacity(clock.now()), 0.0001);
	}

	@Test
	public void opacityNeverRisesWhileTheMessageIsUp()
	{
		IntroState state = new IntroState();
		state.start(START);

		double previous = state.opacity(START);
		for (long millis = 25; millis <= 6000; millis += 25)
		{
			double current = state.opacity(afterStart(millis));
			assertTrue("opacity rose at " + millis + "ms,"
					+ " from " + previous + " to " + current,
				current <= previous);
			previous = current;
		}
	}
}
