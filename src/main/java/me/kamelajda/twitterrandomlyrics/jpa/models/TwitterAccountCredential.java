package me.kamelajda.twitterrandomlyrics.jpa.models;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Builder
public class TwitterAccountCredential {

    @Id
    @Column(name = "geniusId", nullable = false)
    private Long geniusId;

    private String code;

    private String accessToken;
    private String refreshToken;

}
