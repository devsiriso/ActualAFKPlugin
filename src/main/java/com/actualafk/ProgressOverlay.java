package com.actualafk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;
import net.runelite.client.util.QuantityFormatter;

public final class ProgressOverlay extends OverlayPanel
{
	private static final int CANVAS_ICON_SIZE = 24;
	private static final long NANOS_PER_HOUR = 3_600_000_000_000L;
	private static final int BORDER_SIZE = 2;
	private static final int CONTENT_GAP = 3;
	private static final int ICON_GAP = 4;
	private static final Rectangle ICON_CONTENT_BORDER = new Rectangle(2, 0, 4, 0);
	private static final Color PROGRESS_BACKGROUND = new Color(61, 56, 49);
	private static final Color INACTIVE_BACKGROUND = new Color(92, 64, 32, 190);

	private final PanelComponent iconContentPanel = new PanelComponent();
	private final BufferedImage clockIcon;
	private volatile DisplayValues displayValues = DisplayValues.initial();

	public ProgressOverlay(AfkPlugin plugin, BufferedImage clockIcon)
	{
		super(plugin);
		this.clockIcon = scaleCanvasIcon(Objects.requireNonNull(clockIcon, "clockIcon"));
		setPosition(OverlayPosition.TOP_LEFT);
		setMovable(true);
		setSnappable(true);
		setResizable(false);
		panelComponent.setBorder(new Rectangle(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
		panelComponent.setGap(new Point(0, CONTENT_GAP));
		iconContentPanel.setBorder(ICON_CONTENT_BORDER);
		iconContentPanel.setBackgroundColor(null);
	}

	public void updateDisplayedValues(
		int afkLevel,
		long totalAfkExperience,
		long currentLevelExperience,
		long nextLevelExperience,
		long sessionQualifyingTicks,
		String activityState)
	{
		long safeCurrentLevelExperience = Math.max(0, currentLevelExperience);
		displayValues = new DisplayValues(
			Math.max(1, Math.min(99, afkLevel)),
			Math.max(0, totalAfkExperience),
			safeCurrentLevelExperience,
			Math.max(safeCurrentLevelExperience, nextLevelExperience),
			Math.max(0, sessionQualifyingTicks),
			"Inactive".equals(activityState));
	}

	public void updateDisplayedValues(ProgressSnapshot values)
	{
		Objects.requireNonNull(values, "values");
		updateDisplayedValues(
			values.getLevel(),
			values.getTotalExperience(),
			values.getCurrentLevelExperience(),
			values.getNextLevelExperience(),
			values.getSessionQualifyingTicks(),
			values.getActivityState());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		iconContentPanel.getChildren().clear();
		panelComponent.setGap(new Point(0, CONTENT_GAP));
		graphics.setFont(FontManager.getRunescapeSmallFont());

		DisplayValues values = displayValues;
		panelComponent.setBackgroundColor(values.inactive
			? INACTIVE_BACKGROUND
			: ComponentConstants.STANDARD_BACKGROUND_COLOR);
		long levelRange = values.nextLevelExperience - values.currentLevelExperience;
		double progressPercentage = levelRange == 0
			? 100.0
			: Math.min(100.0, Math.max(0, values.totalAfkExperience - values.currentLevelExperience)
				* 100.0 / levelRange);
		LineComponent experienceLine = LineComponent.builder()
			.left("AFK XP:")
			.leftColor(ColorScheme.BRAND_ORANGE)
			.right(QuantityFormatter.quantityToStackSize(values.totalAfkExperience))
			.build();
		LineComponent afkTimeLine = LineComponent.builder()
			.left("AFK Time:")
			.right(formatSessionAfkTime(values.sessionQualifyingTicks))
			.build();
		PanelComponent textPanel = new PanelComponent();
		textPanel.setBorder(new Rectangle());
		textPanel.setGap(new Point(0, 0));
		textPanel.setBackgroundColor(null);
		textPanel.getChildren().add(experienceLine);
		textPanel.getChildren().add(afkTimeLine);
		SplitComponent iconAndText = SplitComponent.builder()
			.first(new ImageComponent(clockIcon))
			.second(textPanel)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(ICON_GAP, 0))
			.build();
		iconContentPanel.getChildren().add(iconAndText);

		ProgressBarComponent progressBar = new ProgressBarComponent();
		progressBar.setBackgroundColor(PROGRESS_BACKGROUND);
		progressBar.setForegroundColor(ColorScheme.BRAND_ORANGE);
		progressBar.setLeftLabel(Integer.toString(values.afkLevel));
		progressBar.setRightLabel(values.afkLevel == 99 ? "Max" : Integer.toString(values.afkLevel + 1));
		progressBar.setCenterLabel("");
		progressBar.setValue(progressPercentage);

		panelComponent.getChildren().add(iconContentPanel);
		panelComponent.getChildren().add(progressBar);
		return super.render(graphics);
	}

	static int calculateProgressPercentage(long totalExperience, long currentLevelExperience, long nextLevelExperience)
	{
		long safeCurrent = Math.max(0, currentLevelExperience);
		long safeNext = Math.max(safeCurrent, nextLevelExperience);
		long range = safeNext - safeCurrent;
		if (range == 0)
		{
			return 100;
		}
		long intoLevel = Math.max(0, totalExperience - safeCurrent);
		return (int) Math.min(100, intoLevel * 100.0 / range);
	}

	static long calculateExperiencePerHour(long qualifyingTicks, long elapsedNanos)
	{
		if (qualifyingTicks <= 0 || elapsedNanos <= 0)
		{
			return 0;
		}
		return Math.round(qualifyingTicks * (double) NANOS_PER_HOUR / elapsedNanos);
	}

	static String formatSessionAfkTime(long qualifyingTicks)
	{
		long safeTicks = Math.max(0, qualifyingTicks);
		long seconds = safeTicks / 5 * 3 + safeTicks % 5 * 3 / 5;
		long hours = seconds / 3600;
		long minutes = seconds % 3600 / 60;
		long remainingSeconds = seconds % 60;
		if (hours > 0)
		{
			String value = hours + "h";
			if (minutes > 0)
			{
				value += minutes + "m";
			}
			return value;
		}
		if (minutes > 0)
		{
			return minutes + "m" + (remainingSeconds > 0 ? remainingSeconds + "s" : "");
		}
		return remainingSeconds + "s";
	}

	private static BufferedImage scaleCanvasIcon(BufferedImage source)
	{
		BufferedImage scaled = new BufferedImage(CANVAS_ICON_SIZE, CANVAS_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			graphics.drawImage(source, 0, 0, CANVAS_ICON_SIZE, CANVAS_ICON_SIZE, null);
		}
		finally
		{
			graphics.dispose();
		}
		return scaled;
	}

	Color getBackgroundColor()
	{
		return panelComponent.getBackgroundColor();
	}

	private static final class DisplayValues
	{
		private final int afkLevel;
		private final long totalAfkExperience;
		private final long currentLevelExperience;
		private final long nextLevelExperience;
		private final long sessionQualifyingTicks;
		private final boolean inactive;

		private DisplayValues(
			int afkLevel,
			long totalAfkExperience,
			long currentLevelExperience,
			long nextLevelExperience,
			long sessionQualifyingTicks,
			boolean inactive)
		{
			this.afkLevel = afkLevel;
			this.totalAfkExperience = totalAfkExperience;
			this.currentLevelExperience = currentLevelExperience;
			this.nextLevelExperience = nextLevelExperience;
			this.sessionQualifyingTicks = sessionQualifyingTicks;
			this.inactive = inactive;
		}

		private static DisplayValues initial()
		{
			return new DisplayValues(1, 0, 0, AfkProgress.getExperienceForLevel(2), 0, false);
		}
	}
}
