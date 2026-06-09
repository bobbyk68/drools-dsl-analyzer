package uk.gov.hmrc.cleaner.strategy;

import java.util.regex.Pattern;

/**
 * Dynamically builds structural deletion patterns designed to safely sweep
 * redundant mapping expressions out of the original DSL files.
 */
public final class DslDeletionPatterns {

    private DslDeletionPatterns() {}

    /**
     * Creates a forgiving, multi-line regular expression specifically customized to match
     * a target token. It is immune to internal semicolons inside Java assignments.
     */
    public static Pattern buildForgivingDeletionPattern(String plainTextToken) {
        // 1. Leverage our matcher engine utility to crush any internal spacing anomalies first
        String normalized = DslrMatcherPatterns.normalizeForVerification(plainTextToken);

        // 2. Convert literal single spaces into flexible regex wildcards (\s+)
        String flexibleSpacesRegex = normalized.replace(" ", "\\s+");

        // 3. Assemble the atomic structural block with an end-of-block lookahead guardrail
        String finalRegexStr = "\\[then\\]\\s*" + flexibleSpacesRegex + "\\s*=\\s*[\\s\\S]*?;(?=\\s*(?:\\[then\\]|$))";

        return Pattern.compile(finalRegexStr);
    }
}