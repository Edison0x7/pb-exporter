package com.pbexporter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PbExporterConfig.GROUP)
public interface PbExporterConfig extends Config
{
	String GROUP = "pbexporter";

	@ConfigItem(
		keyName = "serverUrl",
		name = "Server URL",
		description = "Server URL for submitting Personal Bests",
		position = 0
	)
	default String serverUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "apiToken",
		name = "API Token",
		description = "Personal token for server authentication",
		position = 1,
		secret = true
	)
	default String apiToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "copyRawData",
		name = "Copy raw data",
		description = "Copy Personal Bests to clipboard in JSON format",
		position = 2
	)
	default boolean copyRawData()
	{
		return false;
	}
}