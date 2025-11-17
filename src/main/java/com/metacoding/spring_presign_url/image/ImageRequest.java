package com.metacoding.spring_presign_url.image;

public class ImageRequest {
    public record PresignRequest(
            String fileName,
            String contentType) {
    }

}
