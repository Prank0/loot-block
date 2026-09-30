package com.lootblock;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Loot Block",
	description = "Deprioritizes configured ground items and prevents picking them up",
	tags = {"loot", "ground", "item", "block", "pickup"},
	enabledByDefault = false
)
public class LootBlockPlugin extends Plugin
{
	static final String CONFIG_GROUP = "loot-block";
	private static final String BLOCKED_ITEMS_KEY = "blockedItemIds";
	private static final String BLOCKED_MESSAGE = "Looting this item is disabled.";

	@Inject
	private LootBlockConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatMessageManager chatMessageManager;

	private volatile Set<Integer> blockedItemIds = Collections.emptySet();
	private LootBlockPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		loadBlockedItems();
		panel = new LootBlockPanel(this, itemManager);
		navigationButton = NavigationButton.builder()
			.tooltip("Loot Block")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		panel = null;
		navigationButton = null;
		blockedItemIds = Collections.emptySet();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()) && BLOCKED_ITEMS_KEY.equals(event.getKey()))
		{
			loadBlockedItems();
			refreshPanel();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		if (isGroundItemAction(entry.getType()) && isBlocked(event.getIdentifier()))
		{
			entry.setDeprioritized(true);
			entry.setForceLeftClick(false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (isGroundItemAction(event.getMenuAction()) && isBlocked(event.getId()))
		{
			event.consume();
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(BLOCKED_MESSAGE)
				.build());
		}
	}

	void addBlockedItem(int itemId)
	{
		clientThread.invoke(() ->
		{
			int canonicalId = itemManager.canonicalize(itemId);
			Set<Integer> updated = new HashSet<>(blockedItemIds);
			if (updated.add(canonicalId))
			{
				saveBlockedItems(updated);
			}
		});
	}

	void removeBlockedItem(int itemId)
	{
		clientThread.invoke(() ->
		{
			Set<Integer> updated = new HashSet<>(blockedItemIds);
			if (updated.remove(itemId))
			{
				saveBlockedItems(updated);
			}
		});
	}

	private boolean isBlocked(int itemId)
	{
		return blockedItemIds.contains(itemManager.canonicalize(itemId));
	}

	private void loadBlockedItems()
	{
		Set<Integer> parsed = new HashSet<>();
		String value = config.blockedItemIds();
		if (value != null && !value.trim().isEmpty())
		{
			for (String token : value.split(","))
			{
				try
				{
					parsed.add(Integer.parseInt(token.trim()));
				}
				catch (NumberFormatException ignored)
				{
					// Ignore malformed legacy/manual values.
				}
			}
		}
		blockedItemIds = Collections.unmodifiableSet(parsed);
	}

	private void saveBlockedItems(Set<Integer> itemIds)
	{
		String value = itemIds.stream()
			.sorted()
			.map(String::valueOf)
			.collect(Collectors.joining(","));
		blockedItemIds = Collections.unmodifiableSet(new HashSet<>(itemIds));
		configManager.setConfiguration(CONFIG_GROUP, BLOCKED_ITEMS_KEY, value);
		refreshPanel();
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			List<LootBlockPanel.ItemChoice> items = new ArrayList<>();
			for (int itemId : blockedItemIds)
			{
				String name = itemManager.getItemComposition(itemId).getMembersName();
				items.add(new LootBlockPanel.ItemChoice(itemId, name));
			}
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setBlockedItems(items);
				}
			});
		});
	}

	private static boolean isGroundItemAction(MenuAction action)
	{
		return action == MenuAction.GROUND_ITEM_FIRST_OPTION
			|| action == MenuAction.GROUND_ITEM_SECOND_OPTION
			|| action == MenuAction.GROUND_ITEM_THIRD_OPTION
			|| action == MenuAction.GROUND_ITEM_FOURTH_OPTION
			|| action == MenuAction.GROUND_ITEM_FIFTH_OPTION
			|| action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM;
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(2.5f));
		graphics.setColor(new Color(210, 55, 55));
		graphics.drawOval(2, 2, 11, 11);
		graphics.drawLine(4, 12, 12, 4);
		graphics.dispose();
		return image;
	}

	@Provides
	LootBlockConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LootBlockConfig.class);
	}
}
