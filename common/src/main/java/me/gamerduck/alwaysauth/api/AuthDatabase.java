package me.gamerduck.alwaysauth.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.gamerduck.alwaysauth.Platform;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthDatabase {
    private Connection connection;
    private final Platform platform;
    private final Gson gson;
    private final boolean isRemote;
    private final EncryptionHelper encryptionHelper;

    public AuthDatabase(File dbFile, Platform platform) {
        this.platform = platform;
        this.gson = new Gson();
        this.isRemote = false;
        this.encryptionHelper = new EncryptionHelper(platform.config().getSecretKey());

        try {
            Class.forName("org.h2.Driver");

            String url = "jdbc:h2:file:" + dbFile.getAbsolutePath().replace("\\", "/") + ";AUTO_SERVER=TRUE;MODE=MySQL";

            connection = DriverManager.getConnection(url, "sa", "");

            initializeTables();

            if (platform.config().getDebug()) {
                platform.sendLogMessage("H2 database initialized at: " + dbFile.getAbsolutePath() + ".mv.db");
                platform.sendLogMessage("Database encryption: ENABLED");
            }
        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to initialize H2 database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public AuthDatabase(String host, int port, String database, String username, String password, String dbType, Platform platform) {
        this.platform = platform;
        this.gson = new Gson();
        this.isRemote = true;
        this.encryptionHelper = new EncryptionHelper(platform.config().getSecretKey());

        try {
            String url;
            String driverClass;

            url = switch (dbType.toLowerCase()) {
                case "mysql" -> {
                    driverClass = "com.mysql.cj.jdbc.Driver";
                    yield "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
                }
                case "mariadb" -> {
                    driverClass = "org.mariadb.jdbc.Driver";
                    yield "jdbc:mariadb://" + host + ":" + port + "/" + database;
                }
                default ->
                        throw new IllegalArgumentException("Unsupported database type: " + dbType + ". Supported types: mysql, mariadb");
            };

            Class.forName(driverClass);
            connection = DriverManager.getConnection(url, username, password);

            initializeTables();

            if (platform.config().getDebug()) {
                platform.sendLogMessage("Remote database connection established to " + host + ":" + port + "/" + database);
                platform.sendLogMessage("Database encryption: ENABLED");
            }

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
                last_ip TEXT NOT NULL,
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

            String encryptedIp = encryptionHelper.encrypt(ip != null ? ip : "unknown");

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
                pstmt.setString(3, encryptedIp);
                pstmt.setLong(4, timestamp);
                pstmt.setString(5, profileJson);
                pstmt.executeUpdate();
            }

            if (platform.isDebug()) {
                platform.sendLogMessage("Cached authentication for " + username);
            }
        } catch (Exception e) {
            platform.sendWarningLogMessage("Failed to cache authentication: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getFallbackAuth(String username, String ip, String securityLevel, int maxOfflineHours) {
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
                        String encryptedIp = rs.getString("last_ip");
                        long lastSeen = rs.getLong("last_seen");
                        String profileData = rs.getString("profile_data");

                        String cachedIp = encryptionHelper.decrypt(encryptedIp);

                        if (ip != null && !ip.equals("unknown") && !ip.equals(cachedIp)) {
                            platform.sendWarningLogMessage("IP mismatch for " + username + " - cached: [ENCRYPTED], current: [ENCRYPTED]");
                            return null;
                        }

                        if (securityLevel.equals("medium") && maxOfflineHours > 0) {
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

        } catch (Exception e) {
            platform.sendWarningLogMessage("Database error during fallback auth: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public CacheStats getStats() {
        int tempTotalPlayers = 0;
        int tempRecentPlayers = 0;
        try {
            String sql = "SELECT COUNT(*) as total FROM player_auth";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    tempTotalPlayers = rs.getInt("total");
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
                        tempRecentPlayers = rs.getInt("recent");
                    }
                }
            }

        } catch (SQLException e) {
            platform.sendWarningLogMessage("Failed to get stats: " + e.getMessage());
        }

        return new CacheStats(tempTotalPlayers, tempRecentPlayers);
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

    private static class EncryptionHelper {
        private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
        private static final int GCM_IV_LENGTH = 12;
        private static final int GCM_TAG_LENGTH = 128;

        private final SecretKeySpec keySpec;
        private final SecureRandom secureRandom;

        public EncryptionHelper(String secretKey) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
                this.keySpec = new SecretKeySpec(keyBytes, "AES");
                this.secureRandom = new SecureRandom();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize encryption: " + e.getMessage(), e);
            }
        }

        public String encrypt(String plaintext) {
            if (plaintext == null || plaintext.isEmpty()) {
                return "";
            }

            try {
                byte[] iv = new byte[GCM_IV_LENGTH];
                secureRandom.nextBytes(iv);

                Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

                byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

                byte[] combined = new byte[iv.length + ciphertext.length];
                System.arraycopy(iv, 0, combined, 0, iv.length);
                System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

                return Base64.getEncoder().encodeToString(combined);

            } catch (Exception e) {
                throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
            }
        }

        public String decrypt(String encrypted) {
            if (encrypted == null || encrypted.isEmpty()) {
                return "";
            }

            try {
                byte[] combined = Base64.getDecoder().decode(encrypted);

                byte[] iv = new byte[GCM_IV_LENGTH];
                System.arraycopy(combined, 0, iv, 0, iv.length);

                byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
                System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

                Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

                byte[] plaintext = cipher.doFinal(ciphertext);
                return new String(plaintext, StandardCharsets.UTF_8);

            } catch (Exception e) {
                throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
            }
        }
    }

    public record CacheStats(int totalPlayers, int recentPlayers) {
        @Override
        public String toString() {
            return "Total cached players: " + totalPlayers + ", Active (24h): " + recentPlayers;
        }
    }
}
