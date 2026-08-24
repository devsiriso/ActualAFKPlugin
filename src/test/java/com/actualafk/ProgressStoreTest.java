package com.actualafk;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProgressStoreTest
{
	@Test
	public void lifetimeValuesRoundTripAndSessionValuesRestartAtZero()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		ProgressStore store = new ProgressStore(configuration);
		AfkProgress expected = new AfkProgress(100, 4, 8, 12, 250);

		store.save(expected);
		AfkProgress actual = store.load();

		assertEquals(100, actual.getTotalAfkXp());
		assertEquals(0, actual.getCurrentIdleStreakTicks());
		assertEquals(8, actual.getLongestIdleStreakTicks());
		assertEquals(0, actual.getSessionQualifyingTicks());
		assertEquals(250, actual.getTotalObservedTicks());
		assertEquals("2", configuration.values.get("formatVersion"));
		assertNull(configuration.values.get("currentIdleStreakTicks"));
		assertNull(configuration.values.get("sessionQualifyingTicks"));
		assertEquals(ProgressStore.LoadStatus.LOADED, store.getLoadStatus());
	}

	@Test
	public void missingValuesLoadAsZero()
	{
		AfkProgress progress = new ProgressStore(new InMemoryConfigurationStore()).load();

		assertEquals(0, progress.getTotalAfkXp());
		assertEquals(0, progress.getLongestIdleStreakTicks());
	}

	@Test
	public void malformedAndNegativeValuesLoadAsZero()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "1");
		configuration.values.put("totalVirtualXp", "not-a-number");
		configuration.values.put("currentIdleStreakTicks", "-1");
		configuration.values.put("longestIdleStreakTicks", "8");
		configuration.values.put("sessionQualifyingTicks", "12");

		AfkProgress progress = new ProgressStore(configuration).load();

		assertEquals(0, progress.getTotalAfkXp());
		assertEquals(0, progress.getCurrentIdleStreakTicks());
		assertEquals(8, progress.getLongestIdleStreakTicks());
		assertEquals(0, progress.getSessionQualifyingTicks());
	}

	@Test
	public void unsupportedVersionLoadsDefaults()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "3");
		configuration.values.put("totalVirtualXp", "100");

		ProgressStore store = new ProgressStore(configuration);
		assertEquals(0, store.load().getTotalAfkXp());
		assertEquals(ProgressStore.LoadStatus.UNSUPPORTED_VERSION, store.getLoadStatus());
		store.save(new AfkProgress(999, 0, 0, 0));
		assertEquals("100", configuration.values.get("totalVirtualXp"));
	}

	@Test
	public void missingMarkerWithDataIsNotOverwritten()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("totalVirtualXp", "100");
		ProgressStore store = new ProgressStore(configuration);

		assertEquals(0, store.load().getTotalAfkXp());
		assertEquals(ProgressStore.LoadStatus.INCOMPLETE, store.getLoadStatus());
		store.save(new AfkProgress(999, 0, 0, 0));
		assertEquals("100", configuration.values.get("totalVirtualXp"));
		assertNull(configuration.values.get("formatVersion"));
	}

	@Test
	public void transientReadFailureRefusesSave()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "2");
		configuration.values.put("totalVirtualXp", "100");
		configuration.failReadsFor.add("longestIdleStreakTicks");
		ProgressStore store = new ProgressStore(configuration);

		assertEquals(100, store.load().getTotalAfkXp());
		assertEquals(ProgressStore.LoadStatus.READ_FAILED, store.getLoadStatus());
		store.save(new AfkProgress(999, 0, 0, 0));
		assertEquals("100", configuration.values.get("totalVirtualXp"));
	}

	@Test
	public void malformedFieldDefaultsWithoutPreventingValidFieldsFromLoading()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "2");
		configuration.values.put("totalVirtualXp", "not-a-number");
		configuration.values.put("longestIdleStreakTicks", "8");
		configuration.values.put("totalObservedTicks", Long.toString(Long.MAX_VALUE));

		ProgressStore store = new ProgressStore(configuration);
		AfkProgress progress = store.load();

		assertEquals(0, progress.getTotalAfkXp());
		assertEquals(8, progress.getLongestIdleStreakTicks());
		assertEquals(Long.MAX_VALUE, progress.getTotalObservedTicks());
		assertEquals(ProgressStore.LoadStatus.LOADED, store.getLoadStatus());
	}

	@Test
	public void saveWritesFormatMarkerLast()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		ProgressStore store = new ProgressStore(configuration);

		store.save(new AfkProgress(42, 0, 7, 0));

		assertEquals("formatVersion", configuration.writes.get(configuration.writes.size() - 1));
	}

	@Test
	public void interruptedUpdateRemovesCommitMarkerAndProtectsPartialData()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		ProgressStore store = new ProgressStore(configuration);
		store.save(new AfkProgress(10, 0, 2, 0));
		configuration.failWritesFor.add("longestIdleStreakTicks");

		try
		{
			store.save(new AfkProgress(20, 0, 4, 0));
			assertTrue("expected simulated write failure", false);
		}
		catch (IllegalStateException expected)
		{
			assertEquals("simulated write failure", expected.getMessage());
		}

		ProgressStore reloadedStore = new ProgressStore(configuration);
		reloadedStore.load();
		assertEquals(ProgressStore.LoadStatus.INCOMPLETE, reloadedStore.getLoadStatus());
		assertNull(configuration.values.get("formatVersion"));
	}

	@Test
	public void versionOneDataMigratesWithKnownIdleTicksAsObservedBaseline()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "1");
		configuration.values.put("totalVirtualXp", "100");

		AfkProgress progress = new ProgressStore(configuration).load();

		assertEquals(100, progress.getTotalAfkXp());
		assertEquals(100, progress.getTotalObservedTicks());
	}

	@Test
	public void resetRemovesOnlyProgressKeys()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		ProgressStore store = new ProgressStore(configuration);
		store.save(new AfkProgress(10, 1, 2, 3));
		configuration.values.put("unrelatedSetting", "keep");

		store.reset();

		assertEquals(0, new ProgressStore(configuration).load().getTotalAfkXp());
		assertEquals("keep", configuration.values.get("unrelatedSetting"));
	}

	@Test
	public void explicitResetAllowsSavingAfterUnsupportedVersion()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "3");
		configuration.values.put("totalVirtualXp", "100");
		ProgressStore store = new ProgressStore(configuration);
		store.load();

		store.reset();
		store.save(new AfkProgress(5, 0, 0, 0));

		assertEquals(ProgressStore.LoadStatus.EMPTY, store.getLoadStatus());
		assertEquals("2", configuration.values.get("formatVersion"));
		assertEquals("5", configuration.values.get("totalVirtualXp"));
	}

	@Test
	public void unavailableProfileCannotBeOverwrittenWithDefaults()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "1");
		configuration.values.put("totalVirtualXp", "280");
		configuration.available = false;

		ProgressStore store = new ProgressStore(configuration);
		store.save(new AfkProgress());
		store.reset();

		assertEquals("1", configuration.values.get("formatVersion"));
		assertEquals("280", configuration.values.get("totalVirtualXp"));
	}

	@Test
	public void progressLoadsAfterProfileBecomesAvailable()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		configuration.values.put("formatVersion", "1");
		configuration.values.put("totalVirtualXp", "280");
		configuration.available = false;
		ProgressStore store = new ProgressStore(configuration);

		assertFalse(store.isAvailable());
		configuration.available = true;

		assertTrue(store.isAvailable());
		assertEquals(280, store.load().getTotalAfkXp());
	}

	@Test
	public void progressCanBeSavedToAnExplicitPreviousProfile()
	{
		InMemoryConfigurationStore configuration = new InMemoryConfigurationStore();
		ProgressStore store = new ProgressStore(configuration);

		store.save(new AfkProgress(42, 0, 7, 0), "previous-profile");

		assertEquals("previous-profile", configuration.lastWrittenProfileKey);
		assertEquals("42", configuration.values.get("totalVirtualXp"));
		assertEquals("7", configuration.values.get("longestIdleStreakTicks"));
	}

	@Test
	public void runeLiteAdapterPassesGroupBeforeExplicitProfile()
	{
		RecordingConfigManagerAccess configManager = new RecordingConfigManagerAccess();
		ProgressStore store = new ProgressStore(new ProgressStore.RuneLiteConfigurationStore(configManager));

		store.save(new AfkProgress(42, 0, 7, 0), "previous-profile");

		assertEquals(ProgressStore.CONFIG_GROUP, configManager.lastSetGroup);
		assertEquals("previous-profile", configManager.lastSetProfile);
		assertEquals(ProgressStore.CONFIG_GROUP, configManager.lastUnsetGroup);
		assertEquals("previous-profile", configManager.lastUnsetProfile);
	}

	private static final class InMemoryConfigurationStore implements ProgressStore.ConfigurationStore
	{
		private final Map<String, String> values = new HashMap<>();
		private final List<String> writes = new ArrayList<>();
		private final List<String> failReadsFor = new ArrayList<>();
		private final List<String> failWritesFor = new ArrayList<>();
		private boolean available = true;
		private String lastWrittenProfileKey;

		@Override
		public boolean isAvailable()
		{
			return available;
		}

		@Override
		public String get(String key)
		{
			if (failReadsFor.contains(key))
			{
				throw new IllegalStateException("simulated read failure");
			}
			return values.get(key);
		}

		@Override
		public void set(String profileKey, String key, String value)
		{
			if (failWritesFor.contains(key))
			{
				throw new IllegalStateException("simulated write failure");
			}
			lastWrittenProfileKey = profileKey;
			writes.add(key);
			values.put(key, value);
		}

		@Override
		public void unset(String profileKey, String key)
		{
			values.remove(key);
		}
	}

	private static final class RecordingConfigManagerAccess implements ProgressStore.ConfigManagerAccess
	{
		private final Map<String, String> values = new HashMap<>();
		private String lastSetGroup;
		private String lastSetProfile;
		private String lastUnsetGroup;
		private String lastUnsetProfile;

		@Override
		public String getRSProfileKey()
		{
			return "current-profile";
		}

		@Override
		public String getRSProfileConfiguration(String groupName, String key)
		{
			return values.get(key);
		}

		@Override
		public void setRSProfileConfiguration(String groupName, String key, String value)
		{
			values.put(key, value);
		}

		@Override
		public void unsetRSProfileConfiguration(String groupName, String key)
		{
			values.remove(key);
		}

		@Override
		public void setConfiguration(String groupName, String profileKey, String key, String value)
		{
			lastSetGroup = groupName;
			lastSetProfile = profileKey;
			values.put(key, value);
		}

		@Override
		public void unsetConfiguration(String groupName, String profileKey, String key)
		{
			lastUnsetGroup = groupName;
			lastUnsetProfile = profileKey;
			values.remove(key);
		}
	}
}
