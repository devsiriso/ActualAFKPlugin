package com.actualafk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Actual AFK",
	description = "Track your AFK time and progress.",
	tags = {"afk", "tracker", "time"})
public class AfkPlugin extends Plugin
{
	private static final int SAVE_BATCH_SIZE = 10;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AfkConfig config;

	@Inject
	private ProgressPanel panel;

	@Inject
	private OverlayManager overlayManager;

	private NavigationButton navigationButton;
	private ProgressStore progressStore;
	private AfkProgress afkProgress;
	private IdleTracker idleActivityTracker;
	private ProgressOverlay canvasOverlay;
	private boolean canvasOverlayAdded;
	private boolean activitySignalPending;
	private boolean progressLoadedForProfile;
	private int observedTicksSinceSave;
	private int lastProcessedTickCount = Integer.MIN_VALUE;
	private boolean started;
	private ProgressSnapshot lastDisplayedSnapshot;
	private long sessionObservedTicks;

	@Provides
	AfkConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (started)
		{
			return;
		}

		started = true;
		progressStore = new ProgressStore(configManager);
		afkProgress = new AfkProgress();
		progressLoadedForProfile = false;
		idleActivityTracker = createIdleTracker();
		activitySignalPending = false;
		observedTicksSinceSave = 0;
		lastProcessedTickCount = Integer.MIN_VALUE;
		panel.resetSessionTiming();
		lastDisplayedSnapshot = null;
		sessionObservedTicks = 0;
		panel.setResetAction(this::resetLocalProgress);
		panel.setCanvasOverlayToggleAction(this::setCanvasOverlayEnabled);
		loadProgressForCurrentProfile();
		canvasOverlay = new ProgressOverlay(this, loadClockIcon());
		applyCanvasOverlayEnabled(config.showCanvasOverlay());

		navigationButton = NavigationButton.builder()
			.icon(loadClockIcon())
			.tooltip("Actual AFK")
			.onClick(() -> clientToolbar.openPanel(navigationButton))
			.panel(panel)
			.priority(10)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshPanel(currentDisplayState());
	}

	@Override
	protected void shutDown()
	{
		if (!started)
		{
			return;
		}

		if (progressLoadedForProfile && progressStore != null && afkProgress != null)
		{
			progressStore.save(afkProgress);
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		removeCanvasOverlay();

		started = false;
		progressLoadedForProfile = false;
		activitySignalPending = false;
		observedTicksSinceSave = 0;
		lastProcessedTickCount = Integer.MIN_VALUE;
		navigationButton = null;
		progressStore = null;
		afkProgress = null;
		idleActivityTracker = null;
		canvasOverlay = null;
		panel.setResetAction(() ->
		{
		});
		panel.setCanvasOverlayToggleAction(enabled ->
		{
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!started)
		{
			return;
		}
		if (!beginGameTick(client.getTickCount()))
		{
			return;
		}
		if (!progressLoadedForProfile)
		{
			loadProgressForCurrentProfile();
			if (!progressLoadedForProfile)
			{
				refreshPanel(currentDisplayState());
				return;
			}
		}

		if (!config.trackingEnabled())
		{
			idleActivityTracker.reset();
			afkProgress.setCurrentIdleStreakTicks(0);
			activitySignalPending = false;
			refreshPanel(IdleTracker.TickState.PAUSED);
			return;
		}
		if (!progressStore.canSave())
		{
			idleActivityTracker.reset();
			afkProgress.setCurrentIdleStreakTicks(0);
			activitySignalPending = false;
			refreshPanel(IdleTracker.TickState.PAUSED);
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (loggedIn && localPlayer != null)
		{
			sessionObservedTicks++;
			afkProgress.addObservedTick();
			observedTicksSinceSave++;
		}
		IdleTracker.TickObservation observation =
			IdleTracker.TickObservation.withActivitySignals(
			loggedIn,
			localPlayer != null,
			localPlayer == null ? null : localPlayer.getWorldLocation(),
			localPlayer == null ? null : localPlayer.getAnimation() != -1,
			localPlayer == null ? null : localPlayer.getInteracting() != null,
			activitySignalPending,
			false,
			false,
			false);
		activitySignalPending = false;

		IdleTracker.TickResult result = idleActivityTracker.observe(observation);
		afkProgress.setCurrentIdleStreakTicks(result.currentStreakTicks());
		if (result.qualifies())
		{
			afkProgress.addQualifyingTick(result.currentStreakTicks());
		}
		if (observedTicksSinceSave >= SAVE_BATCH_SIZE)
		{
			progressStore.save(afkProgress);
			observedTicksSinceSave = 0;
		}

		refreshPanel(result.currentState());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!started || !AfkConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (AfkConfig.SHOW_CANVAS_OVERLAY_KEY.equals(event.getKey()))
		{
			applyCanvasOverlayEnabled(config.showCanvasOverlay());
		}
		else if (event.getKey().startsWith(AfkConfig.SHOW_PANEL_STATISTIC_KEY_PREFIX)
			|| event.getKey().startsWith(AfkConfig.SHOW_SIDEBAR_STATISTIC_KEY_PREFIX))
		{
			panel.refreshStatisticVisibility();
		}
		else if (AfkConfig.MINIMUM_IDLE_TICKS_KEY.equals(event.getKey())
			|| AfkConfig.ACTIVITY_GRACE_TICKS_KEY.equals(event.getKey()))
		{
			idleActivityTracker = createIdleTracker();
			afkProgress.setCurrentIdleStreakTicks(0);
			activitySignalPending = false;
			lastProcessedTickCount = Integer.MIN_VALUE;
			refreshPanel(currentDisplayState());
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		if (!started)
		{
			return;
		}

		if (progressLoadedForProfile && progressStore != null && afkProgress != null
			&& event.getPreviousProfile() != null)
		{
			progressStore.save(afkProgress, event.getPreviousProfile());
		}
		progressLoadedForProfile = false;
		afkProgress = new AfkProgress();
		sessionObservedTicks = 0;
		activitySignalPending = false;
		lastDisplayedSnapshot = null;
		panel.resetSessionTiming();
		loadProgressForCurrentProfile();
		refreshPanel(currentDisplayState());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!started)
		{
			return;
		}
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loadProgressForCurrentProfile();
			return;
		}

		idleActivityTracker.reset();
		lastProcessedTickCount = Integer.MIN_VALUE;
		afkProgress.setCurrentIdleStreakTicks(0);
		activitySignalPending = false;
		if (progressLoadedForProfile)
		{
			progressStore.save(afkProgress);
		}
		observedTicksSinceSave = 0;
		refreshPanel(displayStateFor(event.getGameState()));
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (started && isLocalPlayer(event.getActor()))
		{
			markActivityGrace();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (started && (isLocalPlayer(event.getSource()) || isLocalPlayer(event.getTarget())))
		{
			markActivityGrace();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// HitsplatApplied's actor is the actor receiving the hit. Only incoming
		// hitsplats are relevant; outgoing damage must not suppress AFK tracking.
		if (started && isLocalPlayer(event.getActor()))
		{
			markActivityGrace();
		}
	}

	private void markActivityGrace()
	{
		activitySignalPending = true;
	}

	private IdleTracker createIdleTracker()
	{
		return new IdleTracker(config.minimumIdleTicks(), config.activityGraceTicks());
	}

	private boolean isLocalPlayer(Actor actor)
	{
		return actor != null && actor == client.getLocalPlayer();
	}

	boolean beginGameTick(int tickCount)
	{
		if (tickCount == lastProcessedTickCount)
		{
			return false;
		}
		lastProcessedTickCount = tickCount;
		return true;
	}

	private void resetLocalProgress()
	{
		if (!started || clientThread == null)
		{
			return;
		}
		clientThread.invoke(this::resetLocalProgressOnClientThread);
	}

	private void resetLocalProgressOnClientThread()
	{
		if (!started || !progressLoadedForProfile || progressStore == null || afkProgress == null)
		{
			return;
		}

		afkProgress.reset();
		sessionObservedTicks = 0;
		idleActivityTracker.reset();
		activitySignalPending = false;
		observedTicksSinceSave = 0;
		lastDisplayedSnapshot = null;
		panel.resetSessionTiming();
		progressStore.reset();
		refreshPanel(currentDisplayState());
	}

	private void loadProgressForCurrentProfile()
	{
		if (progressLoadedForProfile || progressStore == null || !progressStore.isAvailable())
		{
			return;
		}

		afkProgress = progressStore.load();
		progressLoadedForProfile = true;
		idleActivityTracker.reset();
		activitySignalPending = false;
		observedTicksSinceSave = 0;
	}

	private void setCanvasOverlayEnabled(boolean enabled)
	{
		if (!started)
		{
			return;
		}

		if (config.showCanvasOverlay() != enabled)
		{
			configManager.setConfiguration(
				AfkConfig.CONFIG_GROUP,
				AfkConfig.SHOW_CANVAS_OVERLAY_KEY,
				enabled);
		}
		applyCanvasOverlayEnabled(enabled);
	}

	private void applyCanvasOverlayEnabled(boolean enabled)
	{
		if (!started || canvasOverlay == null)
		{
			return;
		}

		if (enabled && !canvasOverlayAdded)
		{
			overlayManager.add(canvasOverlay);
			canvasOverlayAdded = true;
		}
		else if (!enabled)
		{
			removeCanvasOverlay();
		}
		if (panel != null)
		{
			panel.setCanvasOverlayVisible(enabled);
		}
	}

	private void removeCanvasOverlay()
	{
		if (canvasOverlayAdded && canvasOverlay != null)
		{
			overlayManager.remove(canvasOverlay);
		}
		canvasOverlayAdded = false;
		if (panel != null)
		{
			panel.setCanvasOverlayVisible(false);
		}
	}

	private void refreshPanel(IdleTracker.TickState state)
	{
		if (panel == null || afkProgress == null)
		{
			return;
		}

		ProgressSnapshot snapshot = new ProgressSnapshot(
			afkProgress.getAfkLevel(),
			afkProgress.getTotalAfkXp(),
			afkProgress.getCurrentLevelXp(),
			afkProgress.getNextLevelXp(),
			afkProgress.getCurrentIdleStreakTicks(),
			afkProgress.getLongestIdleStreakTicks(),
			afkProgress.getSessionQualifyingTicks(),
			sessionObservedTicks,
			afkProgress.getTotalObservedTicks(),
			stateText(state));
		if (canvasOverlay != null)
		{
			canvasOverlay.updateDisplayedValues(snapshot);
		}
		if (snapshot.equals(lastDisplayedSnapshot))
		{
			return;
		}

		lastDisplayedSnapshot = snapshot;
		panel.updateDisplayedValues(snapshot);
	}

	private IdleTracker.TickState currentDisplayState()
	{
		if (client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null)
		{
			return IdleTracker.TickState.ACTIVE;
		}
		return displayStateFor(client.getGameState());
	}

	private IdleTracker.TickState displayStateFor(GameState gameState)
	{
		if (gameState == GameState.LOADING || gameState == GameState.HOPPING || gameState == GameState.STARTING)
		{
			return IdleTracker.TickState.PAUSED;
		}
		return IdleTracker.TickState.LOGGED_OUT;
	}

	private String stateText(IdleTracker.TickState state)
	{
		switch (state)
		{
			case INACTIVE:
				return "Inactive";
			case PENDING:
				return "Pending";
			case PAUSED:
				return "Paused";
			case LOGGED_OUT:
				return "Logged out";
			case ACTIVE:
			default:
				return "Active";
		}
	}

	private BufferedImage loadClockIcon()
	{
		try (InputStream inputStream = AfkPlugin.class.getResourceAsStream(
			"/com/actualafk/icon.png"))
		{
			if (inputStream == null)
			{
				return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			}
			BufferedImage image = ImageIO.read(inputStream);
			if (image == null)
			{
				return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			}
			BufferedImage resized = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D graphics = resized.createGraphics();
			try
			{
				graphics.setRenderingHint(
					java.awt.RenderingHints.KEY_INTERPOLATION,
					java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics.drawImage(image, 0, 0, 16, 16, null);
			}
			finally
			{
				graphics.dispose();
			}
			return resized;
		}
		catch (IOException exception)
		{
			return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		}
	}

}
