package com.grangloria.returns.strategy.factory;

import com.grangloria.returns.event.ReturnRequest;
import com.grangloria.returns.strategy.TriageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TriageFactory {

    // This is filled by Spring, the scanner scans the project looking for classes marked @Compnent, @Service, or @Bean
    // Spring finds the all the classes that implement the TriageStrategy interface.
    // Spring looks at the TriageFactory which is annotated with @RequiredArgsConstructor asking for a list
    // of TriageStrategies. The annotation @RequiredArgsConstructor follows a very specific logic.
    // It looks at your class and generates a constructor for: Any field marked final and Any field marked with the @NonNull annotation.
    private final List<TriageStrategy> strategies;

    public TriageStrategy getStrategy(ReturnRequest request) {
        return strategies.stream()
                .filter(s -> s.isApplicable(request))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for request"));
    }
}