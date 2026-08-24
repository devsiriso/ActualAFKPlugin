package com.actualafk;

import java.util.Objects;

public final class IdleTracker
{
	private final int minimumIdleTicks;
	private final int activityGraceTicks;
	private Object previousLocation;
	private int pendingInactiveTicks;
	private int currentStreakTicks;
	private int remainingActivityGraceTicks;
	private TickObservation lastObservation;
	private TickResult lastResult = TickResult.initial();

	public IdleTracker(int minimumIdleTicks)
	{
		this(minimumIdleTicks, 0);
	}

	/**
	 * Creates a tracker with an exact trailing grace window. An observation with
	 * a known activity signal is active itself and keeps the following
	 * {@code activityGraceTicks} observations active. Pending observations are
	 * never converted into qualifying observations when the window expires.
	 */
	public IdleTracker(int minimumIdleTicks, int activityGraceTicks)
	{
		this.minimumIdleTicks = Math.max(1, minimumIdleTicks);
		this.activityGraceTicks = Math.max(0, activityGraceTicks);
	}

	public TickResult observe(TickObservation observation)
	{
		Objects.requireNonNull(observation, "observation");
		if (observation == lastObservation)
		{
			return lastResult;
		}
		lastObservation = observation;

		if (!Boolean.TRUE.equals(observation.loggedIn()))
		{
			return reset(TickState.LOGGED_OUT);
		}

		if (!Boolean.TRUE.equals(observation.playerAvailable()) || observation.location() == null)
		{
			return reset(TickState.PAUSED);
		}

		if (previousLocation == null)
		{
			previousLocation = observation.location();
			if (hasKnownActivitySignal(observation))
			{
				startActivityGrace();
			}
			return resetResult(TickState.ACTIVE);
		}

		boolean locationChanged = !Objects.equals(previousLocation, observation.location());
		previousLocation = observation.location();

		if (locationChanged)
		{
			startActivityGrace();
			return activeResult();
		}

		if (!isInactiveObservation(observation))
		{
			if (hasKnownActivitySignal(observation))
			{
				startActivityGrace();
			}
			else if (remainingActivityGraceTicks > 0)
			{
				remainingActivityGraceTicks--;
			}
			return activeResult();
		}

		if (remainingActivityGraceTicks > 0)
		{
			remainingActivityGraceTicks--;
			return activeResult();
		}

		if (pendingInactiveTicks < Integer.MAX_VALUE)
		{
			pendingInactiveTicks++;
		}
		if (pendingInactiveTicks < minimumIdleTicks)
		{
			lastResult = new TickResult(false, currentStreakTicks, TickState.PENDING);
			return lastResult;
		}

		if (currentStreakTicks < Integer.MAX_VALUE)
		{
			currentStreakTicks++;
		}
		lastResult = new TickResult(true, currentStreakTicks, TickState.INACTIVE);
		return lastResult;
	}

	public TickResult getLastResult()
	{
		return lastResult;
	}

	public int getMinimumIdleTicks()
	{
		return minimumIdleTicks;
	}

	public int getActivityGraceTicks()
	{
		return activityGraceTicks;
	}

	public int getRemainingActivityGraceTicks()
	{
		return remainingActivityGraceTicks;
	}

	public int getPendingInactiveTicks()
	{
		return pendingInactiveTicks;
	}

	public int getCurrentStreakTicks()
	{
		return currentStreakTicks;
	}

	public void reset()
	{
		previousLocation = null;
		pendingInactiveTicks = 0;
		currentStreakTicks = 0;
		remainingActivityGraceTicks = 0;
		lastObservation = null;
		lastResult = TickResult.initial();
	}

	private boolean isInactiveObservation(TickObservation observation)
	{
		return Boolean.FALSE.equals(observation.animationActive())
			&& Boolean.FALSE.equals(observation.interactionActive())
			&& Boolean.FALSE.equals(observation.activityGrace())
			&& Boolean.FALSE.equals(observation.combatActive())
			&& Boolean.FALSE.equals(observation.hitsplatActive())
			&& Boolean.FALSE.equals(observation.trackedActionGrace());
	}

	private boolean hasKnownActivitySignal(TickObservation observation)
	{
		return Boolean.TRUE.equals(observation.animationActive())
			|| Boolean.TRUE.equals(observation.interactionActive())
			|| Boolean.TRUE.equals(observation.activityGrace())
			|| Boolean.TRUE.equals(observation.combatActive())
			|| Boolean.TRUE.equals(observation.hitsplatActive())
			|| Boolean.TRUE.equals(observation.trackedActionGrace());
	}

	private void startActivityGrace()
	{
		remainingActivityGraceTicks = activityGraceTicks;
	}

	private TickResult activeResult()
	{
		pendingInactiveTicks = 0;
		currentStreakTicks = 0;
		lastResult = new TickResult(false, 0, TickState.ACTIVE);
		return lastResult;
	}

	private TickResult reset(TickState state)
	{
		reset();
		lastResult = new TickResult(false, 0, state);
		return lastResult;
	}

	private TickResult resetResult(TickState state)
	{
		pendingInactiveTicks = 0;
		currentStreakTicks = 0;
		lastResult = new TickResult(false, 0, state);
		return lastResult;
	}

	public enum TickState
	{
		INACTIVE,
		PENDING,
		ACTIVE,
		PAUSED,
		LOGGED_OUT
	}

	public static final class TickObservation
	{
		private final Boolean loggedIn;
		private final Boolean playerAvailable;
		private final Object location;
		private final Boolean animationActive;
		private final Boolean interactionActive;
		private final Boolean activityGrace;
		private final Boolean combatActive;
		private final Boolean hitsplatActive;
		private final Boolean trackedActionGrace;

		public TickObservation(
			Boolean loggedIn,
			Boolean playerAvailable,
			Object location,
			Boolean animationActive,
			Boolean interactionActive,
			Boolean activityGrace)
		{
			this.loggedIn = loggedIn;
			this.playerAvailable = playerAvailable;
			this.location = location;
			this.animationActive = animationActive;
			this.interactionActive = interactionActive;
			this.activityGrace = activityGrace;
			this.combatActive = false;
			this.hitsplatActive = false;
			this.trackedActionGrace = false;
		}

		private TickObservation(
			Boolean loggedIn,
			Boolean playerAvailable,
			Object location,
			Boolean animationActive,
			Boolean interactionActive,
			Boolean activityGrace,
			Boolean combatActive,
			Boolean hitsplatActive,
			Boolean trackedActionGrace)
		{
			this.loggedIn = loggedIn;
			this.playerAvailable = playerAvailable;
			this.location = location;
			this.animationActive = animationActive;
			this.interactionActive = interactionActive;
			this.activityGrace = activityGrace;
			this.combatActive = combatActive;
			this.hitsplatActive = hitsplatActive;
			this.trackedActionGrace = trackedActionGrace;
		}

		/**
		 * Creates an observation with the activity signals that require
		 * conservative classification. A null signal is unknown and therefore
		 * makes the observation active, just like the legacy fields.
		 */
		public static TickObservation withActivitySignals(
			Boolean loggedIn,
			Boolean playerAvailable,
			Object location,
			Boolean animationActive,
			Boolean interactionActive,
			Boolean activityGrace,
			Boolean combatActive,
			Boolean hitsplatActive,
			Boolean trackedActionGrace)
		{
			return new TickObservation(
				loggedIn,
				playerAvailable,
				location,
				animationActive,
				interactionActive,
				activityGrace,
				combatActive,
				hitsplatActive,
				trackedActionGrace);
		}

		public Boolean loggedIn()
		{
			return loggedIn;
		}

		public Boolean playerAvailable()
		{
			return playerAvailable;
		}

		public Object location()
		{
			return location;
		}

		public Boolean animationActive()
		{
			return animationActive;
		}

		public Boolean interactionActive()
		{
			return interactionActive;
		}

		public Boolean activityGrace()
		{
			return activityGrace;
		}

		public Boolean combatActive()
		{
			return combatActive;
		}

		public Boolean hitsplatActive()
		{
			return hitsplatActive;
		}

		public Boolean trackedActionGrace()
		{
			return trackedActionGrace;
		}
	}

	public static final class TickResult
	{
		private final boolean qualifies;
		private final int currentStreakTicks;
		private final TickState currentState;

		public TickResult(boolean qualifies, int currentStreakTicks, TickState currentState)
		{
			this.qualifies = qualifies;
			this.currentStreakTicks = currentStreakTicks;
			this.currentState = Objects.requireNonNull(currentState, "currentState");
		}

		public boolean qualifies()
		{
			return qualifies;
		}

		public int currentStreakTicks()
		{
			return currentStreakTicks;
		}

		public TickState currentState()
		{
			return currentState;
		}

		private static TickResult initial()
		{
			return new TickResult(false, 0, TickState.PAUSED);
		}
	}
}
