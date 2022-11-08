package me.kamelajda.twitterrandomlyrics.services;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.twitter.clientlib.ApiClientCallback;
import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.auth.TwitterOAuth20Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.twitterrandomlyrics.config.SpringConfiguration;
import me.kamelajda.twitterrandomlyrics.jpa.models.TweetModel;
import me.kamelajda.twitterrandomlyrics.jpa.models.TwitterAccountCredential;
import me.kamelajda.twitterrandomlyrics.jpa.repository.TweetModelRepository;
import me.kamelajda.twitterrandomlyrics.jpa.repository.TwitterAccountCredentialRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class TwitterJPAService {

    private final TweetModelRepository tweetModelRepository;
    private final TwitterAccountCredentialRepository twitterAccountCredentialRepository;
    private final SpringConfiguration springConfiguration;
    private final TwitterOAuth20Service twitterOAuth2;
    private final PKCE pkce = new PKCE();
    private final Map<Long, TwitterApi> instancesMap;

    public TwitterJPAService(TweetModelRepository tweetModelRepository, TwitterAccountCredentialRepository twitterAccountCredentialRepository, SpringConfiguration springConfiguration) {
        this.tweetModelRepository = tweetModelRepository;
        this.twitterAccountCredentialRepository = twitterAccountCredentialRepository;
        this.springConfiguration = springConfiguration;
        this.twitterOAuth2 = new TwitterOAuth20Service(springConfiguration.getApiKey(), springConfiguration.getApiSecret(), springConfiguration.getCallBack(), springConfiguration.getScope());
        this.instancesMap = new HashMap<>();

        pkce.setCodeChallenge("challenge");
        pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
        pkce.setCodeVerifier("challenge");

        log.info("===========================");
        log.info("OAuth2 URL: {}", twitterOAuth2.getAuthorizationUrl(pkce, "state"));
        log.info("===========================");
    }

    public TwitterAccountCredential getAccountCredential(Long geniusId) {
        return twitterAccountCredentialRepository.findById(geniusId).orElse(null);
    }

    public void createAccessToken(TwitterAccountCredential credential) throws IOException, ExecutionException, InterruptedException {
        if (credential.getCode() == null) throw new IllegalStateException("Cannot create access token without code!");

        OAuth2AccessToken oauthToken = twitterOAuth2.getAccessToken(pkce, credential.getCode());

        credential.setAccessToken(oauthToken.getAccessToken());
        credential.setRefreshToken(oauthToken.getRefreshToken());
        credential.setCode(null);

        twitterAccountCredentialRepository.save(credential);
    }

    public void updateAccessToken(Long geniusId, String accessToken, String refreshToken) {
        TwitterAccountCredential credential = getAccountCredential(geniusId);
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        credential.setCode(null);
        twitterAccountCredentialRepository.save(credential);
    }

    public TwitterApi createInstance(TwitterAccountCredential credential) {
        if (credential.getAccessToken() == null || credential.getRefreshToken() == null) throw new IllegalStateException("Cannot create access token without access/refresh token!");

        if (instancesMap.containsKey(credential.getGeniusId())) return instancesMap.get(credential.getGeniusId());

        TwitterCredentialsOAuth2 auth2 = new TwitterCredentialsOAuth2(springConfiguration.getApiKey(), springConfiguration.getApiSecret(), credential.getAccessToken(), credential.getRefreshToken(), true);

        TwitterApi twitterApi = new TwitterApi(auth2);
        twitterApi.addCallback(new RefreshCallback(credential.getGeniusId()));

        instancesMap.put(credential.getGeniusId(), twitterApi);
        return twitterApi;
    }

    public void uploadTweet(Long geniusId, String text) {
        tweetModelRepository.save(TweetModel.builder().geniusId(geniusId).text(text).build());
    }

    public boolean containsTweet(Long geniusId, String text) {
        return tweetModelRepository.findByGeniusIdAndText(geniusId, text) != null;
    }

    @RequiredArgsConstructor
    public class RefreshCallback implements ApiClientCallback {

        private final Long geniusId;

        @Override
        public void onAfterRefreshToken(OAuth2AccessToken token) {
            updateAccessToken(geniusId, token.getAccessToken(), token.getRefreshToken());
        }

    }

}
