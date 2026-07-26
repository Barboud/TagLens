package dev.barboud.taglens.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Image {
    private UUID id;
    private UUID userId;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private Instant createdAt;
}
