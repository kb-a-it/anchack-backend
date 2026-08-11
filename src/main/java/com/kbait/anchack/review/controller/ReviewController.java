package com.kbait.anchack.review.controller;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewReactionRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
        ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    /**
     * 특정 행정동의 공개 리뷰 목록 조회
     *
     * GET /api/reviews?adminDongId=1
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse>>
    getReviewsByAdminDong(
        HttpServletRequest httpRequest,
        @RequestParam Long adminDongId
    ) {
        // 좋아요/싫어요는 로그인하지 않아도 목록 조회는 되지만,
        // 로그인한 사용자라면 내가 남긴 반응(myReaction)을 함께 내려준다.
        Long viewerId =
            getOptionalAuthenticatedUserId(httpRequest);

        List<ReviewResponse> reviews =
            reviewService.getReviewsByAdminDong(
                adminDongId,
                viewerId
            );

        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 상세 조회
     *
     * GET /api/reviews/1
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId
    ) {
        Long viewerId =
            getOptionalAuthenticatedUserId(httpRequest);

        ReviewResponse review =
            reviewService.getReview(reviewId, viewerId);

        return ResponseEntity.ok(review);
    }

    /**
     * 로그인 사용자가 작성한 리뷰 조회
     *
     * GET /api/reviews/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>>
    getMyReviews(
        HttpServletRequest httpRequest
    ) {
        Long userId = getAuthenticatedUserId(
            httpRequest
        );

        List<ReviewResponse> reviews =
            reviewService.getMyReviews(userId);

        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 등록
     *
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
        HttpServletRequest httpRequest,
        @RequestBody ReviewCreateRequest request
    ) {
        Long userId = getAuthenticatedUserId(
            httpRequest
        );

        ReviewResponse createdReview =
            reviewService.createReview(
                userId,
                request
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdReview);
    }

    /**
     * 리뷰 수정
     *
     * PUT /api/reviews/1
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId,
        @RequestBody ReviewUpdateRequest request
    ) {
        Long userId = getAuthenticatedUserId(
            httpRequest
        );

        ReviewResponse updatedReview =
            reviewService.updateReview(
                userId,
                reviewId,
                request
            );

        return ResponseEntity.ok(updatedReview);
    }

    /**
     * 리뷰 삭제
     *
     * 실제 데이터 삭제가 아니라 상태를 DELETED로 변경한다.
     *
     * DELETE /api/reviews/1
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>>
    deleteReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId
    ) {
        Long userId = getAuthenticatedUserId(
            httpRequest
        );

        reviewService.deleteReview(
            userId,
            reviewId
        );

        Map<String, Object> response =
            new LinkedHashMap<>();

        response.put("success", true);
        response.put(
            "message",
            "리뷰가 삭제되었습니다."
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 좋아요 / 싫어요
     *
     * 같은 반응을 다시 누르면 취소되고, 반대 반응을 누르면 바뀐다.
     *
     * POST /api/reviews/1/reactions
     * body: { "reactionType": "LIKE" | "DISLIKE" }
     */
    @PostMapping("/{reviewId}/reactions")
    public ResponseEntity<ReviewReactionResponse> reactToReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId,
        @RequestBody ReviewReactionRequest request
    ) {
        Long userId = getAuthenticatedUserId(
            httpRequest
        );

        ReviewReactionResponse response =
            reviewService.reactToReview(
                userId,
                reviewId,
                request == null ? null : request.getReactionType()
            );

        return ResponseEntity.ok(response);
    }

    /**
     * JWT 필터가 request에 저장한 사용자 ID를 가져온다.
     */
    private Long getAuthenticatedUserId(
        HttpServletRequest request
    ) {
        Object userIdAttribute =
            request.getAttribute(
                JwtAuthenticationFilter
                    .USER_ID_ATTRIBUTE
            );

        if (userIdAttribute == null) {
            throw new SecurityException(
                "로그인이 필요합니다."
            );
        }

        if (userIdAttribute instanceof Long) {
            return (Long) userIdAttribute;
        }

        if (userIdAttribute instanceof Number) {
            return ((Number) userIdAttribute)
                .longValue();
        }

        try {
            return Long.valueOf(
                userIdAttribute.toString()
            );
        } catch (NumberFormatException exception) {
            throw new SecurityException(
                "유효하지 않은 인증 정보입니다."
            );
        }
    }

    /**
     * getAuthenticatedUserId()와 달리 로그인하지 않았어도 예외를 던지지 않고
     * null을 반환한다. 공개 리뷰 조회에서 "내가 남긴 반응"을 선택적으로
     * 채워주기 위해 사용한다.
     */
    private Long getOptionalAuthenticatedUserId(
        HttpServletRequest request
    ) {
        try {
            return getAuthenticatedUserId(request);
        } catch (SecurityException exception) {
            return null;
        }
    }
}
