package uk.gov.hmrc.cleaner.strategy;

import uk.gov.hmrc.cleaner.model.ParsedDslEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DslParsingStrategy {


    List<ParsedDslEntry> parse(Path dslPath) throws IOException;
}