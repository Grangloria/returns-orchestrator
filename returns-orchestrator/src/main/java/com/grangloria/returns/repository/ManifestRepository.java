package com.grangloria.returns.repository;

import com.grangloria.returns.entity.ReturnManifest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ManifestRepository extends ReactiveCrudRepository<ReturnManifest, UUID> {

    Mono<ReturnManifest> findByTrackingId(String trackingId);
}