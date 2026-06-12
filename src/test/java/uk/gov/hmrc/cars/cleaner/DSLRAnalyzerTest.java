package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmrc.cleaner.model.ParsedDslEntry;
import uk.gov.hmrc.cleaner.report.DslrAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DSLRAnalyzerTest {

    @Test
    void testIsTokenUsedProtectsBootstrappingContexts(@TempDir Path tempWorkspace) throws IOException {
        Path mockDslr = tempWorkspace.resolve("rules.dslr");
        
        // Simulation scenario: Rule engine initializes via context, but LHS isn't explicitly used
        Files.writeString(mockDslr, "ValidationResult vr = new VR();\nEmit active business message");

        DslrAnalyzer analyzer = new DslrAnalyzer();

        // Entry A: Genuine active rule called explicitly by statement string name mapping
        ParsedDslEntry entryActive = new ParsedDslEntry("Emit active business message", "...", "...", 1);
        
        // Entry B: Hidden engine variables required by system layout initialization block (RHS)
        ParsedDslEntry entryBootstrapper = new ParsedDslEntry("Emit blank error", "ValidationResult vr = new ValidationResult();", "...", 2);

        // Entry C: True redundant dead definition entry block
        ParsedDslEntry entryDead = new ParsedDslEntry("Orphaned Dead Text mapping entry", "setResult(\"ERR\");", "...", 3);

        assertTrue(analyzer.isTokenUsed(mockDslr, entryActive));
        assertTrue(analyzer.isTokenUsed(mockDslr, entryBootstrapper)); // Automated safety net passes!
        assertFalse(analyzer.isTokenUsed(mockDslr, entryDead)); // Safely caught as dead code
    }
}