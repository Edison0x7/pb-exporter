package com.pbexporter;

import com.pbexporter.PbExporterPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PbExporterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PbExporterPlugin.class);
		RuneLite.main(args);
	}
}