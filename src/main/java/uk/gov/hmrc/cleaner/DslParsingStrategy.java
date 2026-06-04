package uk.gov.hmrc.cleaner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DslParsingStrategy {

    record ParsedDslEntry(String rawBlock, String plainTextToken, int lineNumber) {}

    List<ParsedDslEntry> parse(Path dslPath) throws IOException;
}