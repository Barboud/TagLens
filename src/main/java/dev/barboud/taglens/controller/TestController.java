package dev.barboud.taglens.controller;

import dev.barboud.taglens.service.TaggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final TaggingService taggingService;

    @PostMapping("/ai-image")
    public JsonNode testAiImage(@RequestParam("file") MultipartFile file) throws Exception {
        return taggingService.generateTags(file.getBytes(), file.getContentType());
    }
}