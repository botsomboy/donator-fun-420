package com.donator.fun420;

import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BannerStateTest
{
	@Test
	public void visibleOnApril20WhenNeverDismissed()
	{
		assertTrue(BannerState.isVisible(LocalDate.of(2026, 4, 20), null));
	}

	@Test
	public void hiddenAfterBeingDismissedToday()
	{
		LocalDate today = LocalDate.of(2026, 4, 20);
		assertFalse(BannerState.isVisible(today, today));
	}

	@Test
	public void visibleAgainNextYear()
	{
		assertTrue(BannerState.isVisible(LocalDate.of(2027, 4, 20), LocalDate.of(2026, 4, 20)));
	}

	@Test
	public void dismissalFromAnotherDayDoesNotHideIt()
	{
		assertTrue(BannerState.isVisible(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 1, 1)));
		assertTrue(BannerState.isVisible(LocalDate.of(2026, 4, 20), LocalDate.of(2027, 4, 20)));
	}

	@Test
	public void hiddenOnEveryOtherDay()
	{
		assertFalse(BannerState.isVisible(LocalDate.of(2026, 4, 19), null));
		assertFalse(BannerState.isVisible(LocalDate.of(2026, 4, 21), null));
		assertFalse(BannerState.isVisible(LocalDate.of(2026, 8, 3), null));
	}
}
