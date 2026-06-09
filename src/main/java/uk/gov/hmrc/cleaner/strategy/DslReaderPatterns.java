package uk.gov.hmrc.cleaner.strategy;

import java.util.regex.Pattern;

/**
 * Encapsulates regular expressions used strictly for reading and breaking down
 * raw target DSL configuration file structures.
 */
public final class DslReaderPatterns {

    // The advanced lookahead extraction pattern that isolates rules accurately
    private static final Pattern RULES_EXTRACTOR =
            Pattern.compile("(\\[then\\][\\s\\S]*?)\\s*=\\s*([\\s\\S]*?);(?=\\s*(?:\\[then\\]|$))");

    private DslReaderPatterns() {}

    public static Pattern getExtractorPattern() {
        return RULES_EXTRACTOR;
    }
}