package com.metacoding.spring_presign_url.image;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Presigner presigner;

    //
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    private final ImageRepository imageRepository;

    /**
     * original/{uuid}.ext 업로드 이후
     * Lambda가 resized/{uuid}.jpg를 만들 때까지 최대 5초간 S3를 폴링한 뒤
     * 두 파일이 모두 존재하면 DB에 저장하는 로직
     */
    public ImageResponse.DTO checkAndSave(String originalKey) {

        // original/{uuid}.ext → uuid 추출
        String uuid = originalKey.replace("original/", "").split("\\.")[0];

        // resized/{uuid}.jpg 경로 구성
        String resizedKey = "resized/" + uuid + ".jpg";

        // AWS 정식 URL 구성
        String originalUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + originalKey;
        String resizedUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + resizedKey;

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

        // === 1번만 체크 ===
        boolean originalExists = exists(originalKey); // 원본 파일 존재 여부
        boolean resizedExists = exists(resizedKey); // 리사이즈 파일 존재 여부

        // 두 파일 모두 존재하면 → DB 저장 후 응답 반환
        if (originalExists && resizedExists) {
            ImageEntity entity = ImageEntity.builder()
                    .uuid(uuid)
                    .fileName(uuid)
                    .originalUrl(originalUrl)
                    .resizedUrl(resizedUrl)
                    .createdAt(LocalDateTime.now())
                    .build();

            imageRepository.save(entity);
            return ImageResponse.DTO.fromEntity(entity);
        }

        // 5초 안에 Lambda가 파일을 생성하지 못한 경우
        throw new RuntimeException("5초 안에 original 또는 resized 파일이 생성되지 않았습니다.");
    }

    /**
     * S3 key 존재 여부 확인
     * headObject()는 key가 없으면 NoSuchKeyException 발생 → try/catch 필수
     */
    private boolean exists(String key) {
        try {
            s3Client.headObject(builder -> builder.bucket(bucket).key(key));
            return true; // 파일 존재
        } catch (NoSuchKeyException e) {
            return false; // 파일 없음
        } catch (Exception e) {
            return false; // 기타 오류도 '존재하지 않음'으로 처리
        }
    }

    public ImageController.PresignedUrlResponse generatePresignedUrl(ImageRequest.PresignRequest req) {

        // 1. UUID 생성
        String uuid = java.util.UUID.randomUUID().toString();

        // 2. 파일 확장자 추출
        String ext = req.fileName().substring(req.fileName().lastIndexOf('.') + 1);

        // 3. S3에 저장될 key 생성
        String key = "original/" + uuid + "." + ext;

        // 4. Presigned URL 생성
        // key 업로드될 S3의 key (예: original/uuid.png)
        // contentType 파일 타입 (image/png 등)
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(req.contentType())
                .build();

        // Presigned URL 유효기간 (15분)
        PresignedPutObjectRequest presignedRequest = presigner
                .presignPutObject(builder -> builder.signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(objectRequest));

        return new ImageController.PresignedUrlResponse(key, presignedRequest.url().toString());
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
