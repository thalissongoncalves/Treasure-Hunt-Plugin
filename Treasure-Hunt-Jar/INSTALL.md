# TreasureHunt - Installation Guide

## 1. Plugin Installation

1. Download the file `treasurehunt-1.0.0.jar`
2. Place it in your server's `plugins/` folder
3. Start or restart your **Paper 1.20+** server

## 2. Database Configuration

After the first startup, the plugin will automatically generate the configuration file:

`plugins/TreasureHunt/config.yml`

Open this file and configure your MySQL database connection:

```yaml
database:
  host: localhost
  port: 3306
  database: treasurehunt
  username: root
  password: "YOUR_PASSWORD_HERE"

  # Connection pool settings (optional - can be left as default)
  max-pool-size: 10
  min-pool-size: 5
  max-lifetime: 1800000
  idle-timeout: 600000
```

> **Important:** Replace `"YOUR_PASSWORD_HERE"` with your actual MySQL password.

The plugin will create the necessary database tables (`treasures` and `treasure_completed`) automatically on first run.

## 3. Permissions

Give your administrators the required permission:

```bash
/lp user <player> permission set treasurehunt.admin true
```

or simply give them **OP**.

## 4. Main Commands

| Command                              | Description                                      |
|--------------------------------------|--------------------------------------------------|
| `/treasure`                          | Open the management GUI                          |
| `/treasure create <id> <command>`    | Start treasure creation mode                     |
| `/treasure delete <id>`              | Delete a treasure                                |
| `/treasure completed <id>`           | List players who found this treasure             |
| `/treasure list`                     | List all registered treasures                    |
| `/treasure gui`                      | Open the graphical interface                     |
| `/treasure reload`                   | Reload treasures from the database               |

## Support

If you need help, feel free to open an Issue on the GitHub repository:

https://github.com/thalissongoncalves/Treasure-Hunt-Plugin