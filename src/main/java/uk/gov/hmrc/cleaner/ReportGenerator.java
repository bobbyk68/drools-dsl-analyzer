package uk.gov.hmrc.cleaner;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);
    
    public record RedundantEntry(int lineNumber, String description) {}
    private final Map<String, List<RedundantEntry>> reportData = new LinkedHashMap<>();

    public void logRedundant(String ruleId, int lineNumber, String description) {
        reportData.computeIfAbsent(ruleId, k -> new ArrayList<>())
                  .add(new RedundantEntry(lineNumber, description));
    }

    public void generateReportFile(File targetReportFile) {
        StringBuilder reportBuilder = new StringBuilder();
        
        reportBuilder.append("========================================================\n");
        reportBuilder.append("                DSL REDUNDANCY REPORT                   \n");
        reportBuilder.append("========================================================\n");
        
        if (reportData.isEmpty()) {
            reportBuilder.append("No redundant DSL entries detected across rule sets.\n");
        } else {
            reportData.forEach((ruleId, entries) -> {
                reportBuilder.append("Rule Set: ").append(ruleId).append("\n");
                for (RedundantEntry entry : entries) {
                    reportBuilder.append(String.format("  - Line %-4d: %s%n", entry.lineNumber(), entry.description()));
                }
                reportBuilder.append("\n");
            });
        }
        reportBuilder.append("========================================================\n");

        String finalReport = reportBuilder.toString();
        log.info("\n{}", finalReport);

        try {
            FileUtils.writeStringToFile(targetReportFile, finalReport, StandardCharsets.UTF_8);
            log.info("Execution report successfully written to: {}", targetReportFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write report payload to file destination", e);
        }
    }
}
