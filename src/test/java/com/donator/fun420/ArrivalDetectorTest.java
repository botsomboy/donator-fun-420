package com.donator.fun420;

import net.runelite.api.GameState;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pins the sequences the client really posts. LOGGED_IN on its own says
 * nothing: it comes back after every region load, so only the sequences below
 * decide what counts as arriving in the game.
 */
public class ArrivalDetectorTest
{
	/** The states of a login, up to and including arriving in the game. */
	private static final GameState[] LOGIN = {
		GameState.LOGIN_SCREEN,
		GameState.LOGGING_IN,
		GameState.LOADING,
		GameState.LOGGED_IN,
	};

	/** The states of running across a region border. */
	private static final GameState[] REGION_CHANGE = {
		GameState.LOADING,
		GameState.LOGGED_IN,
	};

	private final ArrivalDetector detector = new ArrivalDetector();

	private int arrivals(GameState... states)
	{
		int count = 0;
		for (GameState state : states)
		{
			if (detector.onState(state))
			{
				count++;
			}
		}
		return count;
	}

	@Test
	public void aLoginIsAnArrival()
	{
		assertEquals(1, arrivals(LOGIN));
	}

	@Test
	public void aLoginThroughTheAuthenticatorIsOneArrival()
	{
		assertEquals(1, arrivals(
			GameState.LOGIN_SCREEN,
			GameState.LOGIN_SCREEN_AUTHENTICATOR,
			GameState.LOGGING_IN,
			GameState.LOADING,
			GameState.LOGGED_IN));
	}

	@Test
	public void regionChangesAreNotArrivals()
	{
		arrivals(LOGIN);

		assertEquals(0, arrivals(
			GameState.LOADING, GameState.LOGGED_IN,
			GameState.LOADING, GameState.LOGGED_IN,
			GameState.LOADING, GameState.LOGGED_IN));
	}

	@Test
	public void aWorldHopIsAnArrival()
	{
		arrivals(LOGIN);

		assertEquals(1, arrivals(
			GameState.HOPPING,
			GameState.LOADING,
			GameState.LOGGED_IN));
	}

	@Test
	public void reconnectingAfterALostConnectionIsAnArrival()
	{
		arrivals(LOGIN);

		assertEquals(1, arrivals(
			GameState.CONNECTION_LOST,
			GameState.LOADING,
			GameState.LOGGED_IN));
	}

	@Test
	public void aLostConnectionFollowedByARelogIsOneArrival()
	{
		arrivals(LOGIN);

		assertEquals(1, arrivals(
			GameState.CONNECTION_LOST,
			GameState.LOGIN_SCREEN,
			GameState.LOGGING_IN,
			GameState.LOADING,
			GameState.LOGGED_IN));
	}

	@Test
	public void aRegionChangeAfterSeedingInGameIsNotAnArrival()
	{
		// The plugin was enabled while the player was already logged in.
		detector.seed(GameState.LOGGED_IN);

		assertEquals(0, arrivals(REGION_CHANGE));
	}

	@Test
	public void loggingOutAndBackInAfterSeedingInGameIsOneArrival()
	{
		detector.seed(GameState.LOGGED_IN);

		assertEquals(1, arrivals(LOGIN));
	}

	@Test
	public void aWholeDayOfPlayFiresOnlyForTheLoginAndTheHop()
	{
		int count = arrivals(LOGIN);
		for (int i = 0; i < 5; i++)
		{
			count += arrivals(REGION_CHANGE);
		}
		count += arrivals(GameState.HOPPING, GameState.LOADING, GameState.LOGGED_IN);
		for (int i = 0; i < 3; i++)
		{
			count += arrivals(REGION_CHANGE);
		}

		assertEquals(2, count);
	}

	@Test
	public void seedingOutsideTheGameLeavesTheNextLoginAnArrival()
	{
		detector.seed(GameState.LOGIN_SCREEN);

		assertEquals(1, arrivals(LOGIN));
	}

	@Test
	public void resettingMakesTheNextLoggedInAnArrivalAgain()
	{
		arrivals(LOGIN);
		detector.reset();

		assertTrue(detector.onState(GameState.LOGGED_IN));
	}

	@Test
	public void loadingOnItsOwnIsNeverAnArrival()
	{
		// It sits in the tail of a login as well as in every region change, so
		// on its own it says nothing about arriving.
		assertFalse(detector.onState(GameState.LOADING));
	}

	@Test
	public void beingInTheGameCoversLoadingSoALoadingScreenIsNotLoggedOut()
	{
		assertTrue(ArrivalDetector.isInGame(GameState.LOGGED_IN));
		assertTrue(ArrivalDetector.isInGame(GameState.LOADING));
		assertFalse(ArrivalDetector.isInGame(GameState.LOGIN_SCREEN));
		assertFalse(ArrivalDetector.isInGame(GameState.LOGGING_IN));
		assertFalse(ArrivalDetector.isInGame(GameState.HOPPING));
		assertFalse(ArrivalDetector.isInGame(GameState.CONNECTION_LOST));
	}
}
