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

        Path basePath = Paths.get("./src/main/resources/rules/dms");
        File reportOutputFile = new File("./dsl-redundancy-report.txt");

        RuleSetLocator locator = new RuleSetLocator();
        ReportGenerator reportGenerator = new ReportGenerator();
        DslAnalyzerEngine engine = new DslAnalyzerEngine(reportGenerator, isDryRun);

        try {
            List<RuleSetLocator.RuleSetPair> targets = locator.locateRuleSets(
                basePath.resolve("dsl"), 
                basePath.resolve("dslr")
            );

            if (isDryRun) {
                log.info("Running in DRY RUN mode. Structural changes will not be saved.");
            }
            
            log.info("Discovered {} matching DSL/DSLR rule clusters. Initiating analysis...", targets.size());
            targets.forEach(engine::analyze);

            reportGenerator.generateReportFile(reportOutputFile);

        } catch (IOException e) {
            log.error("Global engine context crash during file mapping scanning execution", e);
        }
    }
}
