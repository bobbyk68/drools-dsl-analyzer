package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmrc.cleaner.model.ReportRegistry;
import uk.gov.hmrc.cleaner.report.HtmlReportWriter;
import uk.gov.hmrc.cleaner.report.TextReportWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReportingWritersTest {

    @Test
    void testTextAndHtmlReportWritersOutputValidStructures(@TempDir Path tempOut) throws IOException {
        ReportRegistry registry = new ReportRegistry();
        registry.initializeRuleSetMetrics("BR150", 5);
        registry.registerRedundantMapping("BR150", 3, "[then] Dead Constraint");
        
        // Track visual comparison differences inside memory model map
        registry.registerFileDiff("BR150", "[then] Dead Constraint = ...;\n[then] Keep = ...;", "[then] Keep = ...;");

        Path textReport = tempOut.resolve("report.txt");
        Path htmlReport = tempOut.resolve("report.html");

        // Execute writers to confirm file creation and string rendering
        TextReportWriter.writeSummary(textReport, registry);
        HtmlReportWriter.generateHtmlReportFile(htmlReport, registry);

        assertTrue(Files.exists(textReport));
        assertTrue(Files.exists(htmlReport));

        String textLines = Files.readString(textReport);
        String htmlLines = Files.readString(htmlReport);

        // Verify key structural milestones are preserved
        assertTrue(textLines.contains("BR150"));
        assertTrue(htmlLines.contains("Footprint Optimization"));
        assertTrue(htmlLines.contains("mint-green")); // Asserts color contrast choice
    }
}