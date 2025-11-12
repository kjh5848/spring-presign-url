package com.metacoding.spring_base64.image;

public class ImageRequest {

    public record UploadDTO(
        String fileName,
        String fileData
    ){}
    
}
