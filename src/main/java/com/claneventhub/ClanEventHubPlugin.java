package com.claneventhub;

import com.google.inject.Provides;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Clan Event Hub",
	description = "Track clan events, bingo, splits, and drops via your clan's server",
	tags = {"clan", "events", "bingo", "drops", "splits", "loot", "proof"}
)
public class ClanEventHubPlugin extends Plugin
{
	private static final Pattern VALUABLE_DROP_PATTERN = Pattern.compile(
		"Valuable drop: (.+?) \\(([\\d,]+) coins\\)", Pattern.CASE_INSENSITIVE);
	private static final Pattern UNTRADEABLE_DROP_PATTERN = Pattern.compile(
		"Untradeable drop: (.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
		"New item added to your collection log: (.+)", Pattern.CASE_INSENSITIVE);

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClanEventHubConfig config;

	@Inject
	@Getter
	private ClanEventHubClient apiClient;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ScheduledExecutorService executor;

	@Getter
	private ClanEventHubPanel panel;
	private NavigationButton navButton;

	@Getter
	private boolean inClan;

	@Provides
	ClanEventHubConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanEventHubConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = injector.getInstance(ClanEventHubPanel.class);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Clan Event Hub")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		inClan = false;
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (event.isGuest())
		{
			return;
		}

		checkClanMembership();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == net.runelite.api.GameState.LOGGED_IN)
		{
			executor.execute(() ->
			{
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return;
				}
				checkClanMembership();
			});
		}
	}

	private void checkClanMembership()
	{
		String targetClan = config.clanName();
		if (targetClan.isEmpty())
		{
			return;
		}

		ClanSettings clanSettings = client.getClanSettings();
		boolean wasClan = inClan;

		if (clanSettings != null)
		{
			inClan = targetClan.equalsIgnoreCase(clanSettings.getName());
		}
		else
		{
			ClanChannel clanChannel = client.getClanChannel();
			inClan = clanChannel != null && targetClan.equalsIgnoreCase(clanChannel.getName());
		}

		if (inClan && !wasClan)
		{
			log.info("Detected clan membership: {}", targetClan);
			SwingUtilities.invokeLater(() -> panel.onClanDetected());
		}
		else if (!inClan && wasClan)
		{
			SwingUtilities.invokeLater(() -> panel.onClanLeft());
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = event.getMessage();

		Matcher valuableMatcher = VALUABLE_DROP_PATTERN.matcher(message);
		if (valuableMatcher.find())
		{
			String itemName = valuableMatcher.group(1);
			String valueStr = valuableMatcher.group(2).replace(",", "");
			int value = Integer.parseInt(valueStr);

			if (config.autoScreenshot() && value >= config.minDropValue())
			{
				handleDropDetected(itemName);
			}
			return;
		}

		Matcher collectionMatcher = COLLECTION_LOG_PATTERN.matcher(message);
		if (collectionMatcher.find())
		{
			String itemName = collectionMatcher.group(1);
			if (config.autoScreenshot())
			{
				handleDropDetected(itemName);
			}
			return;
		}

		Matcher untradeableMatcher = UNTRADEABLE_DROP_PATTERN.matcher(message);
		if (untradeableMatcher.find())
		{
			String itemName = untradeableMatcher.group(1);
			if (config.autoScreenshot())
			{
				handleDropDetected(itemName);
			}
		}
	}

	private void handleDropDetected(String itemName)
	{
		log.debug("Drop detected: {}", itemName);
		captureScreenshot(image ->
		{
			if (config.autoSubmitDrops())
			{
				SwingUtilities.invokeLater(() -> panel.submitDropAutomatically(itemName, image));
			}
		});
	}

	void captureScreenshot(Consumer<BufferedImage> callback)
	{
		drawManager.requestNextFrameListener(image ->
			executor.execute(() ->
			{
				BufferedImage buffered;
				if (image instanceof BufferedImage)
				{
					buffered = (BufferedImage) image;
				}
				else
				{
					buffered = new BufferedImage(
						image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = buffered.createGraphics();
					g.drawImage(image, 0, 0, null);
					g.dispose();
				}
				callback.accept(buffered);
			})
		);
	}

	String getLocalPlayerName()
	{
		Player local = client.getLocalPlayer();
		return local != null ? local.getName() : null;
	}

	String getApiBase()
	{
		String url = config.apiBaseUrl();
		if (url.endsWith("/"))
		{
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}
}
