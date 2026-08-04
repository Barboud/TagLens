package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.image.ImageResponseDto;
import dev.barboud.taglens.model.Image;
import dev.barboud.taglens.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;


    @PostMapping("/upload")
    public ResponseEntity<Void> uploadImage(
            @RequestParam("file") MultipartFile file,
            @CookieValue("session_id") UUID sessionId
    ) throws Exception {

        imageService.uploadImage(file, sessionId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getImageLink(
            @PathVariable UUID id,
            @CookieValue("session_id") UUID sessionId
    ) {
        String url = imageService.getImageLink(id, sessionId);
        return ResponseEntity.ok(Map.of("url", url));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id,
            @CookieValue("session_id") UUID sessionId) {
        imageService.deleteImage(id, sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<ImageResponseDto>> getMyImages(
            @CookieValue("session_id") UUID sessionId
    ) {
        List<ImageResponseDto> imageResponseDto = imageService.getMyImages(sessionId);
        return ResponseEntity.ok(imageResponseDto);
    }

    @GetMapping
    public List<ImageResponseDto> browseImages(
            @RequestParam(value = "search", required = false) String search
    ) {
        if (search != null && !search.isBlank()) {
            return imageService.searchImages(search);
        }
        return imageService.getRecentImages();
    }

}
