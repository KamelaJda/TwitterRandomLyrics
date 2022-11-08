package me.kamelajda.twitterrandomlyrics;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TwitterRandomLyricsApplication {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(TwitterRandomLyricsApplication.class, args);
    }

}
