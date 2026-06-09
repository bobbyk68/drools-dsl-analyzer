package uk.gov.hmrc.cleaner.strategy;
import java.util.regex.Pattern;

public final class DslrMatcherPatterns {
    private static final Pattern ANY_WHITESPACE_SEQUENCE = Pattern.compile("\\s+");

    private DslrMatcherPatterns() {}

    public static String normalizeForVerification(String rawText) {
        if (rawText == null) return "";
        return ANY_WHITESPACE_SEQUENCE.matcher(rawText).replaceAll(" ");
    }
}