package uk.gov.hmrc.cleaner.report;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmrc.cleaner.model.ReportRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;

public class TextReportWriter {
    private static final Logger log = LoggerFactory.getLogger(TextReportWriter.class);

    public void generateReportFile(ReportRegistry reportRegistry, File targetReportFile) {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("========================================================================\n");
        reportBuilder.append("                      DSL REDUNDANCY REPORT                             \n");
        reportBuilder.append("========================================================================\n");
        
        Map<String, ReportRegistry.RuleSetMetrics> registry = reportRegistry.getRegistry();

        if (registry.isEmpty()) {
            reportBuilder.append("No rule sets were processed.\n");
        } else {
            int totalRuleSets = registry.size();
            int globalTotal = 0;
            int globalUsed = 0;
            int globalUnused = 0;

            for (ReportRegistry.RuleSetMetrics m : registry.values()) {
                globalTotal += m.getTotalEntries();
                globalUsed += m.getUsedEntries();
                globalUnused += m.getUnusedEntries();
            }

            reportBuilder.append("GLOBAL SUMMARY:\n");
            reportBuilder.append(String.format("  Total Rule Sets Scanned : %d%n", totalRuleSets));
            reportBuilder.append(String.format("  Total Mappings Found    : %d%n", globalTotal));
            reportBuilder.append(String.format("  Active Mappings In Use  : %d%n", globalUsed));
            reportBuilder.append(String.format("  Redundant Mappings Cut  : %d%n", globalUnused));
            reportBuilder.append("========================================================================\n\n");

            registry.forEach((ruleId, metrics) -> {
                metrics.getRedundantDetails().sort(Comparator.comparingInt(ReportRegistry.RedundantEntry::lineNumber));
                reportBuilder.append("Rule Set: ").append(ruleId).append("\n");
                reportBuilder.append("------------------------------------------------------------------------\n");
                reportBuilder.append(String.format("  Total Entries Found : %d%n", metrics.getTotalEntries()));
                reportBuilder.append(String.format("  Entries In Use      : %d%n", metrics.getUsedEntries()));
                reportBuilder.append(String.format("  Unused Count        : %d%n", metrics.getUnusedEntries()));
                reportBuilder.append("\n");

                if (metrics.getRedundantDetails().isEmpty()) {
                    reportBuilder.append("  -> Status: Clean (All entries are actively used in DSLR)\n");
                } else {
                    reportBuilder.append("  Unused Entries Breakdown:\n");
                    for (ReportRegistry.RedundantEntry detail : metrics.getRedundantDetails()) {
                        reportBuilder.append(String.format("    - Line %-4d : %s%n", detail.lineNumber(), detail.lhsToken()));
                    }
                }
                reportBuilder.append("========================================================================\n");
            });
        }

        try {
            FileUtils.writeStringToFile(targetReportFile, reportBuilder.toString(), StandardCharsets.UTF_8);
            log.info("Metrics text report written to: {}", targetReportFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write text report", e);
        }
    }
}