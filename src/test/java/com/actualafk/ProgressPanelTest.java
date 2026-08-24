package com.actualafk;

import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProgressPanelTest
{
	@Test
	public void canvasContextMenuTracksOverlayState() throws Exception
	{
		AtomicBoolean requestedState = new AtomicBoolean();
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel();
			panel.setCanvasOverlayToggleAction(requestedState::set);

			assertEquals("Add to canvas", panel.getCanvasMenuText());
			panel.activateCanvasMenuItem();
			assertTrue(requestedState.get());

			panel.setCanvasOverlayVisible(true);
			assertEquals("Remove from canvas", panel.getCanvasMenuText());
			panel.activateCanvasMenuItem();
			assertFalse(requestedState.get());
		});
	}

	@Test
	public void outerPanelRetainsRuneLitePadding() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel();
			Insets insets = panel.getBorder().getBorderInsets(panel);

			assertEquals(new Insets(6, 6, 6, 6), insets);
		});
	}

	@Test
	public void runeLiteScrollPaneDoesNotPaintAnOuterBorder() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel();
			JScrollPane scrollPane = findScrollPane(panel.getWrappedPanel());

			assertNotNull(scrollPane);
			assertNull(scrollPane.getBorder());
			assertNull(scrollPane.getViewportBorder());
		});
	}

	@Test
	public void afkTickDurationsUseTheNominalGameTickLength()
	{
		assertEquals("0s", ProgressPanel.formatTickDuration(0));
		assertEquals("1m 00s", ProgressPanel.formatTickDuration(100));
		assertEquals("1h 00m 00s", ProgressPanel.formatTickDuration(6000));
		assertEquals("1d 00h 00m", ProgressPanel.formatTickDuration(144000));
	}

	@Test
	public void sessionAfkPercentageUsesObservedLoggedInTicks()
	{
		assertEquals("0.0%", ProgressPanel.formatPercentage(0, 0));
		assertEquals("25.0%", ProgressPanel.formatPercentage(25, 100));
		assertEquals("33.3%", ProgressPanel.formatPercentage(1, 3));
		assertEquals("100.0%", ProgressPanel.formatPercentage(200, 100));
	}

	@Test
	public void sidebarStatisticsRespectIndividualSettings() throws Exception
	{
		AtomicBoolean showSessionRate = new AtomicBoolean(true);
		AtomicBoolean showTotalTime = new AtomicBoolean(false);
		AfkConfig config = new AfkConfig()
		{
			@Override public boolean showPanelSessionXpPerHour() { return showSessionRate.get(); }
			@Override public boolean showPanelTotalTime() { return showTotalTime.get(); }
		};
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel(config);
			assertTrue(panel.isTotalExperienceVisible());
			assertTrue(panel.isSessionExperiencePerHourVisible());
			assertTrue(panel.isLevelProgressVisible());
			assertFalse(panel.isStatisticVisible(PanelStatistic.TOTAL_TIME));

			showTotalTime.set(true);
			showSessionRate.set(false);
			panel.refreshStatisticVisibility();

			assertTrue(panel.isStatisticVisible(PanelStatistic.TOTAL_TIME));
			assertFalse(panel.isSessionExperiencePerHourVisible());
		});
	}

	@Test
	public void everyConfiguredStatisticIsRegisteredWithThePanel() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel();
			for (PanelStatistic statistic : PanelStatistic.values())
			{
				assertTrue(statistic.toString(), panel.isStatisticVisible(statistic));
			}
		});
	}

	@Test
	public void hiddenRowsCollapseAndEmptySessionHeadingDisappears() throws Exception
	{
		AfkConfig config = new AfkConfig()
		{
			@Override public boolean showPanelSessionAfkTicks() { return false; }
			@Override public boolean showPanelSessionAfkTime() { return false; }
			@Override public boolean showPanelSessionTotalTime() { return false; }
			@Override public boolean showPanelSessionIdlePercentage() { return false; }
			@Override public boolean showPanelSessionActivePercentage() { return false; }
			@Override public boolean showPanelSessionAfkXp() { return false; }
			@Override public boolean showPanelSessionXpPerHour() { return false; }
		};
		SwingUtilities.invokeAndWait(() ->
		{
			ProgressPanel panel = new ProgressPanel(config);
			assertFalse(panel.isSectionHeadingVisible("Session"));
			assertTrue(panel.isSectionHeadingVisible("Total"));
			assertTrue(panel.isSectionHeadingVisible("Streak"));
			assertEquals(26, panel.getDisplayedStatisticComponentCount());
		});
	}

	private static JScrollPane findScrollPane(Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JScrollPane)
			{
				return (JScrollPane) component;
			}
			if (component instanceof Container)
			{
				JScrollPane scrollPane = findScrollPane((Container) component);
				if (scrollPane != null)
				{
					return scrollPane;
				}
			}
		}
		return null;
	}
}
