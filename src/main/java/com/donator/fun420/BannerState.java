package com.donator.fun420;

import java.time.LocalDate;

/**
 * Decides whether the banner belongs on screen. Stores nothing itself; the
 * plugin keeps the date on which the player clicked it away.
 */
public final class BannerState
{
	private BannerState()
	{
	}

	/**
	 * @param today       the current date; never null
	 * @param dismissedOn the date the player clicked the banner away, or null if never.
	 *                    Compared as {@code today.equals(dismissedOn)} so that a null
	 *                    dismissal is safe; reversing the operands would throw on the
	 *                    common never-dismissed path.
	 */
	public static boolean isVisible(LocalDate today, LocalDate dismissedOn)
	{
		if (!Fun420Clock.isBannerDay(today))
		{
			return false;
		}
		return !today.equals(dismissedOn);
	}
}
