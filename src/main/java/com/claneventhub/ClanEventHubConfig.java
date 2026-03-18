package com.claneventhub;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ClanEventHubConfig.GROUP)
public interface ClanEventHubConfig extends Config
{
	String GROUP = "claneventhub";

	@ConfigSection(
		name = "Server",
		description = "Your clan server connection settings",
		position = 0
	)
	String server = "server";

	@ConfigSection(
		name = "Authentication",
		description = "Authentication tokens for your clan server",
		position = 1
	)
	String auth = "auth";

	@ConfigSection(
		name = "Split Tracker",
		description = "Split tracker bot settings",
		position = 2
	)
	String splits = "splits";

	@ConfigSection(
		name = "Drops",
		description = "Drop tracking settings",
		position = 3
	)
	String drops = "drops";

	@ConfigItem(
		keyName = "clanName",
		name = "Clan Name",
		description = "Your clan's name (must match in-game clan name exactly for auto-detection)",
		section = server,
		position = 0
	)
	default String clanName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "apiBaseUrl",
		name = "API Base URL",
		description = "Base URL for your clan's event API (e.g. https://yourclan.com/api)",
		section = server,
		position = 1
	)
	default String apiBaseUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "botSecret",
		name = "Bot Secret",
		description = "Bot secret token for your clan server (x-bot-secret header)",
		section = auth,
		secret = true
	)
	default String botSecret()
	{
		return "";
	}

	@ConfigItem(
		keyName = "splitBotUrl",
		name = "Split Bot URL",
		description = "URL for the split tracker bot API (e.g. http://yourbot:3000/api)",
		section = splits
	)
	default String splitBotUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "splitApiKey",
		name = "Split API Key",
		description = "Bearer token for the split tracker bot",
		section = splits,
		secret = true
	)
	default String splitApiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "autoScreenshot",
		name = "Auto-screenshot drops",
		description = "Automatically capture a screenshot when a valuable drop is received",
		section = drops
	)
	default boolean autoScreenshot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoSubmitDrops",
		name = "Auto-submit drops",
		description = "Automatically submit detected valuable drops to the clan Hall of Fame",
		section = drops
	)
	default boolean autoSubmitDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "minDropValue",
		name = "Min drop value (gp)",
		description = "Minimum drop value to trigger auto-screenshot/submit",
		section = drops
	)
	default int minDropValue()
	{
		return 1000000;
	}
}
