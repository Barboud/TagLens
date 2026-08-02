package dev.barboud.taglens.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    private UUID id;
    private UUID userId;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private Instant createdAt;
    private Map<String, Object> tags;
}
