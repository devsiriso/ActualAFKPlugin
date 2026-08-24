package com.actualafk;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressOverlayTest
{
	@Test
	public void progressPercentageUsesCurrentAfkLevelRange()
	{
		assertEquals(0, ProgressOverlay.calculateProgressPercentage(83, 83, 174));
		assertEquals(50, ProgressOverlay.calculateProgressPercentage(128, 83, 173));
		assertEquals(100, ProgressOverlay.calculateProgressPercentage(200, 83, 173));
		assertEquals(100, ProgressOverlay.calculateProgressPercentage(13034431, 13034431, 13034431));
	}

	@Test
	public void inactiveStateUsesMutedDarkOrangeBackground()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		ProgressOverlay overlay = new ProgressOverlay(null, icon);

		overlay.updateDisplayedValues(1, 0, 0, 83, 0, "Active");
		render(overlay);
		assertEquals(ComponentConstants.STANDARD_BACKGROUND_COLOR, overlay.getBackgroundColor());

		overlay.updateDisplayedValues(1, 1, 0, 83, 1, "Inactive");
		render(overlay);
		assertEquals(new java.awt.Color(92, 64, 32, 190), overlay.getBackgroundColor());
	}

	@Test
	public void persistentOverlayRendersWithStandardPlacementBehavior()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		ProgressOverlay overlay = new ProgressOverlay(null, icon);
		overlay.updateDisplayedValues(2, 100, 83, 174, 100, "Inactive");

		BufferedImage canvas = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		Dimension renderedSize;
		try
		{
			renderedSize = overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}

		assertTrue(renderedSize.width > 0);
		assertTrue(renderedSize.height > 0);
		assertTrue(renderedSize.height <= 45);
		assertEquals(OverlayPosition.TOP_LEFT, overlay.getPosition());
		assertTrue(overlay.isMovable());
		assertTrue(overlay.isSnappable());
		assertFalse(overlay.isResizable());
	}

	@Test
	public void experiencePerHourUsesQualifyingXpOverEntireElapsedSession()
	{
		long tenSeconds = 10_000_000_000L;
		assertEquals(0, ProgressOverlay.calculateExperiencePerHour(0, tenSeconds));
		assertEquals(6000, ProgressOverlay.calculateExperiencePerHour(10, 6_000_000_000L));
		assertEquals(3000, ProgressOverlay.calculateExperiencePerHour(5, 6_000_000_000L));
		assertEquals(0, ProgressOverlay.calculateExperiencePerHour(10, 0));
	}

	@Test
	public void sessionAfkTimeUsesCompactSecondMinuteAndHourUnits()
	{
		assertEquals("0s", ProgressOverlay.formatSessionAfkTime(0));
		assertEquals("59s", ProgressOverlay.formatSessionAfkTime(99));
		assertEquals("1m", ProgressOverlay.formatSessionAfkTime(100));
		assertEquals("1m1s", ProgressOverlay.formatSessionAfkTime(102));
		assertEquals("59m59s", ProgressOverlay.formatSessionAfkTime(5_999));
		assertEquals("1h", ProgressOverlay.formatSessionAfkTime(6_000));
		assertEquals("1h", ProgressOverlay.formatSessionAfkTime(6_002));
		assertEquals("1h1m", ProgressOverlay.formatSessionAfkTime(6_102));
		assertEquals("0s", ProgressOverlay.formatSessionAfkTime(-1));
	}

	private static void render(ProgressOverlay overlay)
	{
		BufferedImage canvas = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try
		{
			overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
	}
}
