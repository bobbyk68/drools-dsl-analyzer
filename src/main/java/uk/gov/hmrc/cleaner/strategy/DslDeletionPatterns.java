package uk.gov.hmrc.cleaner.strategy;

import java.util.regex.Pattern;


import java.util.regex.Pattern;

public final class DslDeletionPatterns {

    private DslDeletionPatterns() {}

    public static Pattern buildForgivingDeletionPattern(String plainTextToken) {
        // 1. Collapse spaces uniformly
        String normalized = DslrMatcherPatterns.normalizeForVerification(plainTextToken);

        // 2. Escape regex special characters safely
        String escapedToken = normalized
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace(" ", "\\s+");

        // 3. FIX: Add [ \t]*\r?\n? to swallow trailing spaces and the end of the line,
        // but absolutely nothing further!
        String finalRegexStr = "\\[then\\]\\s*" + escapedToken + "\\s*=\\s*((?:(?!\\[then\\])[\\s\\S])*?;)[ \\t]*\\r?\\n?";

        return Pattern.compile(finalRegexStr);
    }
}