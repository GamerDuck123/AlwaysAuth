> I have opened a support discord, join now! [https://discord.gg/uHm8EbfCFr](https://discord.gg/uHm8EbfCFr)
# AlwaysAuth

**AlwaysAuth** is a fallback authentication solution for Minecraft servers that ensures your players can log in even if Mojang's authentication servers experience downtime. Unlike traditional offline-mode workarounds, AlwaysAuth keeps your server in **online mode** while providing a seamless failover mechanism to a local authentication system.

---

### How It Works

AlwaysAuth operates by inserting a **local authentication server** between your Minecraft server and Mojang’s official authentication servers:

1. When a player tries to log in, AlwaysAuth first sends the authentication request to Mojang.
2. If Mojang is online and responds, the request passes back to your server normally.
3. If Mojang is offline, AlwaysAuth uses its local fallback system to verify the user.

This approach ensures:

* **Instant failover:** No waiting for periodic checks. Even short outages are handled seamlessly.
* **Online mode stays enabled:** You never have to switch your server to offline mode.
* **Reliable authentication during extended downtime:** Long outages are handled just as smoothly.

---

### AlwaysAuth vs AlwaysOnline

| Feature                   | AlwaysAuth | AlwaysOnline |
| ------------------------- |------------| ------------ |
| Spigot Support            | ✅          | ✅            |
| Paper Support             | ✅          | ✅            |
| BungeeCord Support        | ❌*         | ✅            |
| Velocity Support          | ✅          | ✅            |
| Sponge Support            | ❌**        | ✅            |
| Fabric Support            | ✅          | ❌            |
| NeoForge Support          | ✅          | ❌            |
| Instant Failover          | ✅          | ❌            |
| Online Mode Stays Enabled | ✅          | ❌            |
| Remote Database Support   | ✅          | ✅            |
| Records Metrics           | ❌          | ✅            |
| Multiple Security Modes   | ✅          | ❌            |
| IP-Based Validation       | ✅          | ✅            |

*There is no possible way to set a custom session server on bungeecord, nor ever will according to md_5, see
[here](https://github.com/SpigotMC/BungeeCord/pull/3201). I will be creating a work around that involves many extra steps, however I recommend switching to Velocity or using the Standalone jar

**Sponge support is planned to be added later, I am just unfamiliar with the SpongeAPI

---

### Key Benefits

* **Keep Online Mode Active:** No need to compromise security for downtime.
* **Supports Multiple Platforms:** Works with Spigot, Paper, BungeeCord, Velocity, Fabric, and NeoForge.
* **Flexible Authentication:** Choose from multiple security modes and fallback methods.
* **Reliable:** Handles both short blips and extended Mojang outages effortlessly.

---

### Installation

1. Download the AlwaysAuth plugin for your server platform.
2. Place the plugin in your server’s `plugins` (or equivalent) folder.
3. Restart your server.
4. Configure AlwaysAuth in the config file to enable features like local authentication, remote database support, or IP-based validation.

> Give yourself the permission alwaysauth.admin to access the commands

---

### Configuration

AlwaysAuth provides a flexible configuration file where you can:

* Enable or disable specific fallback methods.
* Set up remote databases for cross-server authentication.
* Choose between different security modes.

```properties
###################################
#                                 #
#    Always Auth Configuration    #
#                                 #
###################################

# Whether or not there should be debug message
# This won't work on the standalone jar
debug=false
# Check for updates and notify staff (and console) on join who have the permission alwaysauth.admin
check-updates=true
# The ip for the session server
# If set anything other than 127.0.0.1 or 0.0.0.0 (allows public access), it will treat as external server
# An external server means only port needs to be set (to match that external server) and it will use that to authenticate.
# Please note as of right now you will not see console logs on the server if you are using an external server
ip-address=127.0.0.1
# Port for the session server
port=8765

###########################
#    Security Settings    #
###########################

# Enable HMAC-SHA256 signature verification for authorized servers
# Currently DISABLED by default due to Minecraft URL handling limitations
# Use firewall rules or localhost restriction for access control instead
# Database encryption works regardless of this setting
authentication-enabled=true
# Secret key for database encryption (auto-generated)
# KEEP THIS SECRET! Used to encrypt IP addresses and profile data in database
# If deleted database will need to also be reset!
# To regenerate, delete this line and restart the server
secret-key=GENERATED ON STARTUP

###########################
#    Fallback Settings    #
###########################

# Enable session fallback when Mojang servers are down
fallback-enabled=true
# Maximum hours a player can stay offline before requiring re-authentication (0 = always require)
max-offline-hours=72
# Days before old session data is cleaned up
cleanup-days=30
# Security level: 'basic' (always verify) or 'medium' (use max-offline-hours)
security-level=basic
# Upstream Session Server URL
# Default is Mojang's official one but this option is here to work with things like minehut's external servers
upstream-server=https://sessionserver.mojang.com

###########################
#    Database Settings    #
###########################

# Database type: h2, mysql, or mariadb
database.type=h2
# Database host (not used for H2)
database.host=localhost
# Database port (not used for H2)
database.port=3306
# Database name
database.name=minecraft
# Database username (not used for H2)
database.username=root
# Database password (not used for H2)
database.password=

```
