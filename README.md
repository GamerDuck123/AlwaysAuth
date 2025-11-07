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

---

### Configuration

AlwaysAuth provides a flexible configuration file where you can:

* Enable or disable specific fallback methods.
* Set up remote databases for cross-server authentication.
* Choose between different security modes.
---
