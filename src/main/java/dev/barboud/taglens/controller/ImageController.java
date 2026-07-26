package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.image.ImageResponseDto;
import dev.barboud.taglens.model.Image;
import dev.barboud.taglens.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;


    @PostMapping("/upload")
    public ResponseEntity<ImageResponseDto> uploadImage(
            @RequestParam("file") MultipartFile file,
            @CookieValue("session_id") UUID sessionId
    ) throws Exception {

        ImageResponseDto imageResponseDto = imageService.uploadImage(file, sessionId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(imageResponseDto);
    }

//    @GetMapping("/link")
//    public ResponseEntity<Map<String, String>> getImageLink(@RequestParam String key) {
//        return ResponseEntity.ok(Map.of("url", imageService.getImageLink(key, 15)));
//    }
//
//    @DeleteMapping
//    public ResponseEntity<Void> deleteImage(@RequestParam String key) {
//        imageService.deleteImage(key);
//        return ResponseEntity.noContent().build();
//    }

}
