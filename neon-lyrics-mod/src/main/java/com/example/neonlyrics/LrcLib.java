package com.example.neonlyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Fetches synced lyrics from https://lrclib.net (free, no key). */
public final class LrcLib {
    public record Result(String title, String artist, String lrc) {}

    public static Result search(String query) throws Exception {
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        URI u = URI.create("https://lrclib.net/api/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpRequest rq = HttpRequest.newBuilder(u)
                .header("User-Agent", "NeonLyrics/1.0 (Fabric mod)")
                .timeout(Duration.ofSeconds(10)).GET().build();
        String body = c.send(rq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()) {
                String t = o.has("trackName") && !o.get("trackName").isJsonNull() ? o.get("trackName").getAsString() : query;
                String a = o.has("artistName") && !o.get("artistName").isJsonNull() ? o.get("artistName").getAsString() : "";
                return new Result(t, a, o.get("syncedLyrics").getAsString());
            }
        }
        return null;
    }
}
