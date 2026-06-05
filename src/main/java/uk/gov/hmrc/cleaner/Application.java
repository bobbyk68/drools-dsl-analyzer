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
        // 1. Read configuration toggles directly from System Properties (-D)
        boolean isDryRun = Boolean.getBoolean("dryRun");
        String strategyParam = System.getProperty("strategy", "drools").toLowerCase();

        DslParsingStrategy strategy = strategyParam.equals("regex")
                ? new RegexDslParsingStrategy()
                : new DroolsDslParsingStrategy();

        // 2. Resolve target rules path from args or fallback
        Path basePath;
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            basePath = Paths.get(args[0]);
        } else {
            basePath = Paths.get("./src/main/resources/rules/dms");
        }

        log.info("========================================================================");
        log.info("Starting Drools DSL Analyzer");
        log.info("Strategy: {} | DryRun: {} | Path: {}", strategy.getClass().getSimpleName(), isDryRun, basePath.toAbsolutePath());
        log.info("========================================================================");

        File txtReport = new File("./dsl-redundancy-report.txt");
        File htmlReport = new File("./dsl-redundancy-report.html"); // Added HTML target

        RuleSetLocator locator = new RuleSetLocator();
        ReportGenerator reportGenerator = new ReportGenerator();
        DslAnalyzerEngine engine = new DslAnalyzerEngine(strategy, reportGenerator, isDryRun);

        try {
            List<RuleSetLocator.RuleSetPair> targets = locator.locateRuleSets(
                    basePath.resolve("dsl"),
                    basePath.resolve("dslr")
            );

            log.info("Discovered {} matching rule clusters. Processing...", targets.size());
            targets.forEach(engine::analyze);

            // 3. Output reports in both flat-text and visual HTML formats
            reportGenerator.generateReportFile(txtReport);
            reportGenerator.generateHtmlReportFile(htmlReport);

        } catch (IOException e) {
            log.error("Global engine context crash during file processing execution", e);
        }
    }


}