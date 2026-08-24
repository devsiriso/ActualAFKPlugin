package com.actualafk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IdleTrackerTest
{
	private static final String LOCATION_A = "location-a";
	private static final String LOCATION_B = "location-b";

	@Test
	public void firstObservationOnlyEstablishesLocation()
	{
		IdleTracker tracker = new IdleTracker(1);

		IdleTracker.TickResult result = tracker.observe(inactiveObservation(LOCATION_A));

		assertFalse(result.qualifies());
		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
	}

	@Test
	public void stationaryInactiveTickQualifiesAtThresholdOne()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(inactiveObservation(LOCATION_A));

		assertTrue(result.qualifies());
		assertEquals(1, result.currentStreakTicks());
		assertEquals(IdleTracker.TickState.INACTIVE, result.currentState());
	}

	@Test
	public void thresholdDoesNotBackfillEarlierTicks()
	{
		IdleTracker tracker = new IdleTracker(3);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult firstPending = tracker.observe(inactiveObservation(LOCATION_A));
		IdleTracker.TickResult secondPending = tracker.observe(inactiveObservation(LOCATION_A));
		assertFalse(firstPending.qualifies());
		assertEquals(IdleTracker.TickState.PENDING, firstPending.currentState());
		assertFalse(secondPending.qualifies());
		assertEquals(IdleTracker.TickState.PENDING, secondPending.currentState());
		IdleTracker.TickResult qualifyingResult = tracker.observe(inactiveObservation(LOCATION_A));

		assertTrue(qualifyingResult.qualifies());
		assertEquals(1, qualifyingResult.currentStreakTicks());
		assertEquals(3, tracker.getPendingInactiveTicks());
	}

	@Test
	public void movementResetsPendingAndStreak()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(inactiveObservation(LOCATION_B));

		assertFalse(result.qualifies());
		assertEquals(0, result.currentStreakTicks());
		assertEquals(0, tracker.getPendingInactiveTicks());
	}

	@Test
	public void animationResetsPendingAndStreak()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(observation(LOCATION_A, true, false, false));

		assertFalse(result.qualifies());
		assertEquals(0, result.currentStreakTicks());
	}

	@Test
	public void interactionResetsPendingAndStreak()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(observation(LOCATION_A, false, true, false));

		assertFalse(result.qualifies());
		assertEquals(0, result.currentStreakTicks());
	}

	@Test
	public void graceActivityResetsPendingAndStreak()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(observation(LOCATION_A, false, false, true));

		assertFalse(result.qualifies());
		assertEquals(0, result.currentStreakTicks());
	}

	@Test
	public void configuredGraceKeepsFollowingTicksActiveWithoutBackfill()
	{
		IdleTracker tracker = new IdleTracker(1, 2);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult signal = tracker.observe(activitySignals(
			LOCATION_A, false, false, false, true, false, false));
		assertEquals(IdleTracker.TickState.ACTIVE, signal.currentState());
		assertEquals(2, tracker.getRemainingActivityGraceTicks());

		IdleTracker.TickResult firstGrace = tracker.observe(inactiveObservation(LOCATION_A));
		assertEquals(IdleTracker.TickState.ACTIVE, firstGrace.currentState());
		assertEquals(1, tracker.getRemainingActivityGraceTicks());
		IdleTracker.TickResult secondGrace = tracker.observe(inactiveObservation(LOCATION_A));
		assertEquals(IdleTracker.TickState.ACTIVE, secondGrace.currentState());
		assertEquals(0, tracker.getRemainingActivityGraceTicks());

		IdleTracker.TickResult firstEligible = tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(firstEligible.qualifies());
		assertEquals(1, firstEligible.currentStreakTicks());
	}

	@Test
	public void combatSignalIsActiveAndStartsGrace()
	{
		IdleTracker tracker = new IdleTracker(1, 1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(activitySignals(
			LOCATION_A, false, false, false, false, true, false));

		assertFalse(result.qualifies());
		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
		assertEquals(1, tracker.getRemainingActivityGraceTicks());
	}

	@Test
	public void hitsplatSignalIsActive()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(activitySignals(
			LOCATION_A, false, false, false, false, false, true));

		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
		assertFalse(result.qualifies());
	}

	@Test
	public void trackedActionGraceSignalIsActive()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(activitySignals(
			LOCATION_A, false, false, false, false, false, true));

		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
		assertFalse(result.qualifies());
	}

	@Test
	public void unknownNewSignalsAreActiveByDefault()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(unknownNewSignals(LOCATION_A));

		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
		assertFalse(result.qualifies());
		assertEquals(0, tracker.getPendingInactiveTicks());
	}

	@Test
	public void graceSignalDoesNotExtendGraceWhenConfiguredZero()
	{
		IdleTracker tracker = new IdleTracker(1, 0);
		tracker.observe(inactiveObservation(LOCATION_A));
		tracker.observe(activitySignals(LOCATION_A, false, false, false, true, false, false));

		IdleTracker.TickResult next = tracker.observe(inactiveObservation(LOCATION_A));

		assertTrue(next.qualifies());
		assertEquals(0, tracker.getRemainingActivityGraceTicks());
	}

	@Test
	public void missingPlayerPausesAndResets()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(
			new IdleTracker.TickObservation(true, false, null, false, false, false));

		assertFalse(result.qualifies());
		assertEquals(IdleTracker.TickState.PAUSED, result.currentState());
		assertEquals(0, tracker.getCurrentStreakTicks());
	}

	@Test
	public void logoutResetsAndReportsLoggedOut()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());

		IdleTracker.TickResult result = tracker.observe(
			new IdleTracker.TickObservation(false, false, null, null, null, null));

		assertFalse(result.qualifies());
		assertEquals(IdleTracker.TickState.LOGGED_OUT, result.currentState());
		assertEquals(0, tracker.getCurrentStreakTicks());
	}

	@Test
	public void unknownActivitySignalIsActiveByDefault()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));

		IdleTracker.TickResult result = tracker.observe(
			new IdleTracker.TickObservation(true, true, LOCATION_A, null, false, false));

		assertFalse(result.qualifies());
		assertEquals(IdleTracker.TickState.ACTIVE, result.currentState());
		assertEquals(0, tracker.getCurrentStreakTicks());
	}

	@Test
	public void activeTickResetsStreakThenStationaryTicksCanContinue()
	{
		IdleTracker tracker = new IdleTracker(1);
		tracker.observe(inactiveObservation(LOCATION_A));
		assertTrue(tracker.observe(inactiveObservation(LOCATION_A)).qualifies());
		assertEquals(1, tracker.getCurrentStreakTicks());

		tracker.observe(observation(LOCATION_A, true, false, false));
		IdleTracker.TickResult result = tracker.observe(inactiveObservation(LOCATION_A));

		assertTrue(result.qualifies());
		assertEquals(1, result.currentStreakTicks());
	}

	@Test
	public void oneObservationCannotAwardTwice()
	{
		IdleTracker tracker = new IdleTracker(1);
		IdleTracker.TickObservation firstObservation = inactiveObservation(LOCATION_A);
		tracker.observe(firstObservation);

		IdleTracker.TickObservation secondObservation = inactiveObservation(LOCATION_A);
		IdleTracker.TickResult firstResult = tracker.observe(secondObservation);
		IdleTracker.TickResult repeatedResult = tracker.observe(secondObservation);
		IdleTracker.TickResult sameResult = tracker.getLastResult();

		assertTrue(firstResult.qualifies());
		assertEquals(firstResult.qualifies(), repeatedResult.qualifies());
		assertEquals(firstResult.currentStreakTicks(), repeatedResult.currentStreakTicks());
		assertEquals(firstResult.qualifies(), sameResult.qualifies());
		assertEquals(1, sameResult.currentStreakTicks());
	}

	@Test
	public void nullObservationIsRejected()
	{
		IdleTracker tracker = new IdleTracker(1);

		try
		{
			tracker.observe(null);
			assertTrue("expected null observation to be rejected", false);
		}
		catch (NullPointerException expected)
		{
			assertEquals("observation", expected.getMessage());
		}
	}

	private static IdleTracker.TickObservation inactiveObservation(Object location)
	{
		return observation(location, false, false, false);
	}

	private static IdleTracker.TickObservation observation(
		Object location,
		Boolean animationActive,
		Boolean interactionActive,
		Boolean activityGrace)
	{
		return new IdleTracker.TickObservation(
			true,
			true,
			location,
			animationActive,
			interactionActive,
			activityGrace);
	}

	private static IdleTracker.TickObservation activitySignals(
		Object location,
		Boolean animationActive,
		Boolean interactionActive,
		Boolean activityGrace,
		Boolean combatActive,
		Boolean hitsplatActive,
		Boolean trackedActionGrace)
	{
		return IdleTracker.TickObservation.withActivitySignals(
			true,
			true,
			location,
			animationActive,
			interactionActive,
			activityGrace,
			combatActive,
			hitsplatActive,
			trackedActionGrace);
	}

	private static IdleTracker.TickObservation unknownNewSignals(Object location)
	{
		return IdleTracker.TickObservation.withActivitySignals(
			true,
			true,
			location,
			false,
			false,
			false,
			null,
			null,
			null);
	}
}
