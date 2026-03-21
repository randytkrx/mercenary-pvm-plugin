package com.claneventhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
class ClanEventHubPanel extends PluginPanel
{
	private static final int PANEL_WIDTH = PluginPanel.PANEL_WIDTH - 17;

	private final ClanEventHubPlugin plugin;
	private final ClanEventHubConfig config;

	// Top-level
	private final JLabel clanStatusLabel = new JLabel("Configure clan in settings");
	private final JLabel playerNameLabel = new JLabel("");

	// Events tab
	private final JPanel eventsListPanel = new JPanel();
	private final JLabel eventNameLabel = new JLabel("Select an event");
	private final JLabel eventStatusLabel = new JLabel("");
	private final JLabel taskTitleLabel = new JLabel("");
	private final JTextArea taskDescArea = new JTextArea();
	private final JLabel countdownLabel = new JLabel("");
	private final JLabel pointsLabel = new JLabel("");
	private final JLabel statusLabel = new JLabel("");
	private final JLabel submissionStatusLabel = new JLabel("");
	private final JButton joinButton = new JButton("Join");
	private final JButton screenshotSubmitButton = new JButton("Submit Proof");
	private final JButton lifelineButton = new JButton("Use Lifeline");

	// Bingo tab
	private final JPanel bingoListPanel = new JPanel();
	private final JLabel bingoNameLabel = new JLabel("Select a board");
	private final JPanel bingoTilesPanel = new JPanel();
	private final JComboBox<String> bingoTileSelector = new JComboBox<>();
	private final JButton bingoSubmitButton = new JButton("Submit Tile Proof");

	// Drops tab
	private final JPanel dropsListPanel = new JPanel();
	private final JTextField dropTitleField = new JTextField();
	private final JButton submitDropButton = new JButton("Submit Drop");
	private final JButton refreshDropsButton = new JButton("Refresh");

	// Split tab
	private final JTextField splitAmountField = new JTextField();
	private final JTextField splitWithField = new JTextField();
	private final JButton splitSubmitButton = new JButton("Submit Split");
	private final JButton splitScreenshotButton = new JButton("With Screenshot");
	private final JLabel splitStatusLabel = new JLabel("");

	// Chat tab
	private final JPanel chatMessagesPanel = new JPanel();
	private final JTextField chatInputField = new JTextField();
	private final JButton sendChatButton = new JButton("Send");
	private final JButton refreshChatButton = new JButton("Refresh");

	// State
	private String sessionToken;
	private String selectedEventId;
	private String selectedEventType;
	private String selectedBoardId;
	private JsonArray currentBingoTiles;

	@Inject
	ClanEventHubPanel(ClanEventHubPlugin plugin, ClanEventHubConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 10, 8, 10));

		JLabel title = new JLabel("Mercenary PvM");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		header.add(title);

		clanStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		clanStatusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		clanStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		header.add(clanStatusLabel);

		playerNameLabel.setFont(FontManager.getRunescapeSmallFont());
		playerNameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		playerNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		header.add(playerNameLabel);

		JButton reloadButton = new JButton("Reload");
		styleButton(reloadButton);
		reloadButton.setAlignmentX(LEFT_ALIGNMENT);
		reloadButton.setMaximumSize(new Dimension(PANEL_WIDTH, 28));
		reloadButton.addActionListener(e -> onClanDetected());
		header.add(createSpacer(4));
		header.add(reloadButton);

		add(header, BorderLayout.NORTH);

		// Tabs
		JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tabs.setForeground(Color.WHITE);
		tabs.setFont(FontManager.getRunescapeSmallFont());

		tabs.addTab("Events", buildEventsTab());
		tabs.addTab("Bingo", buildBingoTab());
		tabs.addTab("Splits", buildSplitTab());
		tabs.addTab("Drops", buildDropsTab());
		tabs.addTab("Chat", buildChatTab());

		add(tabs, BorderLayout.CENTER);
	}

	// --- Events Tab ---

	private JPanel buildEventsTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel eventsHeader = sectionHeader("Active Events");
		topPanel.add(eventsHeader);
		topPanel.add(createSpacer(4));

		eventsListPanel.setLayout(new BoxLayout(eventsListPanel, BoxLayout.Y_AXIS));
		eventsListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		eventsListPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventsListPanel);
		topPanel.add(createSpacer(8));

		// Event detail
		eventNameLabel.setFont(FontManager.getRunescapeBoldFont());
		eventNameLabel.setForeground(Color.WHITE);
		eventNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventNameLabel);

		eventStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		eventStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		eventStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventStatusLabel);
		topPanel.add(createSpacer(6));

		topPanel.add(sectionHeader("Current Task"));
		topPanel.add(createSpacer(2));

		taskTitleLabel.setFont(FontManager.getRunescapeSmallFont());
		taskTitleLabel.setForeground(Color.WHITE);
		taskTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(taskTitleLabel);

		taskDescArea.setEditable(false);
		taskDescArea.setLineWrap(true);
		taskDescArea.setWrapStyleWord(true);
		taskDescArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		taskDescArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		taskDescArea.setFont(FontManager.getRunescapeSmallFont());
		taskDescArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		taskDescArea.setAlignmentX(LEFT_ALIGNMENT);
		taskDescArea.setMaximumSize(new Dimension(PANEL_WIDTH, 70));
		topPanel.add(taskDescArea);
		topPanel.add(createSpacer(4));

		countdownLabel.setFont(FontManager.getRunescapeSmallFont());
		countdownLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		countdownLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(countdownLabel);
		topPanel.add(createSpacer(4));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(statusLabel);

		pointsLabel.setFont(FontManager.getRunescapeSmallFont());
		pointsLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		pointsLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(pointsLabel);

		submissionStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		submissionStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		submissionStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(submissionStatusLabel);
		topPanel.add(createSpacer(8));

		// Buttons stacked vertically
		JPanel buttonGrid = new JPanel(new GridLayout(0, 2, 4, 4));
		buttonGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonGrid.setAlignmentX(LEFT_ALIGNMENT);
		buttonGrid.setMaximumSize(new Dimension(PANEL_WIDTH, 60));

		styleButton(joinButton);
		buttonGrid.add(joinButton);
		joinButton.addActionListener(e -> onJoinEvent());

		styleButton(screenshotSubmitButton);
		buttonGrid.add(screenshotSubmitButton);
		screenshotSubmitButton.addActionListener(e -> onScreenshotAndSubmitProof());

		topPanel.add(buttonGrid);
		topPanel.add(createSpacer(4));

		styleButton(lifelineButton);
		lifelineButton.setAlignmentX(LEFT_ALIGNMENT);
		lifelineButton.setMaximumSize(new Dimension(PANEL_WIDTH, 28));
		lifelineButton.addActionListener(e -> onUseLifeline());
		topPanel.add(lifelineButton);

		JScrollPane scroll = new JScrollPane(topPanel);
		scroll.setBorder(null);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scroll, BorderLayout.CENTER);

		return panel;
	}

	// --- Bingo Tab ---

	private JPanel buildBingoTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		topPanel.add(sectionHeader("Bingo Boards"));
		topPanel.add(createSpacer(4));

		bingoListPanel.setLayout(new BoxLayout(bingoListPanel, BoxLayout.Y_AXIS));
		bingoListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bingoListPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoListPanel);
		topPanel.add(createSpacer(8));

		bingoNameLabel.setFont(FontManager.getRunescapeBoldFont());
		bingoNameLabel.setForeground(Color.WHITE);
		bingoNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoNameLabel);
		topPanel.add(createSpacer(4));

		bingoTilesPanel.setLayout(new BoxLayout(bingoTilesPanel, BoxLayout.Y_AXIS));
		bingoTilesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bingoTilesPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoTilesPanel);
		topPanel.add(createSpacer(8));

		JLabel submitLabel = new JLabel("Submit proof:");
		submitLabel.setFont(FontManager.getRunescapeSmallFont());
		submitLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		submitLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(submitLabel);
		topPanel.add(createSpacer(2));

		bingoTileSelector.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
		bingoTileSelector.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoTileSelector);
		topPanel.add(createSpacer(4));

		styleButton(bingoSubmitButton);
		bingoSubmitButton.setAlignmentX(LEFT_ALIGNMENT);
		bingoSubmitButton.setMaximumSize(new Dimension(PANEL_WIDTH, 28));
		bingoSubmitButton.addActionListener(e -> onSubmitBingoProof());
		topPanel.add(bingoSubmitButton);

		JScrollPane scroll = new JScrollPane(topPanel);
		scroll.setBorder(null);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scroll, BorderLayout.CENTER);

		return panel;
	}

	// --- Split Tab ---

	private JPanel buildSplitTab()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		panel.add(sectionHeader("Split Tracker"));
		panel.add(createSpacer(2));

		JLabel desc = new JLabel("<html>Submit a split for Discord approval.</html>");
		desc.setFont(FontManager.getRunescapeSmallFont());
		desc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		desc.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(desc);
		panel.add(createSpacer(8));

		panel.add(fieldLabel("Total amount (gp):"));
		splitAmountField.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
		splitAmountField.setAlignmentX(LEFT_ALIGNMENT);
		splitAmountField.setToolTipText("e.g. 500m, 1.5b, 200k");
		panel.add(splitAmountField);
		panel.add(createSpacer(6));

		panel.add(fieldLabel("Split with (comma-separated):"));
		splitWithField.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
		splitWithField.setAlignmentX(LEFT_ALIGNMENT);
		splitWithField.setToolTipText("e.g. PlayerB, PlayerC");
		panel.add(splitWithField);
		panel.add(createSpacer(8));

		JPanel buttonGrid = new JPanel(new GridLayout(1, 2, 4, 0));
		buttonGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonGrid.setAlignmentX(LEFT_ALIGNMENT);
		buttonGrid.setMaximumSize(new Dimension(PANEL_WIDTH, 28));

		styleButton(splitSubmitButton);
		splitSubmitButton.addActionListener(e -> onSubmitSplit(false));
		buttonGrid.add(splitSubmitButton);

		styleButton(splitScreenshotButton);
		splitScreenshotButton.addActionListener(e -> onSubmitSplit(true));
		buttonGrid.add(splitScreenshotButton);

		panel.add(buttonGrid);
		panel.add(createSpacer(6));

		splitStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		splitStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		splitStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(splitStatusLabel);

		return panel;
	}

	// --- Drops Tab ---

	private JPanel buildDropsTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel submitPanel = new JPanel();
		submitPanel.setLayout(new BoxLayout(submitPanel, BoxLayout.Y_AXIS));
		submitPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		submitPanel.add(sectionHeader("Hall of Fame"));
		submitPanel.add(createSpacer(4));

		submitPanel.add(fieldLabel("Drop name:"));
		dropTitleField.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
		dropTitleField.setAlignmentX(LEFT_ALIGNMENT);
		submitPanel.add(dropTitleField);
		submitPanel.add(createSpacer(4));

		JPanel buttonGrid = new JPanel(new GridLayout(1, 2, 4, 0));
		buttonGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonGrid.setAlignmentX(LEFT_ALIGNMENT);
		buttonGrid.setMaximumSize(new Dimension(PANEL_WIDTH, 28));

		styleButton(submitDropButton);
		submitDropButton.addActionListener(e -> onSubmitDrop());
		buttonGrid.add(submitDropButton);

		styleButton(refreshDropsButton);
		refreshDropsButton.addActionListener(e -> onRefreshDrops());
		buttonGrid.add(refreshDropsButton);

		submitPanel.add(buttonGrid);
		submitPanel.add(createSpacer(8));

		panel.add(submitPanel, BorderLayout.NORTH);

		dropsListPanel.setLayout(new BoxLayout(dropsListPanel, BoxLayout.Y_AXIS));
		dropsListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(dropsListPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	// --- Chat Tab ---

	private JPanel buildChatTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
		chatMessagesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(chatMessagesPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		inputPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

		chatInputField.setColumns(12);
		inputPanel.add(chatInputField, BorderLayout.CENTER);

		JPanel chatButtons = new JPanel(new GridLayout(1, 2, 3, 0));
		chatButtons.setBackground(ColorScheme.DARK_GRAY_COLOR);

		styleButton(sendChatButton);
		sendChatButton.addActionListener(e -> onSendChat());
		chatButtons.add(sendChatButton);

		styleButton(refreshChatButton);
		refreshChatButton.addActionListener(e -> onRefreshChat());
		chatButtons.add(refreshChatButton);

		inputPanel.add(chatButtons, BorderLayout.SOUTH);
		panel.add(inputPanel, BorderLayout.SOUTH);

		return panel;
	}

	// --- Clan detection callbacks ---

	void onClanDetected()
	{
		String rsn = plugin.getLocalPlayerName();
		clanStatusLabel.setText(config.clanName() + " member");
		clanStatusLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		playerNameLabel.setText(rsn != null ? "RSN: " + rsn : "");

		if (plugin.getApiBase().isEmpty())
		{
			eventNameLabel.setText("Set API Base URL in plugin config");
			return;
		}

		loadAllData();
	}

	void onClanLeft()
	{
		clanStatusLabel.setText("Configure clan in settings");
		clanStatusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		playerNameLabel.setText("");
	}

	private void loadAllData()
	{
		loadActiveEvents();
		onRefreshDrops();
	}

	// --- Active events loading ---

	private void loadActiveEvents()
	{
		eventsListPanel.removeAll();
		eventsListPanel.add(dimLabel("Loading..."));
		eventsListPanel.revalidate();

		bingoListPanel.removeAll();
		bingoListPanel.add(dimLabel("Loading..."));
		bingoListPanel.revalidate();

		plugin.getApiClient().getActiveEvents(plugin.getApiBase())
			.thenAccept(response ->
			{
				JsonArray allEvents = response.has("events") ? response.getAsJsonArray("events") : new JsonArray();

				JsonArray gameEvents = new JsonArray();
				JsonArray bingoEvents = new JsonArray();
				for (JsonElement el : allEvents)
				{
					JsonObject ev = el.getAsJsonObject();
					String type = getStr(ev, "type");
					if ("bingo".equals(type))
					{
						bingoEvents.add(ev);
					}
					gameEvents.add(ev);
				}

				SwingUtilities.invokeLater(() ->
				{
					updateEventsList(gameEvents);
					updateBingoFromEvents(bingoEvents);
				});
			})
			.exceptionally(ex ->
			{
				log.warn("Failed to load active events", ex);
				SwingUtilities.invokeLater(() ->
				{
					eventsListPanel.removeAll();
					eventsListPanel.add(errorLabel("Failed to load events"));
					eventsListPanel.revalidate();

					bingoListPanel.removeAll();
					bingoListPanel.add(errorLabel("Failed to load boards"));
					bingoListPanel.revalidate();
				});
				return null;
			});
	}

	private void updateEventsList(JsonArray events)
	{
		eventsListPanel.removeAll();

		if (events.size() == 0)
		{
			eventsListPanel.add(dimLabel("No active events"));
		}

		for (JsonElement el : events)
		{
			JsonObject ev = el.getAsJsonObject();
			String id = getStr(ev, "id");
			String rawName = getStr(ev, "name");
			if (rawName.isEmpty())
			{
				rawName = "Unknown";
			}
			final String eventName = rawName;
			String type = getStr(ev, "type");
			if (type.isEmpty())
			{
				type = "event";
			}
			final String eventType = type;
			String players = getStr(ev, "players");

			String typeTag = eventType.replace("_", " ");
			typeTag = typeTag.substring(0, 1).toUpperCase() + typeTag.substring(1);

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(4, 6, 4, 6)
			));
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setMaximumSize(new Dimension(PANEL_WIDTH, 36));
			row.setAlignmentX(LEFT_ALIGNMENT);

			JLabel nameLabel = new JLabel(eventName);
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			nameLabel.setForeground(Color.WHITE);
			row.add(nameLabel, BorderLayout.CENTER);

			String info = typeTag;
			if (!players.isEmpty())
			{
				info = players + "p | " + typeTag;
			}
			JLabel infoLabel = new JLabel(info);
			infoLabel.setFont(FontManager.getRunescapeSmallFont());
			infoLabel.setForeground(ColorScheme.BRAND_ORANGE);
			row.add(infoLabel, BorderLayout.EAST);

			row.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e)
				{
					selectEvent(id, eventName, eventType);
				}
			});

			eventsListPanel.add(row);
		}

		eventsListPanel.revalidate();
		eventsListPanel.repaint();
	}

	private void selectEvent(String eventId, String name, String type)
	{
		selectedEventId = eventId;
		selectedEventType = type;
		eventNameLabel.setText(name);
		refreshSelectedEvent();
	}

	private void refreshSelectedEvent()
	{
		if (selectedEventId == null)
		{
			return;
		}

		plugin.getApiClient().getEventState(plugin.getApiBase(), selectedEventId, sessionToken)
			.thenAccept(state -> SwingUtilities.invokeLater(() -> updateEventState(state)))
			.exceptionally(ex ->
			{
				log.warn("Failed to get event state", ex);
				return null;
			});

		plugin.getApiClient().getCountdown(plugin.getApiBase(), selectedEventId)
			.thenAccept(cd -> SwingUtilities.invokeLater(() -> updateCountdown(cd)))
			.exceptionally(ex ->
			{
				log.warn("Failed to get countdown", ex);
				return null;
			});
	}

	// --- Event actions ---

	private void onJoinEvent()
	{
		if (selectedEventId == null)
		{
			showMessage("Select an event first.");
			return;
		}

		String rsn = plugin.getLocalPlayerName();
		if (rsn == null)
		{
			showMessage("Log into the game first.");
			return;
		}

		joinButton.setEnabled(false);
		plugin.getApiClient().joinEvent(plugin.getApiBase(), selectedEventId, rsn)
			.thenAccept(response ->
			{
				sessionToken = response.has("token") ? response.get("token").getAsString() : sessionToken;
				SwingUtilities.invokeLater(() ->
				{
					joinButton.setEnabled(true);
					showMessage("Joined event!");
					refreshSelectedEvent();
				});
			})
			.exceptionally(ex ->
			{
				SwingUtilities.invokeLater(() ->
				{
					joinButton.setEnabled(true);
					showMessage("Failed: " + ex.getMessage());
				});
				return null;
			});
	}

	private void updateEventState(JsonObject state)
	{
		if (state.has("event"))
		{
			JsonObject event = state.getAsJsonObject("event");
			eventStatusLabel.setText("Status: " + getStr(event, "status")
				+ " | Day " + getStr(event, "current_day"));
		}

		if (state.has("currentTask") && !state.get("currentTask").isJsonNull())
		{
			JsonObject task = state.getAsJsonObject("currentTask");
			taskTitleLabel.setText(getStr(task, "title"));
			taskDescArea.setText(getStr(task, "description"));
		}
		else
		{
			taskTitleLabel.setText("No active task");
			taskDescArea.setText("");
		}

		if (state.has("myParticipant") && !state.get("myParticipant").isJsonNull())
		{
			JsonObject me = state.getAsJsonObject("myParticipant");
			statusLabel.setText("Status: " + getStr(me, "status"));
			pointsLabel.setText("Points: " + getStr(me, "total_points"));

			int lifelines = me.has("lifelines") ? me.get("lifelines").getAsInt() : 0;
			lifelineButton.setText("Lifeline (" + lifelines + ")");
			lifelineButton.setEnabled(lifelines > 0);
		}
		else
		{
			statusLabel.setText("Not joined");
			pointsLabel.setText("");
			lifelineButton.setEnabled(false);
		}

		if (state.has("mySubmission") && !state.get("mySubmission").isJsonNull())
		{
			JsonObject sub = state.getAsJsonObject("mySubmission");
			String subStatus = getStr(sub, "status");
			submissionStatusLabel.setText("Submission: " + subStatus);
			submissionStatusLabel.setForeground(
				"approved".equals(subStatus) ? ColorScheme.PROGRESS_COMPLETE_COLOR :
				"pending".equals(subStatus) ? ColorScheme.PROGRESS_INPROGRESS_COLOR :
				ColorScheme.LIGHT_GRAY_COLOR
			);
		}
		else
		{
			submissionStatusLabel.setText("No submission yet");
			submissionStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
	}

	private void updateCountdown(JsonObject countdown)
	{
		if (countdown.has("remaining"))
		{
			long remaining = countdown.get("remaining").getAsLong();
			long hours = remaining / 3600000;
			long minutes = (remaining % 3600000) / 60000;
			countdownLabel.setText(String.format("Deadline: %dh %dm", hours, minutes));
			countdownLabel.setForeground(hours < 1 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}
		else
		{
			countdownLabel.setText("");
		}
	}

	private void onScreenshotAndSubmitProof()
	{
		if (selectedEventId == null || sessionToken == null)
		{
			showMessage("Join an event first.");
			return;
		}

		screenshotSubmitButton.setEnabled(false);
		screenshotSubmitButton.setText("Capturing...");

		plugin.captureScreenshot(image ->
		{
			SwingUtilities.invokeLater(() -> screenshotSubmitButton.setText("Uploading..."));

			plugin.getApiClient().uploadImage(plugin.getApiBase(), image, sessionToken, config.botSecret())
				.thenAccept(uploadResult ->
				{
					String imageUrl = getStr(uploadResult, "full_url");

					plugin.getApiClient().submitProof(plugin.getApiBase(), selectedEventId, sessionToken, imageUrl, "Via RuneLite")
						.thenAccept(result -> SwingUtilities.invokeLater(() ->
						{
							screenshotSubmitButton.setEnabled(true);
							screenshotSubmitButton.setText("Submit Proof");
							showMessage("Proof submitted!");
							refreshSelectedEvent();
						}))
						.exceptionally(ex ->
						{
							SwingUtilities.invokeLater(() ->
							{
								screenshotSubmitButton.setEnabled(true);
								screenshotSubmitButton.setText("Submit Proof");
								showMessage("Submit failed: " + ex.getMessage());
							});
							return null;
						});
				})
				.exceptionally(ex ->
				{
					SwingUtilities.invokeLater(() ->
					{
						screenshotSubmitButton.setEnabled(true);
						screenshotSubmitButton.setText("Submit Proof");
						showMessage("Upload failed: " + ex.getMessage());
					});
					return null;
				});
		});
	}

	private void onUseLifeline()
	{
		if (selectedEventId == null || sessionToken == null)
		{
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to use a lifeline?", "Confirm",
			JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION)
		{
			return;
		}

		lifelineButton.setEnabled(false);
		plugin.getApiClient().useLifeline(plugin.getApiBase(), selectedEventId, sessionToken)
			.thenAccept(result -> SwingUtilities.invokeLater(() ->
			{
				showMessage("Lifeline used!");
				refreshSelectedEvent();
			}))
			.exceptionally(ex ->
			{
				SwingUtilities.invokeLater(() ->
				{
					lifelineButton.setEnabled(true);
					showMessage("Failed: " + ex.getMessage());
				});
				return null;
			});
	}

	// --- Bingo ---

	private void updateBingoFromEvents(JsonArray bingoEvents)
	{
		bingoListPanel.removeAll();

		if (bingoEvents.size() == 0)
		{
			bingoListPanel.add(dimLabel("No active bingo boards"));
		}

		for (JsonElement el : bingoEvents)
		{
			JsonObject board = el.getAsJsonObject();
			String id = getStr(board, "id");
			String name = getStr(board, "name");
			if (name.isEmpty())
			{
				name = id;
			}
			final String boardName = name;

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(4, 6, 4, 6)
			));
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setMaximumSize(new Dimension(PANEL_WIDTH, 32));
			row.setAlignmentX(LEFT_ALIGNMENT);

			JLabel nameLabel = new JLabel(boardName);
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			nameLabel.setForeground(Color.WHITE);
			row.add(nameLabel, BorderLayout.CENTER);

			JLabel statusLbl = new JLabel("Active");
			statusLbl.setFont(FontManager.getRunescapeSmallFont());
			statusLbl.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			row.add(statusLbl, BorderLayout.EAST);

			row.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e)
				{
					selectBingoBoard(board);
				}
			});

			bingoListPanel.add(row);
		}

		bingoListPanel.revalidate();
		bingoListPanel.repaint();

		if (bingoEvents.size() == 1)
		{
			selectBingoBoard(bingoEvents.get(0).getAsJsonObject());
		}
	}

	private void selectBingoBoard(JsonObject board)
	{
		selectedBoardId = getStr(board, "id");
		String name = getStr(board, "name");
		bingoNameLabel.setText(name.isEmpty() ? selectedBoardId : name);

		String rsn = plugin.getLocalPlayerName();
		String myTeamId = null;
		String myTeamName = null;

		if (rsn != null && board.has("members"))
		{
			for (JsonElement m : board.getAsJsonArray("members"))
			{
				JsonObject member = m.getAsJsonObject();
				if (rsn.equalsIgnoreCase(getStr(member, "username")))
				{
					myTeamId = getStr(member, "team_id");
					break;
				}
			}
		}

		if (myTeamId != null && board.has("teams"))
		{
			for (JsonElement t : board.getAsJsonArray("teams"))
			{
				JsonObject team = t.getAsJsonObject();
				if (myTeamId.equals(getStr(team, "id")))
				{
					myTeamName = getStr(team, "name");
					break;
				}
			}
		}

		if (myTeamName != null)
		{
			bingoNameLabel.setText(bingoNameLabel.getText() + " - " + myTeamName);
		}

		bingoTilesPanel.removeAll();
		bingoTileSelector.removeAllItems();
		currentBingoTiles = board.has("tiles") ? board.getAsJsonArray("tiles") : new JsonArray();

		for (JsonElement el : currentBingoTiles)
		{
			JsonObject tile = el.getAsJsonObject();
			int tileId = tile.has("id") ? tile.get("id").getAsInt() : 0;
			String item = getStr(tile, "item");
			int points = tile.has("points") ? tile.get("points").getAsInt() : 0;
			boolean completed = tile.has("completed") && tile.get("completed").getAsBoolean();

			JPanel tileRow = new JPanel(new BorderLayout(4, 0));
			tileRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			tileRow.setBorder(new EmptyBorder(2, 4, 2, 4));
			tileRow.setMaximumSize(new Dimension(PANEL_WIDTH, 24));
			tileRow.setAlignmentX(LEFT_ALIGNMENT);

			JLabel tileLabel = new JLabel(item);
			tileLabel.setFont(FontManager.getRunescapeSmallFont());
			tileLabel.setForeground(completed ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);
			tileRow.add(tileLabel, BorderLayout.CENTER);

			String rightText = completed ? "Done" : points + "pts";
			JLabel rightLabel = new JLabel(rightText);
			rightLabel.setFont(FontManager.getRunescapeSmallFont());
			rightLabel.setForeground(completed ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
			tileRow.add(rightLabel, BorderLayout.EAST);

			bingoTilesPanel.add(tileRow);

			if (!completed)
			{
				bingoTileSelector.addItem(tileId + " - " + item);
			}
		}

		bingoTilesPanel.revalidate();
		bingoTilesPanel.repaint();
	}

	private void onSubmitBingoProof()
	{
		if (selectedBoardId == null || currentBingoTiles == null || currentBingoTiles.size() == 0)
		{
			showMessage("Select a bingo board first.");
			return;
		}

		int selectedIndex = bingoTileSelector.getSelectedIndex();
		if (selectedIndex < 0)
		{
			showMessage("Select an incomplete tile.");
			return;
		}

		String rsn = plugin.getLocalPlayerName();
		if (rsn == null)
		{
			showMessage("Log into the game first.");
			return;
		}

		String botSecret = config.botSecret();
		if (sessionToken == null && botSecret.isEmpty())
		{
			showMessage("Set Bot Secret in config, or join an event first.");
			return;
		}

		String selected = (String) bingoTileSelector.getSelectedItem();
		int tileId;
		try
		{
			tileId = Integer.parseInt(selected.split(" - ")[0].trim());
		}
		catch (NumberFormatException e)
		{
			showMessage("Invalid tile selection.");
			return;
		}

		bingoSubmitButton.setEnabled(false);
		bingoSubmitButton.setText("Capturing...");

		plugin.captureScreenshot(image ->
		{
			SwingUtilities.invokeLater(() -> bingoSubmitButton.setText("Uploading..."));

			plugin.getApiClient().submitBingoProof(plugin.getApiBase(),
				selectedBoardId, tileId, rsn, image, sessionToken, botSecret)
				.thenAccept(result -> SwingUtilities.invokeLater(() ->
				{
					bingoSubmitButton.setEnabled(true);
					bingoSubmitButton.setText("Submit Tile Proof");
					showMessage("Bingo proof submitted!");
					loadActiveEvents();
				}))
				.exceptionally(ex ->
				{
					SwingUtilities.invokeLater(() ->
					{
						bingoSubmitButton.setEnabled(true);
						bingoSubmitButton.setText("Submit Tile Proof");
						showMessage("Failed: " + ex.getMessage());
					});
					return null;
				});
		});
	}

	// --- Splits ---

	private void onSubmitSplit(boolean withScreenshot)
	{
		String rsn = plugin.getLocalPlayerName();
		if (rsn == null)
		{
			showMessage("Log into the game first.");
			return;
		}

		long amount = parseGpAmount(splitAmountField.getText().trim());
		if (amount <= 0)
		{
			showMessage("Enter a valid GP amount.");
			return;
		}

		String splitWithText = splitWithField.getText().trim();
		if (splitWithText.isEmpty())
		{
			showMessage("Enter at least one IGN to split with.");
			return;
		}

		String[] splitWith = splitWithText.split(",");
		for (int i = 0; i < splitWith.length; i++)
		{
			splitWith[i] = splitWith[i].trim();
		}

		String apiKey = config.splitApiKey();

		splitSubmitButton.setEnabled(false);
		splitScreenshotButton.setEnabled(false);
		splitStatusLabel.setText("Submitting...");
		splitStatusLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);

		if (withScreenshot)
		{
			final String[] partners = splitWith;
			plugin.captureScreenshot(image ->
			{
				String base64 = imageToBase64(image);
				doSplitSubmit(rsn, amount, partners, base64, apiKey);
			});
		}
		else
		{
			doSplitSubmit(rsn, amount, splitWith, null, apiKey);
		}
	}

	private void doSplitSubmit(String rsn, long amount, String[] splitWith, String screenshot, String apiKey)
	{
		plugin.getApiClient().submitSplit(config.splitBotUrl(), rsn, amount, splitWith, screenshot, apiKey)
			.thenAccept(result -> SwingUtilities.invokeLater(() ->
			{
				splitSubmitButton.setEnabled(true);
				splitScreenshotButton.setEnabled(true);
				splitStatusLabel.setText("Split submitted for approval!");
				splitStatusLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
				splitAmountField.setText("");
				splitWithField.setText("");
			}))
			.exceptionally(ex ->
			{
				SwingUtilities.invokeLater(() ->
				{
					splitSubmitButton.setEnabled(true);
					splitScreenshotButton.setEnabled(true);
					splitStatusLabel.setText("Failed: " + ex.getMessage());
					splitStatusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
				});
				return null;
			});
	}

	private static long parseGpAmount(String input)
	{
		if (input.isEmpty())
		{
			return 0;
		}

		input = input.toLowerCase().replace(",", "").replace(" ", "");

		double multiplier = 1;
		if (input.endsWith("b"))
		{
			multiplier = 1_000_000_000;
			input = input.substring(0, input.length() - 1);
		}
		else if (input.endsWith("m"))
		{
			multiplier = 1_000_000;
			input = input.substring(0, input.length() - 1);
		}
		else if (input.endsWith("k"))
		{
			multiplier = 1_000;
			input = input.substring(0, input.length() - 1);
		}

		try
		{
			return (long) (Double.parseDouble(input) * multiplier);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private static String imageToBase64(BufferedImage image)
	{
		try
		{
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(image, "png", baos);
			return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
		}
		catch (java.io.IOException e)
		{
			log.warn("Failed to encode screenshot as base64", e);
			return null;
		}
	}

	// --- Drops ---

	private void onSubmitDrop()
	{
		String title = dropTitleField.getText().trim();
		if (title.isEmpty())
		{
			showMessage("Enter a drop name.");
			return;
		}

		String rsn = plugin.getLocalPlayerName();
		if (rsn == null)
		{
			showMessage("Log into the game first.");
			return;
		}

		String botSecret = config.botSecret();
		if (botSecret.isEmpty())
		{
			showMessage("Set Bot Secret in plugin config.");
			return;
		}

		submitDropButton.setEnabled(false);
		submitDropButton.setText("Capturing...");

		plugin.captureScreenshot(image ->
		{
			SwingUtilities.invokeLater(() -> submitDropButton.setText("Uploading..."));

			plugin.getApiClient().uploadImage(plugin.getApiBase(), image, sessionToken, botSecret)
				.thenAccept(uploadResult ->
				{
					String imageUrl = getStr(uploadResult, "full_url");

					plugin.getApiClient().addDrop(plugin.getApiBase(), title, rsn, imageUrl, botSecret)
						.thenAccept(result -> SwingUtilities.invokeLater(() ->
						{
							submitDropButton.setEnabled(true);
							submitDropButton.setText("Submit Drop");
							dropTitleField.setText("");
							showMessage("Drop submitted!");
							onRefreshDrops();
						}))
						.exceptionally(ex ->
						{
							SwingUtilities.invokeLater(() ->
							{
								submitDropButton.setEnabled(true);
								submitDropButton.setText("Submit Drop");
								showMessage("Submit failed: " + ex.getMessage());
							});
							return null;
						});
				})
				.exceptionally(ex ->
				{
					SwingUtilities.invokeLater(() ->
					{
						submitDropButton.setEnabled(true);
						submitDropButton.setText("Submit Drop");
						showMessage("Upload failed: " + ex.getMessage());
					});
					return null;
				});
		});
	}

	void onRefreshDrops()
	{
		refreshDropsButton.setEnabled(false);
		plugin.getApiClient().getDrops(plugin.getApiBase(), 50)
			.thenAccept(drops -> SwingUtilities.invokeLater(() ->
			{
				updateDropsList(drops);
				refreshDropsButton.setEnabled(true);
			}))
			.exceptionally(ex ->
			{
				SwingUtilities.invokeLater(() -> refreshDropsButton.setEnabled(true));
				log.warn("Failed to load drops", ex);
				return null;
			});
	}

	private void updateDropsList(JsonArray drops)
	{
		dropsListPanel.removeAll();

		if (drops.size() == 0)
		{
			dropsListPanel.add(dimLabel("No drops yet"));
		}

		for (JsonElement el : drops)
		{
			JsonObject drop = el.getAsJsonObject();
			JPanel dropPanel = new JPanel(new BorderLayout());
			dropPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			dropPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(4, 5, 4, 5)
			));

			String dropTitle = getStr(drop, "title");
			String player = getStr(drop, "player");
			String date = getStr(drop, "date");

			JLabel titleLabel = new JLabel(dropTitle.isEmpty() ? "Unknown" : dropTitle);
			titleLabel.setFont(FontManager.getRunescapeSmallFont());
			titleLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			dropPanel.add(titleLabel, BorderLayout.CENTER);

			String detail = player + (date.length() >= 10 ? " - " + date.substring(0, 10) : "");
			JLabel detailLabel = new JLabel(detail);
			detailLabel.setFont(FontManager.getRunescapeSmallFont());
			detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			dropPanel.add(detailLabel, BorderLayout.SOUTH);

			dropPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 40));
			dropPanel.setAlignmentX(LEFT_ALIGNMENT);
			dropsListPanel.add(dropPanel);
		}

		dropsListPanel.revalidate();
		dropsListPanel.repaint();
	}

	// --- Chat ---

	private void onSendChat()
	{
		String message = chatInputField.getText().trim();
		if (selectedEventId == null || sessionToken == null)
		{
			showMessage("Join an event first.");
			return;
		}
		if (message.isEmpty())
		{
			return;
		}

		sendChatButton.setEnabled(false);
		plugin.getApiClient().sendChat(plugin.getApiBase(), selectedEventId, sessionToken, message)
			.thenAccept(result -> SwingUtilities.invokeLater(() ->
			{
				chatInputField.setText("");
				sendChatButton.setEnabled(true);
				onRefreshChat();
			}))
			.exceptionally(ex ->
			{
				SwingUtilities.invokeLater(() ->
				{
					sendChatButton.setEnabled(true);
					showMessage("Send failed: " + ex.getMessage());
				});
				return null;
			});
	}

	void onRefreshChat()
	{
		if (selectedEventId == null)
		{
			return;
		}

		plugin.getApiClient().getChat(plugin.getApiBase(), selectedEventId)
			.thenAccept(messages -> SwingUtilities.invokeLater(() -> updateChatMessages(messages)))
			.exceptionally(ex ->
			{
				log.warn("Failed to load chat", ex);
				return null;
			});
	}

	private void updateChatMessages(JsonArray messages)
	{
		chatMessagesPanel.removeAll();

		for (JsonElement el : messages)
		{
			JsonObject msg = el.getAsJsonObject();
			String user = getStr(msg, "user_id");
			String text = getStr(msg, "message");

			JLabel msgLabel = new JLabel("<html><b>" + escapeHtml(user) + ":</b> " + escapeHtml(text) + "</html>");
			msgLabel.setFont(FontManager.getRunescapeSmallFont());
			msgLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			msgLabel.setBorder(new EmptyBorder(2, 0, 2, 0));
			msgLabel.setAlignmentX(LEFT_ALIGNMENT);
			chatMessagesPanel.add(msgLabel);
		}

		chatMessagesPanel.revalidate();
		chatMessagesPanel.repaint();
	}

	// --- Auto-submit support ---

	void submitDropAutomatically(String dropName, BufferedImage screenshot)
	{
		String rsn = plugin.getLocalPlayerName();
		String botSecret = config.botSecret();

		if (rsn == null || botSecret.isEmpty())
		{
			return;
		}

		log.debug("Auto-submitting drop: {}", dropName);

		plugin.getApiClient().uploadImage(plugin.getApiBase(), screenshot, sessionToken, botSecret)
			.thenAccept(uploadResult ->
			{
				String imageUrl = getStr(uploadResult, "full_url");

				plugin.getApiClient().addDrop(plugin.getApiBase(), dropName, rsn, imageUrl, botSecret)
					.thenAccept(result -> log.debug("Auto-submitted drop {} to Hall of Fame", dropName))
					.exceptionally(ex ->
					{
						log.warn("Failed to auto-submit drop {}", dropName, ex);
						return null;
					});
			})
			.exceptionally(ex ->
			{
				log.warn("Failed to upload screenshot for drop {}", dropName, ex);
				return null;
			});
	}

	// --- Utility ---

	private static String getStr(JsonObject obj, String key)
	{
		return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
	}

	private JLabel sectionHeader(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JLabel fieldLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JLabel dimLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private JLabel errorLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		return label;
	}

	private JPanel createSpacer(int height)
	{
		JPanel spacer = new JPanel();
		spacer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		spacer.setMaximumSize(new Dimension(PANEL_WIDTH, height));
		spacer.setPreferredSize(new Dimension(0, height));
		spacer.setAlignmentX(LEFT_ALIGNMENT);
		return spacer;
	}

	private void styleButton(JButton button)
	{
		button.setFocusPainted(false);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(4, 8, 4, 8)
		));
	}

	private void showMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Mercenary PvM", JOptionPane.INFORMATION_MESSAGE);
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
