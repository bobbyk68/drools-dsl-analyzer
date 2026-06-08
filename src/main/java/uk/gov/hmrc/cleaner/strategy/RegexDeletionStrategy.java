package uk.gov.hmrc.cleaner.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmrc.cleaner.model.ParsedDslEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDeletionStrategy implements DslDeletionStrategy {
    private static final Logger log = LoggerFactory.getLogger(RegexDeletionStrategy.class);

    @Override
    public String deleteEntry(String fullContent, ParsedDslEntry entry) {
        StringBuilder buffer = new StringBuilder(fullContent);

        // 1. Try a clean, fast exact string lookup first (covers standard single-line formats)
        int exactIndex = buffer.indexOf(entry.rawBlock());
        if (exactIndex != -1) {
            buffer.delete(exactIndex, exactIndex + entry.rawBlock().length());
            return buffer.toString();
        }

        // 2. Fallback: Robust Structural Multi-Line Regex Matching
        // Collapse arbitrary spaces in the token to a single character space first
        String normalizedToken = entry.plainTextToken().replaceAll("\\s+", " ");
        
        // Manually escape special regex structural symbols to avoid compilation faults
        String escapedToken = normalizedToken
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace(" ", "\\s+"); // Turns spaces into cross-line whitespace wildcards

        // Complete structural block matcher:
        // [then] + escaped text sequence + = sign + non-greedy characters/newlines up to semicolon
        String blockRegex = "\\[then\\]\\s*" + escapedToken + "\\s*=\\s*[\\s\\S]*?;";

        Matcher matcher = Pattern.compile(blockRegex, Pattern.MULTILINE).matcher(fullContent);

        if (matcher.find()) {
            // Delete the atomically matched rule entry cleanly out of the buffer string layout
            buffer.delete(matcher.start(), matcher.end());
        } else {
            log.warn("RegexDeletion: Unable to find structural block for line {}: {}", 
                    entry.lineNumber(), entry.plainTextToken());
        }

        return buffer.toString();
    }
}