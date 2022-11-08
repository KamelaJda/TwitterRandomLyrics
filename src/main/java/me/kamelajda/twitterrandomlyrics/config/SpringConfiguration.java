package me.kamelajda.twitterrandomlyrics.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "twitter")
public class SpringConfiguration {

    private String geniusToken;
    private String apiKey;
    private String apiSecret;
    private String callBack;
    private String scope;
    private String clientId;
    private List<Artist> artists;

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Artist {
        private Long geniusId;
    }


}
