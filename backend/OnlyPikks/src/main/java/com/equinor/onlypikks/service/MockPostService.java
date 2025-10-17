package com.equinor.onlypikks.service;

import com.equinor.onlypikks.api.model.CommentResponse;
import com.equinor.onlypikks.api.model.PagedResponse;
import com.equinor.onlypikks.api.model.PostResponse;
import com.equinor.onlypikks.api.model.PostSummaryResponse;
import com.equinor.onlypikks.api.model.PostVisibility;
import com.equinor.onlypikks.api.model.UpdatePostMetadataRequest;
import com.equinor.onlypikks.auth.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MockPostService {

    private final Map<String, PostRecord> posts = new ConcurrentHashMap<>();
    private final AtomicLong postSequence = new AtomicLong(1000);
    private final AtomicLong commentSequence = new AtomicLong(5000);

    public MockPostService() {
        seedData();
    }

    public PagedResponse<PostSummaryResponse> listPosts(
            int page,
            int perPage,
            Optional<String> ownerFilter,
            boolean includePrivate,
            boolean includeUnlisted,
            Optional<String> query,
            Optional<PostVisibility> visibilityFilter
    ) {
        List<PostSummaryResponse> filtered = posts.values().stream()
                .sorted(Comparator.comparing(PostRecord::createdAt).reversed())
                .filter(post -> ownerFilter.map(owner -> post.ownerId.equals(owner)).orElse(true))
                .filter(post -> includePrivate || post.visibility != PostVisibility.PRIVATE)
                .filter(post -> includeUnlisted || post.visibility != PostVisibility.UNLISTED)
                .filter(post -> visibilityFilter.map(v -> post.visibility == v).orElse(true))
                .filter(post -> query.map(q -> post.matchesQuery(q)).orElse(true))
                .map(PostRecord::toSummary)
                .toList();

        int safePage = Math.max(page, 1);
        int safePerPage = Math.max(Math.min(perPage, 100), 1);
        int fromIndex = Math.min((safePage - 1) * safePerPage, filtered.size());
        int toIndex = Math.min(fromIndex + safePerPage, filtered.size());
        List<PostSummaryResponse> pageItems = filtered.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageItems, safePage, safePerPage, filtered.size());
    }

    public Optional<PostResponse> findPost(String postId, Optional<AuthContext> auth) {
        PostRecord record = posts.get(postId);
        if (record == null) {
            return Optional.empty();
        }
        boolean isOwner = auth.map(authContext -> authContext.userId().equals(record.ownerId)).orElse(false);
        if (record.visibility == PostVisibility.PRIVATE && !isOwner) {
            return Optional.empty();
        }
        return Optional.of(record.toResponse());
    }

    public PostResponse createPost(
            AuthContext auth,
            String title,
            String description,
            List<String> tags,
            PostVisibility visibility,
            String originalFileName,
            long fileSizeBytes
    ) {
        String postId = "post-" + postSequence.incrementAndGet();
        Instant now = Instant.now();
        String fileId = UUID.randomUUID().toString();
        PostRecord record = new PostRecord(
                postId,
                title,
                description,
                sanitizeTags(tags),
                visibility,
                auth.userId(),
                auth.displayName(),
                fileId,
                buildFileUrl(fileId),
                buildThumbnailUrl(fileId),
                originalFileName,
                fileSizeBytes,
                now,
                now,
                0,
                0,
                new ArrayList<>()
        );
        posts.put(postId, record);
        return record.toResponse();
    }

    public Optional<PostResponse> replaceMedia(
            String postId,
            AuthContext auth,
            String originalFileName,
            long fileSizeBytes
    ) {
        PostRecord record = posts.get(postId);
        if (record == null || !record.ownerId.equals(auth.userId())) {
            return Optional.empty();
        }
        String newFileId = UUID.randomUUID().toString();
        record.fileId = newFileId;
        record.fileUrl = buildFileUrl(newFileId);
        record.thumbnailUrl = buildThumbnailUrl(newFileId);
        record.originalFileName = originalFileName;
        record.fileSize = fileSizeBytes;
        record.updatedAt = Instant.now();
        return Optional.of(record.toResponse());
    }

    public Optional<PostResponse> updateMetadata(
            String postId,
            AuthContext auth,
            UpdatePostMetadataRequest request
    ) {
        PostRecord record = posts.get(postId);
        if (record == null || !record.ownerId.equals(auth.userId())) {
            return Optional.empty();
        }
        if (StringUtils.hasText(request.title())) {
            record.title = request.title();
        }
        if (request.description() != null) {
            record.description = request.description();
        }
        if (request.tags() != null) {
            record.tags = sanitizeTags(request.tags());
        }
        if (request.visibility() != null) {
            record.visibility = request.visibility();
        }
        record.updatedAt = Instant.now();
        return Optional.of(record.toResponse());
    }

    public boolean deletePost(String postId, AuthContext auth) {
        PostRecord record = posts.get(postId);
        if (record == null || !record.ownerId.equals(auth.userId())) {
            return false;
        }
        posts.remove(postId);
        return true;
    }

    public boolean postExists(String postId) {
        return posts.containsKey(postId);
    }

    public Optional<PagedResponse<CommentResponse>> listComments(
            String postId,
            int page,
            int perPage
    ) {
        PostRecord record = posts.get(postId);
        if (record == null) {
            return Optional.empty();
        }
        List<CommentResponse> all = record.comments.stream()
                .sorted(Comparator.comparing(CommentRecord::createdAt))
                .map(CommentRecord::toResponse)
                .toList();

        int safePage = Math.max(page, 1);
        int safePerPage = Math.max(Math.min(perPage, 100), 1);
        int fromIndex = Math.min((safePage - 1) * safePerPage, all.size());
        int toIndex = Math.min(fromIndex + safePerPage, all.size());
        List<CommentResponse> pageItems = all.subList(fromIndex, toIndex);
        return Optional.of(new PagedResponse<>(pageItems, safePage, safePerPage, all.size()));
    }

    public Optional<CommentResponse> addComment(String postId, AuthContext auth, String text) {
        PostRecord record = posts.get(postId);
        if (record == null) {
            return Optional.empty();
        }
        String commentId = "comment-" + commentSequence.incrementAndGet();
        Instant now = Instant.now();
        CommentRecord comment = new CommentRecord(
                commentId,
                postId,
                auth.userId(),
                auth.displayName(),
                text,
                now,
                now
        );
        record.comments.add(comment);
        record.commentCount = record.comments.size();
        record.updatedAt = now;
        return Optional.of(comment.toResponse());
    }

    public DeleteCommentResult deleteComment(String postId, String commentId, AuthContext auth) {
        PostRecord record = posts.get(postId);
        if (record == null) {
            return DeleteCommentResult.POST_NOT_FOUND;
        }
        Optional<CommentRecord> target = record.comments.stream()
                .filter(comment -> comment.id.equals(commentId))
                .findFirst();
        if (target.isEmpty()) {
            return DeleteCommentResult.COMMENT_NOT_FOUND;
        }
        CommentRecord comment = target.get();
        boolean canDelete = comment.authorId.equals(auth.userId()) || record.ownerId.equals(auth.userId());
        if (!canDelete) {
            return DeleteCommentResult.FORBIDDEN;
        }
        record.comments.remove(comment);
        record.commentCount = record.comments.size();
        record.updatedAt = Instant.now();
        return DeleteCommentResult.SUCCESS;
    }

    private void seedData() {
        Instant now = Instant.now();
        PostRecord post1 = new PostRecord(
                "post-1001",
                "Hydrogen platform launch",
                "Kick-off imagery from the new hydrogen platform.",
                List.of("energy", "hydrogen", "launch"),
                PostVisibility.PUBLIC,
                "alice",
                "Alice Jensen",
                UUID.randomUUID().toString(),
                buildFileUrl("post-1001"),
                buildThumbnailUrl("post-1001"),
                "launch.png",
                2_048_000,
                now.minusSeconds(86_400),
                now.minusSeconds(43_200),
                2,
                42,
                new ArrayList<>()
        );
        post1.comments.add(new CommentRecord(
                "comment-5001",
                post1.id,
                "bob",
                "Bob Smith",
                "Fantastic shot!",
                now.minusSeconds(60_000),
                now.minusSeconds(60_000)
        ));
        post1.comments.add(new CommentRecord(
                "comment-5002",
                post1.id,
                "carol",
                "Carol Nguyen",
                "Looking forward to the next update.",
                now.minusSeconds(30_000),
                now.minusSeconds(30_000)
        ));
        post1.commentCount = post1.comments.size();

        PostRecord post2 = new PostRecord(
                "post-1002",
                "Subsea installation timelapse",
                "Compressed timelapse of the subsea installation.",
                List.of("subsea", "timelapse"),
                PostVisibility.UNLISTED,
                "bob",
                "Bob Smith",
                UUID.randomUUID().toString(),
                buildFileUrl("post-1002"),
                buildThumbnailUrl("post-1002"),
                "install.mp4",
                25_000_000,
                now.minusSeconds(172_800),
                now.minusSeconds(172_800),
                0,
                12,
                new ArrayList<>()
        );

        PostRecord post3 = new PostRecord(
                "post-1003",
                "Concept art: autonomous rigs",
                "Internal-only renders for next-gen rigs.",
                List.of("concept", "autonomous", "internal"),
                PostVisibility.PRIVATE,
                "carol",
                "Carol Nguyen",
                UUID.randomUUID().toString(),
                buildFileUrl("post-1003"),
                buildThumbnailUrl("post-1003"),
                "rigs.pdf",
                4_500_000,
                now.minusSeconds(259_200),
                now.minusSeconds(259_200),
                0,
                0,
                new ArrayList<>()
        );

        posts.put(post1.id, post1);
        posts.put(post2.id, post2);
        posts.put(post3.id, post3);
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(tag -> tag.replaceAll("\\s+", "-").toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildFileUrl(String fileId) {
        return "https://cdn.example.com/files/" + fileId;
    }

    private String buildThumbnailUrl(String fileId) {
        return "https://cdn.example.com/thumbnails/" + fileId + ".jpg";
    }

    private static final class PostRecord {
        private final String id;
        private String title;
        private String description;
        private List<String> tags;
        private PostVisibility visibility;
        private final String ownerId;
        private final String ownerDisplayName;
        private String fileId;
        private String fileUrl;
        private String thumbnailUrl;
        private String originalFileName;
        private long fileSize;
        private Instant createdAt;
        private Instant updatedAt;
        private long commentCount;
        private long likeCount;
        private final List<CommentRecord> comments;

        private PostRecord(
                String id,
                String title,
                String description,
                List<String> tags,
                PostVisibility visibility,
                String ownerId,
                String ownerDisplayName,
                String fileId,
                String fileUrl,
                String thumbnailUrl,
                String originalFileName,
                long fileSize,
                Instant createdAt,
                Instant updatedAt,
                long commentCount,
                long likeCount,
                List<CommentRecord> comments
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.tags = new ArrayList<>(tags);
            this.visibility = visibility;
            this.ownerId = ownerId;
            this.ownerDisplayName = ownerDisplayName;
            this.fileId = fileId;
            this.fileUrl = fileUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.commentCount = commentCount;
            this.likeCount = likeCount;
            this.comments = comments;
        }

        private boolean matchesQuery(String query) {
            String q = query.toLowerCase();
            return (title != null && title.toLowerCase().contains(q))
                    || (description != null && description.toLowerCase().contains(q))
                    || tags.stream().anyMatch(tag -> tag.contains(q));
        }

        private Instant createdAt() {
            return createdAt;
        }

        private PostSummaryResponse toSummary() {
            return new PostSummaryResponse(
                    id,
                    title,
                    description,
                    List.copyOf(tags),
                    visibility,
                    ownerId,
                    ownerDisplayName,
                    thumbnailUrl,
                    createdAt,
                    updatedAt,
                    commentCount,
                    likeCount
            );
        }

        private PostResponse toResponse() {
            List<CommentResponse> latest = comments.stream()
                    .sorted(Comparator.comparing(CommentRecord::createdAt).reversed())
                    .limit(3)
                    .map(CommentRecord::toResponse)
                    .toList();
            return new PostResponse(
                    id,
                    title,
                    description,
                    List.copyOf(tags),
                    visibility,
                    ownerId,
                    ownerDisplayName,
                    fileId,
                    fileUrl,
                    thumbnailUrl,
                    originalFileName,
                    fileSize,
                    createdAt,
                    updatedAt,
                    commentCount,
                    likeCount,
                    latest
            );
        }
    }

    private static final class CommentRecord {
        private final String id;
        private final String postId;
        private final String authorId;
        private final String authorDisplayName;
        private final String text;
        private final Instant createdAt;
        private final Instant updatedAt;

        private CommentRecord(
                String id,
                String postId,
                String authorId,
                String authorDisplayName,
                String text,
                Instant createdAt,
                Instant updatedAt
        ) {
            this.id = id;
            this.postId = postId;
            this.authorId = authorId;
            this.authorDisplayName = authorDisplayName;
            this.text = text;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        private Instant createdAt() {
            return createdAt;
        }

        private CommentResponse toResponse() {
            return new CommentResponse(
                    id,
                    postId,
                    authorId,
                    authorDisplayName,
                    text,
                    createdAt,
                    updatedAt
            );
        }
    }

    public enum DeleteCommentResult {
        SUCCESS,
        POST_NOT_FOUND,
        COMMENT_NOT_FOUND,
        FORBIDDEN
    }
}
