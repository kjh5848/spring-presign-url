package com.metacoding.spring_base64.image;

import java.time.LocalDateTime;

public class ImageResponse {

    public record DTO(
            Long id,
            String uuid,
            String fileName,
            String url,
            LocalDateTime createdAt) {
        public static DTO fromEntity(ImageEntity imageEntity) {
            return new DTO(
                    imageEntity.getId(),
                    imageEntity.getUuid(),
                    imageEntity.getFileName(),
                    imageEntity.getUrl(),
                    imageEntity.getCreatedAt());
        }
    }
}