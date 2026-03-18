package com.claneventhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
	private final JButton joinButton = new JButton("Join Event");
	private final JButton screenshotSubmitButton = new JButton("Screenshot & Submit Proof");
	private final JButton lifelineButton = new JButton("Use Lifeline");

	// Bingo tab
	private final JPanel bingoListPanel = new JPanel();
	private final JPanel bingoDetailPanel = new JPanel();
	private final JLabel bingoNameLabel = new JLabel("Select a bingo board");
	private final JPanel bingoTilesPanel = new JPanel();
	private final JComboBox<String> bingoTileSelector = new JComboBox<>();
	private final JButton bingoSubmitButton = new JButton("Screenshot & Submit Tile");

	// Drops tab
	private final JPanel dropsListPanel = new JPanel();
	private final JTextField dropTitleField = new JTextField();
	private final JButton submitDropButton = new JButton("Screenshot & Submit Drop");
	private final JButton refreshDropsButton = new JButton("Refresh");

	// Split tab
	private final JTextField splitAmountField = new JTextField();
	private final JTextField splitWithField = new JTextField();
	private final JButton splitSubmitButton = new JButton("Submit Split");
	private final JButton splitScreenshotButton = new JButton("Screenshot & Submit Split");
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
		reloadButton.addActionListener(e -> onClanDetected());
		header.add(reloadButton);

		add(header, BorderLayout.NORTH);

		// Tabs
		JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tabs.setForeground(Color.WHITE);

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
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Top: event list
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel eventsHeader = new JLabel("Active Events");
		eventsHeader.setFont(FontManager.getRunescapeBoldFont());
		eventsHeader.setForeground(ColorScheme.BRAND_ORANGE);
		eventsHeader.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventsHeader);
		topPanel.add(createSpacer(5));

		eventsListPanel.setLayout(new BoxLayout(eventsListPanel, BoxLayout.Y_AXIS));
		eventsListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		eventsListPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventsListPanel);
		topPanel.add(createSpacer(10));

		// Event detail
		eventNameLabel.setFont(FontManager.getRunescapeBoldFont());
		eventNameLabel.setForeground(Color.WHITE);
		eventNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventNameLabel);

		eventStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		eventStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(eventStatusLabel);
		topPanel.add(createSpacer(5));

		JLabel taskHeader = new JLabel("Current Task");
		taskHeader.setFont(FontManager.getRunescapeSmallFont());
		taskHeader.setForeground(ColorScheme.BRAND_ORANGE);
		taskHeader.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(taskHeader);

		taskTitleLabel.setFont(FontManager.getRunescapeBoldFont());
		taskTitleLabel.setForeground(Color.WHITE);
		taskTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(taskTitleLabel);

		taskDescArea.setEditable(false);
		taskDescArea.setLineWrap(true);
		taskDescArea.setWrapStyleWord(true);
		taskDescArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		taskDescArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		taskDescArea.setFont(FontManager.getRunescapeSmallFont());
		taskDescArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		taskDescArea.setAlignmentX(LEFT_ALIGNMENT);
		taskDescArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
		topPanel.add(taskDescArea);
		topPanel.add(createSpacer(5));

		countdownLabel.setFont(FontManager.getRunescapeBoldFont());
		countdownLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		countdownLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(countdownLabel);
		topPanel.add(createSpacer(5));

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(statusLabel);

		pointsLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		pointsLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(pointsLabel);

		submissionStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		submissionStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(submissionStatusLabel);
		topPanel.add(createSpacer(10));

		// Action buttons
		JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		actionRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		actionRow.setAlignmentX(LEFT_ALIGNMENT);

		styleButton(joinButton);
		joinButton.addActionListener(e -> onJoinEvent());
		actionRow.add(joinButton);

		styleButton(screenshotSubmitButton);
		screenshotSubmitButton.addActionListener(e -> onScreenshotAndSubmitProof());
		actionRow.add(screenshotSubmitButton);

		topPanel.add(actionRow);
		topPanel.add(createSpacer(5));

		styleButton(lifelineButton);
		lifelineButton.setAlignmentX(LEFT_ALIGNMENT);
		lifelineButton.addActionListener(e -> onUseLifeline());
		topPanel.add(lifelineButton);

		JScrollPane scroll = new JScrollPane(topPanel);
		scroll.setBorder(null);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.add(scroll, BorderLayout.CENTER);

		return panel;
	}

	// --- Bingo Tab ---

	private JPanel buildBingoTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel bingoHeader = new JLabel("Bingo Boards");
		bingoHeader.setFont(FontManager.getRunescapeBoldFont());
		bingoHeader.setForeground(ColorScheme.BRAND_ORANGE);
		bingoHeader.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoHeader);
		topPanel.add(createSpacer(5));

		bingoListPanel.setLayout(new BoxLayout(bingoListPanel, BoxLayout.Y_AXIS));
		bingoListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bingoListPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoListPanel);
		topPanel.add(createSpacer(10));

		// Board detail
		bingoNameLabel.setFont(FontManager.getRunescapeBoldFont());
		bingoNameLabel.setForeground(Color.WHITE);
		bingoNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoNameLabel);
		topPanel.add(createSpacer(5));

		bingoTilesPanel.setLayout(new BoxLayout(bingoTilesPanel, BoxLayout.Y_AXIS));
		bingoTilesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bingoTilesPanel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoTilesPanel);
		topPanel.add(createSpacer(10));

		// Submit proof for tile
		JLabel submitLabel = new JLabel("Submit proof for tile:");
		submitLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		submitLabel.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(submitLabel);

		bingoTileSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		bingoTileSelector.setAlignmentX(LEFT_ALIGNMENT);
		topPanel.add(bingoTileSelector);
		topPanel.add(createSpacer(5));

		styleButton(bingoSubmitButton);
		bingoSubmitButton.setAlignmentX(LEFT_ALIGNMENT);
		bingoSubmitButton.addActionListener(e -> onSubmitBingoProof());
		topPanel.add(bingoSubmitButton);

		JScrollPane scroll = new JScrollPane(topPanel);
		scroll.setBorder(null);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.add(scroll, BorderLayout.CENTER);

		return panel;
	}

	// --- Drops Tab ---

	private JPanel buildDropsTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel submitPanel = new JPanel();
		submitPanel.setLayout(new BoxLayout(submitPanel, BoxLayout.Y_AXIS));
		submitPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel submitHeader = new JLabel("Submit Drop to Hall of Fame");
		submitHeader.setFont(FontManager.getRunescapeBoldFont());
		submitHeader.setForeground(ColorScheme.BRAND_ORANGE);
		submitHeader.setAlignmentX(LEFT_ALIGNMENT);
		submitPanel.add(submitHeader);
		submitPanel.add(createSpacer(5));

		JLabel dropNameLabel = new JLabel("Drop name:");
		dropNameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dropNameLabel.setAlignmentX(LEFT_ALIGNMENT);
		submitPanel.add(dropNameLabel);

		dropTitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		dropTitleField.setAlignmentX(LEFT_ALIGNMENT);
		submitPanel.add(dropTitleField);
		submitPanel.add(createSpacer(5));

		JPanel dropButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		dropButtons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dropButtons.setAlignmentX(LEFT_ALIGNMENT);
		styleButton(submitDropButton);
		submitDropButton.addActionListener(e -> onSubmitDrop());
		dropButtons.add(submitDropButton);
		styleButton(refreshDropsButton);
		refreshDropsButton.addActionListener(e -> onRefreshDrops());
		dropButtons.add(refreshDropsButton);
		submitPanel.add(dropButtons);
		submitPanel.add(createSpacer(10));

		panel.add(submitPanel, BorderLayout.NORTH);

		dropsListPanel.setLayout(new BoxLayout(dropsListPanel, BoxLayout.Y_AXIS));
		dropsListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(dropsListPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	// --- Chat Tab ---

	private JPanel buildChatTab()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
		chatMessagesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(chatMessagesPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		inputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

		chatInputField.setColumns(15);
		inputPanel.add(chatInputField, BorderLayout.CENTER);

		JPanel chatButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		chatButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		styleButton(sendChatButton);
		sendChatButton.addActionListener(e -> onSendChat());
		chatButtonPanel.add(sendChatButton);

		styleButton(refreshChatButton);
		refreshChatButton.addActionListener(e -> onRefreshChat());
		chatButtonPanel.add(refreshChatButton);

		inputPanel.add(chatButtonPanel, BorderLayout.SOUTH);
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

	// --- Active events loading (feeds both Events + Bingo tabs) ---

	private void loadActiveEvents()
	{
		eventsListPanel.removeAll();
		JLabel loadingLabel = new JLabel("Loading...");
		loadingLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		eventsListPanel.add(loadingLabel);
		eventsListPanel.revalidate();

		bingoListPanel.removeAll();
		JLabel bingoLoading = new JLabel("Loading...");
		bingoLoading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		bingoListPanel.add(bingoLoading);
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
					JLabel err = new JLabel("Failed to load events");
					err.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
					eventsListPanel.add(err);
					eventsListPanel.revalidate();

					bingoListPanel.removeAll();
					JLabel bErr = new JLabel("Failed to load boards");
					bErr.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
					bingoListPanel.add(bErr);
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
			JLabel empty = new JLabel("No active events");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			eventsListPanel.add(empty);
		}

		for (JsonElement el : events)
		{
			JsonObject ev = el.getAsJsonObject();
			String id = getStr(ev, "id");
			String name = getStr(ev, "name");
			if (name.isEmpty())
			{
				name = "Unknown";
			}
			final String eventName = name;
			String type = getStr(ev, "type");
			if (type.isEmpty())
			{
				type = "event";
			}
			final String eventType = type;
			String players = getStr(ev, "players");

			JPanel row = new JPanel(new BorderLayout());
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(5, 8, 5, 8)
			));
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
			row.setAlignmentX(LEFT_ALIGNMENT);

			String typeTag = eventType.replace("_", " ");
			typeTag = typeTag.substring(0, 1).toUpperCase() + typeTag.substring(1);

			JLabel nameLabel = new JLabel("<html>" + escapeHtml(eventName) + "</html>");
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			nameLabel.setForeground(Color.WHITE);
			row.add(nameLabel, BorderLayout.CENTER);

			JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
			rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			if (!players.isEmpty())
			{
				JLabel playersLabel = new JLabel(players + "p");
				playersLabel.setFont(FontManager.getRunescapeSmallFont());
				playersLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				rightPanel.add(playersLabel);
			}
			JLabel typeLabel = new JLabel(typeTag);
			typeLabel.setFont(FontManager.getRunescapeSmallFont());
			typeLabel.setForeground(ColorScheme.BRAND_ORANGE);
			rightPanel.add(typeLabel);
			row.add(rightPanel, BorderLayout.EAST);

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

		// Try to load survivor state (works for survivor-type events)
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
			showMessage("Select an event from the list first.");
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
			lifelineButton.setText("Use Lifeline (" + lifelines + " left)");
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
			countdownLabel.setText(String.format("Deadline: %dh %dm remaining", hours, minutes));
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

					plugin.getApiClient().submitProof(plugin.getApiBase(), selectedEventId, sessionToken, imageUrl, "Submitted via RuneLite plugin")
						.thenAccept(result -> SwingUtilities.invokeLater(() ->
						{
							screenshotSubmitButton.setEnabled(true);
							screenshotSubmitButton.setText("Screenshot & Submit Proof");
							showMessage("Proof submitted!");
							refreshSelectedEvent();
						}))
						.exceptionally(ex ->
						{
							SwingUtilities.invokeLater(() ->
							{
								screenshotSubmitButton.setEnabled(true);
								screenshotSubmitButton.setText("Screenshot & Submit Proof");
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
						screenshotSubmitButton.setText("Screenshot & Submit Proof");
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
			"Are you sure you want to use a lifeline?", "Confirm Lifeline",
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
			JLabel empty = new JLabel("No active bingo boards");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			bingoListPanel.add(empty);
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

			JPanel row = new JPanel(new BorderLayout());
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(5, 8, 5, 8)
			));
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
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

		// Auto-select first board
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

		// Detect player's team
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

		// Load tiles
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

			JPanel tileRow = new JPanel(new BorderLayout());
			tileRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			tileRow.setBorder(new EmptyBorder(3, 5, 3, 5));
			tileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
			tileRow.setAlignmentX(LEFT_ALIGNMENT);

			JLabel tileLabel = new JLabel(item + " (" + points + "pts)");
			tileLabel.setFont(FontManager.getRunescapeSmallFont());
			tileLabel.setForeground(completed ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);
			tileRow.add(tileLabel, BorderLayout.CENTER);

			if (completed)
			{
				JLabel check = new JLabel("Done");
				check.setFont(FontManager.getRunescapeSmallFont());
				check.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
				tileRow.add(check, BorderLayout.EAST);
			}

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
			showMessage("Set Bot Secret in plugin config, or join a survivor event first for auth.");
			return;
		}

		// Parse tile ID from selector text ("123 - Item Name")
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
					bingoSubmitButton.setText("Screenshot & Submit Tile");
					showMessage("Bingo proof submitted!");
					loadActiveEvents();
				}))
				.exceptionally(ex ->
				{
					SwingUtilities.invokeLater(() ->
					{
						bingoSubmitButton.setEnabled(true);
						bingoSubmitButton.setText("Screenshot & Submit Tile");
						showMessage("Failed: " + ex.getMessage());
					});
					return null;
				});
		});
	}

	// --- Splits ---

	private JPanel buildSplitTab()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel header = new JLabel("Split Tracker");
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(ColorScheme.BRAND_ORANGE);
		header.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(header);
		panel.add(createSpacer(3));

		JLabel desc = new JLabel("<html>Submit a split for approval in Discord.</html>");
		desc.setFont(FontManager.getRunescapeSmallFont());
		desc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		desc.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(desc);
		panel.add(createSpacer(10));

		JLabel amountLabel = new JLabel("Total amount (gp):");
		amountLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		amountLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(amountLabel);

		splitAmountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		splitAmountField.setAlignmentX(LEFT_ALIGNMENT);
		splitAmountField.setToolTipText("e.g. 500000000 or 500m");
		panel.add(splitAmountField);
		panel.add(createSpacer(8));

		JLabel splitWithLabel = new JLabel("Split with (comma-separated IGNs):");
		splitWithLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		splitWithLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(splitWithLabel);

		splitWithField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		splitWithField.setAlignmentX(LEFT_ALIGNMENT);
		splitWithField.setToolTipText("e.g. PlayerB, PlayerC");
		panel.add(splitWithField);
		panel.add(createSpacer(10));

		styleButton(splitSubmitButton);
		splitSubmitButton.setAlignmentX(LEFT_ALIGNMENT);
		splitSubmitButton.addActionListener(e -> onSubmitSplit(false));
		panel.add(splitSubmitButton);
		panel.add(createSpacer(5));

		styleButton(splitScreenshotButton);
		splitScreenshotButton.setAlignmentX(LEFT_ALIGNMENT);
		splitScreenshotButton.addActionListener(e -> onSubmitSplit(true));
		panel.add(splitScreenshotButton);
		panel.add(createSpacer(8));

		splitStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		splitStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		splitStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(splitStatusLabel);

		return panel;
	}

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
			showMessage("Enter a valid GP amount (e.g. 500000000 or 500m).");
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
							submitDropButton.setText("Screenshot & Submit Drop");
							dropTitleField.setText("");
							showMessage("Drop submitted to Hall of Fame!");
							onRefreshDrops();
						}))
						.exceptionally(ex ->
						{
							SwingUtilities.invokeLater(() ->
							{
								submitDropButton.setEnabled(true);
								submitDropButton.setText("Screenshot & Submit Drop");
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
						submitDropButton.setText("Screenshot & Submit Drop");
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
			JLabel empty = new JLabel("No drops yet");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			dropsListPanel.add(empty);
		}

		for (JsonElement el : drops)
		{
			JsonObject drop = el.getAsJsonObject();
			JPanel dropPanel = new JPanel(new BorderLayout());
			dropPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			dropPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
				new EmptyBorder(5, 5, 5, 5)
			));

			String dropTitle = getStr(drop, "title");
			String player = getStr(drop, "player");
			String date = getStr(drop, "date");

			JLabel titleLabel = new JLabel(dropTitle.isEmpty() ? "Unknown" : dropTitle);
			titleLabel.setFont(FontManager.getRunescapeSmallFont());
			titleLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			dropPanel.add(titleLabel, BorderLayout.NORTH);

			String detail = player + (date.length() >= 10 ? " - " + date.substring(0, 10) : "");
			JLabel detailLabel = new JLabel(detail);
			detailLabel.setFont(FontManager.getRunescapeSmallFont());
			detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			dropPanel.add(detailLabel, BorderLayout.SOUTH);

			dropPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
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

	private JPanel createSpacer(int height)
	{
		JPanel spacer = new JPanel();
		spacer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		spacer.setPreferredSize(new Dimension(0, height));
		spacer.setAlignmentX(LEFT_ALIGNMENT);
		return spacer;
	}

	private void styleButton(JButton button)
	{
		button.setFocusPainted(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(5, 10, 5, 10)
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
