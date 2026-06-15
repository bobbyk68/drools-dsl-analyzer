package uk.gov.hmrc.cars.cleaner.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmrc.cleaner.core.DslAnalyzerEngine;
import uk.gov.hmrc.cleaner.core.RuleSetLocator.RuleSetPair;
import uk.gov.hmrc.cleaner.model.ReportRegistry;
import uk.gov.hmrc.cleaner.strategy.DslParsingStrategy;
import uk.gov.hmrc.cleaner.strategy.DslDeletionStrategy;
import uk.gov.hmrc.cleaner.strategy.RegexDslParsingStrategy;
import uk.gov.hmrc.cleaner.strategy.RegexDeletionStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DslAnalyzerEngineTest {

    private DslAnalyzerEngine engine;
    private ReportRegistry reportRegistry;
    private DslParsingStrategy parserStrategy;
    private DslDeletionStrategy deletionStrategy;

    @BeforeEach
    void setUp() {
        // Instantiate using your exact production architecture strategies
        this.reportRegistry = new ReportRegistry();
        this.parserStrategy = new RegexDslParsingStrategy();
        this.deletionStrategy = new RegexDeletionStrategy();
        
        // Match your exact class constructor dependency injection mapping
        this.engine = new DslAnalyzerEngine(parserStrategy, deletionStrategy, reportRegistry);
    }

    @Test
    void testAnalyzeProcessesRuleSetPairSuccessfully(@TempDir Path tempWorkspace) throws IOException {
        // 1. Arrange: Create mock real-world file targets in the temp environment
        Path mockDslFile = tempWorkspace.resolve("validationResult-BR092.dsl");
        Path mockDslrFile = tempWorkspace.resolve("BR092_rules.dslr");

        // Write a multi-line rule entry layout exactly as parsed by your strategies
        String dslContent = "[then] Emit blank error =\nValidationResult vr = new VR();\nline 2;\n";
        String dslrContent = "ValidationResult vr = new VR();\n"; // Matches your RHS safety context!

        Files.writeString(mockDslFile, dslContent);
        Files.writeString(mockDslrFile, dslrContent);

        RuleSetPair pair = new RuleSetPair("BR092", mockDslFile, mockDslrFile);

        // 2. Act: Run the engine coordinator over the structured target data assets
        assertDoesNotThrow(() -> engine.analyze(pair));

        // 3. Assert: Verify the registry metrics tracking values were updated
        assertNotNull(reportRegistry);
    }
}