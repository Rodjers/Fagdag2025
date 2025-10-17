package com.equinor.onlypikks.controller;

import com.equinor.onlypikks.api.model.PagedResponse;
import com.equinor.onlypikks.api.model.PostResponse;
import com.equinor.onlypikks.api.model.PostSummaryResponse;
import com.equinor.onlypikks.api.model.PostVisibility;
import com.equinor.onlypikks.api.model.UpdatePostMetadataRequest;
import com.equinor.onlypikks.auth.AuthContext;
import com.equinor.onlypikks.auth.AuthService;
import com.equinor.onlypikks.exception.ForbiddenException;
import com.equinor.onlypikks.exception.NotFoundException;
import com.equinor.onlypikks.exception.UnauthorizedException;
import com.equinor.onlypikks.service.MockPostService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping(path = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostsController {

    private final MockPostService postService;
    private final AuthService authService;

    public PostsController(MockPostService postService, AuthService authService) {
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
                .body(applyAbsoluteUrls(response));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "title", required = false) String title,
            @RequestPart(name = "description", required = false) String description,
            @RequestPart(name = "tags", required = false) List<String> tags,
            @RequestPart(name = "visibility", required = false) String visibility
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        PostResponse created = createPostWithMetadata(
                auth,
                title,
                description,
                tags,
                visibility,
                file.getOriginalFilename(),
                file.getSize()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<PostResponse> createPostFromBinary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody byte[] payload,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "tags", required = false) List<String> tags,
            @RequestParam(name = "visibility", required = false) String visibility,
            @RequestParam(name = "filename", required = false) String filename,
            @RequestHeader(value = "X-Filename", required = false) String filenameHeader,
            @RequestHeader(value = "Slug", required = false) String slugHeader,
            @RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) String contentDisposition
    ) {
        AuthContext auth = authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        String originalFileName = resolveOriginalFileName(filename, filenameHeader, slugHeader, contentDisposition);
        long fileSize = payload != null ? payload.length : 0L;
        PostResponse created = createPostWithMetadata(
                auth,
                title,
                description,
                tags,
                visibility,
                originalFileName,
                fileSize
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
                .map(this::applyAbsoluteUrls)
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
            return applyAbsoluteUrls(updated.get());
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
            return applyAbsoluteUrls(updated.get());
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

    private PostResponse createPostWithMetadata(
            AuthContext auth,
            String title,
            String description,
            List<String> tags,
            String visibility,
            String originalFileName,
            long fileSize
    ) {
        String normalizedVisibility = StringUtils.hasText(visibility) ? visibility : "public";
        PostVisibility postVisibility = PostVisibility.valueOf(normalizedVisibility.toUpperCase());
        String resolvedTitle = StringUtils.hasText(title) ? title : "Untitled post";
        String resolvedOriginalFileName = StringUtils.hasText(originalFileName) ? originalFileName : "upload.bin";
        List<String> normalizedTags = normalizeTags(tags);
        PostResponse created = postService.createPost(
                auth,
                resolvedTitle,
                description,
                normalizedTags,
                postVisibility,
                resolvedOriginalFileName,
                fileSize
        );
        return applyAbsoluteUrls(created);
    }

    private String resolveOriginalFileName(
            String explicitFileName,
            String headerFileName,
            String slugHeader,
            String contentDispositionValue
    ) {
        if (StringUtils.hasText(explicitFileName)) {
            return explicitFileName;
        }
        if (StringUtils.hasText(headerFileName)) {
            return headerFileName;
        }
        if (StringUtils.hasText(slugHeader)) {
            return slugHeader;
        }
        if (StringUtils.hasText(contentDispositionValue)) {
            try {
                ContentDisposition disposition = ContentDisposition.parse(contentDispositionValue);
                if (StringUtils.hasText(disposition.getFilename())) {
                    return disposition.getFilename();
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid content disposition headers and fall back to default name.
            }
        }
        return "upload.bin";
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return tags;
        }
        if (tags.size() == 1 && tags.get(0) != null && tags.get(0).contains(",")) {
            return Arrays.stream(tags.get(0).split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return tags;
    }

    private PagedResponse<PostSummaryResponse> applyAbsoluteUrls(PagedResponse<PostSummaryResponse> response) {
        List<PostSummaryResponse> normalizedItems = response.items().stream()
                .map(this::applyAbsoluteUrls)
                .toList();
        return new PagedResponse<>(normalizedItems, response.page(), response.perPage(), response.total());
    }

    private PostSummaryResponse applyAbsoluteUrls(PostSummaryResponse summary) {
        return new PostSummaryResponse(
                summary.id(),
                summary.title(),
                summary.description(),
                summary.tags(),
                summary.visibility(),
                summary.ownerId(),
                summary.ownerDisplayName(),
                ensureAbsoluteUrl(summary.thumbnailUrl()),
                summary.createdAt(),
                summary.updatedAt(),
                summary.commentCount(),
                summary.likeCount()
        );
    }

    private PostResponse applyAbsoluteUrls(PostResponse response) {
        return new PostResponse(
                response.id(),
                response.title(),
                response.description(),
                response.tags(),
                response.visibility(),
                response.ownerId(),
                response.ownerDisplayName(),
                response.fileId(),
                ensureAbsoluteUrl(response.fileUrl()),
                ensureAbsoluteUrl(response.thumbnailUrl()),
                response.originalFileName(),
                response.fileSize(),
                response.createdAt(),
                response.updatedAt(),
                response.commentCount(),
                response.likeCount(),
                response.latestComments()
        );
    }

    private String ensureAbsoluteUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String normalizedPath = url.startsWith("/") ? url : "/" + url;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(normalizedPath)
                .toUriString();
    }
}
