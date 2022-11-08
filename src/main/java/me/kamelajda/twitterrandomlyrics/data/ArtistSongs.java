package me.kamelajda.twitterrandomlyrics.data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Setter
public class ArtistSongs {

    private int currentPage = 1;
    private boolean ended = false;
    private final Queue<JSONObject> songs = new ConcurrentLinkedQueue<>();
    private final Set<String> verses = new HashSet<>();

}
