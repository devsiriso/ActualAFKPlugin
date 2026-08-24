package com.actualafk;

public enum PanelStatistic
{
	TOTAL_AFK_XP("Total AFK XP"),
	CURRENT_STREAK_TICKS("Current streak ticks"),
	CURRENT_STREAK_TIME("Current streak time (est.)"),
	LONGEST_STREAK_TICKS("Longest streak ticks"),
	LONGEST_STREAK_TIME("Longest streak time (est.)"),
	SESSION_AFK_TICKS("Session AFK ticks"),
	SESSION_AFK_TIME("Session AFK time (est.)"),
	SESSION_TOTAL_TIME("Session total time (est.)"),
	SESSION_IDLE_PERCENTAGE("Session idle percentage"),
	SESSION_ACTIVE_PERCENTAGE("Session active percentage"),
	SESSION_AFK_XP("Session AFK XP"),
	SESSION_XP_PER_HOUR("Session XP/hr"),
	TOTAL_AFK_TICKS("Total AFK ticks"),
	TOTAL_TIME("Total time (est.)"),
	TOTAL_IDLE_PERCENTAGE("Total idle percentage"),
	TOTAL_ACTIVE_PERCENTAGE("Total active percentage"),
	XP_TO_NEXT_LEVEL("XP to next level");

	private final String displayName;

	PanelStatistic(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	boolean isVisible(AfkConfig config)
	{
		if (config == null)
		{
			return true;
		}
		switch (this)
		{
			case TOTAL_AFK_XP: return config.showPanelTotalAfkXp();
			case CURRENT_STREAK_TICKS: return config.showPanelCurrentStreakTicks();
			case CURRENT_STREAK_TIME: return config.showPanelCurrentStreakTime();
			case LONGEST_STREAK_TICKS: return config.showPanelLongestStreakTicks();
			case LONGEST_STREAK_TIME: return config.showPanelLongestStreakTime();
			case SESSION_AFK_TICKS: return config.showPanelSessionAfkTicks();
			case SESSION_AFK_TIME: return config.showPanelSessionAfkTime();
			case SESSION_TOTAL_TIME: return config.showPanelSessionTotalTime();
			case SESSION_IDLE_PERCENTAGE: return config.showPanelSessionIdlePercentage();
			case SESSION_ACTIVE_PERCENTAGE: return config.showPanelSessionActivePercentage();
			case SESSION_AFK_XP: return config.showPanelSessionAfkXp();
			case SESSION_XP_PER_HOUR: return config.showPanelSessionXpPerHour();
			case TOTAL_AFK_TICKS: return config.showPanelTotalAfkTicks();
			case TOTAL_TIME: return config.showPanelTotalTime();
			case TOTAL_IDLE_PERCENTAGE: return config.showPanelTotalIdlePercentage();
			case TOTAL_ACTIVE_PERCENTAGE: return config.showPanelTotalActivePercentage();
			case XP_TO_NEXT_LEVEL: return config.showPanelXpToNextLevel();
			default: throw new IllegalStateException("Unhandled panel statistic: " + this);
		}
	}
}
