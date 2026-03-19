# Mercenary PvM

A RuneLite plugin for clans to track events, bingo boards, loot splits, and drop proofs through their own server.

> **Privacy:** This plugin does not send any data unless the user explicitly configures external endpoints. All communication is initiated by user actions.

## Features

### Event Tracker
- View all active clan events (survivor, bingo, region conquest, monopoly)
- See current tasks, deadlines, and countdown timers
- Join events and track your status and points
- Submit proof screenshots directly from the game
- Use lifelines when available

### Bingo Boards
- Browse active bingo boards with tile names and point values
- Auto-detects your team by matching your in-game name
- See which tiles are completed vs incomplete
- Screenshot and submit tile proofs in one click

### Split Tracker
- Submit loot splits for Discord approval
- Supports shorthand amounts (500m, 1.5b, 200k)
- Optionally attach a screenshot as proof
- Your IGN is detected automatically from the game

### Drop Uploads (Hall of Fame)
- Manually submit drops with a screenshot
- Browse recent clan drops
- Auto-screenshot on valuable drops and collection log entries
- Optional auto-submit to your clan's Hall of Fame

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

## For Clan Server Admins

This plugin expects the following API endpoints on your server:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/active-events` | GET | List active events with type, status, tiles |
| `/surv/join` | POST | Join a survivor event |
| `/surv/state` | GET | Get event state and current task |
| `/surv/countdown` | GET | Get deadline countdown |
| `/surv/submit` | POST | Submit event proof |
| `/surv/lifeline` | POST | Use a lifeline |
| `/surv/chat` | GET/POST | Event chat |
| `/board/{id}/proof` | POST | Submit bingo tile proof (multipart) |
| `/upload` | POST | Upload screenshot (multipart) |
| `/hall-of-fame` | GET/POST | Drop tracking |

Auth is via `x-session-token` or `x-bot-secret` headers. Split tracker uses `Authorization: Bearer <key>`.
