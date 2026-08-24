package com.actualafk;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class DeveloperLauncher
{
	private DeveloperLauncher()
	{
	}

	public static void main(String[] arguments) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkPlugin.class);
		RuneLite.main(arguments);
	}
}
