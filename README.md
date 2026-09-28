# ThanlyTPA

A clean, fully configurable **TPA plugin for Paper 1.21.x** with inventory menus, clickable chat buttons and a teleport delay.

## Features
- `/tpa` and `/tpahere` with **player-head menus** (run them without a name).
- **Clickable `[ACCEPT]` / `[DENY]` buttons** in chat, with hover text.
- **Requests menu** (`/tpamenu`): left-click to accept, right-click to deny.
- **Teleport delay** (default 5 s) with action-bar countdown and sounds, cancelled if the player **moves** or **takes damage**.
- Requests **expire** (default 60 s) and there is a **cooldown** between requests (default 10 s).
- **Bypass permissions** for staff (no delay / no cooldown).
- **Multi-language**: English and Spanish included, all messages editable with [MiniMessage](https://docs.advntr.dev/minimessage/format.html).
- `/tpareload` to reload the config without restarting.

## Commands
| Command | Description |
|---|---|
| `/tpa [player]` | Ask to teleport to a player. No name opens the players menu. |
| `/tpahere [player]` | Ask a player to teleport to you. No name opens the players menu. |
| `/tpaccept [player]` | Accept a request. With several pending and no name, opens the menu. |
| `/tpadeny [player]` | Deny a request (same behaviour as above). |
| `/tpamenu` (`/tpagui`) | Open your pending requests menu. |
| `/tpareload` | Reload config and language files. |

## Permissions
| Permission | Default | Description |
|---|---|---|
| `thanlytpa.use` | everyone | Use the TPA commands |
| `thanlytpa.bypass.delay` | op | Teleport instantly |
| `thanlytpa.bypass.cooldown` | op | No cooldown between requests |
| `thanlytpa.admin` | op | `/tpareload` |

## Configuration
`plugins/ThanlyTPA/config.yml`
```yaml
language: en          # en, es, or your own file in lang/
teleport-delay: 5
cancel-on-move: true
cancel-on-damage: true
request-timeout: 60
request-cooldown: 10
sounds: true
```
Messages live in `plugins/ThanlyTPA/lang/<language>.yml`.

## Requirements
- Paper 1.21.1 or newer 1.21.x
- Java 21+

## Building
```
./gradlew build        # Windows PowerShell: .\gradlew build
```
The plugin jar is created in `build/libs/`.

## Author
Made by **Thanly** — available for custom plugin commissions.
