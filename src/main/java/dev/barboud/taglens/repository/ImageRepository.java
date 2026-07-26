package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveImage(Image image) {

        jdbcTemplate.update("""
            INSERT INTO images
            (id, user_id, storage_key, content_type, file_size)
            VALUES (?, ?, ?, ?, ?)
            """,

                image.getId(),
                image.getUserId(),
                image.getStorageKey(),
                image.getContentType(),
                image.getFileSize()
        );
    }
}