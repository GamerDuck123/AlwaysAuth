package me.gamerduck.alwaysauth.api.updates;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fetches version numbers from Modrinth API.
 */
public class ModrinthUpdateChecker {
    private static final String API_URL = "https://api.modrinth.com/v2/project/@modrinthToken@/version";
    private static final String VERSION = "@version@";
    private static final String LOADER = "@loader@";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static Optional<String> getNewer() {
        Optional<String> newestVersion = fetchVersionNumbers().stream().filter(s -> s.endsWith("-" + LOADER))
                .map(s -> s.split("-")[0]).findFirst();
        if (newestVersion.isPresent() && FlexVerComparator.compare(newestVersion.get(), VERSION) > 0)
            return Optional.of(newestVersion.get());
        return Optional.empty();
    }

    /**
     * Fetches all version numbers from Modrinth API.
     *
     * @return List of version number strings, or null if fetch fails
     */
    private static List<String> fetchVersionNumbers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "AlwaysAuth/Modrinth-Update-Checker")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch Modrinth versions: HTTP " + response.statusCode());
                return null;
            }

            return parseVersionNumbers(response.body());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching Modrinth versions: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the JSON response to extract version numbers.
     *
     * @param json Raw JSON response
     * @return List of version number strings
     */
    private static List<String> parseVersionNumbers(String json) {
        List<String> versionNumbers = new ArrayList<>();
        JsonArray array = GSON.fromJson(json, JsonArray.class);

        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            String versionNumber = obj.get("version_number").getAsString();
            versionNumbers.add(versionNumber);
        }

        return versionNumbers;
    }
}