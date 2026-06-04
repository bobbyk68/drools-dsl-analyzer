package uk.gov.hmrc.cleaner;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DslAnalyzerEngine {

    private static final Logger log = LoggerFactory.getLogger(DslAnalyzerEngine.class);

    private final DslParsingStrategy parserStrategy;
    private final DslrAnalyzer analyzer = new DslrAnalyzer();
    private final ReportGenerator reportGenerator;
    private final boolean dryRun;

    public DslAnalyzerEngine(DslParsingStrategy parserStrategy, ReportGenerator reportGenerator, boolean dryRun) {
        this.parserStrategy = parserStrategy;
        this.reportGenerator = reportGenerator;
        this.dryRun = dryRun;
    }

    public void analyze(RuleSetLocator.RuleSetPair pair) {
        try {
            List<DslParsingStrategy.ParsedDslEntry> entries = parserStrategy.parse(pair.dslPath());

            // 1. Initialize total base counts inside our summary aggregator
            reportGenerator.initializeRuleSetMetrics(pair.ruleId(), entries.size());

            if (entries.isEmpty()) return;

            String dslContent = Files.readString(pair.dslPath());
            StringBuilder cleanDslBuffer = new StringBuilder(dslContent);
            boolean changesFound = false;

            for (DslParsingStrategy.ParsedDslEntry entry : entries) {
                if (!analyzer.isTokenUsed(pair.dslrPath(), entry.plainTextToken())) {
                    // 2. Logs exact line mapping details and increments the metrics profile internally
                    reportGenerator.logRedundant(pair.ruleId(), entry.lineNumber(), entry.plainTextToken());
                    changesFound = true;

                    int index = cleanDslBuffer.indexOf(entry.rawBlock());
                    if (index == -1) {
                        index = cleanDslBuffer.indexOf(entry.plainTextToken());
                    }
                    if (index != -1) {
                        cleanDslBuffer.delete(index, index + entry.rawBlock().length());
                    }
                }
            }

            if (changesFound && !dryRun) {
                String baseName = FilenameUtils.getBaseName(pair.dslPath().toString());
                String newFileName = baseName + "-cleaned.dsl";
                Path cleanedPath = pair.dslPath().getParent().resolve(newFileName);

                String cleanedOutput = cleanDslBuffer.toString().replaceAll("(?m)^\\s*$\\n+", "");
                Files.writeString(cleanedPath, cleanedOutput, StandardCharsets.UTF_8);
                log.info("Generated cleaned file asset: {}", cleanedPath.getFileName());
            }

        } catch (IOException e) {
            log.error("Execution processing aborted for rule set: {}", pair.ruleId(), e);
        }
    }
}