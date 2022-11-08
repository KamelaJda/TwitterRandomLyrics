package me.kamelajda.twitterrandomlyrics.jpa.repository;

import me.kamelajda.twitterrandomlyrics.jpa.models.TweetModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TweetModelRepository extends JpaRepository<TweetModel, Long> {

    TweetModel findByGeniusIdAndText(Long geniusId, String text);

}
