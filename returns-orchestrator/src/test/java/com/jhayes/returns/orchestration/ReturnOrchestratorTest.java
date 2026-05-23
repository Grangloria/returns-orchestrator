package com.jhayes.returns.orchestration;

import com.jhayes.returns.client.CarrierClient;
import com.jhayes.returns.event.ReturnRequest;
import com.jhayes.returns.event.ReturnResponse;
import com.jhayes.returns.domain.service.ReturnService;
import com.jhayes.returns.repository.ManifestRepository;
import com.jhayes.returns.strategy.TriageStrategy;
import com.jhayes.returns.strategy.factory.TriageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReturnOrchestratorTest {

    @Mock
    private ManifestRepository repository;
    @Mock
    private CarrierClient carrierClient;
    @Mock
    private TriageFactory triageFactory;
    @Mock
    private ReturnService returnService;

    @InjectMocks
    private ReturnOrchestrator orchestrator;

    @Test
    void shouldProcessReturnSuccessfully() {
        // 1. GIVEN
        ReturnRequest request = new ReturnRequest(
                "HAYE-123", 1, "90210", "test@test.com", "DEFECTIVE", "ORD-1", "ORD-2026-99"
        );
        ReturnResponse mockTriageRes = new ReturnResponse("PRCL-123", "APPROVED", "http://label.com", System.currentTimeMillis());

        // SENIOR MOVE: Mock the Strategy object itself
        // Replace 'TriageStrategy' with whatever your strategy interface is named
        TriageStrategy mockStrategy = mock(TriageStrategy.class);

        when(returnService.validate(any())).thenReturn(Mono.just(request));

        // Now Mockito knows exactly what type getStrategy returns
        when(triageFactory.getStrategy(any())).thenReturn(mockStrategy);

        // And we tell the strategy how to behave when its execute method is called
        when(mockStrategy.execute(any())).thenReturn(Mono.just(mockTriageRes));

        when(carrierClient.requestLabel(any())).thenReturn(Mono.just("http://fake-label.com"));
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. WHEN & THEN
        StepVerifier.create(orchestrator.processReturn(request, "trace-id"))
                .assertNext(response -> {
                    assertEquals("PRCL-123", response.trackingId());
                    assertEquals("APPROVED", response.status());
                })
                .verifyComplete();
    }

    @Test
    void shouldProcessReturnEvenWhenCarrierIsDown() {
        // 1. GIVEN
        ReturnRequest request = new ReturnRequest("HAYE-123", 1, "90210", "CUST-MOCK-ID", "test@test.com", "DEFECTIVE", "ORD-1");
        ReturnResponse mockTriageRes = new ReturnResponse("PRCL-123", "APPROVED", null, System.currentTimeMillis());

        // Create the mock strategy explicitly
        TriageStrategy mockStrategy = mock(TriageStrategy.class);

        when(returnService.validate(any())).thenReturn(Mono.just(request));

        // Stub the factory to return the mock strategy
        when(triageFactory.getStrategy(any())).thenReturn(mockStrategy);

        // Stub the strategy itself
        when(mockStrategy.execute(any())).thenReturn(Mono.just(mockTriageRes));

        // SIMULATE FAILURE: The carrier client returns a fallback string
        when(carrierClient.requestLabel(any())).thenReturn(Mono.just("PENDING_GENERATION"));

        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // 2. WHEN & THEN
        StepVerifier.create(orchestrator.processReturn(request, "trace-id"))
                .assertNext(response -> {
                    assertEquals("PRCL-123", response.trackingId());
                    System.out.println("Handled carrier failure gracefully.");
                })
                .verifyComplete();
    }
}