package uk.gov.hmrc.cleaner;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    public record RedundantEntry(int lineNumber, String lhsToken) {}

    private static class RuleSetMetrics {
        int totalEntries = 0;
        int usedEntries = 0;
        int unusedEntries = 0;
        final List<RedundantEntry> redundantDetails = new ArrayList<>();
    }

    private final Map<String, RuleSetMetrics> reportRegistry = new TreeMap<>();

    public void initializeRuleSetMetrics(String ruleId, int totalCount) {
        RuleSetMetrics metrics = reportRegistry.computeIfAbsent(ruleId, k -> new RuleSetMetrics());
        metrics.totalEntries = totalCount;
        metrics.usedEntries = totalCount;
    }

    public void logRedundant(String ruleId, int lineNumber, String lhsToken) {
        RuleSetMetrics metrics = reportRegistry.get(ruleId);
        if (metrics != null) {
            metrics.redundantDetails.add(new RedundantEntry(lineNumber, lhsToken));
            metrics.unusedEntries++;
            metrics.usedEntries--;
        }
    }

    public void generateReportFile(File targetReportFile) {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("========================================================================\n");
        reportBuilder.append("                      DSL REDUNDANCY REPORT                             \n");
        reportBuilder.append("========================================================================\n");

        if (reportRegistry.isEmpty()) {
            reportBuilder.append("No rule sets were processed.\n");
        } else {
            // Global aggregates for text report
            int totalRuleSets = reportRegistry.size();
            int globalTotal = 0;
            int globalUsed = 0;
            int globalUnused = 0;

            for (RuleSetMetrics m : reportRegistry.values()) {
                globalTotal += m.totalEntries;
                globalUsed += m.usedEntries;
                globalUnused += m.unusedEntries;
            }

            reportBuilder.append(String.format("GLOBAL SUMMARY:%n"));
            reportBuilder.append(String.format("  Total Rule Sets Scanned : %d%n", totalRuleSets));
            reportBuilder.append(String.format("  Total Mappings Found    : %d%n", globalTotal));
            reportBuilder.append(String.format("  Active Mappings In Use  : %d%n", globalUsed));
            reportBuilder.append(String.format("  Redundant Mappings Cut  : %d%n", globalUnused));
            reportBuilder.append("========================================================================\n\n");

            reportRegistry.forEach((ruleId, metrics) -> {
                metrics.redundantDetails.sort(Comparator.comparingInt(RedundantEntry::lineNumber));

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

        try {
            FileUtils.writeStringToFile(targetReportFile, reportBuilder.toString(), StandardCharsets.UTF_8);
            log.info("Metrics text report written to: {}", targetReportFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write text report", e);
        }
    }

    public void generateHtmlReportFile(File targetHtmlFile) {
        // Calculate Global Dashboard KPIs
        int totalRuleSets = reportRegistry.size();
        int globalTotal = 0;
        int globalUsed = 0;
        int globalUnused = 0;

        for (RuleSetMetrics m : reportRegistry.values()) {
            globalTotal += m.totalEntries;
            globalUsed += m.usedEntries;
            globalUnused += m.unusedEntries;
        }

        double optimizationRate = globalTotal == 0 ? 0.0 : ((double) globalUnused / globalTotal) * 100;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>DSL Redundancy Report</title>");
        html.append("<style>")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f8f9fa; color: #333; }")
                .append("h1 { color: #0f172a; margin-bottom: 5px; }")
                .append(".subtitle { color: #64748b; font-size: 0.95rem; margin-bottom: 30px; }")

                // Dashboard KPI Layout
                .append(".dashboard { display: grid; grid-template-columns: repeat(4, 100fr); gap: 20px; margin-bottom: 40px; }")
                .append(".kpi-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.06); border-left: 5px solid #cbd5e1; }")
                .append(".kpi-card.scanned { border-left-color: #3b82f6; }")
                .append(".kpi-card.active { border-left-color: #10b981; }")
                .append(".kpi-card.pruned { border-left-color: #ef4444; }")
                .append(".kpi-card.opt { border-left-color: #8b5cf6; }")
                .append(".kpi-label { font-size: 0.8rem; font-weight: 600; text-transform: uppercase; color: #64748b; margin-bottom: 5px; }")
                .append(".kpi-value { font-size: 1.8rem; font-weight: 700; color: #1e293b; }")

                // Individual Rule Cards
                .append(".card { background: white; border-radius: 8px; padding: 25px; margin-bottom: 25px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }")
                .append(".card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 15px; margin-bottom: 15px; }")
                .append(".rule-title { font-size: 1.3rem; font-weight: bold; color: #0f172a; }")

                // Status Badges
                .append(".badge { font-size: 0.75rem; font-weight: 700; padding: 6px 12px; border-radius: 50px; text-transform: uppercase; }")
                .append(".badge.clean { background: #dcfce7; color: #15803d; }")
                .append(".badge.attention { background: #fee2e2; color: #b91c1c; }")

                .append(".metric-grid { display: flex; gap: 15px; margin-bottom: 20px; }")
                .append(".metric { background: #f8fafc; padding: 8px 14px; border-radius: 6px; font-size: 0.85rem; border: 1px solid #edf2f7; }")
                .append(".metric span { font-weight: bold; color: #0f172a; }")
                .append(".clean-msg { color: #16a34a; font-weight: 500; display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }")

                // Table Configurations
                .append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }")
                .append("th { text-align: left; padding: 10px; background: #f8fafc; font-size: 0.8rem; text-transform: uppercase; color: #64748b; border-bottom: 2px solid #edf2f7; }")
                .append("td { padding: 12px 10px; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem; }")
                .append(".line-num { font-family: monospace; color: #94a3b8; width: 90px; font-weight: 600; }")
                .append(".removed-token { font-family: monospace; color: #991b1b; background: #fee2e2; padding: 4px 8px; border-radius: 4px; border: 1px solid #fecaca; font-weight: 500; display: inline-block; word-break: break-all; }")
                .append("</style></head><body>");

        // Header Title Block
        html.append("<h1>DSL Redundancy Analysis</h1>");
        html.append("<div class='subtitle'>Automated structural optimization report for Drools domain rule sets</div>");

        // Executive Dashboard Section
        html.append("<div class='dashboard'>");
        html.append("  <div class='kpi-card scanned'><div class='kpi-label'>Rule Sets Scanned</div><div class='kpi-value'>").append(totalRuleSets).append("</div></div>");
        html.append("  <div class='kpi-card active'><div class='kpi-label'>Active Mappings In-Use</div><div class='kpi-value'>").append(globalUsed).append("</div></div>");
        html.append("  <div class='kpi-card pruned'><div class='kpi-label'>Redundant Mappings Cut</div><div class='kpi-value' style='color:").append(globalUnused > 0 ? "#ef4444" : "#1e293b").append(";'>").append(globalUnused).append("</div></div>");
        html.append("  <div class='kpi-card opt'><div class='kpi-label'>Footprint Optimization</div><div class='kpi-value'>").append(String.format("%.1f%%", optimizationRate)).append("</div></div>");
        html.append("</div>");

        // Rule Breakdown Lists
        if (reportRegistry.isEmpty()) {
            html.append("<div class='card'><p>No rule sets were processed.</p></div>");
        } else {
            reportRegistry.forEach((ruleId, metrics) -> {
                metrics.redundantDetails.sort(Comparator.comparingInt(RedundantEntry::lineNumber));
                boolean isClean = metrics.redundantDetails.isEmpty();

                html.append("<div class='card'>");
                html.append("  <div class='card-header'>");
                html.append("    <div class='rule-title'>Rule Set: ").append(ruleId).append("</div>");
                if (isClean) {
                    html.append("    <span class='badge clean'>Clean</span>");
                } else {
                    html.append("    <span class='badge attention'>").append(metrics.unusedEntries).append(" Redundant</span>");
                }
                html.append("  </div>");

                html.append("  <div class='metric-grid'>");
                html.append("    <div class='metric'>Total DSL Entries: <span>").append(metrics.totalEntries).append("</span></div>");
                html.append("    <div class='metric'>Active Mappings: <span>").append(metrics.usedEntries).append("</span></div>");
                html.append("  </div>");

                if (isClean) {
                    html.append("<div class='clean-msg'>✓ <strong>Status Clean:</strong> 100% of these mappings are fully cross-referenced in the DSLR rule definitions.</div>");
                } else {
                    html.append("  <table><thead><tr><th class='line-num'>Line</th><th>Redundant LHS Mapping Key (Pruned)</th></tr></thead><tbody>");
                    for (RedundantEntry detail : metrics.redundantDetails) {
                        html.append("  <tr>");
                        html.append("    <td class='line-num'>Line ").append(detail.lineNumber()).append("</td>");
                        html.append("    <td><span class='removed-token'>").append(detail.lhsToken().replace("<", "&lt;").replace(">", "&gt;")).append("</span></td>");
                        html.append("  </tr>");
                    }
                    html.append("  </tbody></table>");
                }
                html.append("</div>");
            });
        }

        html.append("</body></html>");

        try {
            FileUtils.writeStringToFile(targetHtmlFile, html.toString(), StandardCharsets.UTF_8);
            log.info("Visual HTML Diff Dashboard successfully written to: {}", targetHtmlFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write HTML report", e);
        }
    }
}