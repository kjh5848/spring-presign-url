package com.metacoding.spring_presign_url.image;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/presigned")
    public PresignedUrlResponse presign(@RequestBody ImageRequest.PresignRequest presignRequest) {
        return imageService.generatePresignedUrl(presignRequest);
    }

    @GetMapping("/list")
    public List<ImageResponse.DTO> getAllImages() {
        return imageService.listAll();
    }

    @GetMapping("/{id}")
    public ImageResponse.DTO getImageDetail(@PathVariable Long id) {
        return imageService.findById(id);
    }

    @GetMapping("/complete")
    public ImageResponse.DTO complete(@RequestParam String key) {
        return imageService.checkAndSave(key);
    }

    public static record PresignedUrlResponse(String key, String presignedUrl) {
    }
}
