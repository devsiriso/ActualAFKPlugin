package com.actualafk;

import java.util.Objects;

/** Immutable values shared by the sidebar and canvas overlay. */
public final class ProgressSnapshot
{
	private final int level;
	private final long totalExperience;
	private final long currentLevelExperience;
	private final long nextLevelExperience;
	private final long currentStreakTicks;
	private final long longestStreakTicks;
	private final long sessionQualifyingTicks;
	private final long sessionObservedTicks;
	private final long totalObservedTicks;
	private final String activityState;

	public ProgressSnapshot(
		int level,
		long totalExperience,
		long currentLevelExperience,
		long nextLevelExperience,
		long currentStreakTicks,
		long longestStreakTicks,
		long sessionQualifyingTicks,
		long sessionObservedTicks,
		long totalObservedTicks,
		String activityState)
	{
		this.level = level;
		this.totalExperience = totalExperience;
		this.currentLevelExperience = currentLevelExperience;
		this.nextLevelExperience = nextLevelExperience;
		this.currentStreakTicks = currentStreakTicks;
		this.longestStreakTicks = longestStreakTicks;
		this.sessionQualifyingTicks = sessionQualifyingTicks;
		this.sessionObservedTicks = sessionObservedTicks;
		this.totalObservedTicks = totalObservedTicks;
		this.activityState = Objects.requireNonNull(activityState, "activityState");
	}

	public static ProgressSnapshot initial()
	{
		return new ProgressSnapshot(1, 0, 0, AfkProgress.getExperienceForLevel(2),
			0, 0, 0, 0, 0, "Logged out");
	}

	public int getLevel() { return level; }
	public long getTotalExperience() { return totalExperience; }
	public long getCurrentLevelExperience() { return currentLevelExperience; }
	public long getNextLevelExperience() { return nextLevelExperience; }
	public long getCurrentStreakTicks() { return currentStreakTicks; }
	public long getLongestStreakTicks() { return longestStreakTicks; }
	public long getSessionQualifyingTicks() { return sessionQualifyingTicks; }
	public long getSessionObservedTicks() { return sessionObservedTicks; }
	public long getTotalObservedTicks() { return totalObservedTicks; }
	public String getActivityState() { return activityState; }

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof ProgressSnapshot))
		{
			return false;
		}
		ProgressSnapshot that = (ProgressSnapshot) other;
		return level == that.level
			&& totalExperience == that.totalExperience
			&& currentLevelExperience == that.currentLevelExperience
			&& nextLevelExperience == that.nextLevelExperience
			&& currentStreakTicks == that.currentStreakTicks
			&& longestStreakTicks == that.longestStreakTicks
			&& sessionQualifyingTicks == that.sessionQualifyingTicks
			&& sessionObservedTicks == that.sessionObservedTicks
			&& totalObservedTicks == that.totalObservedTicks
			&& activityState.equals(that.activityState);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(level, totalExperience, currentLevelExperience,
			nextLevelExperience, currentStreakTicks, longestStreakTicks,
			sessionQualifyingTicks, sessionObservedTicks, totalObservedTicks, activityState);
	}
}
