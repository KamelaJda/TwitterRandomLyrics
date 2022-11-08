package me.kamelajda.twitterrandomlyrics.services;

import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.TweetCreateRequest;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.twitterrandomlyrics.config.SpringConfiguration;
import me.kamelajda.twitterrandomlyrics.data.ArtistSongs;
import me.kamelajda.twitterrandomlyrics.jpa.models.TwitterAccountCredential;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@EnableScheduling
public class RandomVerseService {

    private static final String GENIUS_BASIC_API = "https://api.genius.com";
    private static final Integer PER_PAGE = 5;
    private static final String GENIUS_EMBED_URL_HEAD = "https://genius.com/songs/";
    private static final String GENIUS_EMBED_URL_TAIL = "/embed.js";

    private final OkHttpClient okHttpClient;
    private final SpringConfiguration springConfiguration;
    private final TwitterJPAService twitterJPAService;
    private final Map<SpringConfiguration.Artist, ArtistSongs> artistsMap;

    public RandomVerseService(SpringConfiguration springConfiguration, OkHttpClient okHttpClient, TwitterJPAService twitterJPAService) throws JSONException {
        this.springConfiguration = springConfiguration;
        this.okHttpClient = okHttpClient;
        this.twitterJPAService = twitterJPAService;
        this.artistsMap = new HashMap<>();

        for (SpringConfiguration.Artist artist : springConfiguration.getArtists()) {
            ArtistSongs artistSongs = new ArtistSongs();
            JSONArray songs = getSongs(artist, 1);

            artistSongs.setCurrentPage(1);

            for (int i = 0; i < songs.length(); i++) {
                artistSongs.getSongs().add(songs.getJSONObject(i));
            }

            artistsMap.put(artist, artistSongs);
        }

    }

    @Async
    @Scheduled(fixedDelay = 1_800_000)
    public void schedule() {
        for (SpringConfiguration.Artist artist : springConfiguration.getArtists()) {
            try {
                String verse = getRandomVerse(artist);

                while (twitterJPAService.containsTweet(artist.getGeniusId(), verse)) {
                    verse = getRandomVerse(artist);
                }

                log.info("================================");
                log.info("New tweet: ID={}, verse={}", artist.getGeniusId(), verse);
                log.info("================================");

                twitterJPAService.uploadTweet(artist.getGeniusId(), verse);

                TweetCreateRequest tweetCreateRequest = new TweetCreateRequest();
                tweetCreateRequest.setText(verse);

                TwitterAccountCredential credential = twitterJPAService.getAccountCredential(artist.getGeniusId());
                if (credential.getCode() != null) twitterJPAService.createAccessToken(credential);

                TwitterApi instance = twitterJPAService.createInstance(credential);

                instance.tweets().createTweet(tweetCreateRequest).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getRandomVerse(SpringConfiguration.Artist artist) throws JSONException {
        ArtistSongs artistSong = artistsMap.get(artist);

        if (artistSong.getVerses().isEmpty()) configureArtistVerses(artist, artistSong);

        String verse = artistSong.getVerses().stream().findFirst().orElse(null);
        if (verse == null) return null;

        artistSong.getVerses().remove(verse);

        if (verse.length() > 280) verse = verse.substring(0, 280);
        return verse;
    }

    private void configureArtistVerses(SpringConfiguration.Artist artist, ArtistSongs artistSong) throws JSONException {
        Set<String> verses =  new HashSet<>();
        JSONObject song = getArtistSong(artist);

        try {
            String raw = getRaw(GENIUS_EMBED_URL_HEAD + song.getLong("id") + GENIUS_EMBED_URL_TAIL);

            for (String s : getReadable(raw).split("\n\n")) {
                verses.addAll(Arrays.asList(s.split("\n")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        verses.removeIf(v -> v.isEmpty() || v.isBlank());

        artistSong.getVerses().addAll(verses);
    }

    private String getReadable(String rawLyrics) {
        return rawLyrics.replaceAll("[\\S\\s]*<div class=\\\\\\\\\\\\\"rg_embed_body\\\\\\\\\\\\\">[ (\\\\n)]*", "")
            .replaceAll("[ (\\\\n)]*<\\\\/div>[\\S\\s]*", "")
            .replaceAll("\\\\\\\\n","\n")
            .replaceAll("\\\\'", "'")
            .replaceAll("\\\\\\\\\\\\\"", "\"")
            .replaceAll("\\[[^()]*]", "")
            .replaceAll("<[^<>]*>", "");
    }

    private JSONObject getArtistSong(SpringConfiguration.Artist artist) throws JSONException {
        ArtistSongs songs = artistsMap.get(artist);
        if (songs == null) throw new NullPointerException("Artist " + artist.toString() + " doesn't exist?");

        JSONObject peek = songs.getSongs().poll();
        if (peek != null) return peek;

        int page = songs.isEnded() ? 1 : songs.getCurrentPage() + 1;

        JSONArray newSongs = getSongs(artist, page);
        songs.setCurrentPage(page);
        songs.setEnded(newSongs.length() < PER_PAGE);

        for (int i = 0; i < newSongs.length(); i++) {
            songs.getSongs().add(newSongs.getJSONObject(i));
        }

        return songs.getSongs().poll();
    }

    @Nullable
    public JSONArray getSongs(SpringConfiguration.Artist artist, int page) {
        try {
            JSONObject json = getJson(GENIUS_BASIC_API + String.format("/artists/%s/songs?sort=popularity&per_page=%s&page=%s", artist.getGeniusId(), PER_PAGE, page));
            if (json == null) return null;

            return json.getJSONObject("response").getJSONArray("songs");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONObject getJson(String url) throws IOException, JSONException {
        Request req = new Request.Builder()
            .header("Authorization", "Bearer " + springConfiguration.getGeniusToken())
            .url(url)
            .build();
        try (Response res = okHttpClient.newCall(req).execute()) {
            return res.body() == null ? null : new JSONObject(res.body().string());
        }
    }

    private String getRaw(String url) throws IOException {
        Request req = new Request.Builder()
            .header("Authorization", "Bearer " + springConfiguration.getGeniusToken())
            .url(url)
            .build();
        try (Response res = okHttpClient.newCall(req).execute()) {
            return res.body().string();
        }
    }

}