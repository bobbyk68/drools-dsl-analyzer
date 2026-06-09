package uk.gov.hmrc.cleaner.strategy;

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

        // 3. FIX: Match up to a semicolon, but ONLY if followed by whitespaces/comments leading to the next [then] or EOF
        String finalRegexStr = "\\[then\\]\\s*" + escapedToken + "\\s*=\\s*(?:(?!\\[then\\])[\\s\\S])*?;(?=\\s*(?:\\/\\*([\\s\\S]*?)\\*\\/|\\/\\/.*|\\s)*(?:\\[then\\]|$))[ \\t]*\\r?\\n?";

        return Pattern.compile(finalRegexStr);
    }
}