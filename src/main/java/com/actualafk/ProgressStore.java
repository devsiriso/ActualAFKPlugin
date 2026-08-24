package com.actualafk;

import net.runelite.client.config.ConfigManager;

public final class ProgressStore
{
	public static final String CONFIG_GROUP = "afk-mastery";

	private static final int FORMAT_VERSION = 2;
	private static final String FORMAT_VERSION_KEY = "formatVersion";
	private static final String TOTAL_AFK_XP_KEY = "totalVirtualXp";
	private static final String LONGEST_IDLE_STREAK_TICKS_KEY = "longestIdleStreakTicks";
	private static final String TOTAL_OBSERVED_TICKS_KEY = "totalObservedTicks";
	private static final String LEGACY_CURRENT_IDLE_STREAK_TICKS_KEY = "currentIdleStreakTicks";
	private static final String LEGACY_SESSION_QUALIFYING_TICKS_KEY = "sessionQualifyingTicks";

	private final ConfigurationStore configurationStore;
	private LoadStatus loadStatus = LoadStatus.NOT_LOADED;
	private boolean readFailed;

	/** The result of the most recent load attempt. */
	public enum LoadStatus
	{
		NOT_LOADED,
		EMPTY,
		LOADED,
		UNAVAILABLE,
		READ_FAILED,
		UNSUPPORTED_VERSION,
		INCOMPLETE
	}

	public ProgressStore(ConfigManager configManager)
	{
		this(new RuneLiteConfigurationStore(new RuneLiteConfigManagerAccess(configManager)));
	}

	ProgressStore(ConfigurationStore configurationStore)
	{
		this.configurationStore = configurationStore;
	}

	public AfkProgress load()
	{
		readFailed = false;
		try
		{
			if (!configurationStore.isAvailable())
			{
				loadStatus = LoadStatus.UNAVAILABLE;
				return new AfkProgress();
			}
		}
		catch (RuntimeException exception)
		{
			loadStatus = LoadStatus.READ_FAILED;
			return new AfkProgress();
		}

		String versionValue = readValue(FORMAT_VERSION_KEY);
		if (readFailed)
		{
			loadStatus = LoadStatus.READ_FAILED;
			return new AfkProgress();
		}

		if (versionValue == null)
		{
			// The marker is written last. Data without it is an interrupted or
			// otherwise incomplete write and must not be treated as committed.
			boolean hasProgress = hasAnyProgressValue();
			if (readFailed)
			{
				loadStatus = LoadStatus.READ_FAILED;
				return new AfkProgress();
			}
			if (hasProgress)
			{
				loadStatus = LoadStatus.INCOMPLETE;
				return new AfkProgress();
			}
			loadStatus = LoadStatus.EMPTY;
			return new AfkProgress();
		}

		long formatVersion;
		try
		{
			formatVersion = Long.parseLong(versionValue);
		}
		catch (RuntimeException exception)
		{
			loadStatus = LoadStatus.UNSUPPORTED_VERSION;
			return new AfkProgress();
		}
		if (formatVersion != 1 && formatVersion != FORMAT_VERSION)
		{
			loadStatus = LoadStatus.UNSUPPORTED_VERSION;
			return new AfkProgress();
		}

		long totalAfkXp = readNumber(TOTAL_AFK_XP_KEY, 0);
		AfkProgress progress = new AfkProgress(
			totalAfkXp,
			0,
			readNumber(LONGEST_IDLE_STREAK_TICKS_KEY, 0),
			0,
			formatVersion == 1 ? totalAfkXp : readNumber(TOTAL_OBSERVED_TICKS_KEY, totalAfkXp));
		loadStatus = readFailed ? LoadStatus.READ_FAILED : LoadStatus.LOADED;
		return progress;
	}

	public LoadStatus getLoadStatus()
	{
		return loadStatus;
	}

	/**
	 * Returns whether a normal save may replace the stored progress.
	 * A caller should load before saving; NOT_LOADED is allowed for backwards
	 * compatibility with a brand-new store.
	 */
	public boolean canSave()
	{
		return loadStatus == LoadStatus.NOT_LOADED
			|| loadStatus == LoadStatus.EMPTY
			|| loadStatus == LoadStatus.LOADED;
	}

	public boolean isAvailable()
	{
		return configurationStore.isAvailable();
	}

	public void save(AfkProgress progress)
	{
		save(progress, null);
	}

	public void save(AfkProgress progress, String profileKey)
	{
		if (!canSave() || (profileKey == null && !isAvailable()))
		{
			return;
		}

		// Remove the previous commit marker before updating an existing record.
		// If any later write fails, the next load sees an incomplete record and
		// refuses to overwrite it automatically.
		configurationStore.unset(profileKey, FORMAT_VERSION_KEY);
		configurationStore.set(profileKey, TOTAL_AFK_XP_KEY, Long.toString(progress.getTotalAfkXp()));
		configurationStore.set(profileKey, LONGEST_IDLE_STREAK_TICKS_KEY, Long.toString(progress.getLongestIdleStreakTicks()));
		configurationStore.set(profileKey, TOTAL_OBSERVED_TICKS_KEY, Long.toString(progress.getTotalObservedTicks()));
		configurationStore.unset(profileKey, LEGACY_CURRENT_IDLE_STREAK_TICKS_KEY);
		configurationStore.unset(profileKey, LEGACY_SESSION_QUALIFYING_TICKS_KEY);
		// Commit marker must be written last so an interrupted first write cannot
		// make a partially written record look valid.
		configurationStore.set(profileKey, FORMAT_VERSION_KEY, Integer.toString(FORMAT_VERSION));
	}

	public void reset()
	{
		if (!isAvailable())
		{
			return;
		}

		configurationStore.unset(null, FORMAT_VERSION_KEY);
		configurationStore.unset(null, TOTAL_AFK_XP_KEY);
		configurationStore.unset(null, LONGEST_IDLE_STREAK_TICKS_KEY);
		configurationStore.unset(null, TOTAL_OBSERVED_TICKS_KEY);
		configurationStore.unset(null, LEGACY_CURRENT_IDLE_STREAK_TICKS_KEY);
		configurationStore.unset(null, LEGACY_SESSION_QUALIFYING_TICKS_KEY);
		readFailed = false;
		loadStatus = LoadStatus.EMPTY;
	}

	private long readNumber(String key, long defaultValue)
	{
		String value = readValue(key);
		if (value == null)
		{
			return defaultValue;
		}
		try
		{
			long parsedValue = Long.parseLong(value);
			return parsedValue < 0 ? defaultValue : parsedValue;
		}
		catch (RuntimeException exception)
		{
			// A malformed field does not invalidate unrelated fields.
			return defaultValue;
		}
	}

	private String readValue(String key)
	{
		try
		{
			return configurationStore.get(key);
		}
		catch (RuntimeException exception)
		{
			readFailed = true;
			return null;
		}
	}

	private boolean hasAnyProgressValue()
	{
		return readValue(TOTAL_AFK_XP_KEY) != null
			|| readValue(LONGEST_IDLE_STREAK_TICKS_KEY) != null
			|| readValue(TOTAL_OBSERVED_TICKS_KEY) != null
			|| readValue(LEGACY_CURRENT_IDLE_STREAK_TICKS_KEY) != null
			|| readValue(LEGACY_SESSION_QUALIFYING_TICKS_KEY) != null;
	}

	interface ConfigurationStore
	{
		boolean isAvailable();

		String get(String key);

		void set(String profileKey, String key, String value);

		void unset(String profileKey, String key);
	}

	static final class RuneLiteConfigurationStore implements ConfigurationStore
	{
		private final ConfigManagerAccess configManager;

		RuneLiteConfigurationStore(ConfigManagerAccess configManager)
		{
			this.configManager = configManager;
		}

		@Override
		public boolean isAvailable()
		{
			return configManager.getRSProfileKey() != null;
		}

		@Override
		public String get(String key)
		{
			return configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
		}

		@Override
		public void set(String profileKey, String key, String value)
		{
			if (profileKey == null)
			{
				configManager.setRSProfileConfiguration(CONFIG_GROUP, key, value);
			}
			else
			{
				configManager.setConfiguration(CONFIG_GROUP, profileKey, key, value);
			}
		}

		@Override
		public void unset(String profileKey, String key)
		{
			if (profileKey == null)
			{
				configManager.unsetRSProfileConfiguration(CONFIG_GROUP, key);
			}
			else
			{
				configManager.unsetConfiguration(CONFIG_GROUP, profileKey, key);
			}
		}
	}

	interface ConfigManagerAccess
	{
		String getRSProfileKey();

		String getRSProfileConfiguration(String groupName, String key);

		void setRSProfileConfiguration(String groupName, String key, String value);

		void unsetRSProfileConfiguration(String groupName, String key);

		void setConfiguration(String groupName, String profileKey, String key, String value);

		void unsetConfiguration(String groupName, String profileKey, String key);
	}

	private static final class RuneLiteConfigManagerAccess implements ConfigManagerAccess
	{
		private final ConfigManager configManager;

		private RuneLiteConfigManagerAccess(ConfigManager configManager)
		{
			this.configManager = configManager;
		}

		@Override
		public String getRSProfileKey()
		{
			return configManager.getRSProfileKey();
		}

		@Override
		public String getRSProfileConfiguration(String groupName, String key)
		{
			return configManager.getRSProfileConfiguration(groupName, key);
		}

		@Override
		public void setRSProfileConfiguration(String groupName, String key, String value)
		{
			configManager.setRSProfileConfiguration(groupName, key, value);
		}

		@Override
		public void unsetRSProfileConfiguration(String groupName, String key)
		{
			configManager.unsetRSProfileConfiguration(groupName, key);
		}

		@Override
		public void setConfiguration(String groupName, String profileKey, String key, String value)
		{
			configManager.setConfiguration(groupName, profileKey, key, value);
		}

		@Override
		public void unsetConfiguration(String groupName, String profileKey, String key)
		{
			configManager.unsetConfiguration(groupName, profileKey, key);
		}
	}
}
