package com.grangloria.returns.controller;

import com.grangloria.returns.dto.request.PresignedUrlRequest;
import com.grangloria.returns.dto.response.PresignedUrlResponse;
import com.grangloria.returns.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/returns/evidence")
@RequiredArgsConstructor
public class PresignedUrlController {

    private final S3PresignedUrlService s3PresignedUrlService;

    @PostMapping("/presigned-url")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PresignedUrlResponse> createPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        return s3PresignedUrlService.generatePresignedPutUrl(request);
    }
}