package com.metacoding.spring_presign_url.image;

import java.time.LocalDateTime;

public class ImageResponse {
    public record DTO(
            Long id,
            String uuid,
            String originalUrl,
            String resizedUrl,
            String fileName,
            LocalDateTime createdAt) {
        public static DTO fromEntity(ImageEntity imageEntity) {
            return new DTO(
                    imageEntity.getId(),
                    imageEntity.getUuid(),
                    imageEntity.getOriginalUrl(),
                    imageEntity.getResizedUrl(),
                    imageEntity.getFileName(),
                    imageEntity.getCreatedAt());
        }
    }
    public static record PresignedUrlResponse(String key, String presignedUrl) {
    }
}