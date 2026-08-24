package com.actualafk;

public final class AfkProgress
{
	private static final int MAXIMUM_LEVEL = 99;
	private static final long[] EXPERIENCE_FOR_LEVEL = createExperienceTable();

	private long totalAfkXp;
	private long currentIdleStreakTicks;
	private long longestIdleStreakTicks;
	private long sessionQualifyingTicks;
	private long totalObservedTicks;

	public AfkProgress()
	{
	}

	public AfkProgress(long totalAfkXp, long currentIdleStreakTicks,
		long longestIdleStreakTicks, long sessionQualifyingTicks)
	{
		this(totalAfkXp, currentIdleStreakTicks, longestIdleStreakTicks,
			sessionQualifyingTicks, totalAfkXp);
	}

	public AfkProgress(long totalAfkXp, long currentIdleStreakTicks,
		long longestIdleStreakTicks, long sessionQualifyingTicks, long totalObservedTicks)
	{
		this.totalAfkXp = nonNegative(totalAfkXp);
		this.currentIdleStreakTicks = nonNegative(currentIdleStreakTicks);
		this.longestIdleStreakTicks = nonNegative(longestIdleStreakTicks);
		this.sessionQualifyingTicks = nonNegative(sessionQualifyingTicks);
		this.totalObservedTicks = Math.max(this.totalAfkXp, nonNegative(totalObservedTicks));
		if (this.longestIdleStreakTicks < this.currentIdleStreakTicks)
		{
			this.longestIdleStreakTicks = this.currentIdleStreakTicks;
		}
	}

	public void addObservedTick()
	{
		totalObservedTicks = incrementSaturated(totalObservedTicks);
	}

	public void addQualifyingTick()
	{
		addQualifyingTick(incrementSaturated(currentIdleStreakTicks));
	}

	public void addQualifyingTick(long currentIdleStreakTicks)
	{
		this.currentIdleStreakTicks = nonNegative(currentIdleStreakTicks);
		this.totalAfkXp = incrementSaturated(this.totalAfkXp);
		this.sessionQualifyingTicks = incrementSaturated(this.sessionQualifyingTicks);
		if (this.currentIdleStreakTicks > longestIdleStreakTicks)
		{
			longestIdleStreakTicks = this.currentIdleStreakTicks;
		}
	}

	public void setCurrentIdleStreakTicks(long currentIdleStreakTicks)
	{
		this.currentIdleStreakTicks = nonNegative(currentIdleStreakTicks);
	}

	public void reset()
	{
		totalAfkXp = 0;
		currentIdleStreakTicks = 0;
		longestIdleStreakTicks = 0;
		sessionQualifyingTicks = 0;
		totalObservedTicks = 0;
	}

	public long getTotalAfkXp()
	{
		return totalAfkXp;
	}

	public long getCurrentIdleStreakTicks()
	{
		return currentIdleStreakTicks;
	}

	public long getLongestIdleStreakTicks()
	{
		return longestIdleStreakTicks;
	}

	public long getSessionQualifyingTicks()
	{
		return sessionQualifyingTicks;
	}

	public long getTotalObservedTicks()
	{
		return totalObservedTicks;
	}

	public int getAfkLevel()
	{
		for (int level = MAXIMUM_LEVEL; level > 1; level--)
		{
			if (totalAfkXp >= EXPERIENCE_FOR_LEVEL[level])
			{
				return level;
			}
		}
		return 1;
	}

	public long getCurrentLevelXp()
	{
		return EXPERIENCE_FOR_LEVEL[getAfkLevel()];
	}

	public long getNextLevelXp()
	{
		int level = getAfkLevel();
		return EXPERIENCE_FOR_LEVEL[Math.min(level + 1, MAXIMUM_LEVEL)];
	}

	public long getXpToNextLevel()
	{
		return Math.max(0, getNextLevelXp() - totalAfkXp);
	}

	public static long getExperienceForLevel(int level)
	{
		if (level < 1 || level > MAXIMUM_LEVEL)
		{
			throw new IllegalArgumentException("level must be between 1 and 99");
		}
		return EXPERIENCE_FOR_LEVEL[level];
	}

	private static long[] createExperienceTable()
	{
		long[] experienceForLevel = new long[MAXIMUM_LEVEL + 1];
		long cumulativePoints = 0;
		for (int level = 1; level < MAXIMUM_LEVEL; level++)
		{
			cumulativePoints += (long) (level + 300 * Math.pow(2, level / 7.0));
			experienceForLevel[level + 1] = cumulativePoints / 4;
		}
		return experienceForLevel;
	}

	private static long nonNegative(long value)
	{
		return Math.max(0, value);
	}

	private static long incrementSaturated(long value)
	{
		return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1;
	}
}
