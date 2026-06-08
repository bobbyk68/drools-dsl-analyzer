package uk.gov.hmrc.cleaner.report;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmrc.cleaner.model.ReportRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HtmlReportWriter {
    private static final Logger log = LoggerFactory.getLogger(HtmlReportWriter.class);

    public void generateHtmlReportFileOld(ReportRegistry reportRegistry, File targetHtmlFile) {
        Map<String, ReportRegistry.RuleSetMetrics> registry = reportRegistry.getRegistry();

        int totalRuleSets = registry.size();
        int globalTotal = 0;
        int globalUsed = 0;
        int globalUnused = 0;

        for (ReportRegistry.RuleSetMetrics m : registry.values()) {
            globalTotal += m.getTotalEntries();
            globalUsed += m.getUsedEntries();
            globalUnused += m.getUnusedEntries();
        }

        double optimizationRate = globalTotal == 0 ? 0.0 : ((double) globalUnused / globalTotal) * 100;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>DSL Redundancy Report</title>");

        // CSS Styles
        html.append("<style>")
            .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f8f9fa; color: #333; }")
            .append("h1 { color: #0f172a; margin-bottom: 5px; }")
            .append(".subtitle { color: #64748b; font-size: 0.95rem; margin-bottom: 30px; }")
            .append(".dashboard { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 40px; }")
            .append(".kpi-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border-left: 5px solid #cbd5e1; }")
            .append(".kpi-card.scanned { border-left-color: #3b82f6; }")
            .append(".kpi-card.active { border-left-color: #16a34a; }")
            .append(".kpi-card.pruned { border-left-color: #dc2626; }")
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
            .append("table { width: 100%; border-collapse: collapse; margin: 15px 0 25px 0; }")
            .append("th { text-align: left; padding: 10px; background: #f8fafc; font-size: 0.8rem; text-transform: uppercase; color: #64748b; border-bottom: 2px solid #edf2f7; }")
            .append("td { padding: 12px 10px; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem; }")
            .append(".line-num { font-family: monospace; color: #94a3b8; width: 90px; font-weight: 600; }")
            .append(".removed-token { font-family: monospace; color: #991b1b; background: #fee2e2; padding: 4px 8px; border-radius: 4px; border: 1px solid #fecaca; font-weight: 500; display: inline-block; word-break: break-all; }")
            .append(".diff-split-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 15px; font-family: 'Fira Code', Consolas, Monaco, monospace; font-size: 0.82rem; }")
            .append(".diff-pane { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 6px; padding: 15px; overflow-x: auto; color: #1e293b; line-height: 1.6; min-height: 100px; }")
            .append(".pane-title { font-weight: bold; text-transform: uppercase; font-size: 0.75rem; color: #64748b; margin-bottom: 10px; letter-spacing: 0.5px; border-bottom: 1px solid #e2e8f0; padding-bottom: 5px; }")
            .append(".diff-line { display: flex; white-space: pre; min-height: 22px; align-items: center; background: #ffffff; }")
            .append(".diff-line.deletion { background: #ffeeeb; color: #b91c1c; font-weight: 500; }")
            .append(".diff-line.deletion .sign-marker { color: #dc2626; }")
            .append(".diff-line.addition { background: #f0fdf4; color: #16a34a; font-weight: 500; }")
            .append(".diff-line.addition .sign-marker { color: #15803d; }")
            .append(".diff-line.empty-pad { background: #f8fafc; border-color: transparent; }")
            .append(".num-col { width: 40px; display: inline-block; user-select: none; text-align: right; margin-right: 12px; border-right: 1px solid #e2e8f0; padding-right: 8px; color: #94a3b8; font-weight: 500; }")
            .append(".sign-marker { width: 15px; display: inline-block; user-select: none; font-weight: bold; margin-right: 4px; }")
            .append("details summary::-webkit-details-marker { display: none; }")
            .append("details[open] summary span { transform: rotate(90deg); }")
            .append("</style></head><body>");

        // Dashboard Metrics DOM Content Setup
        html.append("<h1>DSL Redundancy Analysis</h1>");
        html.append("<div class='subtitle'>Automated structural optimization report for Drools domain rule sets</div>");
        html.append("<div class='dashboard'>");
        html.append("  <div class='kpi-card scanned'><div class='kpi-label'>Rule Sets Scanned</div><div class='kpi-value'>").append(totalRuleSets).append("</div></div>");
        html.append("  <div class='kpi-card active'><div class='kpi-label'>Active Mappings In-Use</div><div class='kpi-value'>").append(globalUsed).append("</div></div>");
        html.append("  <div class='kpi-card pruned'><div class='kpi-label'>Redundant Mappings Cut</div><div class='kpi-value' style='color:").append(globalUnused > 0 ? "#ef4444" : "#1e293b").append(";'>").append(globalUnused).append("</div></div>");
        html.append("  <div class='kpi-card opt'><div class='kpi-label'>Footprint Optimization</div><div class='kpi-value'>").append(String.format("%.1f%%", optimizationRate)).append("</div></div>");
        html.append("</div>");

        registry.forEach((ruleId, metrics) -> {
            metrics.getRedundantDetails().sort(Comparator.comparingInt(ReportRegistry.RedundantEntry::lineNumber));
            boolean isClean = metrics.getRedundantDetails().isEmpty();

            html.append("<div class='card'>");
            html.append("  <div class='card-header'>");
            html.append("    <div class='rule-title'>Rule Set: ").append(ruleId).append("</div>");
            html.append(isClean ? "    <span class='badge clean'>Clean</span>" : "    <span class='badge attention'>" + metrics.getUnusedEntries() + " Redundant</span>");
            html.append("  </div>");

            html.append("  <div class='metric-grid'>");
            html.append("    <div class='metric'>Total DSL Entries: <span>").append(metrics.getTotalEntries()).append("</span></div>");
            html.append("    <div class='metric'>Active Mappings: <span>").append(metrics.getUsedEntries()).append("</span></div>");
            html.append("  </div>");

            if (isClean) {
                html.append("<div class='clean-msg'>✓ <strong>Status Clean:</strong> 100% of these mappings are fully cross-referenced in the rule definitions.</div>");
            } else {
                html.append("<div style='font-weight:600; font-size:0.95rem; margin-bottom:5px;'>Targeted Redundant Entries List:</div>");
                html.append("<table><thead><tr><th class='line-num'>Line</th><th>Redundant LHS Mapping Key (Pruned)</th></tr></thead><tbody>");
                for (ReportRegistry.RedundantEntry detail : metrics.getRedundantDetails()) {
                    html.append("<tr>");
                    html.append("  <td class='line-num'>Line ").append(detail.lineNumber()).append("</td>");
                    html.append("  <td><span class='removed-token'>").append(escapeHtml(detail.lhsToken())).append("</span></td>");
                    html.append("</tr>");
                }
                html.append("</tbody></table>");

                html.append("<details style='margin-top: 20px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc;'>");
                html.append("  <summary style='font-weight: 600; font-size: 0.95rem; padding: 12px; cursor: pointer; user-select: none; color: #1e293b; display: flex; align-items: center; gap: 8px;'>");
                html.append("    <span style='transition: transform 0.2s;'>▶</span> View Side-by-Side Visual File Comparison");
                html.append("  </summary>");
                html.append("  <div style='padding: 15px; background: white; border-top: 1px solid #e2e8f0;'>");

                StringBuilder leftPane = new StringBuilder();
                StringBuilder rightPane = new StringBuilder();

                List<String> originalLines = Arrays.asList(metrics.getOriginalContent().split("\\R", -1));
                List<String> cleanedLines = Arrays.asList(metrics.getCleanedContent().split("\\R", -1));

                DiffRowGenerator diffRowGenerator = DiffRowGenerator.create()
                        .showInlineDiffs(false)
                        .inlineDiffByWord(true)
                        .build();

                List<DiffRow> rows = diffRowGenerator.generateDiffRows(originalLines, cleanedLines);
                int origLineNum = 1;
                int cleanLineNum = 1;

                for (DiffRow row : rows) {
                    String oldLineText = escapeHtml(row.getOldLine());
                    String newLineText = escapeHtml(row.getNewLine());

                    if (row.getTag() == DiffRow.Tag.DELETE || row.getTag() == DiffRow.Tag.CHANGE) {
                        leftPane.append("<div class='diff-line deletion'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'>-</span>").append(oldLineText).append("</div>");
                        origLineNum++;
                    } else if (row.getTag() == DiffRow.Tag.INSERT) {
                        leftPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                    } else {
                        leftPane.append("<div class='diff-line'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'> </span>").append(oldLineText).append("</div>");
                        origLineNum++;
                    }

                    if (row.getTag() == DiffRow.Tag.INSERT || row.getTag() == DiffRow.Tag.CHANGE) {
                        rightPane.append("<div class='diff-line addition'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'>+</span>").append(newLineText).append("</div>");
                        cleanLineNum++;
                    } else if (row.getTag() == DiffRow.Tag.DELETE) {
                        rightPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                    } else {
                        rightPane.append("<div class='diff-line'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'> </span>").append(newLineText).append("</div>");
                        cleanLineNum++;
                    }
                }

                html.append("<div class='diff-split-panel'>");
                html.append("  <div class='diff-pane'><div class='pane-title'>Original DSL Asset File</div>").append(leftPane).append("</div>");
                html.append("  <div class='diff-pane'><div class='pane-title'>Cleaned Production Output</div>").append(rightPane).append("</div>");
                html.append("</div>");
                html.append("  </div>");
                html.append("</details>");
            }
            html.append("</div>");
        });

        html.append("</body></html>");

        try {
            FileUtils.writeStringToFile(targetHtmlFile, html.toString(), StandardCharsets.UTF_8);
            log.info("Visual Side-by-Side Code-Diff Dashboard written to: {}", targetHtmlFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write HTML report", e);
        }
    }

    public void generateHtmlReportFile(ReportRegistry reportRegistry, File targetHtmlFile) {
        Map<String, ReportRegistry.RuleSetMetrics> registry = reportRegistry.getRegistry();


        int totalRuleSets = registry.size();
        int globalTotal = 0;
        int globalUsed = 0;
        int globalUnused = 0;

        for (ReportRegistry.RuleSetMetrics m : registry.values()) {
            globalTotal += m.getTotalEntries();
            globalUsed += m.getUsedEntries();
            globalUnused += m.getUnusedEntries();
        }

        double optimizationRate = globalTotal == 0 ? 0.0 : ((double) globalUnused / globalTotal) * 100;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>DSL Redundancy Report</title>");

        // CSS Styles
        html.append("<style>")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f8f9fa; color: #333; }")
                .append("h1 { color: #0f172a; margin-bottom: 5px; }")
                .append(".subtitle { color: #64748b; font-size: 0.95rem; margin-bottom: 30px; }")
                .append(".dashboard { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 40px; }")
                .append(".kpi-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border-left: 5px solid #cbd5e1; }")
                .append(".kpi-card.scanned { border-left-color: #3b82f6; }")
                .append(".kpi-card.active { border-left-color: #16a34a; }")
                .append(".kpi-card.pruned { border-left-color: #dc2626; }")
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
                .append("table { width: 100%; border-collapse: collapse; margin: 15px 0 25px 0; }")
                .append("th { text-align: left; padding: 10px; background: #f8fafc; font-size: 0.8rem; text-transform: uppercase; color: #64748b; border-bottom: 2px solid #edf2f7; }")
                .append("td { padding: 12px 10px; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem; }")
                .append(".line-num { font-family: monospace; color: #94a3b8; width: 90px; font-weight: 600; }")
                .append(".removed-token { font-family: monospace; color: #991b1b; background: #fee2e2; padding: 4px 8px; border-radius: 4px; border: 1px solid #fecaca; font-weight: 500; display: inline-block; word-break: break-all; }")
                .append(".diff-split-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 15px; font-family: 'Fira Code', Consolas, Monaco, monospace; font-size: 0.82rem; }")
                .append(".diff-pane { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 6px; padding: 15px; overflow-x: auto; color: #1e293b; line-height: 1.6; min-height: 100px; }")
                .append(".pane-title { font-weight: bold; text-transform: uppercase; font-size: 0.75rem; color: #64748b; margin-bottom: 10px; letter-spacing: 0.5px; border-bottom: 1px solid #e2e8f0; padding-bottom: 5px; }")
                .append(".diff-line { display: flex; white-space: pre; min-height: 22px; align-items: center; background: #ffffff; }")
                .append(".diff-line.deletion { background: #ffeeeb; color: #b91c1c; font-weight: 500; }")
                .append(".diff-line.deletion .sign-marker { color: #dc2626; }")
                .append(".diff-line.addition { background: #f0fdf4; color: #16a34a; font-weight: 500; }")
                .append(".diff-line.addition .sign-marker { color: #15803d; }")
                .append(".diff-line.empty-pad { background: #f8fafc; border-color: transparent; }")
                .append(".num-col { width: 40px; display: inline-block; user-select: none; text-align: right; margin-right: 12px; border-right: 1px solid #e2e8f0; padding-right: 8px; color: #94a3b8; font-weight: 500; }")
                .append(".sign-marker { width: 15px; display: inline-block; user-select: none; font-weight: bold; margin-right: 4px; }")
                .append("details summary::-webkit-details-marker { display: none; }")
                .append("details[open] summary span { transform: rotate(90deg); }")
                .append("</style></head><body>");

        // Dashboard Metrics DOM Content Setup
        html.append("<h1>DSL Redundancy Analysis</h1>");
        html.append("<div class='subtitle'>Automated structural optimization report for Drools domain rule sets</div>");
        html.append("<div class='dashboard'>");
        html.append("  <div class='kpi-card scanned'><div class='kpi-label'>Rule Sets Scanned</div><div class='kpi-value'>").append(totalRuleSets).append("</div></div>");
        html.append("  <div class='kpi-card active'><div class='kpi-label'>Active Mappings In-Use</div><div class='kpi-value'>").append(globalUsed).append("</div></div>");
        html.append("  <div class='kpi-card pruned'><div class='kpi-label'>Redundant Mappings Cut</div><div class='kpi-value' style='color:").append(globalUnused > 0 ? "#ef4444" : "#1e293b").append(";'>").append(globalUnused).append("</div></div>");
        html.append("  <div class='kpi-card opt'><div class='kpi-label'>Footprint Optimization</div><div class='kpi-value'>").append(String.format("%.1f%%", optimizationRate)).append("</div></div>");
        html.append("</div>");


        registry.forEach((ruleId, metrics) -> {
            // OPTION 2: FILTER OUT CLEAN FILES
            // If there are no redundant entries, stop processing this card completely!
            if (metrics.getRedundantDetails().isEmpty()) {
                return;
            }

            metrics.getRedundantDetails().sort(Comparator.comparingInt(ReportRegistry.RedundantEntry::lineNumber));

            html.append("<div class='card'>");
            html.append("  <div class='card-header'>");
            html.append("    <div class='rule-title'>Rule Set: ").append(ruleId).append("</div>");
            html.append("    <span class='badge attention'>").append(metrics.getUnusedEntries()).append(" Redundant</span>");
            html.append("  </div>");

            html.append("  <div class='metric-grid'>");
            html.append("    <div class='metric'>Total DSL Entries: <span>").append(metrics.getTotalEntries()).append("</span></div>");
            html.append("    <div class='metric'>Active Mappings: <span>").append(metrics.getUsedEntries()).append("</span></div>");
            html.append("  </div>");

            html.append("<div style='font-weight:600; font-size:0.95rem; margin-bottom:5px;'>Targeted Redundant Entries List:</div>");
            html.append("<table><thead><tr><th class='line-num'>Line</th><th>Redundant LHS Mapping Key (Pruned)</th></tr></thead><tbody>");
            for (ReportRegistry.RedundantEntry detail : metrics.getRedundantDetails()) {
                html.append("<tr>");
                html.append("  <td class='line-num'>Line ").append(detail.lineNumber()).append("</td>");
                html.append("  <td><span class='removed-token'>").append(escapeHtml(detail.lhsToken())).append("</span></td>");
                html.append("</tr>");
            }
            html.append("</tbody></table>");

            html.append("<details style='margin-top: 20px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc;'>");
            html.append("  <summary style='font-weight: 600; font-size: 0.95rem; padding: 12px; cursor: pointer; user-select: none; color: #1e293b; display: flex; align-items: center; gap: 8px;'>");
            html.append("    <span style='transition: transform 0.2s;'>▶</span> View Side-by-Side Visual File Comparison");
            html.append("  </summary>");
            html.append("  <div style='padding: 15px; background: white; border-top: 1px solid #e2e8f0;'>");

            StringBuilder leftPane = new StringBuilder();
            StringBuilder rightPane = new StringBuilder();

            List<String> originalLines = Arrays.asList(metrics.getOriginalContent().split("\\R", -1));
            List<String> cleanedLines = Arrays.asList(metrics.getCleanedContent().split("\\R", -1));

            // OPTION 1 FIX: Configure the generator to natively track raw source lines
            com.github.difflib.text.DiffRowGenerator diffRowGenerator = com.github.difflib.text.DiffRowGenerator.create()
                    .showInlineDiffs(false)
                    .reportLinesChangedOmittingSpaces(false)
                    .build();

            List<com.github.difflib.text.DiffRow> rows = diffRowGenerator.generateDiffRows(originalLines, cleanedLines);
            int origLineNum = 1;
            int cleanLineNum = 1;

            for (com.github.difflib.text.DiffRow row : rows) {
                String oldLineText = escapeHtml(row.getOldLine());
                String newLineText = escapeHtml(row.getNewLine());

                // LEFT PANEL (Always outputs text unless it is a pure insertion on the right)
                if (row.getTag() == com.github.difflib.text.DiffRow.Tag.INSERT) {
                    leftPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                } else if (row.getTag() == com.github.difflib.text.DiffRow.Tag.DELETE || row.getTag() == com.github.difflib.text.DiffRow.Tag.CHANGE) {
                    leftPane.append("<div class='diff-line deletion'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'>-</span>").append(oldLineText).append("</div>");
                    origLineNum++;
                } else {
                    leftPane.append("<div class='diff-line'><span class='num-col'>").append(origLineNum).append("</span><span class='sign-marker'> </span>").append(oldLineText).append("</div>");
                    origLineNum++;
                }

                // RIGHT PANEL (Always outputs text unless it is a pure deletion on the left)
                if (row.getTag() == com.github.difflib.text.DiffRow.Tag.DELETE) {
                    rightPane.append("<div class='diff-line empty-pad'><span class='num-col'> </span><span class='sign-marker'> </span></div>");
                } else if (row.getTag() == com.github.difflib.text.DiffRow.Tag.INSERT || row.getTag() == com.github.difflib.text.DiffRow.Tag.CHANGE) {
                    rightPane.append("<div class='diff-line addition'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'>+</span>").append(newLineText).append("</div>");
                    cleanLineNum++;
                } else {
                    rightPane.append("<div class='diff-line'><span class='num-col'>").append(cleanLineNum).append("</span><span class='sign-marker'> </span>").append(newLineText).append("</div>");
                    cleanLineNum++;
                }
            }

            html.append("<div class='diff-split-panel'>");
            html.append("  <div class='diff-pane'><div class='pane-title'>Original DSL Asset File</div>").append(leftPane).append("</div>");
            html.append("  <div class='diff-pane'><div class='pane-title'>Cleaned Production Output</div>").append(rightPane).append("</div>");
            html.append("</div>");
            html.append("  </div>");
            html.append("</details>");
            html.append("</div>");
        });

        // ... (Keep your file saving FileUtils block at the bottom the same) ...
    }
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }
}