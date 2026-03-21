# Mercenary

A RuneLite plugin for clans to track events, bingo boards, loot splits, and drop proofs through their own server.

This plugin is not affiliated with or endorsed by Jagex.

> **Privacy:**
> This plugin does not send any data unless the user explicitly configures external endpoints in the settings.
> No data is collected, stored, or transmitted by default.
> All communication is triggered by explicit user actions (e.g., clicking submit) or by optional features the user must manually enable (e.g., auto-submit drops, which is off by default).
> All authentication tokens are stored locally within the RuneLite client and are never shared except with user-configured endpoints.

## Features

### Event Tracking & Bingo
- View all active clan events (survivor, bingo, region conquest, monopoly)
- See current tasks, deadlines, and countdown timers
- Join events and track your status, points, and lifelines
- Submit proof screenshots directly from the game
- Browse bingo boards with tile names and point values
- Auto-detects your team by matching your in-game name
- Screenshot and submit tile proofs in one click

### Split Tracking
- Submit loot splits for Discord approval
- Supports shorthand amounts (500m, 1.5b, 200k)
- Optionally attach a screenshot as proof
- Your IGN is detected automatically from the game

### Drop Tracking
- Manually submit drops with a screenshot to your clan's Hall of Fame
- Browse recent clan drops
- Automatically captures screenshots on valuable drops and collection log entries (local only). Optional auto-submit can be enabled in settings.

### Clan Auto-Detection
- Automatically detects when you're in your configured clan
- Loads all data when clan membership is confirmed
- Manual reload button if you change settings while logged in

## Setup

1. Install the plugin from the RuneLite Plugin Hub
2. Open the plugin settings and configure:

| Setting | Description | Example |
|---------|-------------|---------|
| **Clan Name** | Your in-game clan name (exact match) | `MercenaryPVM` |
| **API Base URL** | Your clan server's API endpoint | `https://yourclan.com/api` |
| **Bot Secret** | Auth token for your clan server | *(from your server admin)* |
| **Split Bot URL** | Split tracker bot API endpoint | `https://yourbot.com/api` |
| **Split API Key** | Auth token for the split bot | *(from your server admin)* |

3. Log in to the game — the plugin auto-detects your clan and loads everything

---

## Server Setup Guide

This section is for **clan server admins** who want to build a compatible backend. The plugin communicates with standard REST endpoints. You can implement this in any language (Node.js, Python, Java, etc.). HTTPS is recommended but not required.

### Authentication

The plugin supports two auth methods, sent as HTTP headers:

| Header | Use Case |
|--------|----------|
| `x-session-token` | Player session token, obtained from `/surv/join` |
| `x-bot-secret` | Shared secret for bot/plugin access (set in your server's `.env`) |

Read-only endpoints (GET) generally don't require auth. Write endpoints (POST) require one of the above headers.

For the split tracker, auth uses a standard `Authorization: Bearer <key>` header.

### Endpoint Reference

All endpoints are relative to your API base URL (e.g. `https://yourclan.com/api`).

---

#### `GET /active-events`

Returns all currently active events. No auth required.

**Response:**
```json
{
  "events": [
    {
      "id": "abc123",
      "name": "Weekly Survivor",
      "type": "survivor",
      "status": "active",
      "current_day": 3,
      "players": 12,
      "current_task": {
        "title": "Kill Corporeal Beast",
        "description": "Get 10 Corp KC this round",
        "deadline_at": "2026-03-21T18:00:00Z",
        "point_reward": 5
      }
    },
    {
      "id": "def456",
      "name": "Raid Drops Bingo",
      "type": "bingo",
      "status": "active",
      "rows": 4,
      "cols": 4,
      "tiles": [
        { "id": 135, "position": 0, "item": "Twisted Bow", "points": 100, "completed": false },
        { "id": 136, "position": 1, "item": "Elder Maul", "points": 3, "completed": true }
      ],
      "teams": [
        { "id": "team1", "name": "Red Team", "color": "#ef4444" }
      ],
      "members": [
        { "username": "PlayerRSN", "team_id": "team1" }
      ]
    }
  ]
}
```

**Supported event types:** `survivor`, `bingo`, `region_conquest`, `monopoly`

Bingo events should include `tiles`, `teams`, and `members` arrays so the plugin can auto-detect the player's team and show tile completion status.

---

#### `POST /surv/join`

Join a survivor event. Returns a session token for future authenticated requests.

**Headers:** None required (creates a new session)

**Request body:**
```json
{
  "eventId": "abc123",
  "username": "PlayerRSN"
}
```

**Response:**
```json
{
  "token": "uuid-session-token",
  "eventId": "abc123",
  "participantId": 5,
  "status": "active"
}
```

---

#### `GET /surv/state?eventId={id}`

Get full event state including current task, participant status, and submission info.

**Headers:** `x-session-token` (optional, needed to see personal data like `myParticipant`)

**Response:**
```json
{
  "event": {
    "id": "abc123",
    "name": "Weekly Survivor",
    "status": "active",
    "current_day": 3
  },
  "currentTask": {
    "title": "Kill Corporeal Beast",
    "description": "Get 10 Corp KC",
    "deadline_at": "2026-03-21T18:00:00Z",
    "point_reward": 5
  },
  "myParticipant": {
    "id": 5,
    "status": "active",
    "lifelines": 2,
    "total_points": 15
  },
  "mySubmission": {
    "id": 10,
    "status": "pending",
    "proof_url": "https://yourclan.com/uploads/proof.png"
  }
}
```

`myParticipant` and `mySubmission` can be `null` if the player hasn't joined or hasn't submitted.

---

#### `GET /surv/countdown?eventId={id}`

Get the time remaining for the current task deadline.

**Response:**
```json
{
  "deadline": "2026-03-21T18:00:00Z",
  "remaining": 43200000,
  "day": 3
}
```

`remaining` is in milliseconds.

---

#### `POST /surv/submit`

Submit proof for the current survivor task.

**Headers:** `x-session-token` (required)

**Request body:**
```json
{
  "eventId": "abc123",
  "proofUrl": "https://yourclan.com/uploads/screenshot.png",
  "notes": "Got the drop!"
}
```

**Response:**
```json
{
  "id": 15,
  "grace_period": false
}
```

---

#### `POST /surv/lifeline`

Use a lifeline to survive elimination.

**Headers:** `x-session-token` (required)

**Request body:**
```json
{
  "eventId": "abc123"
}
```

---

#### `GET /surv/chat?eventId={id}` and `POST /surv/chat`

Read and send event chat messages.

**GET response:**
```json
[
  { "id": 1, "user_id": "PlayerRSN", "message": "Hello!", "sent_at": "2026-03-21T12:00:00Z" }
]
```

**POST headers:** `x-session-token` (required)

**POST body:**
```json
{
  "eventId": "abc123",
  "message": "Hello!"
}
```

---

#### `POST /board/{boardId}/proof`

Submit proof for a bingo tile. Uses multipart form data, not JSON.

**Headers:** `x-session-token` or `x-bot-secret` (required)

**Content-Type:** `multipart/form-data`

**Form fields:**

| Field | Type | Description |
|-------|------|-------------|
| `image` | File | Screenshot (PNG/JPEG, max 10MB) |
| `tile_id` | Number | The tile's `id` from the active-events response |
| `submittedBy` | String | Player RSN (used when authenticating via `x-bot-secret`) |

**Response:**
```json
{
  "id": 1,
  "image_url": "/uploads/proof-abc123.png"
}
```

---

#### `POST /upload`

Standalone image upload endpoint. Returns a full URL to the uploaded image.

**Headers:** `x-session-token` or `x-bot-secret` (required)

**Content-Type:** `multipart/form-data`

**Form fields:**

| Field | Type | Description |
|-------|------|-------------|
| `image` | File | Screenshot (PNG/JPEG, max 10MB) |

**Response:**
```json
{
  "success": true,
  "full_url": "https://yourclan.com/uploads/proof-abc123.png"
}
```

The `full_url` is used as `proofUrl` in subsequent submit calls.

---

#### `GET /hall-of-fame?limit={n}`

List recent drops from the Hall of Fame.

**Response:**
```json
[
  {
    "id": 1,
    "title": "Twisted Bow",
    "player": "PlayerRSN",
    "image": "/uploads/screenshot.png",
    "category": "drop",
    "date": "2026-03-21T10:00:00Z"
  }
]
```

#### `POST /hall-of-fame`

Add a drop to the Hall of Fame.

**Headers:** `x-bot-secret` (required)

**Request body:**
```json
{
  "title": "Dragon Warhammer",
  "player": "PlayerRSN",
  "image": "https://yourclan.com/uploads/screenshot.png",
  "category": "drop"
}
```

---

### Split Tracker API

The split tracker runs as a separate service (e.g. a Discord bot with an HTTP API). It uses a different base URL and auth method.

#### `POST /splitadd`

Submit a loot split for approval. The bot should post an approval message (e.g. to Discord) and process it on approval.

**Headers:** `Authorization: Bearer <api-key>` (if configured)

**Request body:**
```json
{
  "ign": "PlayerRSN",
  "amount": 500000000,
  "splitWith": ["PlayerB", "PlayerC"],
  "screenshot": "<base64 encoded PNG, optional>"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Split request submitted for approval",
  "player": "PlayerRSN",
  "amount": 500000000
}
```

---

### Quick Start Example (Node.js / Express)

A minimal server skeleton to get started:

```js
const express = require('express');
const multer = require('multer');
const app = express();
const upload = multer({ dest: 'uploads/' });

app.use(express.json());

// Session store (replace with database in production)
const sessions = new Map();

// Auth middleware
function auth(req, res, next) {
  const secret = req.headers['x-bot-secret'];
  if (secret && secret === process.env.BOT_SECRET) {
    req.username = req.body?.submittedBy || 'Bot';
    return next();
  }

  const token = req.headers['x-session-token'];
  if (token && sessions.has(token)) {
    req.username = sessions.get(token);
    return next();
  }

  res.status(401).json({ error: 'Unauthorized' });
}

// Active events
app.get('/api/active-events', (req, res) => {
  res.json({ events: [/* your events here */] });
});

// Image upload
app.post('/api/upload', auth, upload.single('image'), (req, res) => {
  const url = `https://yourclan.com/uploads/${req.file.filename}`;
  res.json({ success: true, full_url: url });
});

// Bingo proof
app.post('/api/board/:boardId/proof', auth, upload.single('image'), (req, res) => {
  const { tile_id, submittedBy } = req.body;
  // Save proof to database...
  res.json({ id: 1, image_url: `/uploads/${req.file.filename}` });
});

// Hall of fame
app.get('/api/hall-of-fame', (req, res) => {
  res.json([/* drops from database */]);
});

app.post('/api/hall-of-fame', auth, (req, res) => {
  const { title, player, image, category } = req.body;
  // Save to database...
  res.json({ id: 1 });
});

app.listen(8080, () => console.log('Clan API running on :8080'));
```

Set `BOT_SECRET` in your `.env` file and share it with clan members for their plugin config.
