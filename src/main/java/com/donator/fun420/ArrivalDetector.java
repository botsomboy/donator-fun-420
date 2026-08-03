package com.donator.fun420;

import net.runelite.api.GameState;

/**
 * Tells an arrival in the game apart from a region load. The client posts
 * LOGGED_IN again after loading every region, so that state on its own says
 * nothing: only a LOGGED_IN that follows a spell outside the game is an
 * arrival. Kept out of the plugin so that the decision can be proven against
 * the sequences the client really posts, without a client.
 * <p>
 * Answers only "did the player just arrive". Whether it is April 20, and
 * whether the message is switched on, are somebody else's business.
 */
class ArrivalDetector
{
	/**
	 * Whether the player was in the game at the previous state worth judging.
	 * Volatile because it is written from the thread that posts the state
	 * changes and seeded from the one that starts the plugin.
	 */
	private volatile boolean inGame;

	/**
	 * Judges one state change.
	 *
	 * @return whether the player has just arrived in the game
	 */
	boolean onState(GameState state)
	{
		if (!isInGame(state))
		{
			// The login screen, logging out, hopping and a lost connection all
			// land here, so the next LOGGED_IN really is an arrival.
			inGame = false;
			return false;
		}

		if (state != GameState.LOGGED_IN)
		{
			// LOADING, which sits both in the tail of a login and in every
			// region change and therefore says nothing about arriving. Leave
			// the memory as it is and let the LOGGED_IN behind it judge.
			return false;
		}

		if (inGame)
		{
			// LOGGED_IN after a region load. Nothing arrived.
			return false;
		}

		inGame = true;
		return true;
	}

	/**
	 * Takes the state the client is in without reporting an arrival, for a
	 * plugin that starts while the player is already logged in: the region
	 * load behind it must not be mistaken for an arrival.
	 */
	void seed(GameState state)
	{
		inGame = isInGame(state);
	}

	/** Forgets where the player was, so the next LOGGED_IN is an arrival. */
	void reset()
	{
		inGame = false;
	}

	/**
	 * Whether the player is in the game. LOADING counts: the client enters it
	 * on every region change, and running across a map border is not logging
	 * out, so it must not cut a running alarm short. The plugin shares this
	 * predicate so that the alarm and the arrival cannot disagree about what
	 * counts as logged out.
	 */
	static boolean isInGame(GameState state)
	{
		return state == GameState.LOGGED_IN || state == GameState.LOADING;
	}
}
