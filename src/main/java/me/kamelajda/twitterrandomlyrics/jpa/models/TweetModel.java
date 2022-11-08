package me.kamelajda.twitterrandomlyrics.jpa.models;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Builder
public class TweetModel {

    @Id
    @GeneratedValue
    private Long id;

    private Long geniusId;

    private String text;

}
