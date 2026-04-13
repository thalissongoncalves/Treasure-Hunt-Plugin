# TreasureHunt

**A modern multi-server treasure hunting plugin for Minecraft Paper 1.20+**

Administrators can place custom treasures across the world. When a player finds and clicks a treasure block, a configurable command is executed from console. Each treasure can be collected only **once per player**, even across multiple servers sharing the same MySQL database.

## Features

- Place treasures with custom commands (`%player%` placeholder supported)
- Each treasure is one-time use per player
- Full MySQL support with HikariCP connection pooling
- Cross-server synchronization (claiming works instantly across servers)
- Beautiful paginated GUI for managing treasures
- Search treasures by ID directly in the GUI
- Click cooldown to prevent spam
- Configurable sounds and particles
- Clean and professional code (100% English)

## Commands & Permissions

| Command                        | Description                              | Permission                |
|-------------------------------|------------------------------------------|---------------------------|
| `/treasure`                   | Open management GUI                      | `treasurehunt.admin`      |
| `/treasure create <id> <command>` | Start treasure creation mode            | `treasurehunt.admin`      |
| `/treasure delete <id>`       | Delete a treasure                        | `treasurehunt.admin`      |
| `/treasure completed <id>`    | List players who found this treasure     | `treasurehunt.admin`      |
| `/treasure list`              | List all treasures                       | `treasurehunt.admin`      |
| `/treasure gui`               | Open GUI                                 | `treasurehunt.admin`      |
| `/treasure reload`            | Reload treasures from database           | `treasurehunt.admin`      |

## Installation

1. Download the latest `treasurehunt-1.0.0.jar`
2. Place it in your server's `plugins/` folder
3. Start/restart the server
4. Edit `plugins/TreasureHunt/config.yml` and configure your MySQL database
5. Restart the server again

**Required:** MySQL database (the plugin will create the tables automatically)

## Configuration (config.yml)

Main settings are in the `database` section:

```yaml
database:
  host: localhost
  port: 3306
  database: treasurehunt
  username: root
  password: "your_password"
