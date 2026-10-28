package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.image.ImageResponseDto;
import dev.barboud.taglens.model.Image;
import dev.barboud.taglens.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Images", description = "Upload, view, delete and search images (AI tags are generated after upload)")
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;


    @Operation(summary = "Upload an image",
            description = "Uploads an image file to storage. AI tags are generated asynchronously afterwards. Requires login.")
    @ApiResponse(responseCode = "204", description = "Image uploaded")
    @ApiResponse(responseCode = "400", description = "File is missing or is not an image")
    @ApiResponse(responseCode = "401", description = "Not logged in or session expired")
    @ApiResponse(responseCode = "413", description = "File is too large")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadImage(
            @Parameter(description = "Image file to upload")
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true)
            @CookieValue("session_id") UUID sessionId
    ) throws Exception {

        imageService.uploadImage(file, sessionId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get an image link", description = "Returns a URL for viewing the image. Requires login.")
    @ApiResponse(responseCode = "200", description = "Link returned, e.g. {\"url\": \"https://...\"}")
    @ApiResponse(responseCode = "401", description = "Not logged in or session expired")
    @ApiResponse(responseCode = "404", description = "Image not found")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getImageLink(
            @Parameter(description = "Image ID")
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @CookieValue("session_id") UUID sessionId
    ) {
        String url = imageService.getImageLink(id, sessionId);
        return ResponseEntity.ok(Map.of("url", url));
    }


    @Operation(summary = "Delete an image", description = "Deletes one of your images. Requires login.")
    @ApiResponse(responseCode = "204", description = "Image deleted")
    @ApiResponse(responseCode = "401", description = "Not logged in or session expired")
    @ApiResponse(responseCode = "404", description = "Image not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Image ID")
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @CookieValue("session_id") UUID sessionId) {
        imageService.deleteImage(id, sessionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List my images", description = "Returns all images uploaded by the logged-in user.")
    @ApiResponse(responseCode = "200", description = "List of your images")
    @ApiResponse(responseCode = "401", description = "Not logged in or session expired")
    @GetMapping("/my")
    public ResponseEntity<List<ImageResponseDto>> getMyImages(
            @Parameter(hidden = true)
            @CookieValue("session_id") UUID sessionId
    ) {
        List<ImageResponseDto> imageResponseDto = imageService.getMyImages(sessionId);
        return ResponseEntity.ok(imageResponseDto);
    }

    @Operation(summary = "Browse or search images",
            description = "Public. Without `search` it returns the 50 most recent images; with `search` it does a free-text search on the AI tags.")
    @ApiResponse(responseCode = "200", description = "List of matching images")
    @GetMapping
    public List<ImageResponseDto> browseImages(
            @Parameter(description = "Free-text search on tags, e.g. `dog`")
            @RequestParam(value = "search", required = false) String search
    ) {
        if (search != null && !search.isBlank()) {
            return imageService.searchImages(search);
        }
        return imageService.getRecentImages();
    }

}
