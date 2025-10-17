package com.equinor.onlypikks.controller;

import com.equinor.onlypikks.api.model.PagedResponse;
import com.equinor.onlypikks.api.model.PostResponse;
import com.equinor.onlypikks.api.model.PostSummaryResponse;
import com.equinor.onlypikks.api.model.PostVisibility;
import com.equinor.onlypikks.api.model.UpdatePostMetadataRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostsController {

    private final MockPostService postService;
    private final MockAuthService authService;

    public PostsController(MockPostService postService, MockAuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PostSummaryResponse>> listPosts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "24") int perPage,
            @RequestParam(name = "sort", defaultValue = "created_desc") String sort,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "visibility", required = false) String visibilityParam
    ) {
        Optional<AuthContext> auth = authService.resolve(authorization);
        if (!List.of("created_desc", "created_asc", "popular", "trending").contains(sort)) {
            throw new IllegalArgumentException("Unsupported sort value: " + sort);
        }
        Optional<String> ownerFilter = Optional.empty();
        boolean includePrivate = false;

        if (StringUtils.hasText(owner)) {
            if ("me".equalsIgnoreCase(owner)) {
                AuthContext authContext = auth.orElseThrow(() -> new UnauthorizedException("owner=me requires authentication"));
                ownerFilter = Optional.of(authContext.userId());
                includePrivate = true;
            } else {
                ownerFilter = Optional.of(owner);
                includePrivate = auth.map(authContext -> authContext.userId().equals(owner)).orElse(false);
            }
        }

        Optional<PostVisibility> visibilityFilter = Optional.ofNullable(visibilityParam)
                .filter(StringUtils::hasText)
                .map(value -> PostVisibility.valueOf(value.toUpperCase()));

        boolean includeUnlisted = auth.isPresent() || ownerFilter.isPresent();

        if (visibilityFilter.isPresent()) {
            PostVisibility requestedVisibility = visibilityFilter.get();
            if (requestedVisibility == PostVisibility.PRIVATE && !includePrivate) {
                // silently ignore private filter for non-owners to avoid information leakage.
                visibilityFilter = Optional.empty();
            }
        }

        Optional<String> normalizedQuery = Optional.ofNullable(query).filter(StringUtils::hasText);

        PagedResponse<PostSummaryResponse> response = postService.listPosts(
                page,
                perPage,
                ownerFilter,
                includePrivate,
                includeUnlisted,
                normalizedQuery,
                visibilityFilter
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(response.total()));
        headers.add("X-RateLimit-Limit", "120");
        headers.add("X-RateLimit-Remaining", "118");
        headers.add("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "title", required = false) String title,
            @RequestPart(name = "description", required = false) String description,
            @RequestPart(name = "tags", required = false) List<String> tags,
            @RequestPart(name = "visibility", required = false, defaultValue = "public") String visibility
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        PostVisibility postVisibility = PostVisibility.valueOf(visibility.toUpperCase());
        String resolvedTitle = StringUtils.hasText(title) ? title : "Untitled post";
        String originalFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
        PostResponse created = postService.createPost(
                auth,
                resolvedTitle,
                description,
                tags,
                postVisibility,
                originalFileName,
                file.getSize()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId
    ) {
        Optional<AuthContext> auth = authService.resolve(authorization);
        return postService.findPost(postId, auth)
                .orElseThrow(() -> new NotFoundException("Post not found or inaccessible"));
    }

    @PutMapping(path = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostResponse replaceMedia(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId,
            @RequestPart("file") MultipartFile file
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        Optional<PostResponse> updated = postService.replaceMedia(
                postId,
                auth,
                StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin",
                file.getSize()
        );
        if (updated.isPresent()) {
            return updated.get();
        }
        if (!postService.postExists(postId)) {
            throw new NotFoundException("Post not found");
        }
        throw new ForbiddenException("You are not allowed to replace this media");
    }

    @PatchMapping(path = "/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PostResponse updateMetadata(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId,
            @RequestBody UpdatePostMetadataRequest request
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        Optional<PostResponse> updated = postService.updateMetadata(postId, auth, request);
        if (updated.isPresent()) {
            return updated.get();
        }
        if (!postService.postExists(postId)) {
            throw new NotFoundException("Post not found");
        }
        throw new ForbiddenException("You are not allowed to update this post");
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String postId
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        boolean deleted = postService.deletePost(postId, auth);
        if (!deleted) {
            if (!postService.postExists(postId)) {
                throw new NotFoundException("Post not found");
            }
            throw new ForbiddenException("You are not allowed to delete this post");
        }
        return ResponseEntity.noContent().build();
    }
}
