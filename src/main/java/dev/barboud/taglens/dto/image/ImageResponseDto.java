package dev.barboud.taglens.dto.image;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ImageResponseDto {
    UUID id;
    String contentType;
    Long fileSize;
    private String storageKey;
}
