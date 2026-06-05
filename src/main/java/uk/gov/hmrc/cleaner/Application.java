package uk.gov.hmrc.cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // 1. PROFILER: Start execution timer
        long startTime = System.currentTimeMillis();

        boolean isDryRun = Boolean.getBoolean("dryRun");
        String strategyParam = System.getProperty("strategy", "drools").toLowerCase();

        DslParsingStrategy strategy = strategyParam.equals("regex")
                ? new RegexDslParsingStrategy()
                : new DroolsDslParsingStrategy();

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

        // 2. BACKUP SAFETY SYSTEM: Only create backup zip archiver if actively modifying code
        if (!isDryRun) {
            try {
                createBackupArchive(basePath);
            } catch (IOException e) {
                log.error("CRITICAL SAFETY FAULT: Unable to back up rules cluster. Aborting run to protect data.", e);
                System.exit(1);
            }
        }

        File txtReport = new File("./dsl-redundancy-report.txt");
        File htmlReport = new File("./dsl-redundancy-report.html");

        RuleSetLocator locator = new RuleSetLocator();
        ReportGenerator reportGenerator = new ReportGenerator();
//        DslAnalyzerEngine engine = new DslAnalyzerEngine(strategy, reportGenerator, isDryRun);

        // Inside Application.java main method:
        DslDeletionStrategy deletionStrategy = new StructuralDeletionStrategy(); // Pluggable

        DslAnalyzerEngine engine = new DslAnalyzerEngine(
                strategy,
                deletionStrategy, // Passed over smoothly
                reportGenerator,
                isDryRun
        );
        try {
            List<RuleSetLocator.RuleSetPair> targets = locator.locateRuleSets(
                    basePath.resolve("dsl"),
                    basePath.resolve("dslr")
            );

            log.info("Discovered {} matching rule clusters. Processing...", targets.size());
            targets.forEach(engine::analyze);

            reportGenerator.generateReportFile(txtReport);
            reportGenerator.generateHtmlReportFile(htmlReport);

            // 3. PROFILER CONCLUSION: Compute execution metrics
            long duration = System.currentTimeMillis() - startTime;
            log.info("========================================================================");
            log.info("ANALYSIS COMPLETE: Processed {} modules in {} ms", targets.size(), duration);
            log.info("========================================================================");

        } catch (IOException e) {
            log.error("Global engine context crash during runtime execution loop", e);
        }
    }

    /**
     * Zips up the target rules subdirectory layout into a timestamped archive file
     * inside a local backups/ folder before any code mutation takes place.
     */
    private static void createBackupArchive(Path sourceDirPath) throws IOException {
        Path backupDir = Paths.get("./backups");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File zipFile = backupDir.resolve("rules-backup-" + timestamp + ".zip").toFile();

        log.info("Initializing safety data snapshot: {}", zipFile.getName());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to append asset to zip collection: " + path, e);
                        }
                    });
        }
        log.info("Safety snapshot successfully compiled: {}", zipFile.getAbsolutePath());
    }
}