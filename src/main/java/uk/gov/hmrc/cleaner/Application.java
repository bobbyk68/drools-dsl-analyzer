package uk.gov.hmrc.cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        boolean isDryRun = false;

        // Choose your parsing strategy
        DslParsingStrategy strategy = new DroolsDslParsingStrategy();

        // 1. Check if a path argument was passed via the command line
        Path basePath;
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            basePath = Paths.get(args[0]);
            log.info("Using custom external rules directory: {}", basePath.toAbsolutePath());
        } else {
            // Fallback default if no parameter is passed
            basePath = Paths.get("./src/main/resources/rules/dms");
            log.info("No path parameter provided. Falling back to default: {}", basePath.toAbsolutePath());
        }

        File reportOutputFile = new File("./dsl-redundancy-report.txt");
        RuleSetLocator locator = new RuleSetLocator();
        ReportGenerator reportGenerator = new ReportGenerator();
        DslAnalyzerEngine engine = new DslAnalyzerEngine(strategy, reportGenerator, isDryRun);

        try {
            // Resolve 'dsl' and 'dslr' folders relative to the chosen base path
            List<RuleSetLocator.RuleSetPair> targets = locator.locateRuleSets(
                    basePath.resolve("dsl"),
                    basePath.resolve("dslr")
            );

            if (isDryRun) {
                log.info("Running in DRY RUN mode. Structural changes will not be saved.");
            }

            log.info("Using parsing strategy: {}", strategy.getClass().getSimpleName());
            log.info("Discovered {} matching DSL/DSLR rule clusters. Initiating analysis...", targets.size());
            targets.forEach(engine::analyze);

            reportGenerator.generateReportFile(reportOutputFile);

        } catch (IOException e) {
            log.error("Global engine context crash during file mapping scanning execution", e);
        }
    }
}