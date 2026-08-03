package com.donator.fun420;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class Fun420ClockTest
{
	@Test
	public void afternoonWindowStartsAtTwenty()
	{
		assertTrue(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 16, 20, 0)));
	}

	@Test
	public void oneSecondBeforeIsOutsideTheWindow()
	{
		assertFalse(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 16, 19, 59)));
	}

	@Test
	public void lastSecondOfTheMinuteIsInsideTheWindow()
	{
		assertTrue(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 16, 20, 59)));
	}

	@Test
	public void nextMinuteIsOutsideTheWindow()
	{
		assertFalse(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 16, 21, 0)));
	}

	@Test
	public void nightWindowBehavesTheSame()
	{
		assertTrue(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 4, 20, 30)));
	}

	@Test
	public void otherHoursAtTwentyPastAreNotAlarms()
	{
		assertFalse(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 15, 20, 0)));
		assertFalse(Fun420Clock.isAlarmWindow(LocalDateTime.of(2026, 8, 3, 20, 20, 0)));
	}

	@Test
	public void bannerDayIsAprilTwentieth()
	{
		assertTrue(Fun420Clock.isBannerDay(LocalDate.of(2026, 4, 20)));
	}

	@Test
	public void adjacentDaysAreNotBannerDays()
	{
		assertFalse(Fun420Clock.isBannerDay(LocalDate.of(2026, 4, 19)));
		assertFalse(Fun420Clock.isBannerDay(LocalDate.of(2026, 4, 21)));
	}

	@Test
	public void bannerDayAlsoHoldsInALeapYear()
	{
		assertTrue(Fun420Clock.isBannerDay(LocalDate.of(2028, 4, 20)));
	}

	@Test
	public void simulationMovesTheDateKeepingTimeOfDay()
	{
		Clock base = Clock.fixed(Instant.parse("2026-08-03T14:05:00Z"), ZoneOffset.UTC);
		Fun420Clock clock = new Fun420Clock(base);

		assertEquals(LocalDateTime.of(2026, 8, 3, 14, 5, 0), clock.now());

		clock.simulateApril20();
		assertEquals(LocalDateTime.of(2026, 4, 20, 14, 5, 0), clock.now());

		clock.useRealDate();
		assertEquals(LocalDateTime.of(2026, 8, 3, 14, 5, 0), clock.now());
	}

	@Test
	public void simulationClampsADayThatDoesNotExistInApril()
	{
		Clock base = Clock.fixed(Instant.parse("2026-01-31T09:00:00Z"), ZoneOffset.UTC);
		Fun420Clock clock = new Fun420Clock(base);

		clock.simulateApril20();
		assertEquals(LocalDateTime.of(2026, 4, 20, 9, 0, 0), clock.now());
	}

	@Test
	public void simulationKeepsTimeOfDayAcrossADaylightSavingBoundary()
	{
		ZoneId amsterdam = ZoneId.of("Europe/Amsterdam");
		ZonedDateTime winter = ZonedDateTime.of(2026, 1, 31, 9, 0, 0, 0, amsterdam);
		ZonedDateTime summer = ZonedDateTime.of(2026, 4, 20, 9, 0, 0, 0, amsterdam);
		assertNotEquals("precondition: the zone must actually shift between these dates",
			winter.getOffset(), summer.getOffset());

		Clock base = Clock.fixed(
			ZonedDateTime.of(2026, 1, 31, 9, 0, 0, 0, amsterdam).toInstant(),
			amsterdam);
		Fun420Clock clock = new Fun420Clock(base);

		clock.simulateApril20();

		assertEquals(LocalDateTime.of(2026, 4, 20, 9, 0, 0), clock.now());
	}

	@Test
	public void simulationStaysOnApril20AfterRealMidnight()
	{
		MutableClock base = new MutableClock(Instant.parse("2026-08-03T23:55:00Z"), ZoneOffset.UTC);
		Fun420Clock clock = new Fun420Clock(base);

		clock.simulateApril20();
		base.advance(Duration.ofMinutes(10));

		assertEquals(LocalDate.of(2026, 4, 20), clock.now().toLocalDate());
	}

	@Test
	public void simulatingTwiceStaysOnApril20()
	{
		Clock base = Clock.fixed(Instant.parse("2026-08-03T14:05:00Z"), ZoneOffset.UTC);
		Fun420Clock clock = new Fun420Clock(base);

		clock.simulateApril20();
		clock.simulateApril20();

		assertEquals(LocalDateTime.of(2026, 4, 20, 14, 5, 0), clock.now());
	}

	@Test
	public void togglingBackAndForthStaysOnApril20()
	{
		Clock base = Clock.fixed(Instant.parse("2026-08-03T14:05:00Z"), ZoneOffset.UTC);
		Fun420Clock clock = new Fun420Clock(base);

		clock.simulateApril20();
		clock.useRealDate();
		clock.simulateApril20();

		assertEquals(LocalDateTime.of(2026, 4, 20, 14, 5, 0), clock.now());
	}

	@Test
	public void defaultConstructorTracksTheSystemClock()
	{
		LocalDateTime before = LocalDateTime.now();
		LocalDateTime fromClock = new Fun420Clock().now();
		LocalDateTime after = LocalDateTime.now();

		assertFalse(fromClock.isBefore(before));
		assertFalse(fromClock.isAfter(after));
	}

	/**
	 * A clock that can be moved forward, so a test can cross real midnight.
	 */
	private static final class MutableClock extends Clock
	{
		private final ZoneId zone;
		private Instant instant;

		MutableClock(Instant instant, ZoneId zone)
		{
			this.instant = instant;
			this.zone = zone;
		}

		void advance(Duration amount)
		{
			this.instant = this.instant.plus(amount);
		}

		@Override
		public ZoneId getZone()
		{
			return zone;
		}

		@Override
		public Clock withZone(ZoneId zone)
		{
			return new MutableClock(instant, zone);
		}

		@Override
		public Instant instant()
		{
			return instant;
		}
	}
}
