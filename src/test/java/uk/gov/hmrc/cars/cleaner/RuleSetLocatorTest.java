package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmrc.cleaner.core.RuleSetLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleSetLocatorTest {

    @Test
    void testLocateRuleSetsMapsDslToNestedDslrTree(@TempDir Path tempWorkspace) throws IOException {
        Path dslDir = Files.createDirectories(tempWorkspace.resolve("dsl"));
        Path dslrDir = Files.createDirectories(tempWorkspace.resolve("dslr/BR092"));

        // Setup mock layout infrastructure files
        Files.writeString(dslDir.resolve("validationResult-BR092.dsl"), "[then] rule mapping");
        Files.writeString(dslrDir.resolve("BR092_rules.dslr"), "when condition then execution");

        RuleSetLocator locator = new RuleSetLocator();
        List<RuleSetLocator.RuleSetPair> pairs = locator.locateRuleSets(dslDir, dslrDir);

        assertEquals(1, pairs.size());
        RuleSetLocator.RuleSetPair pairedAssets = pairs.get(0);
        assertEquals("BR092", pairedAssets.ruleId());
        assertTrue(Files.exists(pairedAssets.dslPath()));
        assertTrue(Files.exists(pairedAssets.dslrPath()));
    }
}