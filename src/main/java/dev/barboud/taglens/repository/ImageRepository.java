package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<Image> findImageById(UUID imageId) {
        String sql = """
        SELECT id, user_id, storage_key, content_type, file_size, created_at
        FROM images
        WHERE id = ?
        """;

        List<Image> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Image image = new Image();
            image.setId(UUID.fromString(rs.getString("id")));
            image.setUserId(UUID.fromString(rs.getString("user_id")));
            image.setStorageKey(rs.getString("storage_key"));
            image.setContentType(rs.getString("content_type"));
            image.setFileSize(rs.getLong("file_size"));
            image.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return image;
        }, imageId);

        return results.stream().findFirst();
    }

}