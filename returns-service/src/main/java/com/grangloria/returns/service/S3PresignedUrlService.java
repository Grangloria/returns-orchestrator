package com.grangloria.returns.service;

import com.grangloria.returns.dto.request.PresignedUrlRequest;
import com.grangloria.returns.dto.response.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private static final Duration URL_EXPIRATION = Duration.ofMinutes(15);

    @Value("${aws.s3.bucket:grangloria-return-photos}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    public Mono<PresignedUrlResponse> generatePresignedPutUrl(PresignedUrlRequest request) {
        return Mono.fromCallable(() -> {
            String s3Key = String.format("returns/%s/%s-%s",
                    request.orderId(),
                    UUID.randomUUID().toString().substring(0, 8),
                    request.fileName());

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(request.contentType())
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(URL_EXPIRATION)
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toExternalForm();

            log.info("[S3-PRESIGNER] Generated presigned PUT URL for key [{}]", s3Key);

            return new PresignedUrlResponse(uploadUrl, s3Key, URL_EXPIRATION.toSeconds());
        });
    }
}