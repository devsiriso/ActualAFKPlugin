package com.actualafk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AfkProgressTest
{
	@Test
	public void qualifyingTicksIncreaseXpAndStreaks()
	{
		AfkProgress progress = new AfkProgress();
		progress.addQualifyingTick();
		progress.addQualifyingTick();
		progress.addQualifyingTick(7);

		assertEquals(3, progress.getTotalAfkXp());
		assertEquals(7, progress.getCurrentIdleStreakTicks());
		assertEquals(7, progress.getLongestIdleStreakTicks());
		assertEquals(3, progress.getSessionQualifyingTicks());
	}

	@Test
	public void observedTicksTrackLifetimeDenominatorIndependently()
	{
		AfkProgress progress = new AfkProgress();
		progress.addObservedTick();
		progress.addObservedTick();
		progress.addQualifyingTick(1);

		assertEquals(2, progress.getTotalObservedTicks());
		assertEquals(1, progress.getTotalAfkXp());
	}

	@Test
	public void activeTickCanResetCurrentStreakWithoutChangingLongest()
	{
		AfkProgress progress = new AfkProgress();
		progress.addQualifyingTick(4);
		progress.setCurrentIdleStreakTicks(0);

		assertEquals(0, progress.getCurrentIdleStreakTicks());
		assertEquals(4, progress.getLongestIdleStreakTicks());
		assertEquals(1, progress.getTotalAfkXp());
	}

	@Test
	public void levelBoundariesUseAfkRuneScapeCurve()
	{
		AfkProgress levelOne = new AfkProgress();
		AfkProgress levelTwo = new AfkProgress(AfkProgress.getExperienceForLevel(2), 0, 0, 0);
		AfkProgress levelNinetyNine = new AfkProgress(AfkProgress.getExperienceForLevel(99), 0, 0, 0);

		assertEquals(1, levelOne.getAfkLevel());
		assertEquals(2, levelTwo.getAfkLevel());
		assertEquals(99, levelNinetyNine.getAfkLevel());
		assertEquals(83, AfkProgress.getExperienceForLevel(2));
		assertEquals(13034431, AfkProgress.getExperienceForLevel(99));
	}

	@Test
	public void levelNinetyNineContinuesCountingAndCapsDisplay()
	{
		long beyondMaximum = AfkProgress.getExperienceForLevel(99) + 500;
		AfkProgress progress = new AfkProgress(beyondMaximum, 0, 0, 0);

		assertEquals(99, progress.getAfkLevel());
		assertEquals(beyondMaximum, progress.getTotalAfkXp());
		assertEquals(AfkProgress.getExperienceForLevel(99), progress.getCurrentLevelXp());
		assertEquals(AfkProgress.getExperienceForLevel(99), progress.getNextLevelXp());
		assertEquals(0, progress.getXpToNextLevel());
	}

	@Test
	public void resetClearsAllCounters()
	{
		AfkProgress progress = new AfkProgress(100, 4, 8, 3);
		progress.reset();

		assertEquals(0, progress.getTotalAfkXp());
		assertEquals(0, progress.getCurrentIdleStreakTicks());
		assertEquals(0, progress.getLongestIdleStreakTicks());
		assertEquals(0, progress.getSessionQualifyingTicks());
		assertEquals(0, progress.getTotalObservedTicks());
	}

	@Test
	public void countersSaturateInsteadOfOverflowing()
	{
		AfkProgress progress = new AfkProgress(Long.MAX_VALUE, Long.MAX_VALUE,
			Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

		progress.addObservedTick();
		progress.addQualifyingTick();

		assertEquals(Long.MAX_VALUE, progress.getTotalAfkXp());
		assertEquals(Long.MAX_VALUE, progress.getSessionQualifyingTicks());
		assertEquals(Long.MAX_VALUE, progress.getTotalObservedTicks());
	}
}
