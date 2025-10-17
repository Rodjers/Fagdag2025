package com.equinor.onlypikks.controller;

import com.equinor.onlypikks.api.model.CommentResponse;
import com.equinor.onlypikks.api.model.CreateCommentRequest;
import com.equinor.onlypikks.api.model.PagedResponse;
import com.equinor.onlypikks.auth.AuthContext;
import com.equinor.onlypikks.auth.MockAuthService;
import com.equinor.onlypikks.exception.ForbiddenException;
import com.equinor.onlypikks.exception.NotFoundException;
import com.equinor.onlypikks.exception.UnauthorizedException;
import com.equinor.onlypikks.service.MockPostService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/posts/{postId}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommentsController {

    private final MockPostService postService;
    private final MockAuthService authService;

    public CommentsController(MockPostService postService, MockAuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CommentResponse>> listComments(
            @PathVariable String postId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "20") int perPage
    ) {
        PagedResponse<CommentResponse> response = postService.listComments(postId, page, perPage)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(response.total()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentResponse> createComment(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId,
            @RequestBody CreateCommentRequest request
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        if (!StringUtils.hasText(request.text())) {
            throw new IllegalArgumentException("text is required");
        }
        CommentResponse created = postService.addComment(postId, auth, request.text())
                .orElseThrow(() -> new NotFoundException("Post not found"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        MockPostService.DeleteCommentResult result = postService.deleteComment(postId, commentId, auth);
        return switch (result) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case POST_NOT_FOUND -> throw new NotFoundException("Post not found");
            case COMMENT_NOT_FOUND -> throw new NotFoundException("Comment not found");
            case FORBIDDEN -> throw new ForbiddenException("You are not allowed to delete this comment");
        };
    }
}
