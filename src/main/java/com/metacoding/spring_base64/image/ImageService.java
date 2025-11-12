package com.metacoding.spring_base64.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageResponse.DTO saveImage(ImageRequest.UploadDTO uploadDTO) throws IOException {

        String uuid = UUID.randomUUID().toString();
    
        // 프론트에서 전달한 원본 파일명에서 확장자를 추출
        int dotIndex = uploadDTO.fileName().lastIndexOf('.');
        String fileExtension= uploadDTO.fileName().substring(dotIndex + 1).trim().toLowerCase();

        // 최종 저장 파일명은 uuid.확장자 형태
        String savedFileName = uuid + "." + fileExtension;

        // Base64 → byte[]
        byte[] fileBytes = Base64.getDecoder().decode(uploadDTO.fileData());

        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(savedFileName);
        Files.write(filePath, fileBytes);

        String publicUrl = "/uploads/" + savedFileName;

        ImageEntity entity = ImageEntity.builder()
                .uuid(uuid)
                .fileName(savedFileName)
                .url(publicUrl)
                .createdAt(LocalDateTime.now())
                .build();

        imageRepository.save(entity);

        return ImageResponse.DTO.fromEntity(entity);
    }

    public List<ImageResponse.DTO> listAll() {
        return imageRepository.findAll()
                .stream()
                .map(ImageResponse.DTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ImageResponse.DTO findById(Long id) {
        ImageEntity entity = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
        return ImageResponse.DTO.fromEntity(entity);
    }

}
