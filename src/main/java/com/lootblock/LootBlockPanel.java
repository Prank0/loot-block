package com.lootblock;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.item.ItemPrice;

final class LootBlockPanel extends PluginPanel
{
	private static final int MAX_RESULTS = 50;

	private final LootBlockPlugin plugin;
	private final ItemManager itemManager;
	private final JTextField searchField = new JTextField();
	private final JTextField blockedFilter = new JTextField();
	private final DefaultListModel<ItemChoice> searchModel = new DefaultListModel<>();
	private final DefaultListModel<ItemChoice> blockedModel = new DefaultListModel<>();
	private final JList<ItemChoice> searchResults = new JList<>(searchModel);
	private final JList<ItemChoice> blockedItems = new JList<>(blockedModel);
	private List<ItemChoice> allBlockedItems = new ArrayList<>();

	LootBlockPanel(LootBlockPlugin plugin, ItemManager itemManager)
	{
		super(false);
		this.plugin = plugin;
		this.itemManager = itemManager;
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Loot Block");
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(title, BorderLayout.NORTH);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Add", buildSearchPanel());
		tabs.addTab("Blocked", buildBlockedPanel());
		add(tabs, BorderLayout.CENTER);

		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				updateSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				updateSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				updateSearch();
			}
		});

		blockedFilter.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				filterBlockedItems();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				filterBlockedItems();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				filterBlockedItems();
			}
		});
	}

	private JPanel buildSearchPanel()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 6));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchField.setToolTipText("Search tradeable items by name");
		panel.add(searchField, BorderLayout.NORTH);

		searchResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		searchResults.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (event.getClickCount() == 2)
				{
					addSelected();
				}
			}
		});
		panel.add(new JScrollPane(searchResults), BorderLayout.CENTER);

		JButton add = new JButton("Add selected");
		add.addActionListener(event -> addSelected());
		panel.add(add, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel buildBlockedPanel()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 6));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		blockedFilter.setToolTipText("Filter blocked items by name or ID");
		panel.add(blockedFilter, BorderLayout.NORTH);
		blockedItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		panel.add(new JScrollPane(blockedItems), BorderLayout.CENTER);

		JButton remove = new JButton("Remove selected");
		remove.addActionListener(event ->
		{
			ItemChoice selected = blockedItems.getSelectedValue();
			if (selected != null)
			{
				plugin.removeBlockedItem(selected.id);
			}
		});
		panel.add(remove, BorderLayout.SOUTH);
		return panel;
	}

	private void updateSearch()
	{
		String query = searchField.getText().trim();
		searchModel.clear();
		if (query.length() < 2)
		{
			return;
		}

		List<ItemPrice> matches = itemManager.search(query);
		matches.stream()
			.sorted(Comparator.comparing(ItemPrice::getName, String.CASE_INSENSITIVE_ORDER))
			.limit(MAX_RESULTS)
			.map(item -> new ItemChoice(item.getId(), item.getName()))
			.forEach(searchModel::addElement);
	}

	private void addSelected()
	{
		ItemChoice selected = searchResults.getSelectedValue();
		if (selected != null)
		{
			plugin.addBlockedItem(selected.id);
		}
	}

	void setBlockedItems(List<ItemChoice> items)
	{
		assert SwingUtilities.isEventDispatchThread();
		allBlockedItems = new ArrayList<>(items);
		filterBlockedItems();
	}

	private void filterBlockedItems()
	{
		String query = blockedFilter.getText().trim().toLowerCase();
		blockedModel.clear();
		allBlockedItems.stream()
			.filter(item -> query.isEmpty() || item.toString().toLowerCase().contains(query))
			.sorted(Comparator.comparing(ItemChoice::toString, String.CASE_INSENSITIVE_ORDER))
			.forEach(blockedModel::addElement);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH, 0);
	}

	static final class ItemChoice
	{
		private final int id;
		private final String name;

		ItemChoice(int id, String name)
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name + " (" + id + ")";
		}
	}
}
