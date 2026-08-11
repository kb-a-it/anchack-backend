package com.kbait.anchack.review.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewReaction {

    private Long reactionId;
    private Long reviewId;
    private Long userId;
    private String reactionType;
    private LocalDateTime createdAt;
}
