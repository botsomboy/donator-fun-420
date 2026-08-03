package com.donator.fun420;

import com.google.inject.Provides;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Donator - Fun 420",
	description = "Visual 4:20 alarm and an April 20 banner",
	tags = {"420", "fun", "alarm", "banner", "overlay"}
)
public class Fun420Plugin extends Plugin
{
	/** The value a boolean config item carries once the player switches it on. */
	private static final String CONFIG_TRUE = "true";

	@Inject
	private Client client;

	@Inject
	private Fun420Config config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private AlarmOverlay alarmOverlay;

	@Inject
	private BannerOverlay bannerOverlay;

	@Inject
	private BannerMouseListener bannerMouseListener;

	@Getter
	private final Fun420Clock clock = new Fun420Clock();

	@Getter
	private final AlarmState alarmState = new AlarmState();

	/**
	 * The date on which the player clicked the banner away, or null if never.
	 * Volatile because the dismissal arrives on another thread than the one
	 * that renders, matching {@link AlarmState} and {@link Fun420Clock}.
	 */
	private volatile LocalDate bannerDismissedOn;

	public boolean isBannerVisible()
	{
		return BannerState.isVisible(clock.now().toLocalDate(), bannerDismissedOn);
	}

	/**
	 * Closes the banner for the day it is currently showing. A dismissal taken
	 * while the April 20 simulation is on is deliberately not written to the
	 * config: the clock reports the simulated April 20, so persisting it would
	 * suppress the banner on the real April 20 that was being previewed.
	 */
	void dismissBanner()
	{
		LocalDate today = clock.now().toLocalDate();
		bannerDismissedOn = today;

		if (config.simulateApril20())
		{
			// Skipping the write costs a dismissal that does not survive a
			// restart when it really is April 20 and the preview happens to be
			// on. That is the accepted trade: the alternative silently kills
			// the real banner, so do not "fix" this by always writing.
			log.debug("420 banner preview closed for {}, not stored while simulating", today);
			return;
		}

		configManager.setConfiguration(
			Fun420Config.GROUP,
			Fun420Config.KEY_BANNER_DISMISSED_ON,
			today.toString());
		log.debug("420 banner dismissed for {}", today);
	}

	@Override
	protected void startUp()
	{
		// Loads bannerDismissedOn as well; see applySimulation.
		applySimulation();
		overlayManager.add(alarmOverlay);
		overlayManager.add(bannerOverlay);
		mouseManager.registerMouseListener(bannerMouseListener);
		log.debug("Donator - Fun 420 started");
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(bannerMouseListener);
		overlayManager.remove(alarmOverlay);
		overlayManager.remove(bannerOverlay);
		alarmState.reset();
		clock.useRealDate();
		log.debug("Donator - Fun 420 stopped");
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		alarmState.update(clock.now(), isInGame());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!isInGame(event.getGameState()))
		{
			alarmState.reset();
		}
	}

	private boolean isInGame()
	{
		return isInGame(client.getGameState());
	}

	/**
	 * Whether the player is in the game, which is what clears the alarm when it
	 * turns false. LOADING counts as being in the game: the client enters it on
	 * every region change, and running across a map border is not logging out,
	 * so it must not cut a running alarm short. Shared by the tick and the state
	 * change so that the two cannot disagree about what counts as logged out.
	 */
	private static boolean isInGame(GameState state)
	{
		return state == GameState.LOGGED_IN || state == GameState.LOADING;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!Fun420Config.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (Fun420Config.KEY_TEST_ALARM.equals(event.getKey()))
		{
			// Judged on the value this event carries rather than on a config
			// read: switching the item back off below posts a second event for
			// the same key, and that one carries "false", so it cannot loop.
			if (CONFIG_TRUE.equals(event.getNewValue()))
			{
				alarmState.start(clock.now());
				configManager.setConfiguration(
					Fun420Config.GROUP,
					Fun420Config.KEY_TEST_ALARM,
					false);
			}
			return;
		}

		if (Fun420Config.KEY_SIMULATE_APRIL_20.equals(event.getKey()))
		{
			applySimulation();
		}

		// Any other key of this group, including the hidden dismissal date the
		// plugin writes itself, needs no handling.
	}

	/**
	 * Puts the clock on the simulated or the real calendar. Sole owner of
	 * {@link #bannerDismissedOn}, which is why it writes the field on both
	 * branches instead of leaving one of them to a caller.
	 */
	private void applySimulation()
	{
		if (config.simulateApril20())
		{
			clock.simulateApril20();
			// In-memory in both directions: a stored dismissal for the real
			// April 20 must not blank the preview it is meant to show.
			bannerDismissedOn = null;
			return;
		}

		clock.useRealDate();
		// Back on the real calendar, so drop a dismissal that was only taken to
		// close the preview and was never stored. A real dismissal was stored
		// and comes straight back.
		bannerDismissedOn = readDismissedDate();
	}

	private LocalDate readDismissedDate()
	{
		String raw = configManager.getConfiguration(
			Fun420Config.GROUP,
			Fun420Config.KEY_BANNER_DISMISSED_ON);

		if (raw == null || raw.isEmpty())
		{
			return null;
		}

		try
		{
			return LocalDate.parse(raw);
		}
		catch (DateTimeParseException e)
		{
			log.debug("Unreadable stored date: {}", raw);
			return null;
		}
	}

	@Provides
	Fun420Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(Fun420Config.class);
	}
}
