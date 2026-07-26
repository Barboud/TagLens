package dev.barboud.taglens.controller;


import dev.barboud.taglens.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;


    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) throws Exception {
        String storageKey = imageService.uploadImage(file);
        return ResponseEntity.ok(Map.of("storage_key", storageKey));
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
