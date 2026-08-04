package dev.barboud.taglens.service;

import dev.barboud.taglens.dto.image.ImageResponseDto;
import dev.barboud.taglens.model.Image;
import dev.barboud.taglens.model.Session;
import dev.barboud.taglens.repository.ImageRepository;
import dev.barboud.taglens.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final SessionRepository sessionRepository;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final TaggingService taggingService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public void uploadImage(
            MultipartFile file,
            UUID sessionId
    ) throws Exception {

        // 1. Check if session is valid
        Session session = sessionRepository
                .findValidSession(sessionId)
                .orElseThrow(() ->
                        new RuntimeException("Unauthorized"));

        // 2. Get user ID
        UUID userId = session.getUserId();

        // 3. Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                    "File size must not exceed 10 MB"
            );
        }

        // 4. Check file type
        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // 5. Upload image to Object Storage
        String storageKey =
                storageService.upload(file);

        // 6. Create Image object
        Image image = new Image();

        image.setId(UUID.randomUUID());
        image.setUserId(userId);
        image.setStorageKey(storageKey);
        image.setContentType(file.getContentType());
        image.setFileSize(file.getSize());

        // 7. Save image metadata in Database
        imageRepository.saveImage(image);

        // 8. Generate Ai Tags
        taggingService.tagImageAsync(image.getId(), file.getBytes(), file.getContentType());

    }


    public String getImageLink(UUID imageId, UUID sessionId) {
        Session session = sessionRepository.findValidSession(sessionId)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Image image = imageRepository.findImageById(imageId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (!image.getUserId().equals(session.getUserId())) {
            throw new RuntimeException("Not found");
        }

        return storageService.getLink(image.getStorageKey(), 15);
    }

    public void deleteImage(UUID imageId, UUID sessionId) {
        Session session = sessionRepository.findValidSession(sessionId)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Image image = imageRepository.findImageById(imageId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        if (!image.getUserId().equals(session.getUserId())) {
            throw new RuntimeException("Not found");
        }

        storageService.delete(image.getStorageKey());
        imageRepository.deleteImage(imageId);
    }

    public List<ImageResponseDto> getMyImages(UUID sessionId){
        Session session = sessionRepository.findValidSession(sessionId)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        List<Image> images = imageRepository.findImagesByUserId(session.getUserId());

        return images.stream()
                .map(image -> new ImageResponseDto(
                        image.getId(),
                        image.getContentType(),
                        image.getFileSize(),
                        image.getStorageKey(),
                        image.getTags()
                ))
                .toList();

    }
}
