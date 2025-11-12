package com.metacoding.spring_base64.image;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ImageResponse.DTO uploadImage(@RequestBody ImageRequest.UploadDTO uploadDTO) throws IOException {
        return imageService.saveImage(uploadDTO);
    }

    @GetMapping("/list")
    public List<ImageResponse.DTO> getAllImages() {
        return imageService.listAll();
    }

    @GetMapping("/{id}")
    public ImageResponse.DTO getImageDetail(@PathVariable Long id) {
        return imageService.findById(id);
    }
}
