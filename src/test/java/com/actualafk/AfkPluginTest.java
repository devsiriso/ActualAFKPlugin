package com.actualafk;

import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AfkPluginTest
{
	@Test
	public void descriptorAndToolbarPanelWiringArePresent()
	{
		PluginDescriptor descriptor = AfkPlugin.class.getAnnotation(PluginDescriptor.class);

		assertNotNull(descriptor);
		assertEquals("Actual AFK", descriptor.name());
		assertEquals("Track your AFK time and progress.", descriptor.description());
		assertNotNull(ProgressPanel.class);
		assertNotNull(ProgressOverlay.class);
		assertNotNull(AfkPlugin.class.getResource("/com/actualafk/icon.png"));
	}

	@Test
	public void aClientTickCanOnlyBeProcessedOnce()
	{
		AfkPlugin plugin = new AfkPlugin();

		assertTrue(plugin.beginGameTick(100));
		assertFalse(plugin.beginGameTick(100));
		assertTrue(plugin.beginGameTick(101));
	}
}
