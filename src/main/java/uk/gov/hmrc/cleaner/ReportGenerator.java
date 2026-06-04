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

    public record RedundantEntry(int lineNumber, String lhsToken) {}

    // Domain metric tracking per Rule Set
    private static class RuleSetMetrics {
        int totalEntries = 0;
        int usedEntries = 0;
        int unusedEntries = 0;
        final List<RedundantEntry> redundantDetails = new ArrayList<>();
    }

    private final Map<String, RuleSetMetrics> reportRegistry = new LinkedHashMap<>();

    /**
     * Initializes a rule set summary profile with total structure counts.
     */
    public void initializeRuleSetMetrics(String ruleId, int totalCount) {
        RuleSetMetrics metrics = reportRegistry.computeIfAbsent(ruleId, k -> new RuleSetMetrics());
        metrics.totalEntries = totalCount;
        // Default assuming all are used initially, subtracted dynamically as redundancies are found
        metrics.usedEntries = totalCount;
    }

    /**
     * Logs an identified redundant mapping entry and updates the summary statistics.
     */
    public void logRedundant(String ruleId, int lineNumber, String lhsToken) {
        RuleSetMetrics metrics = reportRegistry.get(ruleId);
        if (metrics != null) {
            metrics.redundantDetails.add(new RedundantEntry(lineNumber, lhsToken));
            metrics.unusedEntries++;
            metrics.usedEntries--;
        }
    }

    /**
     * Compiles the explicit metrics and flat text outputs directly to the targeted reporting file destination.
     */
    public void generateReportFile(File targetReportFile) {
        StringBuilder reportBuilder = new StringBuilder();

        reportBuilder.append("========================================================================\n");
        reportBuilder.append("                      DSL REDUNDANCY REPORT                             \n");
        reportBuilder.append("========================================================================\n");

        if (reportRegistry.isEmpty()) {
            reportBuilder.append("No rule sets were processed.\n");
        } else {
            reportRegistry.forEach((ruleId, metrics) -> {
                reportBuilder.append("Rule Set: ").append(ruleId).append("\n");
                reportBuilder.append("------------------------------------------------------------------------\n");
                reportBuilder.append(String.format("  Total Entries Found : %d%n", metrics.totalEntries));
                reportBuilder.append(String.format("  Entries In Use      : %d%n", metrics.usedEntries));
                reportBuilder.append(String.format("  Unused Count        : %d%n", metrics.unusedEntries));
                reportBuilder.append("\n");

                if (metrics.redundantDetails.isEmpty()) {
                    reportBuilder.append("  -> Status: Clean (All entries are actively used in DSLR)\n");
                } else {
                    reportBuilder.append("  Unused Entries Breakdown:\n");
                    for (RedundantEntry detail : metrics.redundantDetails) {
                        reportBuilder.append(String.format("    - Line %-4d : %s%n", detail.lineNumber(), detail.lhsToken()));
                    }
                }
                reportBuilder.append("========================================================================\n");
            });
        }

        String finalReport = reportBuilder.toString();
        log.info("\n{}", finalReport);

        try {
            FileUtils.writeStringToFile(targetReportFile, finalReport, StandardCharsets.UTF_8);
            log.info("Metrics report successfully written out to: {}", targetReportFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write analytical summary payload to target path", e);
        }
    }
}