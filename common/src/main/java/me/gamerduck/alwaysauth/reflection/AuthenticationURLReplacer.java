package me.gamerduck.alwaysauth.reflection;
import me.gamerduck.alwaysauth.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Replaces Minecraft's authentication URLs using reflection for Paper/Spigot servers.
 * <p>
 * This class uses Java reflection to modify the internal Mojang authentication service URLs
 * at runtime, redirecting them to a local proxy server. This is necessary because Paper/Spigot
 * don't support mixins, so we must use reflection instead.
 * </p>
 *
 * <h2>How It Works</h2>
 * <p>
 * The reflection chain follows this path:
 * <pre>
 * Bukkit.getServer()
 *   → CraftServer.getServer()
 *     → MinecraftServer.services (field)
 *       → Services.sessionService() (method)
 *         → YggdrasilMinecraftSessionService.baseUrl/joinUrl/checkUrl (fields)
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
 * <h3>Step 3: Find MinecraftServer Class</h3>
 * <ol>
 *   <li>Search for "net.minecraft.server.MinecraftServer"</li>
 *   <li>Look for a field that holds server services (usually named "services" or similar)</li>
 *   <li>Note: Paper uses Mojang mappings, so names are usually readable</li>
 *   <li>Update {@link #SERVICES_FIELD} if the name changed</li>
 * </ol>
 *
 * <h3>Step 4: Find Services Class/Record</h3>
 * <ol>
 *   <li>Find "net.minecraft.server.Services" (it's a record class)</li>
 *   <li>Look for the sessionService() accessor method</li>
 *   <li>It should return a MinecraftSessionService or similar type</li>
 *   <li>Update {@link #SESSION_SERVICE_METHOD} if the name changed</li>
 * </ol>
 *
 * <h3>Step 5: Find YggdrasilMinecraftSessionService</h3>
 * <ol>
 *   <li>Search for "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService"</li>
 *   <li>This is from Authlib (Mojang's library), so it rarely changes</li>
 *   <li>Look for these URL fields:
 *     <ul>
 *       <li>baseUrl (String) - base URL for the session server</li>
 *       <li>joinUrl (URL) - endpoint for /session/minecraft/join</li>
 *       <li>checkUrl (URL) - endpoint for /session/minecraft/hasJoined</li>
 *     </ul>
 *   </li>
 *   <li>Update {@link #BASE_URL_FIELD}, {@link #JOIN_URL_FIELD}, {@link #CHECK_URL_FIELD} if changed</li>
 * </ol>
 *
 * <h3>Step 6: Verify HttpAuthenticationService</h3>
 * <ol>
 *   <li>Find "com.mojang.authlib.HttpAuthenticationService"</li>
 *   <li>Look for the constantURL(String) method that creates URL objects</li>
 *   <li>Update {@link #CONSTANT_URL_METHOD} if the name changed</li>
 * </ol>
 *
 * <h3>Step 7: Test Your Changes</h3>
 * <ol>
 *   <li>Enable debug mode in the config (debug=true)</li>
 *   <li>Start your server and look for "Successfully replaced authentication URLs" message</li>
 *   <li>If you see ClassNotFoundException or NoSuchFieldException, double-check your mappings</li>
 *   <li>Test by joining the server - authentication should go through your proxy</li>
 * </ol>
 *
 * <h3>Alternative: Use Mapping Tools</h3>
 * <p>
 * For obfuscated versions, use these tools to find mappings:
 * </p>
 * <ul>
 *   <li><b>Linkie</b>: https://linkie.shedaniel.dev/ - web-based mapping lookup</li>
 *   <li><b>MCPConfig</b>: Official Minecraft mapping repository</li>
 *   <li><b>Paper Mappings</b>: https://github.com/PaperMC/paperweight - Paper's deobfuscation mappings</li>
 * </ul>
 *
 * @see me.gamerduck.alwaysauth.mixin.mixins.AuthenticationURLReplacer The mixin version for Fabric/NeoForge
 */
public class AuthenticationURLReplacer {

    /**
     * Minecraft server class - contains the Services field.
     * Paper uses Mojang mappings, so this should remain stable.
     */
    private static final String MINECRAFT_SERVER_CLASS = "net.minecraft.server.MinecraftServer";

    /**
     * CraftBukkit's server implementation - bridges Bukkit API to NMS.
     */
    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";

    /**
     * Bukkit API entry point for accessing the server instance.
     */
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";

    /**
     * Services record containing all server services (session, profile, etc.).
     * Introduced in newer Minecraft versions as a record class.
     */
    private static final String SERVICES_CLASS = "net.minecraft.server.Services";

    /**
     * Mojang's Yggdrasil authentication implementation.
     * Part of Authlib, rarely changes between versions.
     */
    private static final String YGGDRASIL_SESSION_SERVICE_CLASS = "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService";

    /**
     * HTTP authentication base class from Authlib.
     * Contains utility methods like constantURL().
     */
    private static final String HTTP_AUTH_SERVICE_CLASS = "com.mojang.authlib.HttpAuthenticationService";

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
     * Field name in MinecraftServer that holds the Services record.
     * Check MinecraftServer class if this breaks in future versions.
     */
    private static final String SERVICES_FIELD = "services";

    /**
     * Accessor method for getting session service from Services record.
     * Records generate accessor methods automatically: Services.sessionService()
     */
    private static final String SESSION_SERVICE_METHOD = "sessionService";

    /**
     * Base URL field in YggdrasilMinecraftSessionService.
     * Contains the base path for session server (e.g., "https://sessionserver.mojang.com/session/minecraft/")
     */
    private static final String BASE_URL_FIELD = "baseUrl";

    /**
     * Join URL field in YggdrasilMinecraftSessionService.
     * Full URL for the /session/minecraft/join endpoint.
     */
    private static final String JOIN_URL_FIELD = "joinUrl";

    /**
     * Check URL field in YggdrasilMinecraftSessionService.
     * Full URL for the /session/minecraft/hasJoined endpoint.
     */
    private static final String CHECK_URL_FIELD = "checkUrl";

    /**
     * Method name for creating constant URL objects in HttpAuthenticationService.
     * Signature: public static URL constantURL(String url)
     */
    private static final String CONSTANT_URL_METHOD = "constantURL";

    /**
     * Replaces the Mojang session server URLs with a custom proxy server URL.
     * <p>
     * This method navigates the reflection chain from Bukkit API down to the internal
     * Yggdrasil authentication service, then modifies the URL fields to point at the
     * local AlwaysAuth proxy server instead of Mojang's servers.
     * </p>
     * <p>
     * <b>Warning:</b> This uses deep reflection into Minecraft internals and may break
     * with Minecraft updates. If this fails, check the class-level documentation for
     * instructions on finding updated field/method names.
     * </p>
     *
     * @param platform the platform instance for logging
     * @param customSessionHost the custom session server URL (e.g., "http://127.0.0.1:8765")
     */
    public static void replaceSessionService(Platform platform, String customSessionHost) {
        try {
            // Step 1: Load all required classes
            // These Class.forName calls will throw ClassNotFoundException if the class paths changed
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> minecraftServerClass = Class.forName(MINECRAFT_SERVER_CLASS);
            Class<?> servicesClass = Class.forName(SERVICES_CLASS);
            Class<?> yggdrasilServiceClass = Class.forName(YGGDRASIL_SESSION_SERVICE_CLASS);
            Class<?> httpAuthServiceClass = Class.forName(HTTP_AUTH_SERVICE_CLASS);

            // Step 2: Get the Bukkit server instance
            // Bukkit.getServer() returns the CraftServer instance
            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            // Step 3: Navigate from CraftServer to MinecraftServer
            // CraftServer wraps the internal NMS MinecraftServer
            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            // Step 4: Access the Services record from MinecraftServer
            // Services is a record containing various server services (session, profile, etc.)
            Field servicesField = minecraftServerClass.getDeclaredField(SERVICES_FIELD);
            servicesField.setAccessible(true);
            Object services = servicesField.get(minecraftServer);

            // Step 5: Get the session service from the Services record
            // Services.sessionService() returns the MinecraftSessionService implementation
            Object sessionService = servicesClass
                    .getMethod(SESSION_SERVICE_METHOD)
                    .invoke(services);

            // Step 6: Verify we have the correct session service implementation
            // We need YggdrasilMinecraftSessionService specifically to access URL fields
            if (!yggdrasilServiceClass.isInstance(sessionService)) {
                platform.sendSevereLogMessage("Session service is not " + YGGDRASIL_SESSION_SERVICE_CLASS);
                return;
            }

            // Step 7: Prepare the new URL values
            // The base URL ends with /session/minecraft/, and join/hasJoined are appended
            String newBaseUrl = customSessionHost + "/session/minecraft/";
            Method constantUrlMethod = httpAuthServiceClass.getMethod(CONSTANT_URL_METHOD, String.class);

            // Step 8: Replace the baseUrl field (String)
            // This is used as the base for constructing other URLs
            Field baseUrlField = yggdrasilServiceClass.getDeclaredField(BASE_URL_FIELD);
            baseUrlField.setAccessible(true);
            baseUrlField.set(sessionService, newBaseUrl);

            // Step 9: Replace the joinUrl field (URL)
            // Used when a player attempts to join a server
            Field joinUrlField = yggdrasilServiceClass.getDeclaredField(JOIN_URL_FIELD);
            joinUrlField.setAccessible(true);
            URL newJoinUrl = (URL) constantUrlMethod.invoke(null, newBaseUrl + "join");
            joinUrlField.set(sessionService, newJoinUrl);

            // Step 10: Replace the checkUrl field (URL)
            // Used to verify that a player has authenticated (hasJoined endpoint)
            Field checkUrlField = yggdrasilServiceClass.getDeclaredField(CHECK_URL_FIELD);
            checkUrlField.setAccessible(true);
            URL newCheckUrl = (URL) constantUrlMethod.invoke(null, newBaseUrl + "hasJoined");
            checkUrlField.set(sessionService, newCheckUrl);

            // Step 11: Log success if debug mode is enabled
            // This helps verify that the reflection worked and shows the new URLs
            if (platform.isDebug()) {
                platform.sendLogMessage("Successfully replaced authentication URLs:");
                platform.sendLogMessage("  Base URL: " + newBaseUrl);
                platform.sendLogMessage("  Join URL: " + newJoinUrl);
                platform.sendLogMessage("  Check URL: " + newCheckUrl);
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
            platform.sendSevereLogMessage("Unexpected error during authentication URL replacement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}