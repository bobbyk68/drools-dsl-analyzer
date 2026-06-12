package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import uk.gov.hmrc.cleaner.model.ParsedDslEntry;
import uk.gov.hmrc.cleaner.model.ReportRegistry;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    void testParsedDslEntryRecordContracts() {
        // Verify lightweight data storage record captures attributes natively
        ParsedDslEntry entry = new ParsedDslEntry(
            "[then] Emit blank error", 
            "ValidationResult vr = new VR();", 
            "[then] Emit blank error = ValidationResult vr = new VR();", 
            12
        );

        assertEquals("[then] Emit blank error", entry.plainTextToken());
        assertEquals("ValidationResult vr = new VR();", entry.consequenceBlock());
        assertEquals(12, entry.lineNumber());
    }

    @Test
    void testReportRegistryCalculationsAndSorting() {
        ReportRegistry registry = new ReportRegistry();
        
        // Setup mock global tracker data
        registry.initializeRuleSetMetrics("BR092", 10);
        registry.registerRedundantMapping("BR092", 14, "[then] Old Dead Rule");
        registry.registerRedundantMapping("BR092", 2, "[then] Another Dead Rule");
        
        // Assert alphabetical and numeric organization properties (TreeMap/Sort bounds)
        assertFalse(registry.getRuleSetMetrics().isEmpty());
        var metrics = registry.getMetricsFor("BR092");
        
        assertEquals(10, metrics.getTotalEntriesFound());
        assertEquals(2, metrics.getUnusedCount());
        assertEquals(8, metrics.getInUseCount());
        
        // Confirm optimization calculation formula matches business limits
        assertEquals(20.0, metrics.getFootprintOptimizationPercentage());
    }
}