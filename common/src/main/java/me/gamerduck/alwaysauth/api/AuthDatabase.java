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

/**
 * Authentication database with AES-256-GCM encryption for sensitive data.
 * <p>
 * This class manages a database of player authentication records with built-in encryption
 * for sensitive information. IP addresses are always encrypted, and profile data can be
 * optionally encrypted based on configuration settings.
 * </p>
 * <p>
 * Supports both local H2 databases and remote MySQL/MariaDB databases. All encryption
 * uses AES-256 in GCM (Galois/Counter Mode) for authenticated encryption.
 * </p>
 */
public class AuthDatabase {
    private Connection connection;
    private final Platform platform;
    private final Gson gson;
    private final boolean isRemote;
    private final EncryptionHelper encryptionHelper;

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Constructs a local H2 database instance.
     * <p>
     * Creates or connects to a file-based H2 database with MySQL compatibility mode.
     * Automatically initializes tables and sets up AES-256-GCM encryption.
     * </p>
     *
     * @param dbFile the file path for the H2 database (without .mv.db extension)
     * @param platform the platform implementation for logging and configuration
     */
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
                platform.sendLogMessage("Database encryption: ENABLED (AES-256-GCM)");
            }
        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to initialize H2 database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Constructs a remote database instance.
     * <p>
     * Connects to a remote MySQL or MariaDB database. Automatically initializes
     * tables and sets up AES-256-GCM encryption for sensitive data.
     * </p>
     *
     * @param host the database server hostname or IP address
     * @param port the database server port
     * @param database the database/schema name
     * @param username the database username
     * @param password the database password
     * @param dbType the database type ("mysql" or "mariadb")
     * @param platform the platform implementation for logging and configuration
     * @throws IllegalArgumentException if dbType is not "mysql" or "mariadb"
     */
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
                    yield "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
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
                platform.sendLogMessage("Database encryption: ENABLED (AES-256-GCM)");
            }

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to initialize remote database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes the database schema.
     * <p>
     * Creates the player_auth table if it doesn't exist. The table stores:
     * username, UUID, encrypted IP address, last seen timestamp, and encrypted profile data.
     * </p>
     *
     * @throws SQLException if table creation fails
     */
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

    /**
     * Caches player authentication data in the database.
     * <p>
     * Stores or updates a player's authentication record with encrypted sensitive data.
     * IP addresses are always encrypted. Profile data encryption depends on configuration.
     * Uses UPSERT logic (INSERT or UPDATE if exists) to maintain one record per player.
     * </p>
     *
     * @param username the player's username (case-insensitive)
     * @param ip the player's IP address (will be encrypted)
     * @param profile the player's profile JSON from Mojang (contains UUID, textures, etc.)
     */
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

    /**
     * Retrieves cached authentication data for fallback when Mojang is unavailable.
     * <p>
     * Validates the cached entry against:
     * <ul>
     *     <li>IP address matching (if provided)</li>
     *     <li>Time-based expiry (if maxOfflineHours &gt; 0)</li>
     * </ul>
     * Returns null if validation fails or no cache exists.
     * Automatically decrypts sensitive data before returning.
     * </p>
     *
     * @param username the player's username to look up
     * @param ip the player's current IP address for validation (can be null)
     * @param maxOfflineHours maximum hours since last seen (0 = no limit)
     * @return the decrypted profile JSON if valid, null otherwise
     */
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
                        String encryptedIp = rs.getString("last_ip");
                        long lastSeen = rs.getLong("last_seen");
                        String profileData = rs.getString("profile_data");

                        String cachedIp = encryptionHelper.decrypt(encryptedIp);

                        if (ip != null && !ip.equals("unknown") && !ip.equals(cachedIp)) {
                            platform.sendWarningLogMessage("IP mismatch for " + username + " - cached: [ENCRYPTED], current: [ENCRYPTED]");
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

        } catch (Exception e) {
            platform.sendWarningLogMessage("Database error during fallback auth: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves cache statistics.
     * <p>
     * Calculates:
     * <ul>
     *     <li>Total number of cached players</li>
     *     <li>Number of players active in the last 24 hours</li>
     * </ul>
     * </p>
     *
     * @return a CacheStats object containing the statistics
     */
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

    /**
     * Removes old cached authentication entries.
     * <p>
     * Deletes all player records that haven't been seen in the specified number of days.
     * Useful for maintaining database size and removing stale data.
     * </p>
     *
     * @param daysOld the age threshold in days (entries older than this are deleted)
     * @return the number of entries deleted
     */
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

    /**
     * Closes the database connection.
     * <p>
     * Should be called during server shutdown to properly release database resources.
     * Does nothing if the connection is already closed.
     * </p>
     */
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

    /**
     * Helper class for AES-256-GCM encryption and decryption.
     * <p>
     * Provides authenticated encryption using AES-256 in Galois/Counter Mode.
     * The secret key is hashed with SHA-256 to derive a 256-bit encryption key.
     * Each encryption uses a unique random IV for security.
     * </p>
     */
    private static class EncryptionHelper {
        private final SecretKeySpec keySpec;
        private final SecureRandom secureRandom;

        /**
         * Constructs an EncryptionHelper with a secret key.
         * <p>
         * Derives a 256-bit AES key from the provided secret using SHA-256 hashing.
         * </p>
         *
         * @param secretKey the secret key string to derive the encryption key from
         * @throws RuntimeException if encryption initialization fails
         */
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

        /**
         * Encrypts data using AES-256-GCM.
         * <p>
         * Generates a random 96-bit IV for each encryption.
         * Returns Base64-encoded string in format: IV + ciphertext + authentication tag.
         * </p>
         *
         * @param plaintext the data to encrypt
         * @return Base64-encoded encrypted data, or empty string if plaintext is null/empty
         * @throws RuntimeException if encryption fails
         */
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

        /**
         * Decrypts data using AES-256-GCM.
         * <p>
         * Expects Base64-encoded input in format: IV + ciphertext + authentication tag.
         * Verifies data integrity using the GCM authentication tag.
         * </p>
         *
         * @param encrypted the Base64-encoded encrypted data
         * @return the decrypted plaintext, or empty string if encrypted is null/empty
         * @throws RuntimeException if decryption fails or data is tampered with
         */
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

    /**
     * Container for authentication cache statistics.
     * <p>
     * Holds metrics about cached player authentication data including
     * total players and recent activity.
     * </p>
     */
    public static class CacheStats {
        /** Total number of players in the cache */
        public int totalPlayers = 0;

        /** Number of players active in the last 24 hours */
        public int recentPlayers = 0;

        @Override
        public String toString() {
            return "Total cached players: " + totalPlayers + ", Active (24h): " + recentPlayers;
        }
    }
}