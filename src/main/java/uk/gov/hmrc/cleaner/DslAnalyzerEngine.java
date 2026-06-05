package uk.gov.hmrc.cleaner;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DslAnalyzerEngine {

    private static final Logger log = LoggerFactory.getLogger(DslAnalyzerEngine.class);

    private final DslParsingStrategy parserStrategy;
    private final ReportGenerator reportGenerator;
    private final boolean dryRun;
    private final DslrAnalyzer complianceAnalyzer = new DslrAnalyzer(); // Your rules usage checker

    public DslAnalyzerEngine(DslParsingStrategy parserStrategy, ReportGenerator reportGenerator, boolean dryRun) {
        this.parserStrategy = parserStrategy;
        this.reportGenerator = reportGenerator;
        this.dryRun = dryRun;
    }

    public void analyze(RuleSetLocator.RuleSetPair pair) {
        try {
            List<DslParsingStrategy.ParsedDslEntry> entries = parserStrategy.parse(pair.dslPath());
            reportGenerator.initializeRuleSetMetrics(pair.ruleId(), entries.size());

            if (entries.isEmpty()) return;

            String dslContent = Files.readString(pair.dslPath());
            StringBuilder cleanDslBuffer = new StringBuilder(dslContent);
            boolean changesFound = false;

            for (DslParsingStrategy.ParsedDslEntry entry : entries) {
                // Verify if the token is actively used inside the DSLR file
                if (!complianceAnalyzer.isTokenUsed(pair.dslrPath(), entry.plainTextToken())) {
                    reportGenerator.logRedundant(pair.ruleId(), entry.lineNumber(), entry.plainTextToken());
                    changesFound = true;

                    // 1. STRATEGY A: Try a clean string look up first
                    int exactIndex = cleanDslBuffer.indexOf(entry.rawBlock());
                    if (exactIndex != -1) {
                        cleanDslBuffer.delete(exactIndex, exactIndex + entry.rawBlock().length());
                    } else {
                        // 2. STRATEGY B (ROBUST FALLBACK): Compile a strict structural block regex target.
                        // This pattern normalizes spaces and grabs the mapping key, the '=' sign,
                        // and everything up to the terminating semicolon.
                        String flexibleTokenPattern = Pattern.quote(entry.plainTextToken().replaceAll("\\s+", " "))
                                .replace(" ", "\\s+");

                        String blockRegex = "\\[then\\]\\s*" + flexibleTokenPattern + "\\s*=\\s*[^;\\n\\r]+;";

                        Matcher blockMatcher = Pattern.compile(blockRegex, Pattern.MULTILINE)
                                .matcher(cleanDslBuffer.toString());

                        if (blockMatcher.find()) {
                            // Erase the ENTIRE structural block cleanly out of the buffer loop
                            cleanDslBuffer.delete(blockMatcher.start(), blockMatcher.end());
                        } else {
                            log.warn("Rule Set [{}]: Unable to find structural match for deletion on line {}: {}",
                                    pair.ruleId(), entry.lineNumber(), entry.plainTextToken());
                        }
                    }
                }
            }

            if (changesFound && !dryRun) {
                String baseName = FilenameUtils.getBaseName(pair.dslPath().toString());
                String newFileName = baseName + "-cleaned.dsl";
                Path cleanedPath = pair.dslPath().getParent().resolve(newFileName);

                // Convert line endings and collapse gaps down to a single blank line
                String formattedOutput = cleanDslBuffer.toString()
                        .replace("\r\n", "\n")
                        .replaceAll("\n{3,}", "\n\n");

                Files.writeString(cleanedPath, formattedOutput, StandardCharsets.UTF_8);
                log.info("Generated cleaned file asset: {}", cleanedPath.getFileName());
            }

        } catch (IOException e) {
            log.error("Execution processing aborted for rule set: {}", pair.ruleId(), e);
        }
    }
}