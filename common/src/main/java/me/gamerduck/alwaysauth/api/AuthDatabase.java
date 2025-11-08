package me.gamerduck.alwaysauth.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.gamerduck.alwaysauth.Platform;

import java.io.File;
import java.sql.*;

public class AuthDatabase {
    private Connection connection;
    private final Platform platform;
    private final Gson gson;
    private final boolean isRemote;

    public AuthDatabase(File dbFile, Platform platform) {
        this.platform = platform;
        this.gson = new Gson();
        this.isRemote = false;

        try {
            // Use H2 instead of SQLite
            Class.forName("org.h2.Driver");

            // Create a file-based H2 database (auto-creates .mv.db file)
            String url = "jdbc:h2:file:" + dbFile.getAbsolutePath().replace("\\", "/") + ";AUTO_SERVER=TRUE;MODE=MySQL";

            connection = DriverManager.getConnection(url, "sa", "");

            initializeTables();

            platform.sendLogMessage("H2 database initialized at: " + dbFile.getAbsolutePath() + ".mv.db");

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to initialize H2 database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public AuthDatabase(String host, int port, String database, String username, String password, String dbType, Platform platform) {
        this.platform = platform;
        this.gson = new Gson();
        this.isRemote = true;

        try {
            String url;
            String driverClass;

            url = switch (dbType.toLowerCase()) {
                case "mysql" -> {
                    driverClass = "com.mysql.cj.jdbc.Driver";
                    yield "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
                }
                case "mariadb" -> {
                    driverClass = "org.mariadb.jdbc.Driver";
                    yield "jdbc:mariadb://" + host + ":" + port + "/" + database;
                }
                default ->
                        throw new IllegalArgumentException("Unsupported database type: " + dbType + ". Supported types: mysql, mariadb, postgresql");
            };

            Class.forName(driverClass);
            connection = DriverManager.getConnection(url, username, password);

            initializeTables();

            platform.sendLogMessage("Remote database connection established to " + host + ":" + port + "/" + database);

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to initialize remote database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_auth (
                username VARCHAR(16) PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                last_ip VARCHAR(45) NOT NULL,
                last_seen BIGINT NOT NULL,
                profile_data TEXT NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    public void cacheAuthentication(String username, String ip, JsonObject profile) {
        if (username == null || profile == null) return;

        try {
            String uuid = profile.get("id").getAsString();
            String profileJson = gson.toJson(profile);
            long timestamp = System.currentTimeMillis();

            String sql;
            if (isRemote) {
                sql = """
                    INSERT INTO player_auth (username, uuid, last_ip, last_seen, profile_data)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), last_ip = VALUES(last_ip),
                    last_seen = VALUES(last_seen), profile_data = VALUES(profile_data)
                """;
            } else {
                sql = """
                    MERGE INTO player_auth (username, uuid, last_ip, last_seen, profile_data)
                    KEY (username)
                    VALUES (?, ?, ?, ?, ?)
                """;
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, uuid);
                pstmt.setString(3, ip != null ? ip : "unknown");
                pstmt.setLong(4, timestamp);
                pstmt.setString(5, profileJson);
                pstmt.executeUpdate();
            }

            platform.sendLogMessage("Cached authentication for " + username + " (IP: " + ip + ")");

        } catch (SQLException e) {
            platform.sendWarningLogMessage("Failed to cache authentication: " + e.getMessage());
        }
    }

    public String getFallbackAuth(String username, String ip, int maxOfflineHours) {
        if (username == null) return null;

        try {
            String sql = """
                SELECT profile_data, last_ip, last_seen
                FROM player_auth
                WHERE LOWER(username) = LOWER(?)
            """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String cachedIp = rs.getString("last_ip");
                        long lastSeen = rs.getLong("last_seen");
                        String profileData = rs.getString("profile_data");

                        if (ip != null && !ip.equals("unknown") && !ip.equals(cachedIp)) {
                            platform.sendWarningLogMessage("IP mismatch for " + username + " - cached: " + cachedIp + ", current: " + ip);
                            return null;
                        }

                        if (maxOfflineHours > 0) {
                            long hoursSinceLastSeen = (System.currentTimeMillis() - lastSeen) / (1000 * 60 * 60);
                            if (hoursSinceLastSeen > maxOfflineHours) {
                                platform.sendWarningLogMessage("Auth cache expired for " + username + " - last seen " + hoursSinceLastSeen + " hours ago");
                                return null;
                            }
                        }

                        return profileData;
                    }
                }
            }

        } catch (SQLException e) {
            platform.sendWarningLogMessage("Database error during fallback auth: " + e.getMessage());
        }

        return null;
    }

    public CacheStats getStats() {
        CacheStats stats = new CacheStats();

        try {
            String sql = "SELECT COUNT(*) as total FROM player_auth";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    stats.totalPlayers = rs.getInt("total");
                }
            }

            sql = """
                SELECT COUNT(*) as recent
                FROM player_auth
                WHERE last_seen > ?
            """;
            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, oneDayAgo);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        stats.recentPlayers = rs.getInt("recent");
                    }
                }
            }

        } catch (SQLException e) {
            platform.sendWarningLogMessage("Failed to get stats: " + e.getMessage());
        }

        return stats;
    }

    public int cleanOldEntries(int daysOld) {
        try {
            long cutoffTime = System.currentTimeMillis() - ((long) daysOld * 24 * 60 * 60 * 1000);

            String sql = "DELETE FROM player_auth WHERE last_seen < ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, cutoffTime);
                int deleted = pstmt.executeUpdate();
                platform.sendLogMessage("Cleaned " + deleted + " old entries (older than " + daysOld + " days)");
                return deleted;
            }

        } catch (SQLException e) {
            platform.sendWarningLogMessage("Failed to clean old entries: " + e.getMessage());
            return 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                platform.sendLogMessage("Database connection closed");
            }
        } catch (SQLException e) {
            platform.sendWarningLogMessage("Error closing database: " + e.getMessage());
        }
    }

    public static class CacheStats {
        public int totalPlayers = 0;
        public int recentPlayers = 0;

        @Override
        public String toString() {
            return "Total cached players: " + totalPlayers + ", Active (24h): " + recentPlayers;
        }
    }
}
