package com.grangloria.returns;

import com.grangloria.returns.dto.request.ReturnRequest;
import com.grangloria.returns.dto.response.ReturnResponse;
import com.grangloria.returns.entity.ReturnManifest;
import com.grangloria.returns.entity.ReturnState;
import com.returns.common.event.ReturnInitiatedEvent;
import com.returns.common.event.ReturnLabelReadyEvent;
import com.grangloria.returns.exception.ReturnNotFoundException;
import com.grangloria.returns.messaging.publisher.ReturnLabelReadyEventPublisher;
import com.grangloria.returns.repository.ManifestRepository;
import com.grangloria.returns.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ManifestRepository repository;

    @Mock
    private ReturnLabelReadyEventPublisher labelReadyPublisher;

    @InjectMocks
    private ReturnService returnService;

    private ReturnRequest sampleRequest;
    private ReturnManifest sampleManifest;

    @BeforeEach
    void setUp() {
        sampleRequest = new ReturnRequest(
                "ORD-1001",
                "SKU-8899",
                "Wireless Headphones",
                "customer@example.com",
                1,
                "90210",
                "DEFECTIVE"
        );

        sampleManifest = ReturnManifest.builder()
                .orderId("ORD-1001")
                .sku("SKU-8899")
                .item("Wireless Headphones")
                .quantity(1)
                .customerEmail("customer@example.com")
                .zipCode("90210")
                .reason("DEFECTIVE")
                .status(ReturnState.INITIATED)
                .createdAt(LocalDateTime.now())
                .isNewEntity(true)
                .build();
    }

    // ==========================================
    // 1. processReturn Tests
    // ==========================================

    @Test
    @DisplayName("processReturn - Should persist manifest, publish ReturnInitiatedEvent, and return valid response")
    void processReturn_Success() {
        when(repository.save(any(ReturnManifest.class))).thenReturn(Mono.just(sampleManifest));

        Mono<ReturnResponse> responseMono = returnService.processReturn(sampleRequest);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("ORD-1001", response.orderId());
                    assertEquals("INITIATED", response.status());
                })
                .verifyComplete();

        verify(repository, times(1)).save(any(ReturnManifest.class));
    }

    @Test
    @DisplayName("processReturn - Should propagate error when repository save fails")
    void processReturn_DatabaseError() {
        when(repository.save(any(ReturnManifest.class)))
                .thenReturn(Mono.error(new RuntimeException("R2DBC Connection Timeout")));

        Mono<ReturnResponse> responseMono = returnService.processReturn(sampleRequest);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("R2DBC Connection Timeout"))
                .verify();

    }

    // ==========================================
    // 2. handleLabelGenerated Tests
    // ==========================================

    @Test
    @DisplayName("handleLabelGenerated - Should transition status to LABEL_READY, save manifest, and publish event")
    void handleLabelGenerated_Success() {
        ReturnLabelReadyEvent incomingEvent = new ReturnLabelReadyEvent(
                "RET-ORD-1001",
                "ORD-1001",
                "customer@example.com",
                "TRK-123456",
                "https://shipping.com/label123.pdf",
                "MOCK-CARRIER",
                "SKU-8899",
                1,
                Instant.now()
        );

        ReturnManifest updatedManifest = ReturnManifest.builder()
                .orderId("ORD-1001")
                .sku("SKU-8899")
                .quantity(1)
                .customerEmail("customer@example.com")
                .status(ReturnState.LABEL_READY)
                .labelUrl("https://shipping.com/label123.pdf")
                .build();

        when(repository.findByOrderId("ORD-1001")).thenReturn(Mono.just(sampleManifest));
        when(repository.save(any(ReturnManifest.class))).thenReturn(Mono.just(updatedManifest));

        Mono<Void> resultMono = returnService.handleLabelGenerated(incomingEvent);

        StepVerifier.create(resultMono)
                .verifyComplete();

        verify(repository, times(1)).findByOrderId("ORD-1001");
        verify(repository, times(1)).save(any(ReturnManifest.class));
        verify(labelReadyPublisher, times(1)).publishReturnLabelReadyEvent(any(ReturnLabelReadyEvent.class));
    }

    @Test
    @DisplayName("handleLabelGenerated - Should throw ReturnNotFoundException when order does not exist")
    void handleLabelGenerated_NotFound() {
        ReturnLabelReadyEvent incomingEvent = new ReturnLabelReadyEvent(
                "RET-ORD-9999",
                "ORD-9999",
                "customer@example.com",
                "TRK-123456",
                "https://shipping.com/label123.pdf",
                "MOCK-CARRIER",
                "SKU-8899",
                1,
                Instant.now()
        );

        when(repository.findByOrderId("ORD-9999")).thenReturn(Mono.empty());

        Mono<Void> resultMono = returnService.handleLabelGenerated(incomingEvent);

        StepVerifier.create(resultMono)
                .expectError(ReturnNotFoundException.class)
                .verify();

        verify(repository, times(1)).findByOrderId("ORD-9999");
        verify(repository, never()).save(any());
        verifyNoInteractions(labelReadyPublisher);
    }

    @Test
    @DisplayName("handleLabelGenerated - Should throw IllegalStateException on invalid state transition")
    void handleLabelGenerated_InvalidStateTransition() {
        ReturnLabelReadyEvent incomingEvent = new ReturnLabelReadyEvent(
                "RET-ORD-1001",
                "ORD-1001",
                "customer@example.com",
                "TRK-123456",
                "https://shipping.com/label123.pdf",
                "MOCK-CARRIER",
                "SKU-8899",
                1,
                Instant.now()
        );

        sampleManifest.setStatus(ReturnState.COMPLETED);

        when(repository.findByOrderId("ORD-1001")).thenReturn(Mono.just(sampleManifest));

        Mono<Void> resultMono = returnService.handleLabelGenerated(incomingEvent);

        StepVerifier.create(resultMono)
                .expectError(IllegalStateException.class)
                .verify();

        verify(repository, never()).save(any());
        verifyNoInteractions(labelReadyPublisher);
    }

    // ==========================================
    // 3. getReturnStatus Tests
    // ==========================================

    @Test
    @DisplayName("getReturnStatus - Should return manifest when order ID exists")
    void getReturnStatus_Success() {
        when(repository.findByOrderId("ORD-1001")).thenReturn(Mono.just(sampleManifest));

        Mono<ReturnManifest> manifestMono = returnService.getReturnStatus("ORD-1001");

        StepVerifier.create(manifestMono)
                .assertNext(manifest -> {
                    assertEquals("ORD-1001", manifest.getOrderId());
                    assertEquals(ReturnState.INITIATED, manifest.getStatus());
                })
                .verifyComplete();

        verify(repository, times(1)).findByOrderId("ORD-1001");
    }

    @Test
    @DisplayName("getReturnStatus - Should throw ReturnNotFoundException when order ID does not exist")
    void getReturnStatus_NotFound() {
        when(repository.findByOrderId("ORD-9999")).thenReturn(Mono.empty());

        Mono<ReturnManifest> manifestMono = returnService.getReturnStatus("ORD-9999");

        StepVerifier.create(manifestMono)
                .expectError(ReturnNotFoundException.class)
                .verify();

        verify(repository, times(1)).findByOrderId("ORD-9999");
    }
}