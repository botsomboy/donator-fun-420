package com.donator.fun420;

import com.google.inject.Provides;
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
	@Override
	protected void startUp()
	{
		log.debug("Donator - Fun 420 started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Donator - Fun 420 stopped");
	}

	@Provides
	Fun420Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(Fun420Config.class);
	}
}
