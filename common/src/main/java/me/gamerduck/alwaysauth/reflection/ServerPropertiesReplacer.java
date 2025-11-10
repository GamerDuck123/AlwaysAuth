package me.gamerduck.alwaysauth.reflection;


import me.gamerduck.alwaysauth.Platform;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Forces the "prevent-proxy-connections" server property using reflection and Unsafe.
 * <p>
 * This class uses Java's Unsafe API to modify a final field in Minecraft's server properties
 * at runtime. This is necessary because the field is final and cannot be changed through
 * normal reflection, but we need to ensure it's set to true for security.
 * </p>
 *
 * <h2>Why This Class Exists</h2>
 * <p>
 * The "prevent-proxy-connections" setting in server.properties protects against proxy/VPN
 * detection bypasses. AlwaysAuth needs this enabled to maintain security while providing
 * authentication fallback. Since this is a final field loaded at server startup, we must
 * use Unsafe to modify it after the fact.
 * </p>
 *
 * <h2>How It Works</h2>
 * <p>
 * The reflection chain follows this path:
 * <pre>
 * Bukkit.getServer()
 *   → CraftServer.getServer()
 *     → DedicatedServer.settings (field)
 *       → DedicatedServerProperties.preventProxyConnections (final field)
 *         → Unsafe.putBoolean() to bypass final modifier
 * </pre>
 * </p>
 *
 * <h2>Updating for New Minecraft Versions</h2>
 * <p>
 * When Minecraft updates and this breaks, follow these steps to find new field/method names:
 * </p>
 *
 * <h3>Step 1: Get the Server JAR</h3>
 * <ol>
 *   <li>Download the Paper/Spigot server JAR for your target version</li>
 *   <li>Or extract it from your server's directory</li>
 * </ol>
 *
 * <h3>Step 2: Decompile the JAR</h3>
 * <p>Use a Java decompiler to inspect the code:</p>
 * <ul>
 *   <li><b>JD-GUI</b>: http://java-decompiler.github.io/ (easiest for beginners)</li>
 *   <li><b>IntelliJ IDEA</b>: File → Project Structure → Libraries → Add JAR (built-in decompiler)</li>
 *   <li><b>Fernflower</b>: Command-line decompiler (most accurate)</li>
 * </ul>
 *
 * <h3>Step 3: Find DedicatedServer Class</h3>
 * <ol>
 *   <li>Search for "net.minecraft.server.dedicated.DedicatedServer"</li>
 *   <li>Look for a field that holds server properties (usually named "settings" or "properties")</li>
 *   <li>Note: Paper uses Mojang mappings, so names are usually readable</li>
 *   <li>Update {@link #SETTINGS_FIELD} if the name changed</li>
 * </ol>
 *
 * <h3>Step 4: Find DedicatedServerProperties Class</h3>
 * <ol>
 *   <li>Find "net.minecraft.server.dedicated.DedicatedServerProperties"</li>
 *   <li>This class holds all server.properties values as fields</li>
 *   <li>Look for the "preventProxyConnections" field (or similar name)</li>
 *   <li>Note: This field is usually final, which is why we need Unsafe</li>
 *   <li>Update {@link #PREVENT_PROXY_CONNECTIONS_FIELD} if the name changed</li>
 * </ol>
 *
 * <h3>Step 5: Verify the Field Type</h3>
 * <ol>
 *   <li>Confirm preventProxyConnections is a boolean field</li>
 *   <li>If it changed to a different type, update the Unsafe.putBoolean() call accordingly</li>
 *   <li>Common types: boolean, int, String</li>
 * </ol>
 *
 * <h3>Step 6: Test Your Changes</h3>
 * <ol>
 *   <li>Enable debug mode in the config (debug=true)</li>
 *   <li>Start your server and look for "Successfully set preventProxyConnections" message</li>
 *   <li>If you see ClassNotFoundException or NoSuchFieldException, double-check your mappings</li>
 *   <li>Verify in server logs that prevent-proxy-connections is active</li>
 * </ol>
 *
 * <h3>Alternative: Check Server Properties</h3>
 * <p>
 * You can also examine the server.properties file handling:
 * </p>
 * <ul>
 *   <li>Look at how DedicatedServerProperties reads from server.properties</li>
 *   <li>Each property typically has a corresponding final field</li>
 *   <li>The field name usually matches the property name in camelCase</li>
 *   <li>Example: "prevent-proxy-connections" → "preventProxyConnections"</li>
 * </ul>
 *
 * <h2>Security Warning</h2>
 * <p>
 * This class uses sun.misc.Unsafe, which is:
 * <ul>
 *   <li>Not part of the official Java API</li>
 *   <li>May be removed in future Java versions</li>
 *   <li>Can cause JVM crashes if used incorrectly</li>
 *   <li>Bypasses Java's safety mechanisms</li>
 * </ul>
 * Use with caution and only for this specific, necessary purpose.
 * </p>
 *
 * @see AuthenticationURLReplacer The reflection-based URL replacer (safer alternative when possible)
 */
public class ServerPropertiesReplacer {

    /**
     * CraftBukkit's server implementation - bridges Bukkit API to NMS.
     */
    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";

    /**
     * Bukkit API entry point for accessing the server instance.
     */
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";

    /**
     * Dedicated server class containing server properties.
     * Paper uses Mojang mappings, so this should remain stable.
     */
    private static final String DEDICATED_SERVER_CLASS = "net.minecraft.server.dedicated.DedicatedServer";

    /**
     * Dedicated server properties class holding all server.properties values.
     * Each property in server.properties has a corresponding field here.
     */
    private static final String DEDICATED_SERVER_PROPERTIES_CLASS = "net.minecraft.server.dedicated.DedicatedServerProperties";

    /**
     * Method name for getting server instance from Bukkit.
     * Bukkit API: Bukkit.getServer()
     */
    private static final String GET_SERVER_METHOD = "getServer";

    /**
     * Method name for getting NMS server from CraftServer.
     * CraftServer API: CraftServer.getServer()
     */
    private static final String GET_SERVER_METHOD_CRAFT = "getServer";

    /**
     * Field name in DedicatedServer that holds the server properties.
     * Check DedicatedServer class if this breaks in future versions.
     */
    private static final String SETTINGS_FIELD = "settings";

    /**
     * Field name for the prevent-proxy-connections setting in DedicatedServerProperties.
     * This field is final, so we need Unsafe to modify it.
     */
    private static final String PREVENT_PROXY_CONNECTIONS_FIELD = "preventProxyConnections";

    /**
     * The value to force for prevent-proxy-connections.
     * Always set to true to maintain security while using AlwaysAuth.
     */
    private static final boolean PREVENT_PROXY_CONNECTIONS_VALUE = true;

    /**
     * Obtains the singleton Unsafe instance using reflection.
     * <p>
     * The Unsafe class has a private static field "theUnsafe" that holds the singleton instance.
     * This method uses reflection to access this private field and return the Unsafe instance.
     * </p>
     * <p>
     * <b>Warning:</b> This accesses an internal JDK API that is not guaranteed to exist
     * in all Java implementations or future Java versions.
     * </p>
     *
     * @return the Unsafe instance
     * @throws Exception if the Unsafe instance cannot be obtained (field not found or access denied)
     */
    private static Unsafe getUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    /**
     * Forces the prevent-proxy-connections setting to true using Unsafe.
     * <p>
     * This method navigates the reflection chain from Bukkit API down to the server properties,
     * then uses Unsafe to modify the final preventProxyConnections field. This ensures the
     * security setting is enabled regardless of what's in server.properties.
     * </p>
     * <p>
     * <b>Why Unsafe is Required:</b> The preventProxyConnections field is marked as final
     * and loaded at server startup. Normal reflection cannot modify final fields, so we
     * must use Unsafe.putBoolean() which bypasses Java's safety checks.
     * </p>
     * <p>
     * <b>Warning:</b> This uses deep reflection and Unsafe, which may break with Minecraft
     * or JDK updates. If this fails, check the class-level documentation for instructions
     * on finding updated field/method names.
     * </p>
     *
     * @param platform the platform instance for logging
     */
    public static void forcePreventProxyConnections(Platform platform) {
        try {
            // Step 1: Load all required classes
            // These Class.forName calls will throw ClassNotFoundException if the class paths changed
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> dedicatedServerClass = Class.forName(DEDICATED_SERVER_CLASS);
            Class<?> dedicatedServerPropertiesClass = Class.forName(DEDICATED_SERVER_PROPERTIES_CLASS);

            // Step 2: Get the Bukkit server instance
            // Bukkit.getServer() returns the CraftServer instance
            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            // Step 3: Navigate from CraftServer to DedicatedServer (MinecraftServer)
            // CraftServer wraps the internal NMS DedicatedServer
            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            // Step 4: Access the settings/properties field from DedicatedServer
            // This field holds the DedicatedServerProperties instance with all server.properties values
            Field settingsField = dedicatedServerClass.getDeclaredField(SETTINGS_FIELD);
            settingsField.setAccessible(true);
            Object properties = settingsField.get(minecraftServer);

            // Step 5: Get the preventProxyConnections field metadata
            // We need the Field object to calculate its memory offset for Unsafe
            Field preventProxyField = dedicatedServerPropertiesClass.getDeclaredField(PREVENT_PROXY_CONNECTIONS_FIELD);
            preventProxyField.setAccessible(true);

            // Step 6: Use Unsafe to modify the final field
            // Calculate the field's memory offset, then directly write the new boolean value
            // This bypasses Java's final field protection
            Unsafe unsafe = getUnsafe();
            long fieldOffset = unsafe.objectFieldOffset(preventProxyField);
            unsafe.putBoolean(properties, fieldOffset, PREVENT_PROXY_CONNECTIONS_VALUE);

            // Step 7: Log success if debug mode is enabled
            if (platform.isDebug()) {
                platform.sendLogMessage("Successfully set preventProxyConnections to " + PREVENT_PROXY_CONNECTIONS_VALUE);
            }

        } catch (ClassNotFoundException e) {
            platform.sendSevereLogMessage("Failed to find required class - check if class names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            platform.sendSevereLogMessage("Failed to find required field - check if field names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            platform.sendSevereLogMessage("Failed to find required method - check if method names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            platform.sendSevereLogMessage("Unexpected error modifying server properties (Unsafe may be unavailable): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
