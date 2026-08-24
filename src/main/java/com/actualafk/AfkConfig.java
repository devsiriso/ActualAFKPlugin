package com.actualafk;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("afkmastery")
public interface AfkConfig extends Config
{
	String CONFIG_GROUP = "afkmastery";
	String MINIMUM_IDLE_TICKS_KEY = "minimumIdleTicks";
	String ACTIVITY_GRACE_TICKS_KEY = "activityGraceTicks";
	String SHOW_CANVAS_OVERLAY_KEY = "showCanvasOverlay";
	String SHOW_PANEL_STATISTIC_KEY_PREFIX = "showPanel";
	String SHOW_SIDEBAR_STATISTIC_KEY_PREFIX = "showSidebar";

	@ConfigItem(
		keyName = "trackingEnabled",
		name = "Enable tracking",
		description = "Track AFK time",
		position = 0
	)
	default boolean trackingEnabled()
	{
		return true;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = MINIMUM_IDLE_TICKS_KEY,
		name = "Minimum inactive ticks",
		description = "Consecutive inactive game ticks needed before AFK time starts counting",
		position = 1
	)
	default int minimumIdleTicks()
	{
		return 1;
	}

	@Range(min = 0, max = 20)
	@ConfigItem(
		keyName = ACTIVITY_GRACE_TICKS_KEY,
		name = "Activity grace ticks",
		description = "Ticks after observed animation, interaction, or incoming combat during which AFK time is not counted",
		position = 2
	)
	default int activityGraceTicks()
	{
		return 1;
	}

	@ConfigItem(
		keyName = SHOW_CANVAS_OVERLAY_KEY,
		name = "Show canvas overlay",
		description = "Show AFK progress on the RuneLite canvas",
		position = 3
	)
	default boolean showCanvasOverlay()
	{
		return false;
	}

	@ConfigItem(keyName = "showSidebarTotalAfkXp", name = "Show total AFK XP", description = "Show total client-side AFK XP in the detailed statistics", position = 4)
	default boolean showPanelTotalAfkXp() { return true; }

	@ConfigItem(keyName = "showPanelCurrentStreakTicks", name = "Show current streak ticks", description = "Show current inactive streak ticks in the detailed statistics", position = 6)
	default boolean showPanelCurrentStreakTicks() { return true; }

	@ConfigItem(keyName = "showPanelCurrentStreakTime", name = "Show current streak time", description = "Show estimated current inactive streak time in the RuneLite plugin panel", position = 8)
	default boolean showPanelCurrentStreakTime() { return true; }

	@ConfigItem(keyName = "showPanelLongestStreakTicks", name = "Show longest streak ticks", description = "Show longest inactive streak ticks in the RuneLite plugin panel", position = 9)
	default boolean showPanelLongestStreakTicks() { return true; }

	@ConfigItem(keyName = "showPanelLongestStreakTime", name = "Show longest streak time", description = "Show estimated longest inactive streak time in the RuneLite plugin panel", position = 10)
	default boolean showPanelLongestStreakTime() { return true; }

	@ConfigItem(keyName = "showPanelSessionAfkTicks", name = "Show session AFK ticks", description = "Show session AFK ticks in the RuneLite plugin panel", position = 11)
	default boolean showPanelSessionAfkTicks() { return true; }

	@ConfigItem(keyName = "showPanelSessionAfkTime", name = "Show session AFK time", description = "Show estimated session AFK time in the RuneLite plugin panel", position = 12)
	default boolean showPanelSessionAfkTime() { return true; }

	@ConfigItem(keyName = "showPanelSessionTotalTime", name = "Show session total time", description = "Show estimated session total time in the RuneLite plugin panel", position = 13)
	default boolean showPanelSessionTotalTime() { return true; }

	@ConfigItem(keyName = "showPanelSessionIdlePercentage", name = "Show session idle %", description = "Show session idle percentage in the RuneLite plugin panel", position = 14)
	default boolean showPanelSessionIdlePercentage() { return true; }

	@ConfigItem(keyName = "showPanelSessionActivePercentage", name = "Show session active %", description = "Show session active percentage in the RuneLite plugin panel", position = 15)
	default boolean showPanelSessionActivePercentage() { return true; }

	@ConfigItem(keyName = "showPanelSessionAfkXp", name = "Show session AFK XP", description = "Show session AFK XP", position = 16)
	default boolean showPanelSessionAfkXp() { return true; }

	@ConfigItem(keyName = "showSidebarSessionXpPerHour", name = "Show session XP/hr", description = "Show session AFK XP per hour", position = 17)
	default boolean showPanelSessionXpPerHour() { return true; }

	@ConfigItem(keyName = "showPanelTotalAfkTicks", name = "Show total AFK ticks", description = "Show total AFK ticks in the RuneLite plugin panel", position = 18)
	default boolean showPanelTotalAfkTicks() { return true; }

	@ConfigItem(keyName = "showPanelTotalTime", name = "Show total time", description = "Show estimated total time in the RuneLite plugin panel", position = 19)
	default boolean showPanelTotalTime() { return true; }

	@ConfigItem(keyName = "showPanelTotalIdlePercentage", name = "Show total idle %", description = "Show total idle percentage in the RuneLite plugin panel", position = 20)
	default boolean showPanelTotalIdlePercentage() { return true; }

	@ConfigItem(keyName = "showPanelTotalActivePercentage", name = "Show total active %", description = "Show total active percentage in the RuneLite plugin panel", position = 21)
	default boolean showPanelTotalActivePercentage() { return true; }

	@ConfigItem(keyName = "showPanelXpToNextLevel", name = "Show XP to next level", description = "Show AFK XP needed for the next level", position = 22)
	default boolean showPanelXpToNextLevel() { return true; }
}
