package com.equinor.onlypikks.repository;

import com.equinor.onlypikks.repository.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, String> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(String postId);

    List<CommentEntity> findTop3ByPostIdOrderByCreatedAtDesc(String postId);

    long countByPostId(String postId);

    Optional<CommentEntity> findByIdAndPostId(String id, String postId);

    void deleteByPostId(String postId);
}
