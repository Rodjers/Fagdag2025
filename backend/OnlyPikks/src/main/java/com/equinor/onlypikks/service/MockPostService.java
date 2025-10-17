package com.equinor.onlypikks.service;

import com.equinor.onlypikks.api.model.CommentResponse;
import com.equinor.onlypikks.api.model.PagedResponse;
import com.equinor.onlypikks.api.model.PostResponse;
import com.equinor.onlypikks.api.model.PostSummaryResponse;
import com.equinor.onlypikks.api.model.PostVisibility;
import com.equinor.onlypikks.api.model.UpdatePostMetadataRequest;
import com.equinor.onlypikks.auth.AuthContext;
import com.equinor.onlypikks.repository.CommentRepository;
import com.equinor.onlypikks.repository.PostRepository;
import com.equinor.onlypikks.repository.entity.CommentEntity;
import com.equinor.onlypikks.repository.entity.PostEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MockPostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AtomicLong postSequence;
    private final AtomicLong commentSequence;

    private static final List<PostSeedTemplate> ADDITIONAL_POST_SEEDS = List.of(
            new PostSeedTemplate(
                    "Offshore sunrise capture",
                    "Dawn colors reflecting off the rig deck during inspection.",
                    List.of("offshore", "sunrise", "photography"),
                    PostVisibility.PUBLIC,
                    "offshore-sunrise",
                    "jpg",
                    3_800_000L
            ),
            new PostSeedTemplate(
                    "Drone survey flight path",
                    "Orthomosaic output from the latest offshore drone survey.",
                    List.of("drone", "survey", "mapping"),
                    PostVisibility.UNLISTED,
                    "drone-survey",
                    "mp4",
                    28_000_000L
            ),
            new PostSeedTemplate(
                    "Reservoir heatmap overlay",
                    "Thermal response from reservoir simulation iteration.",
                    List.of("reservoir", "simulation", "heatmap"),
                    PostVisibility.PUBLIC,
                    "reservoir-heatmap",
                    "png",
                    4_200_000L
            ),
            new PostSeedTemplate(
                    "Maintenance checklist briefing",
                    "Confidential checklist for platform-wide maintenance week.",
                    List.of("maintenance", "briefing"),
                    PostVisibility.PRIVATE,
                    "maintenance-checklist",
                    "pdf",
                    1_900_000L
            ),
            new PostSeedTemplate(
                    "Carbon capture module schematic",
                    "Annotated schematic for the new capture module rollout.",
                    List.of("carbon", "capture", "schematic"),
                    PostVisibility.PUBLIC,
                    "carbon-capture",
                    "png",
                    5_100_000L
            ),
            new PostSeedTemplate(
                    "Safety drill highlights",
                    "Clips from last week's emergency response drill.",
                    List.of("safety", "drill", "training"),
                    PostVisibility.UNLISTED,
                    "safety-drill",
                    "mp4",
                    32_000_000L
            ),
            new PostSeedTemplate(
                    "Weather window forecast",
                    "Synoptic forecast for the coming lifting window.",
                    List.of("weather", "forecast"),
                    PostVisibility.PUBLIC,
                    "weather-window",
                    "jpg",
                    2_600_000L
            ),
            new PostSeedTemplate(
                    "Robotics inspection notes",
                    "Field notes from ROV inspection of riser systems.",
                    List.of("rov", "inspection", "notes"),
                    PostVisibility.PRIVATE,
                    "robotics-inspection",
                    "docx",
                    1_200_000L
            ),
            new PostSeedTemplate(
                    "Well intervention blueprint",
                    "Detailed blueprint for planned well intervention sequence.",
                    List.of("well", "intervention", "blueprint"),
                    PostVisibility.PRIVATE,
                    "well-intervention",
                    "pdf",
                    2_400_000L
            ),
            new PostSeedTemplate(
                    "Pipeline corrosion scan",
                    "Laser scan differentials showing corrosion hotspots.",
                    List.of("pipeline", "corrosion", "scan"),
                    PostVisibility.UNLISTED,
                    "corrosion-scan",
                    "png",
                    6_200_000L
            ),
            new PostSeedTemplate(
                    "Remote operations dashboard",
                    "UI mockups for the revamped remote operations dashboard.",
                    List.of("ui", "dashboard", "mockup"),
                    PostVisibility.PUBLIC,
                    "remote-ops-dashboard",
                    "png",
                    3_100_000L
            ),
            new PostSeedTemplate(
                    "Subsurface model update",
                    "Volume rendering from the refreshed subsurface model.",
                    List.of("subsurface", "model", "volume"),
                    PostVisibility.PUBLIC,
                    "subsurface-model",
                    "jpg",
                    4_600_000L
            )
    );

    private static final List<OwnerProfile> ADDITIONAL_POST_OWNERS = List.of(
            new OwnerProfile("alice", "Alice Jensen"),
            new OwnerProfile("bob", "Bob Smith"),
            new OwnerProfile("carol", "Carol Nguyen"),
            new OwnerProfile("david", "David Li"),
            new OwnerProfile("elin", "Elin Aas"),
            new OwnerProfile("farah", "Farah Malik"),
            new OwnerProfile("gustav", "Gustav Eriksen"),
            new OwnerProfile("heidi", "Heidi Solberg"),
            new OwnerProfile("ivan", "Ivan Petrov"),
            new OwnerProfile("julia", "Julia Berg")
    );

    public MockPostService(PostRepository postRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        seedDataIfNecessary();
        this.postSequence = new AtomicLong(resolveHighestPostSequence());
        this.commentSequence = new AtomicLong(resolveHighestCommentSequence());
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
        List<PostSummaryResponse> filtered = postRepository.findAll().stream()
                .sorted(Comparator.comparing(PostEntity::getCreatedAt).reversed())
                .filter(post -> ownerFilter.map(owner -> post.getOwnerId().equals(owner)).orElse(true))
                .filter(post -> includePrivate || post.getVisibility() != PostVisibility.PRIVATE)
                .filter(post -> includeUnlisted || post.getVisibility() != PostVisibility.UNLISTED)
                .filter(post -> visibilityFilter.map(v -> post.getVisibility() == v).orElse(true))
                .filter(post -> query.map(q -> matchesQuery(post, q)).orElse(true))
                .map(this::toSummary)
                .toList();

        int safePage = Math.max(page, 1);
        int safePerPage = Math.max(Math.min(perPage, 100), 1);
        int fromIndex = Math.min((safePage - 1) * safePerPage, filtered.size());
        int toIndex = Math.min(fromIndex + safePerPage, filtered.size());
        List<PostSummaryResponse> pageItems = filtered.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageItems, safePage, safePerPage, filtered.size());
    }

    public Optional<PostResponse> findPost(String postId, Optional<AuthContext> auth) {
        return postRepository.findById(postId)
                .filter(post -> canAccessPost(post, auth))
                .map(this::toResponse);
    }

    @Transactional
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
        PostEntity entity = new PostEntity(
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
                0
        );
        postRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public Optional<PostResponse> replaceMedia(
            String postId,
            AuthContext auth,
            String originalFileName,
            long fileSizeBytes
    ) {
        return postRepository.findById(postId)
                .filter(post -> post.getOwnerId().equals(auth.userId()))
                .map(post -> {
                    String newFileId = UUID.randomUUID().toString();
                    post.setFileId(newFileId);
                    post.setFileUrl(buildFileUrl(newFileId));
                    post.setThumbnailUrl(buildThumbnailUrl(newFileId));
                    post.setOriginalFileName(originalFileName);
                    post.setFileSizeBytes(fileSizeBytes);
                    post.setUpdatedAt(Instant.now());
                    return toResponse(postRepository.save(post));
                });
    }

    @Transactional
    public Optional<PostResponse> updateMetadata(
            String postId,
            AuthContext auth,
            UpdatePostMetadataRequest request
    ) {
        return postRepository.findById(postId)
                .filter(post -> post.getOwnerId().equals(auth.userId()))
                .map(post -> {
                    if (StringUtils.hasText(request.title())) {
                        post.setTitle(request.title());
                    }
                    if (request.description() != null) {
                        post.setDescription(request.description());
                    }
                    if (request.tags() != null) {
                        post.setTags(sanitizeTags(request.tags()));
                    }
                    if (request.visibility() != null) {
                        post.setVisibility(request.visibility());
                    }
                    post.setUpdatedAt(Instant.now());
                    return toResponse(postRepository.save(post));
                });
    }

    @Transactional
    public boolean deletePost(String postId, AuthContext auth) {
        return postRepository.findById(postId)
                .filter(post -> post.getOwnerId().equals(auth.userId()))
                .map(post -> {
                    commentRepository.deleteByPostId(postId);
                    postRepository.deleteById(postId);
                    return true;
                })
                .orElse(false);
    }

    public boolean postExists(String postId) {
        return postRepository.existsById(postId);
    }

    public Optional<PagedResponse<CommentResponse>> listComments(
            String postId,
            int page,
            int perPage
    ) {
        if (!postRepository.existsById(postId)) {
            return Optional.empty();
        }
        List<CommentResponse> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .toList();

        int safePage = Math.max(page, 1);
        int safePerPage = Math.max(Math.min(perPage, 100), 1);
        int fromIndex = Math.min((safePage - 1) * safePerPage, all.size());
        int toIndex = Math.min(fromIndex + safePerPage, all.size());
        List<CommentResponse> pageItems = all.subList(fromIndex, toIndex);
        return Optional.of(new PagedResponse<>(pageItems, safePage, safePerPage, all.size()));
    }

    @Transactional
    public Optional<CommentResponse> addComment(String postId, AuthContext auth, String text) {
        return postRepository.findById(postId)
                .map(post -> {
                    String commentId = "comment-" + commentSequence.incrementAndGet();
                    Instant now = Instant.now();
                    CommentEntity entity = new CommentEntity(
                            commentId,
                            postId,
                            auth.userId(),
                            auth.displayName(),
                            text,
                            now,
                            now
                    );
                    commentRepository.save(entity);
                    post.setCommentCount(commentRepository.countByPostId(postId));
                    post.setUpdatedAt(now);
                    postRepository.save(post);
                    return toCommentResponse(entity);
                });
    }

    @Transactional
    public DeleteCommentResult deleteComment(String postId, String commentId, AuthContext auth) {
        if (!postRepository.existsById(postId)) {
            return DeleteCommentResult.POST_NOT_FOUND;
        }
        Optional<CommentEntity> target = commentRepository.findByIdAndPostId(commentId, postId);
        if (target.isEmpty()) {
            return DeleteCommentResult.COMMENT_NOT_FOUND;
        }
        CommentEntity comment = target.get();
        Optional<PostEntity> post = postRepository.findById(postId);
        if (post.isEmpty()) {
            return DeleteCommentResult.POST_NOT_FOUND;
        }
        PostEntity postEntity = post.get();
        boolean canDelete = comment.getAuthorId().equals(auth.userId()) || postEntity.getOwnerId().equals(auth.userId());
        if (!canDelete) {
            return DeleteCommentResult.FORBIDDEN;
        }
        commentRepository.deleteById(commentId);
        postEntity.setCommentCount(commentRepository.countByPostId(postId));
        postEntity.setUpdatedAt(Instant.now());
        postRepository.save(postEntity);
        return DeleteCommentResult.SUCCESS;
    }

    private void seedDataIfNecessary() {
        if (postRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();

        List<PostEntity> seededPosts = new ArrayList<>();

        PostEntity post1 = createSeedPost(
                "post-1001",
                "Hydrogen platform launch",
                "Kick-off imagery from the new hydrogen platform.",
                List.of("energy", "hydrogen", "launch"),
                PostVisibility.PUBLIC,
                "alice",
                "Alice Jensen",
                "launch.png",
                2_048_000L,
                now.minusSeconds(86_400),
                now.minusSeconds(43_200),
                2,
                42
        );
        PostEntity post2 = createSeedPost(
                "post-1002",
                "Subsea installation timelapse",
                "Compressed timelapse of the subsea installation.",
                List.of("subsea", "timelapse"),
                PostVisibility.UNLISTED,
                "bob",
                "Bob Smith",
                "install.mp4",
                25_000_000L,
                now.minusSeconds(172_800),
                now.minusSeconds(172_800),
                0,
                12
        );
        PostEntity post3 = createSeedPost(
                "post-1003",
                "Concept art: autonomous rigs",
                "Internal-only renders for next-gen rigs.",
                List.of("concept", "autonomous", "internal"),
                PostVisibility.PRIVATE,
                "carol",
                "Carol Nguyen",
                "rigs.pdf",
                4_500_000L,
                now.minusSeconds(259_200),
                now.minusSeconds(259_200),
                0,
                0
        );

        seededPosts.add(post1);
        seededPosts.add(post2);
        seededPosts.add(post3);

        for (int i = 0; i < 97; i++) {
            seededPosts.add(createGeneratedSeedPost(now, i));
        }

        postRepository.saveAll(seededPosts);

        commentRepository.save(new CommentEntity(
                "comment-5001",
                post1.getId(),
                "bob",
                "Bob Smith",
                "Fantastic shot!",
                now.minusSeconds(60_000),
                now.minusSeconds(60_000)
        ));
        commentRepository.save(new CommentEntity(
                "comment-5002",
                post1.getId(),
                "carol",
                "Carol Nguyen",
                "Looking forward to the next update.",
                now.minusSeconds(30_000),
                now.minusSeconds(30_000)
        ));

        post1.setCommentCount(commentRepository.countByPostId(post1.getId()));
        postRepository.save(post1);
    }

    private PostEntity createSeedPost(
            String id,
            String title,
            String description,
            List<String> tags,
            PostVisibility visibility,
            String ownerId,
            String ownerDisplayName,
            String originalFileName,
            long fileSizeBytes,
            Instant createdAt,
            Instant updatedAt,
            long commentCount,
            long likeCount
    ) {
        String fileId = UUID.randomUUID().toString();
        return new PostEntity(
                id,
                title,
                description,
                tags,
                visibility,
                ownerId,
                ownerDisplayName,
                fileId,
                buildFileUrl(fileId),
                buildThumbnailUrl(fileId),
                originalFileName,
                fileSizeBytes,
                createdAt,
                updatedAt,
                commentCount,
                likeCount
        );
    }

    private PostEntity createGeneratedSeedPost(Instant now, int index) {
        PostSeedTemplate template = ADDITIONAL_POST_SEEDS.get(index % ADDITIONAL_POST_SEEDS.size());
        OwnerProfile owner = ADDITIONAL_POST_OWNERS.get((index * 3) % ADDITIONAL_POST_OWNERS.size());

        List<String> tags = new ArrayList<>(template.tags());
        tags.add("series-" + String.format("%02d", (index % 12) + 1));
        tags.add(owner.id());

        String title = template.title() + " #" + (index + 1);
        String description = template.description() + " Segment " + ((index % 7) + 1) + ".";
        String originalFileName = template.assetSlug() + "-" + (index + 1) + "." + template.assetExtension();
        long fileSize = template.baseFileSizeBytes() + (index % 5) * 512_000L;

        Instant createdAt = now.minusSeconds(43_200L + (long) index * 7_200L);
        Instant updatedAt = createdAt.plusSeconds((index % 6) * 1_800L);
        long likeCount = 6L + (index * 9L) % 480L;

        return createSeedPost(
                String.format("post-%04d", 1100 + index),
                title,
                description,
                List.copyOf(tags),
                template.visibility(),
                owner.id(),
                owner.displayName(),
                originalFileName,
                fileSize,
                createdAt,
                updatedAt,
                0,
                likeCount
        );
    }

    private record PostSeedTemplate(
            String title,
            String description,
            List<String> tags,
            PostVisibility visibility,
            String assetSlug,
            String assetExtension,
            long baseFileSizeBytes
    ) {
    }

    private record OwnerProfile(String id, String displayName) {
    }

    private boolean matchesQuery(PostEntity post, String query) {
        String q = query.toLowerCase();
        return (post.getTitle() != null && post.getTitle().toLowerCase().contains(q))
                || (post.getDescription() != null && post.getDescription().toLowerCase().contains(q))
                || post.getTags().stream().anyMatch(tag -> tag.contains(q));
    }

    private boolean canAccessPost(PostEntity post, Optional<AuthContext> auth) {
        if (post.getVisibility() != PostVisibility.PRIVATE) {
            return true;
        }
        return auth.map(context -> context.userId().equals(post.getOwnerId())).orElse(false);
    }

    private PostSummaryResponse toSummary(PostEntity post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                List.copyOf(post.getTags()),
                post.getVisibility(),
                post.getOwnerId(),
                post.getOwnerDisplayName(),
                post.getThumbnailUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getCommentCount(),
                post.getLikeCount()
        );
    }

    private PostResponse toResponse(PostEntity post) {
        List<CommentResponse> latest = commentRepository.findTop3ByPostIdOrderByCreatedAtDesc(post.getId()).stream()
                .map(this::toCommentResponse)
                .toList();
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                List.copyOf(post.getTags()),
                post.getVisibility(),
                post.getOwnerId(),
                post.getOwnerDisplayName(),
                post.getFileId(),
                post.getFileUrl(),
                post.getThumbnailUrl(),
                post.getOriginalFileName(),
                post.getFileSizeBytes(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getCommentCount(),
                post.getLikeCount(),
                latest
        );
    }

    private CommentResponse toCommentResponse(CommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getAuthorDisplayName(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
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

    private long resolveHighestPostSequence() {
        return postRepository.findAll().stream()
                .map(PostEntity::getId)
                .mapToLong(MockPostService::extractPostNumeric)
                .max()
                .orElse(1000L);
    }

    private long resolveHighestCommentSequence() {
        return commentRepository.findAll().stream()
                .map(CommentEntity::getId)
                .mapToLong(MockPostService::extractCommentNumeric)
                .max()
                .orElse(5000L);
    }

    private static long extractPostNumeric(String id) {
        if (id == null) {
            return 0L;
        }
        return extractNumericSuffix(id, "post-");
    }

    private static long extractCommentNumeric(String id) {
        if (id == null) {
            return 0L;
        }
        return extractNumericSuffix(id, "comment-");
    }

    private static long extractNumericSuffix(String id, String prefix) {
        if (!id.startsWith(prefix)) {
            return 0L;
        }
        try {
            return Long.parseLong(id.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public enum DeleteCommentResult {
        SUCCESS,
        POST_NOT_FOUND,
        COMMENT_NOT_FOUND,
        FORBIDDEN
    }
}
