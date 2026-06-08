package uk.gov.hmrc.cleaner.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DslrAnalyzer {
    public boolean isTokenUsed(Path dslrPath, String plainTextToken) throws IOException {
        String dslrContent = Files.readString(dslrPath);
        return dslrContent.contains(plainTextToken);
    }
}
