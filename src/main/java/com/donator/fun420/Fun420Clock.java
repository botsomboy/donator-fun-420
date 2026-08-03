package com.donator.fun420;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

/**
 * Supplies the time the plugin works on and answers the two calendar questions.
 * The base clock is injectable so that tests and the simulation toggle take
 * exactly the same path as production.
 */
public class Fun420Clock
{
	private static final int ALARM_MINUTE = 20;
	private static final int AFTERNOON_HOUR = 16;
	private static final int NIGHT_HOUR = 4;
	private static final int BANNER_DAY_OF_MONTH = 20;

	private final Clock baseClock;
	private volatile boolean simulating;

	public Fun420Clock()
	{
		this(Clock.systemDefaultZone());
	}

	public Fun420Clock(Clock baseClock)
	{
		this.baseClock = baseClock;
	}

	public LocalDateTime now()
	{
		LocalDateTime time = LocalDateTime.now(baseClock);
		return simulating
			? time.withMonth(Month.APRIL.getValue()).withDayOfMonth(BANNER_DAY_OF_MONTH)
			: time;
	}

	/**
	 * The moment on the real calendar, deliberately blind to the April 20
	 * preview. Anything that anchors on a moment and measures from it later
	 * must read this: {@link #now()} jumps months when the preview is switched
	 * on or off, which would strand a running animation in the past or the
	 * future the instant the toggle moves.
	 */
	public LocalDateTime realNow()
	{
		return LocalDateTime.now(baseClock);
	}

	public void useRealDate()
	{
		this.simulating = false;
	}

	public void simulateApril20()
	{
		this.simulating = true;
	}

	static boolean isAlarmWindow(LocalDateTime time)
	{
		if (time.getMinute() != ALARM_MINUTE)
		{
			return false;
		}
		return time.getHour() == AFTERNOON_HOUR || time.getHour() == NIGHT_HOUR;
	}

	static boolean isBannerDay(LocalDate date)
	{
		return date.getMonth() == Month.APRIL && date.getDayOfMonth() == BANNER_DAY_OF_MONTH;
	}
}
