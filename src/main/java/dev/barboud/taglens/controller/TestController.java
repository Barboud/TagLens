package dev.barboud.taglens.controller;

import dev.barboud.taglens.service.TaggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final TaggingService taggingService;

    @GetMapping("/ai")
    public String testAi() {
        return taggingService.testConnection();
    }
}