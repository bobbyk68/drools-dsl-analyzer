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

    public void generateHtmlReportFile(File targetHtmlFile) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>DSL Redundancy Report</title>");
        html.append("<style>")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f8f9fa; color: #333; }")
                .append("h1 { color: #1e293b; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; }")
                .append(".card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 25px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
                .append(".rule-title { font-size: 1.25rem; font-weight: bold; color: #0f172a; margin-bottom: 15px; }")
                .append(".metric-grid { display: flex; gap: 20px; margin-bottom: 15px; }")
                .append(".metric { background: #f1f5f9; padding: 10px 15px; border-radius: 6px; font-size: 0.9rem; }")
                .append(".metric span { font-weight: bold; color: #1e293b; }")
                .append(".clean { color: #15803d; font-weight: 500; background: #f0fdf4; padding: 10px; border-radius: 6px; border: 1px solid #bbf7d0; }")
                .append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }")
                .append("th { text-align: left; padding: 10px; background: #f1f5f9; font-size: 0.85rem; text-transform: uppercase; color: #64748b; }")
                .append("td { padding: 12px 10px; border-bottom: 1px solid #e2e8f0; font-size: 0.9rem; }")
                .append(".line-num { font-family: monospace; color: #94a3b8; width: 80px; }")
                .append(".removed-token { font-family: monospace; color: #991b1b; background: #fee2e2; padding: 4px 8px; border-radius: 4px; border: 1px solid #fecaca; font-weight: 500; }")
                .append("</style></head><body>");

        html.append("<h1>DSL Redundancy Scan Summary</h1>");

        if (reportRegistry.isEmpty()) {
            html.append("<div class='card'><p>No rule sets were processed.</p></div>");
        } else {
            reportRegistry.forEach((ruleId, metrics) -> {
                html.append("<div class='card'>");
                html.append("<div class='rule-title'>Rule Set: ").append(ruleId).append("</div>");
                html.append("<div class='metric-grid'>");
                html.append("<div class='metric'>Total Entries: <span>").append(metrics.totalEntries).append("</span></div>");
                html.append("<div class='metric'>In Use: <span>").append(metrics.usedEntries).append("</span></div>");
                html.append("<div class='metric'>Unused Count: <span style='color:").append(metrics.unusedEntries > 0 ? "#b91c1c" : "#1e293b").append(";'>").append(metrics.unusedEntries).append("</span></div>");
                html.append("</div>");

                if (metrics.redundantDetails.isEmpty()) {
                    html.append("<div class='clean'>✓ Status Clean: All entries are actively mapped inside the matching DSLR ruleset.</div>");
                } else {
                    html.append("<table><thead><tr><th class='line-num'>Line</th><th>Redundant LHS Key Target (Pruned)</th></tr></thead><tbody>");
                    for (RedundantEntry detail : metrics.redundantDetails) {
                        html.append("<tr>");
                        html.append("<td class='line-num'>Line ").append(detail.lineNumber()).append("</td>");
                        html.append("<td><span class='removed-token'>").append(detail.lhsToken().replace("<", "&lt;").replace(">", "&gt;")).append("</span></td>");
                        html.append("</tr>");
                    }
                    html.append("</tbody></table>");
                }
                html.append("</div>");
            });
        }

        html.append("</body></html>");

        try {
            org.apache.commons.io.FileUtils.writeStringToFile(targetHtmlFile, html.toString(), java.nio.charset.StandardCharsets.UTF_8);
            log.info("Visual HTML Diff Report written out to: {}", targetHtmlFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write visual HTML report structure", e);
        }
    }
}