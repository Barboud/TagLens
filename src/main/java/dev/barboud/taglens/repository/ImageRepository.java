package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    private Map<String, Object> parseTags(String tagsJson) {
        if (tagsJson == null) return null;
        return objectMapper.readValue(tagsJson, new TypeReference<Map<String, Object>>() {});
    }

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
        SELECT id, user_id, storage_key, content_type, file_size, created_at, tags
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
            image.setTags(parseTags(rs.getString("tags")));
            return image;
        }, imageId);

        return results.stream().findFirst();
    }

    public void deleteImage(UUID imageId) {
        String sql = """
        DELETE FROM images
        WHERE id = ?
        """;
        jdbcTemplate.update(sql, imageId);
    }

    public List<Image> findImagesByUserId(UUID userId) {
        String sql = """
        SELECT id, user_id, storage_key, content_type, file_size, created_at, tags
        FROM images
        WHERE user_id = ?
        """;
        List<Image> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Image image = new Image();
                    image.setId(UUID.fromString(rs.getString("id")));
                    image.setUserId(UUID.fromString(rs.getString("user_id")));
                    image.setStorageKey(rs.getString("storage_key"));
                    image.setContentType(rs.getString("content_type"));
                    image.setFileSize(rs.getLong("file_size"));
                    image.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    image.setTags(parseTags(rs.getString("tags")));
                    return image;
                }, userId);

        return results;
    }

    public void updateImageTags(UUID imageId, Map<String, Object> tags) {
        String json = objectMapper.writeValueAsString(tags);
        jdbcTemplate.update(
                "UPDATE images SET tags = ?::jsonb WHERE id = ?",
                json, imageId
        );
    }

    public List<Image> findRecentImages() {
        String sql = """
        SELECT id, user_id, storage_key, content_type, file_size, created_at, tags
        FROM images
        ORDER BY created_at DESC
        LIMIT 50
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Image image = new Image();
            image.setId(UUID.fromString(rs.getString("id")));
            image.setUserId(UUID.fromString(rs.getString("user_id")));
            image.setStorageKey(rs.getString("storage_key"));
            image.setContentType(rs.getString("content_type"));
            image.setFileSize(rs.getLong("file_size"));
            image.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            image.setTags(parseTags(rs.getString("tags")));
            return image;
        });
    }

    public List<Image> findImageBySearchQuery(String search) {
        String sql = """
        SELECT id, user_id, storage_key, content_type, file_size, created_at, tags
        FROM images
        WHERE tags::text ILIKE ?
        ORDER BY created_at DESC
        LIMIT 50
        """;

        String pattern = "%" + search + "%";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Image image = new Image();
            image.setId(UUID.fromString(rs.getString("id")));
            image.setUserId(UUID.fromString(rs.getString("user_id")));
            image.setStorageKey(rs.getString("storage_key"));
            image.setContentType(rs.getString("content_type"));
            image.setFileSize(rs.getLong("file_size"));
            image.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            image.setTags(parseTags(rs.getString("tags")));
            return image;
        }, pattern);
    }

}