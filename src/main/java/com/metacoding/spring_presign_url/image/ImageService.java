package com.metacoding.spring_presign_url.image;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    // S3에 업로드하기 위한 'Presigned URL 생성 전용' 객체
    // - 클라이언트(React)가 직접 S3에 PUT 업로드할 수 있도록
    // 제한된 권한이 담긴 URL을 만들어주는 역할
    private final S3Presigner presigner;

    // S3에 직접 접근하는 클라이언트
    // - 파일 존재 여부(headObject), 삭제, 조회 등 서버에서 S3와 직접 통신할 때 사용
    private final S3Client s3Client;

    private final ImageRepository imageRepository;

    /**
     * original/{uuid}.ext 업로드 이후
     * Lambda가 resized/{uuid}.jpg를 만들 때까지 최대 5초간 S3를 폴링한 뒤
     * 두 파일이 모두 존재하면 DB에 저장하는 로직
     */
    public ImageResponse.DTO checkAndSave(ImageRequest.completeRequest reqDTO) {

        String originalKey = reqDTO.key();

        // original/{uuid}.ext → uuid 추출
        String uuid = originalKey.replace("original/", "").split("\\.")[0];

        // resized/{uuid}.jpg 경로 구성
        String resizedKey = "resized/" + uuid + ".jpg";

        // AWS 정식 URL 구성
        String originalUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + originalKey;
        String resizedUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + resizedKey;

        // 5초 대기 후 s3 api 요청
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }
        ImageEntity entity = ImageEntity.builder()
                .uuid(uuid)
                .fileName(reqDTO.fileName())
                .originalUrl(originalUrl)
                .resizedUrl(resizedUrl)
                .createdAt(LocalDateTime.now())
                .build();

        imageRepository.save(entity);
        return ImageResponse.DTO.fromEntity(entity);
    }

    public ImageResponse.PresignedUrlResponse generatePresignedUrl(ImageRequest.PresignRequest reqDTO) {

        // 1. UUID 생성
        String uuid = java.util.UUID.randomUUID().toString();

        // 2. 파일 확장자 추출
        String ext = reqDTO.fileName().substring(reqDTO.fileName().lastIndexOf('.') + 1);

        // 3. S3에 저장될 key 생성
        String key = "original/" + uuid + "." + ext;

        // 4. Presigned URL 생성
        // key 업로드될 S3의 key (예: original/uuid.png)
        // contentType 파일 타입 (image/png 등)
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(reqDTO.contentType())
                .build();

        // Presigned URL 유효기간 (15분)
        PresignedPutObjectRequest presignedRequest = presigner
                .presignPutObject(builder -> builder.signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(objectRequest));

        return new ImageResponse.PresignedUrlResponse(key, presignedRequest.url().toString());
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
