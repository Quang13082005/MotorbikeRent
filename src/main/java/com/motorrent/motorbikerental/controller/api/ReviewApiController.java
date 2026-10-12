package com.motorrent.motorbikerental.controller.api;

import com.motorrent.motorbikerental.dto.request.ReviewRequest;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.repository.UserRepository;
import com.motorrent.motorbikerental.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;
    private final com.motorrent.motorbikerental.security.CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<String> createReview(@Valid @RequestBody ReviewRequest request, Authentication authentication) {
        String customerId = currentUser.customer().getId();
        reviewService.createReview(request, customerId);
        return ResponseEntity.ok("Review submitted successfully");
    }
}
