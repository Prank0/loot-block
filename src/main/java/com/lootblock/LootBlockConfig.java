package com.lootblock;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LootBlockPlugin.CONFIG_GROUP)
public interface LootBlockConfig extends Config
{
	@ConfigItem(
		keyName = "blockedItemIds",
		name = "Blocked item IDs",
		description = "Items managed by the Loot Block sidebar",
		hidden = true
	)
	default String blockedItemIds()
	{
		return "";
	}
}
