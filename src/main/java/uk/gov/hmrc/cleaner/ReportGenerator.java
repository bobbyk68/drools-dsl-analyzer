package uk.gov.hmrc.cleaner;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    public record RedundantEntry(int lineNumber, String lhsToken) {}

    private static class RuleSetMetrics {
        int totalEntries = 0;
        int usedEntries = 0;
        int unusedEntries = 0;
        String originalContent = "";
        String cleanedContent = "";
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

    // Capture original and final text states for the diff view engine
    public void registerFileDiff(String ruleId, String original, String cleaned) {
        RuleSetMetrics metrics = reportRegistry.get(ruleId);
        if (metrics != null) {
            metrics.originalContent = original;
            metrics.cleanedContent = cleaned;
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
            int totalRuleSets = reportRegistry.size();
            int globalTotal = 0;
            int globalUsed = 0;
            int globalUnused = 0;

            for (RuleSetMetrics m : reportRegistry.values()) {
                globalTotal += m.totalEntries;
                globalUsed += m.usedEntries;
                globalUnused += m.unusedEntries;
            }

            reportBuilder.append("GLOBAL SUMMARY:\n");
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
        } catch (IOException e) {
            log.error("Failed to write text report", e);
        }
    }

    public void generateHtmlReportFile(File targetHtmlFile) {
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
                .append(".dashboard { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 40px; }")
                .append(".kpi-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border-left: 5px solid #cbd5e1; }")
                .append(".kpi-card.scanned { border-left-color: #3b82f6; }")
                .append(".kpi-card.active { border-left-color: #16a34a; }") // Strong Green
                .append(".kpi-card.pruned { border-left-color: #dc2626; }") // Strong Red
                .append(".kpi-card.opt { border-left-color: #8b5cf6; }")
                .append(".kpi-label { font-size: 0.8rem; font-weight: 600; text-transform: uppercase; color: #64748b; margin-bottom: 5px; }")
                .append(".kpi-value { font-size: 1.8rem; font-weight: 700; color: #1e293b; }")
                .append(".card { background: white; border-radius: 8px; padding: 25px; margin-bottom: 35px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }")
                .append(".card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 15px; margin-bottom: 15px; }")
                .append(".rule-title { font-size: 1.3rem; font-weight: bold; color: #0f172a; }")
                .append(".badge { font-size: 0.75rem; font-weight: 700; padding: 6px 12px; border-radius: 50px; text-transform: uppercase; }")
                .append(".badge.clean { background: #dcfce7; color: #15803d; }")
                .append(".badge.attention { background: #fee2e2; color: #b91c1c; }")
                .append(".metric-grid { display: flex; gap: 15px; margin-bottom: 20px; }")
                .append(".metric { background: #f8fafc; padding: 8px 14px; border-radius: 6px; font-size: 0.85rem; border: 1px solid #edf2f7; }")
                .append(".metric span { font-weight: bold; color: #0f172a; }")
                .append(".clean-msg { color: #16a34a; font-weight: 500; font-size: 0.95rem; }")

                // Standalone Overview Table
                .append("table { width: 100%; border-collapse: collapse; margin: 15px 0 25px 0; }")
                .append("th { text-align: left; padding: 10px; background: #f8fafc; font-size: 0.8rem; text-transform: uppercase; color: #64748b; border-bottom: 2px solid #edf2f7; }")
                .append("td { padding: 12px 10px; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem; }")
                .append(".line-num { font-family: monospace; color: #94a3b8; width: 90px; font-weight: 600; }")
                .append(".removed-token { font-family: monospace; color: #991b1b; background: #fee2e2; padding: 4px 8px; border-radius: 4px; border: 1px solid #fecaca; font-weight: 500; display: inline-block; word-break: break-all; }")

                // HIGH CONTRAST LIGHT MODE SIDE-BY-SIDE PANELS
                .append(".diff-split-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 15px; font-family: 'Fira Code', Consolas, Monaco, monospace; font-size: 0.82rem; }")
                .append(".diff-pane { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 6px; padding: 15px; overflow-x: auto; color: #1e293b; line-height: 1.6; min-height: 100px; }")
                .append(".pane-title { font-weight: bold; text-transform: uppercase; font-size: 0.75rem; color: #64748b; margin-bottom: 10px; letter-spacing: 0.5px; border-bottom: 1px solid #e2e8f0; padding-bottom: 5px; }")
                .append(".diff-line { display: flex; white-space: pre; min-height: 22px; align-items: center; background: #ffffff; }")

                // Red Deletions Side
                .append(".diff-line.deletion { background: #ffeeeb; color: #b91c1c; font-weight: 500; }")
                .append(".diff-line.deletion .sign-marker { color: #dc2626; }")

                // Green Additions Side
                .append(".diff-line.addition { background: #f0fdf4; color: #16a34a; font-weight: 500; }")
                .append(".diff-line.addition .sign-marker { color: #15803d; }")

                // Gray padding for mismatched lines
                .append(".diff-line.empty-pad { background: #f8fafc; border-color: transparent; }")
                .append(".diff-line.empty-pad .num-col { border-right-color: transparent; }")

                // Line numbers alignment
                .append(".num-col { width: 40px; display: inline-block; user-select: none; text-align: right; margin-right: 12px; border-right: 1px solid #e2e8f0; padding-right: 8px; color: #94a3b8; font-weight: 500; }")
                .append(".sign-marker { width: 15px; display: inline-block; user-select: none; font-weight: bold; margin-right: 4px; }")

                // Collapsible Summary Tweaks
                .append("details summary::-webkit-details-marker { display: none; }")
                .append("details[open] summary span { transform: rotate(90deg); }")
                .append("</style></head><body>");

        html.append("<h1>DSL Redundancy Analysis</h1>");
        html.append("<div class='subtitle'>Automated structural optimization report for Drools domain rule sets</div>");

        html.append("<div class='dashboard'>");
        html.append("  <div class='kpi-card scanned'><div class='kpi-label'>Rule Sets Scanned</div><div class='kpi-value'>").append(totalRuleSets).append("</div></div>");
        html.append("  <div class='kpi-card active'><div class='kpi-label'>Active Mappings In-Use</div><div class='kpi-value'>").append(globalUsed).append("</div></div>");
        html.append("  <div class='kpi-card pruned'><div class='kpi-label'>Redundant Mappings Cut</div><div class='kpi-value' style='color:").append(globalUnused > 0 ? "#ef4444" : "#1e293b").append(";'>").append(globalUnused).append("</div></div>");
        html.append("  <div class='kpi-card opt'><div class='kpi-label'>Footprint Optimization</div><div class='kpi-value'>").append(String.format("%.1f%%", optimizationRate)).append("</div></div>");
        html.append("</div>");

        if (reportRegistry.isEmpty()) {
            html.append("<div class='card'><p>No rule sets were processed.</p></div>");
        } else {
            reportRegistry.forEach((ruleId, metrics) -> {
                metrics.redundantDetails.sort(Comparator.comparingInt(RedundantEntry::lineNumber));
                boolean isClean = metrics.redundantDetails.isEmpty();

                html.append("<div class='card'>");
                html.append("  <div class='card-header'>");
                html.append("    <div class='rule-title'>Rule Set: ").append(ruleId).append("</div>");
                html.append(isClean ? "    <span class='badge clean'>Clean</span>" : "    <span class='badge attention'>" + metrics.unusedEntries + " Redundant</span>");
                html.append("  </div>");

                html.append("  <div class='metric-grid'>");
                html.append("    <div class='metric'>Total DSL Entries: <span>").append(metrics.totalEntries).append("</span></div>");
                html.append("    <div class='metric'>Active Mappings: <span>").append(metrics.usedEntries).append("</span></div>");
                html.append("  </div>");

                if (isClean) {
                    html.append("<div class='clean-msg'>✓ <strong>Status Clean:</strong> 100% of these mappings are fully cross-referenced in the DSLR rule definitions.</div>");
                } else {
                    html.append("<div style='font-weight:600; font-size:0.95rem; margin-bottom:5px;'>Targeted Redundant Entries List:</div>");
                    html.append("<table><thead><tr><th class='line-num'>Line</th><th>Redundant LHS Mapping Key (Pruned)</th></tr></thead><tbody>");
                    for (RedundantEntry detail : metrics.redundantDetails) {
                        html.append("<tr>");
                        html.append("  <td class='line-num'>Line ").append(detail.lineNumber()).append("</td>");
                        html.append("  <td><span class='removed-token'>").append(escapeHtml(detail.lhsToken())).append("</span></td>");
                        html.append("</tr>");
                    }
                    // 1. Target Redundant Entries List Table ends here...
                    html.append("</tbody></table>");

                    // 2. MAKE THE SIDE-BY-SIDE VISUAL FILE COMPARISON COLLAPSIBLE
                    html.append("<details style='margin-top: 20px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc;'>");
                    html.append("  <summary style='font-weight: 600; font-size: 0.95rem; padding: 12px; cursor: pointer; user-select: none; color: #1e293b; display: flex; align-items: center; gap: 8px;'>");
                    html.append("    <span style='transition: transform 0.2s;'>▶</span> View Side-by-Side Visual File Comparison");
                    html.append("  </summary>");
                    html.append("  <div style='padding: 15px; background: white; border-top: 1px solid #e2e8f0;'>");

                    StringBuilder leftPane = new StringBuilder();
                    StringBuilder rightPane = new StringBuilder();

                    List<String> originalLines = Arrays.asList(metrics.originalContent.split("\\R", -1));
                    List<String> cleanedLines = Arrays.asList(metrics.cleanedContent.split("\\R", -1));

                    Patch<String> patch = DiffUtils.diff(originalLines, cleanedLines);
                    List<AbstractDelta<String>> deltas = patch.getDeltas();

                    int origLineNum = 1;
                    int cleanLineNum = 1;
                    int origIdx = 0;

                    for (AbstractDelta<String> delta : deltas) {
                        while (origIdx < delta.getSource().getPosition()) {
                            String lineText = escapeHtml(originalLines.get(origIdx));
                            leftPane.append("<div class='diff-line'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'> </span>").append(lineText).append("</div>");
                            rightPane.append("<div class='diff-line'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'> </span>").append(lineText).append("</div>");
                            origIdx++;
                            origLineNum++;
                            cleanLineNum++;
                        }

                        int sourceSize = delta.getSource().getLines().size();
                        int targetSize = delta.getTarget().getLines().size();
                        int maxLines = Math.max(sourceSize, targetSize);

                        for (int i = 0; i < maxLines; i++) {
                            if (i < sourceSize) {
                                String line = escapeHtml(delta.getSource().getLines().get(i));
                                leftPane.append("<div class='diff-line deletion'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'>-</span>").append(line).append("</div>");
                                origLineNum++;
                            } else {
                                leftPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                            }

                            if (i < targetSize) {
                                String line = escapeHtml(delta.getTarget().getLines().get(i));
                                rightPane.append("<div class='diff-line addition'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'>+</span>").append(line).append("</div>");
                                cleanLineNum++;
                            } else {
                                rightPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                            }
                        }
                        origIdx += sourceSize;
                    }

                    while (origIdx < originalLines.size()) {
                        String lineText = escapeHtml(originalLines.get(origIdx));
                        leftPane.append("<div class='diff-line'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'> </span>").append(lineText).append("</div>");
                        rightPane.append("<div class='diff-line'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'> </span>").append(lineText).append("</div>");
                        origIdx++;
                        origLineNum++;
                        cleanLineNum++;
                    }

                    html.append("<div class='diff-split-panel'>");
                    html.append("  <div class='diff-pane'><div class='pane-title'>Original DSL Asset File</div>").append(leftPane).append("</div>");
                    html.append("  <div class='diff-pane'><div class='pane-title'>Cleaned Production Output</div>").append(rightPane).append("</div>");
                    html.append("</div>");

                    // Close the collapsible details container
                    html.append("  </div>");
                    html.append("</details>");
                }
                html.append("</div>");
            });
        }

        html.append("</body></html>");

        try {
            FileUtils.writeStringToFile(targetHtmlFile, html.toString(), StandardCharsets.UTF_8);
            log.info("Visual Side-by-Side Code-Diff Dashboard written to: {}", targetHtmlFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write HTML report", e);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}