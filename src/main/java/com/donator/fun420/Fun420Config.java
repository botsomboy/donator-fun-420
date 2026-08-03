package com.donator.fun420;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(Fun420Config.GROUP)
public interface Fun420Config extends Config
{
	String GROUP = "donatorfun420";

	// Keys are declared as constants only where the plugin needs them by name
	// (ConfigManager reads/writes and ConfigChanged filtering).
	String KEY_TEST_ALARM = "testAlarm";
	String KEY_SIMULATE_APRIL_20 = "simulateApril20";

	// Hidden persisted state, written by the plugin through ConfigManager.
	// Deliberately has no @ConfigItem: it is not a user-facing setting.
	String KEY_BANNER_DISMISSED_ON = "bannerDismissedOn";

	@ConfigSection(
		name = "Alarm",
		description = "The 4:20 alarm at 16:20 and 04:20",
		position = 0
	)
	String alarmSection = "alarmSection";

	@ConfigSection(
		name = "Banner",
		description = "The banner shown on April 20",
		position = 1
	)
	String bannerSection = "bannerSection";

	@ConfigSection(
		name = "Test",
		description = "Preview both features on any day",
		position = 2,
		closedByDefault = true
	)
	String testSection = "testSection";

	@ConfigItem(
		keyName = "alarmEnabled",
		name = "Enable alarm",
		description = "Show a pulsing border at 16:20 and 04:20 local time",
		section = alarmSection,
		position = 0
	)
	default boolean alarmEnabled()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "alarmColor",
		name = "Alarm colour",
		description = "Colour of the pulsing border and the message; alpha sets the peak brightness of the pulse",
		section = alarmSection,
		position = 1
	)
	default Color alarmColor()
	{
		return new Color(0, 200, 60);
	}

	@ConfigItem(
		keyName = "alarmText",
		name = "Alarm text",
		description = "Message shown in the middle of the screen",
		section = alarmSection,
		position = 2
	)
	default String alarmText()
	{
		return "4:20 - blaze it";
	}

	@ConfigItem(
		keyName = "bannerEnabled",
		name = "Enable banner",
		description = "Show a banner at the top of the screen on April 20",
		section = bannerSection,
		position = 0
	)
	default boolean bannerEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bannerIntroEnabled",
		name = "Enable login message",
		description = "Show a large message in the middle of the screen when you log in on April 20",
		section = bannerSection,
		position = 1
	)
	default boolean bannerIntroEnabled()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "bannerColor",
		name = "Banner colour",
		description = "Background colour of the banner and colour of the login message",
		section = bannerSection,
		position = 2
	)
	default Color bannerColor()
	{
		return new Color(0, 160, 50);
	}

	@ConfigItem(
		keyName = "bannerText",
		name = "Banner text",
		description = "Text shown in the banner and in the login message",
		section = bannerSection,
		position = 3
	)
	default String bannerText()
	{
		return "Happy 420 today";
	}

	@ConfigItem(
		keyName = KEY_TEST_ALARM,
		name = "Trigger alarm now",
		description = "Starts the alarm immediately and switches itself back off",
		section = testSection,
		position = 0
	)
	default boolean testAlarm()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_SIMULATE_APRIL_20,
		name = "Pretend it is April 20",
		description = "Makes the plugin treat today as April 20 so you can see the banner",
		section = testSection,
		position = 1
	)
	default boolean simulateApril20()
	{
		return false;
	}
}
