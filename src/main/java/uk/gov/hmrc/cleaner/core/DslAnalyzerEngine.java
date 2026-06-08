package uk.gov.hmrc.cleaner.core;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmrc.cleaner.model.ParsedDslEntry;
import uk.gov.hmrc.cleaner.strategy.DslDeletionStrategy;
import uk.gov.hmrc.cleaner.strategy.DslParsingStrategy;
import uk.gov.hmrc.cleaner.report.DslrAnalyzer;
import uk.gov.hmrc.cleaner.model.ReportRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DslAnalyzerEngine {

    private static final Logger log = LoggerFactory.getLogger(DslAnalyzerEngine.class);

    private final DslParsingStrategy parserStrategy;
    private final DslDeletionStrategy deletionStrategy; // Injected Strategy!
    private final boolean dryRun;
    private final DslrAnalyzer complianceAnalyzer = new DslrAnalyzer();

    // Inside DslAnalyzerEngine.java:
    private final ReportRegistry reportRegistry; // Updated reference type

    public DslAnalyzerEngine(DslParsingStrategy parserStrategy,
                             DslDeletionStrategy deletionStrategy,
                             ReportRegistry reportRegistry,
                             boolean dryRun) {
        this.parserStrategy = parserStrategy;
        this.deletionStrategy = deletionStrategy;
        this.reportRegistry = reportRegistry;
        this.dryRun = dryRun;
    }

    public void analyze(RuleSetLocator.RuleSetPair pair) {
        try {
            List<ParsedDslEntry> entries = parserStrategy.parse(pair.dslPath());
            reportRegistry.initializeRuleSetMetrics(pair.ruleId(), entries.size());

            if (entries.isEmpty()) return;

            // Load original content once
            String dslContent = Files.readString(pair.dslPath());
            boolean changesFound = false;

            for (ParsedDslEntry entry : entries) {

                if (!complianceAnalyzer.isTokenUsed(pair.dslrPath(), entry.plainTextToken())) {
                    reportRegistry.logRedundant(pair.ruleId(), entry.lineNumber(), entry.plainTextToken());
                    changesFound = true;

                    // Delegate the complex buffer cleanup straight to our pluggable strategy!
                    dslContent = deletionStrategy.deleteEntry(dslContent, entry);
                }
            }

            if (changesFound && !dryRun) {
                String baseName = FilenameUtils.getBaseName(pair.dslPath().toString());
                String newFileName = baseName + "-cleaned.dsl";
                Path cleanedPath = pair.dslPath().getParent().resolve(newFileName);

                String formattedOutput = dslContent
                        .replace("\r\n", "\n")
                        .replaceAll("\n{3,}", "\n\n");

                // NEW INTERACTION: Send snapshots to report generator for diff comparisons
                reportRegistry.registerFileDiff(pair.ruleId(), dslContent, formattedOutput);

                Files.writeString(cleanedPath, formattedOutput, StandardCharsets.UTF_8);
                log.info("Generated cleaned file asset: {}", cleanedPath.getFileName());
            }

        } catch (IOException e) {
            log.error("Execution processing aborted for rule set: {}", pair.ruleId(), e);
        }
    }
}