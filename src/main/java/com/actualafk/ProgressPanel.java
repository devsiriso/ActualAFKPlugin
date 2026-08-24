package com.actualafk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.QuantityFormatter;

public class ProgressPanel extends PluginPanel
{
	private final JLabel levelLabel = new JLabel();
	private final JLabel experienceLabel = new JLabel();
	private final JLabel stateLabel = new JLabel();
	private final JLabel streakLabel = new JLabel();
	private final JLabel currentStreakTicksValue = new JLabel();
	private final JLabel currentStreakTimeValue = new JLabel();
	private final JLabel longestStreakTicksValue = new JLabel();
	private final JLabel longestStreakTimeValue = new JLabel();
	private final JLabel sessionTicksValue = new JLabel();
	private final JLabel sessionTimeValue = new JLabel();
	private final JLabel sessionTotalTimeValue = new JLabel();
	private final JLabel sessionAfkPercentageValue = new JLabel();
	private final JLabel sessionActivePercentageValue = new JLabel();
	private final JLabel sessionExperienceValue = new JLabel();
	private final JLabel totalTicksValue = new JLabel();
	private final JLabel totalTimeValue = new JLabel();
	private final JLabel totalAfkPercentageValue = new JLabel();
	private final JLabel totalActivePercentageValue = new JLabel();
	private final JLabel totalExperienceValue = new JLabel();
	private final JLabel totalExperienceName = new JLabel("Total AFK XP");
	private final JLabel sessionExperiencePerHourValue = new JLabel();
	private final JLabel sessionExperiencePerHourName = new JLabel("Session XP/hr");
	private final JLabel experienceToNextLevelValue = new JLabel();
	private final ProgressBar levelProgressBar = new ProgressBar();
	private final JPanel progressWrapper = new JPanel(new BorderLayout());
	private final JButton resetProgressButton = new JButton("Reset progress");
	private final JPopupMenu canvasMenu = new JPopupMenu();
	private final JMenuItem canvasMenuItem = new JMenuItem("Add to canvas");
	private Runnable resetAction = () ->
	{
	};
	private Consumer<Boolean> canvasOverlayToggleAction = enabled ->
	{
	};
	private boolean canvasOverlayVisible;
	private final AfkConfig config;
	private final EnumMap<PanelStatistic, List<Component>> statisticComponents =
		new EnumMap<>(PanelStatistic.class);
	private final List<StatisticRow> statisticRows = new ArrayList<>();
	private JPanel statisticsPanel;
	private StatisticSection currentStatisticSection = StatisticSection.UNGROUPED;
	private final EnumMap<StatisticSection, List<Component>> sectionHeadingComponents =
		new EnumMap<>(StatisticSection.class);
	private volatile long sessionStartNanos = System.nanoTime();

	public ProgressPanel()
	{
		this(null);
	}

	@Inject
	ProgressPanel(AfkConfig config)
	{
		this.config = config;
		getScrollPane().setBorder(null);
		getScrollPane().setViewportBorder(null);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(createTitle());
		content.add(createLevelCard());
		content.add(createStatisticsCard());
		content.add(createDetailsCard());
		add(content, BorderLayout.NORTH);

		canvasMenuItem.addActionListener(event -> canvasOverlayToggleAction.accept(!canvasOverlayVisible));
		canvasMenu.add(canvasMenuItem);

		updateDisplayedValues(ProgressSnapshot.initial());
	}

	public void setResetAction(Runnable resetAction)
	{
		this.resetAction = Objects.requireNonNull(resetAction, "resetAction");
	}

	public void setCanvasOverlayToggleAction(Consumer<Boolean> canvasOverlayToggleAction)
	{
		this.canvasOverlayToggleAction = Objects.requireNonNull(canvasOverlayToggleAction, "canvasOverlayToggleAction");
	}

	/**
	 * Starts a new session-rate clock. The update is kept on Swing's event
	 * dispatch thread because the rate is read while rendering panel labels.
	 */
	public void resetSessionTiming()
	{
		Runnable reset = () -> sessionStartNanos = System.nanoTime();
		if (SwingUtilities.isEventDispatchThread())
		{
			reset.run();
		}
		else
		{
			SwingUtilities.invokeLater(reset);
		}
	}

	public void setCanvasOverlayVisible(boolean canvasOverlayVisible)
	{
		Runnable update = () ->
		{
			this.canvasOverlayVisible = canvasOverlayVisible;
			canvasMenuItem.setText(canvasOverlayVisible ? "Remove from canvas" : "Add to canvas");
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			update.run();
		}
		else
		{
			SwingUtilities.invokeLater(update);
		}
	}

	public void updateDisplayedValues(ProgressSnapshot values)
	{
		Objects.requireNonNull(values, "values");
		Runnable update = () -> applyDisplayedValues(values);
		if (SwingUtilities.isEventDispatchThread())
		{
			update.run();
		}
		else
		{
			SwingUtilities.invokeLater(update);
		}
	}

	private JPanel createTitle()
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titlePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
			new EmptyBorder(6, 0, 8, 0)));

		JPanel titleLabels = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		titleLabels.setOpaque(false);

		JLabel titleLabel = new JLabel("Actual AFK");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(FontManager.getRunescapeBoldFont().getSize2D() + 3));

		JLabel betaLabel = new JLabel("BETA");
		betaLabel.setForeground(ColorScheme.BRAND_ORANGE);
		betaLabel.setFont(FontManager.getRunescapeBoldFont());

		titleLabels.add(titleLabel);
		titleLabels.add(betaLabel);
		titlePanel.add(titleLabels, BorderLayout.CENTER);
		return titlePanel;
	}

	private JPanel createLevelCard()
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder());

		JLabel iconLabel = new JLabel(loadClockIcon());
		iconLabel.setPreferredSize(new Dimension(42, 42));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);

		JPanel statistics = new JPanel(new DynamicGridLayout(2, 2));
		statistics.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statistics.setBorder(new EmptyBorder(9, 2, 9, 2));
		for (JLabel label : new JLabel[]{levelLabel, experienceLabel, stateLabel, streakLabel})
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			statistics.add(label);
		}

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.add(iconLabel, BorderLayout.WEST);
		header.add(statistics, BorderLayout.CENTER);

		progressWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		progressWrapper.setBorder(new EmptyBorder(0, 7, 7, 7));
		levelProgressBar.setMaximumValue(100);
		levelProgressBar.setBackground(new Color(61, 56, 49));
		levelProgressBar.setForeground(ColorScheme.BRAND_ORANGE);
		progressWrapper.add(levelProgressBar, BorderLayout.CENTER);

		card.add(header, BorderLayout.NORTH);
		card.add(progressWrapper, BorderLayout.SOUTH);
		installCanvasMenu(card);
		return card;
	}

	private JPanel createDetailsCard()
	{
		JPanel details = new JPanel(new BorderLayout());
		details.setBackground(ColorScheme.DARK_GRAY_COLOR);
		details.setBorder(new EmptyBorder(10, 4, 4, 4));

		resetProgressButton.addActionListener(event -> confirmReset());
		resetProgressButton.setFont(FontManager.getRunescapeSmallFont());
		resetProgressButton.setFocusPainted(false);
		resetProgressButton.setBorderPainted(false);
		resetProgressButton.setOpaque(true);
		resetProgressButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		resetProgressButton.setForeground(Color.WHITE);

		details.add(resetProgressButton, BorderLayout.CENTER);
		return details;
	}

	private JPanel createStatisticsCard()
	{
		JPanel statistics = new JPanel(new DynamicGridLayout(0, 2));
		statisticsPanel = statistics;
		statistics.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statistics.setBorder(new EmptyBorder(10, 7, 10, 7));
		addSectionHeading(statistics, "Streak");
		addStatistic(statistics, PanelStatistic.CURRENT_STREAK_TICKS, "Current streak ticks", currentStreakTicksValue);
		addStatistic(statistics, PanelStatistic.CURRENT_STREAK_TIME, "Current time (est.)", currentStreakTimeValue);
		addStatistic(statistics, PanelStatistic.LONGEST_STREAK_TICKS, "Longest streak ticks", longestStreakTicksValue);
		addStatistic(statistics, PanelStatistic.LONGEST_STREAK_TIME, "Longest time (est.)", longestStreakTimeValue);
		addSectionHeading(statistics, "Session");
		addStatistic(statistics, PanelStatistic.SESSION_AFK_TICKS, "Session AFK ticks", sessionTicksValue);
		addStatistic(statistics, PanelStatistic.SESSION_AFK_TIME, "Session AFK time (est.)", sessionTimeValue);
		addStatistic(statistics, PanelStatistic.SESSION_TOTAL_TIME, "Session total time (est.)", sessionTotalTimeValue);
		addStatistic(statistics, PanelStatistic.SESSION_IDLE_PERCENTAGE, "Session idle", sessionAfkPercentageValue);
		addStatistic(statistics, PanelStatistic.SESSION_ACTIVE_PERCENTAGE, "Session active", sessionActivePercentageValue);
		addStatistic(statistics, PanelStatistic.SESSION_AFK_XP, "Session AFK XP", sessionExperienceValue);
		addStatistic(statistics, PanelStatistic.SESSION_XP_PER_HOUR, sessionExperiencePerHourName, sessionExperiencePerHourValue);
		addSectionHeading(statistics, "Total");
		addStatistic(statistics, PanelStatistic.TOTAL_AFK_TICKS, "Total AFK ticks", totalTicksValue);
		addStatistic(statistics, PanelStatistic.TOTAL_TIME, "Total time (est.)", totalTimeValue);
		addStatistic(statistics, PanelStatistic.TOTAL_IDLE_PERCENTAGE, "Total idle", totalAfkPercentageValue);
		addStatistic(statistics, PanelStatistic.TOTAL_ACTIVE_PERCENTAGE, "Total active", totalActivePercentageValue);
		addStatistic(statistics, PanelStatistic.TOTAL_AFK_XP, totalExperienceName, totalExperienceValue);
		addStatistic(statistics, PanelStatistic.XP_TO_NEXT_LEVEL, "XP to next level", experienceToNextLevelValue);
		return statistics;
	}

	private void addSectionHeading(JPanel statistics, String text)
	{
		currentStatisticSection = StatisticSection.valueOf(text.toUpperCase(Locale.ROOT));
		List<Component> headingComponents = new ArrayList<>();
		if (currentStatisticSection != StatisticSection.STREAK)
		{
			JLabel lineSpacerLeft = new JLabel(" ");
			JLabel lineSpacerRight = new JLabel(" ");
			statistics.add(lineSpacerLeft);
			statistics.add(lineSpacerRight);
			headingComponents.add(lineSpacerLeft);
			headingComponents.add(lineSpacerRight);
		}
		JLabel heading = new JLabel(text);
		heading.setForeground(Color.WHITE);
		heading.setFont(FontManager.getRunescapeSmallFont().deriveFont(java.awt.Font.BOLD));
		JLabel spacer = new JLabel();
		statistics.add(heading);
		statistics.add(spacer);
		headingComponents.add(heading);
		headingComponents.add(spacer);
		sectionHeadingComponents.put(currentStatisticSection, headingComponents);
	}

	private void addStatistic(JPanel statistics, PanelStatistic statistic, String name, JLabel valueLabel)
	{
		addStatistic(statistics, statistic, new JLabel(name), valueLabel);
	}

	private static void addPermanentStatistic(JPanel statistics, String name, JLabel valueLabel)
	{
		addPermanentStatistic(statistics, new JLabel(name), valueLabel);
	}

	private static void addPermanentStatistic(JPanel statistics, JLabel nameLabel, JLabel valueLabel)
	{
		styleAndAddStatistic(statistics, nameLabel, valueLabel);
	}

	private void addStatistic(JPanel statistics, PanelStatistic statistic, JLabel nameLabel, JLabel valueLabel)
	{
		styleAndAddStatistic(statistics, nameLabel, valueLabel);
		registerStatistic(statistic, nameLabel, valueLabel);
		statisticRows.add(new StatisticRow(statistic, currentStatisticSection, nameLabel, valueLabel));
	}

	private static void styleAndAddStatistic(JPanel statistics, JLabel nameLabel, JLabel valueLabel)
	{
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setForeground(Color.WHITE);
		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		statistics.add(nameLabel);
		statistics.add(valueLabel);
	}

	private void registerStatistic(PanelStatistic statistic, Component... components)
	{
		List<Component> registered = statisticComponents.computeIfAbsent(statistic, key -> new ArrayList<>());
		for (Component component : components)
		{
			registered.add(component);
		}
	}

	public void refreshStatisticVisibility()
	{
		Runnable update = () ->
		{
			statisticsPanel.removeAll();
			for (StatisticSection section : new StatisticSection[]{
				StatisticSection.STREAK, StatisticSection.SESSION, StatisticSection.TOTAL})
			{
				boolean showHeading = statisticRows.stream()
					.anyMatch(row -> row.section == section && row.statistic.isVisible(config));
				for (Component component : sectionHeadingComponents.getOrDefault(section, java.util.Collections.emptyList()))
				{
					component.setVisible(showHeading);
				}
			}
			for (PanelStatistic statistic : PanelStatistic.values())
			{
				boolean visible = statistic.isVisible(config);
				for (Component component : statisticComponents.getOrDefault(statistic, java.util.Collections.emptyList()))
				{
					component.setVisible(visible);
				}
			}
			StatisticSection addedSection = StatisticSection.UNGROUPED;
			for (StatisticRow row : statisticRows)
			{
				if (!row.statistic.isVisible(config))
				{
					continue;
				}
				if (row.section != StatisticSection.UNGROUPED && row.section != addedSection)
				{
					for (Component heading : sectionHeadingComponents.get(row.section))
					{
						statisticsPanel.add(heading);
					}
					addedSection = row.section;
				}
				statisticsPanel.add(row.name);
				statisticsPanel.add(row.value);
			}
			statisticsPanel.revalidate();
			statisticsPanel.repaint();
			revalidate();
			repaint();
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			update.run();
		}
		else
		{
			SwingUtilities.invokeLater(update);
		}
	}

	private enum StatisticSection
	{
		UNGROUPED,
		STREAK,
		SESSION,
		TOTAL
	}

	private static final class StatisticRow
	{
		private final PanelStatistic statistic;
		private final StatisticSection section;
		private final Component name;
		private final Component value;

		private StatisticRow(PanelStatistic statistic, StatisticSection section, Component name, Component value)
		{
			this.statistic = statistic;
			this.section = section;
			this.name = name;
			this.value = value;
		}
	}

	private void installCanvasMenu(Component component)
	{
		if (component instanceof javax.swing.JComponent)
		{
			((javax.swing.JComponent) component).setComponentPopupMenu(canvasMenu);
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				installCanvasMenu(child);
			}
		}
	}

	String getCanvasMenuText()
	{
		return canvasMenuItem.getText();
	}

	void activateCanvasMenuItem()
	{
		canvasMenuItem.doClick();
	}

	private void applyDisplayedValues(ProgressSnapshot values)
	{
		int displayedLevel = Math.max(1, Math.min(99, values.getLevel()));
		long safeTotalExperience = Math.max(0, values.getTotalExperience());
		long safeCurrentLevelExperience = Math.max(0, values.getCurrentLevelExperience());
		long safeNextLevelExperience = Math.max(safeCurrentLevelExperience, values.getNextLevelExperience());
		long safeCurrentStreakTicks = Math.max(0, values.getCurrentStreakTicks());
		long safeLongestStreakTicks = Math.max(0, values.getLongestStreakTicks());
		long safeSessionQualifyingTicks = Math.max(0, values.getSessionQualifyingTicks());
		long safeSessionObservedTicks = Math.max(safeSessionQualifyingTicks, values.getSessionObservedTicks());
		long safeTotalObservedTicks = Math.max(safeTotalExperience, values.getTotalObservedTicks());
		long levelRange = safeNextLevelExperience - safeCurrentLevelExperience;
		long experienceIntoLevel = Math.max(0, safeTotalExperience - safeCurrentLevelExperience);
		int progress = levelRange == 0 ? 100 : (int) Math.min(100,
			experienceIntoLevel * 100.0 / levelRange);
		int nextLevel = Math.min(99, displayedLevel + 1);

		levelLabel.setText(label("Level", Integer.toString(displayedLevel)));
		experienceLabel.setText(label("XP", QuantityFormatter.quantityToStackSize(safeTotalExperience)));
		stateLabel.setText(label("State", values.getActivityState()));
		streakLabel.setText(label("Streak", safeCurrentStreakTicks + " ticks"));
		currentStreakTicksValue.setText(QuantityFormatter.quantityToStackSize(safeCurrentStreakTicks));
		currentStreakTimeValue.setText(formatTickDuration(safeCurrentStreakTicks));
		longestStreakTicksValue.setText(QuantityFormatter.quantityToStackSize(safeLongestStreakTicks));
		longestStreakTimeValue.setText(formatTickDuration(safeLongestStreakTicks));
		sessionTicksValue.setText(QuantityFormatter.quantityToStackSize(safeSessionQualifyingTicks));
		sessionTimeValue.setText(formatTickDuration(safeSessionQualifyingTicks));
		sessionTotalTimeValue.setText(formatTickDuration(safeSessionObservedTicks));
		sessionAfkPercentageValue.setText(formatPercentage(safeSessionQualifyingTicks, safeSessionObservedTicks));
		sessionActivePercentageValue.setText(formatPercentage(
			safeSessionObservedTicks - safeSessionQualifyingTicks, safeSessionObservedTicks));
		sessionExperienceValue.setText(QuantityFormatter.quantityToStackSize(safeSessionQualifyingTicks));
		totalTicksValue.setText(QuantityFormatter.quantityToStackSize(safeTotalExperience));
		totalTimeValue.setText(formatTickDuration(safeTotalObservedTicks));
		totalAfkPercentageValue.setText(formatPercentage(safeTotalExperience, safeTotalObservedTicks));
		totalActivePercentageValue.setText(formatPercentage(
			safeTotalObservedTicks - safeTotalExperience, safeTotalObservedTicks));
		totalExperienceValue.setText(QuantityFormatter.quantityToStackSize(safeTotalExperience));
		long elapsedNanos = Math.max(0, System.nanoTime() - sessionStartNanos);
		sessionExperiencePerHourValue.setText(QuantityFormatter.quantityToStackSize(
			ProgressOverlay.calculateExperiencePerHour(safeSessionQualifyingTicks, elapsedNanos)));
		experienceToNextLevelValue.setText(displayedLevel == 99
			? "Max"
			: QuantityFormatter.quantityToStackSize(Math.max(0, safeNextLevelExperience - safeTotalExperience)));

		levelProgressBar.setValue(progress);
		levelProgressBar.setLeftLabel("Lvl. " + displayedLevel);
		levelProgressBar.setCenterLabel(progress + "%");
		levelProgressBar.setRightLabel(displayedLevel == 99 ? "Max" : "Lvl. " + nextLevel);
		levelProgressBar.setToolTipText(displayedLevel == 99
			? "maximum level"
			: QuantityFormatter.quantityToStackSize(Math.max(0, safeNextLevelExperience - safeTotalExperience)) + " XP to next level");

		refreshStatisticVisibility();
	}

	boolean isTotalExperienceVisible()
	{
		return totalExperienceValue.isVisible();
	}

	boolean isSessionExperiencePerHourVisible()
	{
		return sessionExperiencePerHourValue.isVisible();
	}

	boolean isLevelProgressVisible()
	{
		return progressWrapper.isVisible();
	}

	boolean isStatisticVisible(PanelStatistic statistic)
	{
		List<Component> components = statisticComponents.get(statistic);
		return components != null && !components.isEmpty()
			&& components.stream().allMatch(Component::isVisible);
	}

	boolean isSectionHeadingVisible(String section)
	{
		List<Component> components = sectionHeadingComponents.get(
			StatisticSection.valueOf(section.toUpperCase(Locale.ROOT)));
		return components != null && components.stream().allMatch(component ->
			component.isVisible() && component.getParent() == statisticsPanel);
	}

	int getDisplayedStatisticComponentCount()
	{
		return statisticsPanel.getComponentCount();
	}

	static String formatTickDuration(long ticks)
	{
		long safeTicks = Math.max(0, ticks);
		long seconds = safeTicks / 5 * 3 + safeTicks % 5 * 3 / 5;
		long days = seconds / 86400;
		long hours = seconds % 86400 / 3600;
		long minutes = seconds % 3600 / 60;
		long remainingSeconds = seconds % 60;
		if (days > 0)
		{
			return String.format("%dd %02dh %02dm", days, hours, minutes);
		}
		if (hours > 0)
		{
			return String.format("%dh %02dm %02ds", hours, minutes, remainingSeconds);
		}
		if (minutes > 0)
		{
			return String.format("%dm %02ds", minutes, remainingSeconds);
		}
		return seconds + "s";
	}

	static String formatPercentage(long part, long total)
	{
		if (total <= 0)
		{
			return "0.0%";
		}
		double percentage = Math.min(100.0, Math.max(0, part) * 100.0 / total);
		return String.format(Locale.ROOT, "%.1f%%", percentage);
	}

	private static String label(String name, String value)
	{
		return "<html><span style='color:#ffffff'>" + name + ": </span><span style='color:#ffffff'>" + value + "</span></html>";
	}

	private void confirmReset()
	{
		int choice = JOptionPane.showConfirmDialog(
			this,
			"Clear Actual AFK progress?",
			"Reset progress",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (choice == JOptionPane.YES_OPTION)
		{
			resetAction.run();
		}
	}

	private ImageIcon loadClockIcon()
	{
		try
		{
			BufferedImage image = ImageIO.read(ProgressPanel.class.getResourceAsStream("/com/actualafk/icon.png"));
			return image == null ? new ImageIcon() : new ImageIcon(
				image.getScaledInstance(36, 36, java.awt.Image.SCALE_SMOOTH));
		}
		catch (Exception exception)
		{
			return new ImageIcon();
		}
	}
}
