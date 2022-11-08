package me.kamelajda.twitterrandomlyrics.jpa.repository;

import me.kamelajda.twitterrandomlyrics.jpa.models.TwitterAccountCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwitterAccountCredentialRepository extends JpaRepository<TwitterAccountCredential, Long> {
}
