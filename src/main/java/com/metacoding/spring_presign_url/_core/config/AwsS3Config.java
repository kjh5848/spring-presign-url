package com.metacoding.spring_presign_url._core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정 파일
 * - AWS 자격증명(AccessKey, SecretKey)을 application.properties에서 주입받는다.
 * - S3Client와 S3Presigner를 Bean으로 등록한다.
 * - Presigned URL 생성은 S3Presigner가 수행한다.
 */
@Configuration
public class AwsS3Config {

    // application.properties에서 환경변수 주입
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region}")
    private String region;

    /**
     * S3 접근을 위한 인증 정보 생성 (Access Key / Secret Key)
     */
    private StaticCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * S3 일반 클라이언트 Bean
     * - 파일 삭제, 조회 등 일반적인 S3 작업에 사용
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.of(region))
                .build();
    }

    /**
     * Presigned URL 생성 전용 Presigner Bean
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.of(region))
                .build();
    }
}