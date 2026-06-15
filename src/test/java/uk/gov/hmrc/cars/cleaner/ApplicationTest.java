package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmrc.cleaner.Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @Test
    void testMainBootstrapPipelineWithArguments(@TempDir Path sharedWorkspace) throws IOException {
        // 1. Arrange: Setup standard subfolder tree structure required by RuleSetLocator
        Path dslSubDir = Files.createDirectories(sharedWorkspace.resolve("dsl"));
        Path dslrSubDir = Files.createDirectories(sharedWorkspace.resolve("dslr/BR092"));

        // Generate matching minimal valid mock configuration pairs to prevent empty run short-circuits
        Files.writeString(dslSubDir.resolve("validationResult-BR092.dsl"), "[then] Simple Rule = setResult(\"OK\");");
        Files.writeString(dslrSubDir.resolve("BR092_rules.dslr"), "Simple Rule");

        // Force system configurations to execute in non-destructive Dry-Run mode
        System.setProperty("dryRun", "true");
        System.setProperty("strategy", "regex");

        // Pass the sandbox base folder path as an absolute string parameter array
        String[] commandLineArgs = new String[]{ sharedWorkspace.toAbsolutePath().toString() };

        // 2. Act & Assert: Execute the main runtime driver engine bootstrapper
        // It must finish smoothly without dropping system exit exceptions
        assertDoesNotThrow(() -> Application.main(commandLineArgs));
    }
}