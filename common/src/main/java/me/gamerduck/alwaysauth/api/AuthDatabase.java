package me.gamerduck.alwaysauth.api;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

public class AuthDatabase {
    private Connection connection;
    private final Logger logger;
    private final Gson gson;

    public AuthDatabase(File dbFile, Logger logger) {
        this.logger = logger;
        this.gson = new Gson();

        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create database connection
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Initialize tables
            initializeTables();

            logger.info("Database initialized at: " + dbFile.getAbsolutePath());

        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_auth (
                username TEXT PRIMARY KEY COLLATE NOCASE,
                uuid TEXT NOT NULL,
                last_ip TEXT NOT NULL,
                last_seen INTEGER NOT NULL,
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

            String sql = """
                INSERT OR REPLACE INTO player_auth (username, uuid, last_ip, last_seen, profile_data)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, uuid);
                pstmt.setString(3, ip != null ? ip : "unknown");
                pstmt.setLong(4, timestamp);
                pstmt.setString(5, profileJson);
                pstmt.executeUpdate();
            }

            logger.info("Cached authentication for " + username + " (IP: " + ip + ")");

        } catch (SQLException e) {
            logger.warning("Failed to cache authentication: " + e.getMessage());
        }
    }

    public String getFallbackAuth(String username, String ip, int maxOfflineHours) {
        if (username == null) return null;

        try {
            String sql = """
                SELECT profile_data, last_ip, last_seen
                FROM player_auth
                WHERE username = ? COLLATE NOCASE
            """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String cachedIp = rs.getString("last_ip");
                        long lastSeen = rs.getLong("last_seen");
                        String profileData = rs.getString("profile_data");

                        // Check IP match (basic security)
                        if (ip != null && !ip.equals("unknown") && !ip.equals(cachedIp)) {
                            logger.warning("IP mismatch for " + username + " - cached: " + cachedIp + ", current: " + ip);
                            return null;
                        }

                        // Check time limit (medium security)
                        if (maxOfflineHours > 0) {
                            long hoursSinceLastSeen = (System.currentTimeMillis() - lastSeen) / (1000 * 60 * 60);
                            if (hoursSinceLastSeen > maxOfflineHours) {
                                logger.warning("Auth cache expired for " + username + " - last seen " + hoursSinceLastSeen + " hours ago");
                                return null;
                            }
                        }

                        return profileData;
                    }
                }
            }

        } catch (SQLException e) {
            logger.warning("Database error during fallback auth: " + e.getMessage());
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
            logger.warning("Failed to get stats: " + e.getMessage());
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
                logger.info("Cleaned " + deleted + " old entries (older than " + daysOld + " days)");
                return deleted;
            }

        } catch (SQLException e) {
            logger.warning("Failed to clean old entries: " + e.getMessage());
            return 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.warning("Error closing database: " + e.getMessage());
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