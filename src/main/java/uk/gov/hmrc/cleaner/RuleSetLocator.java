package uk.gov.hmrc.cleaner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class RuleSetLocator {

    public record RuleSetPair(String ruleId, Path dslPath, Path dslrPath) {}

    public List<RuleSetPair> locateRuleSets(Path dslDir, Path dslrDir) throws IOException {
        try (Stream<Path> stream = Files.list(dslDir)) {
            return stream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().matches("validationResult-BR\\d{3}\\.dsl"))
                .map(dslPath -> createPair(dslPath, dslrDir))
                .filter(Objects::nonNull)
                .toList();
        }
    }

    private RuleSetPair createPair(Path dslPath, Path dslrBaseDir) {
        String fileName = dslPath.getFileName().toString();
        String brId = fileName.substring(17, 22); 
        Path dslrPath = dslrBaseDir.resolve(brId).resolve(brId + "_rules.dslr");

        if (!Files.exists(dslrPath)) {
            System.out.println("INFO: DSLR target missing for: " + fileName + " at " + dslrPath);
            return null;
        }
        return new RuleSetPair(brId, dslPath, dslrPath);
    }
}
