package com.donator.fun420;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class Fun420PluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(Fun420Plugin.class);
		RuneLite.main(args);
	}
}
