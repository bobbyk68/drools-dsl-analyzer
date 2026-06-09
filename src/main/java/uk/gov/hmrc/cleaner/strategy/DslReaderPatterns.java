package uk.gov.hmrc.cleaner.strategy;

import java.util.regex.Pattern;

public final class DslReaderPatterns {

    // FIX: [^\n\r=]+ ensures Group 1 stays strictly locked to a single line header text
    private static final Pattern RULES_EXTRACTOR =
            Pattern.compile("(\\[then\\][^\n\r=]+)\\s*=\\s*((?:(?!\\[then\\])[\\s\\S])*?;)");

    private DslReaderPatterns() {}

    public static Pattern getExtractorPattern() {
        return RULES_EXTRACTOR;
    }
}