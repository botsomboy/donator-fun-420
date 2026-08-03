package com.donator.fun420;

import com.google.inject.Provides;
import java.time.LocalDate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Donator - Fun 420",
	description = "Visual 4:20 alarm and an April 20 banner",
	tags = {"420", "fun", "alarm", "banner", "overlay"}
)
public class Fun420Plugin extends Plugin
{
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

	@Override
	protected void startUp()
	{
		log.debug("Donator - Fun 420 started");
	}

	@Override
	protected void shutDown()
	{
		alarmState.reset();
		clock.useRealDate();
		log.debug("Donator - Fun 420 stopped");
	}

	@Provides
	Fun420Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(Fun420Config.class);
	}
}
