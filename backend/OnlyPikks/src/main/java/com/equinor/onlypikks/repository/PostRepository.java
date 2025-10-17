package com.equinor.onlypikks.repository;

import com.equinor.onlypikks.repository.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostEntity, String> {
}
