package uk.gov.hmrc.cleaner.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmrc.cleaner.model.ParsedDslEntry;

public class StructuralDeletionStrategy implements DslDeletionStrategy {
    private static final Logger log = LoggerFactory.getLogger(StructuralDeletionStrategy.class);

    @Override
    public String deleteEntry(String fullContent, ParsedDslEntry entry) {
        StringBuilder buffer = new StringBuilder(fullContent);

        // 1. Try a clean, fast exact string lookup first
        int exactIndex = buffer.indexOf(entry.rawBlock());
        if (exactIndex != -1) {
            buffer.delete(exactIndex, exactIndex + entry.rawBlock().length());
            return buffer.toString();
        }

        // 2. Structural Scanning Fallback (Whitespace Blind)
        int searchIndex = 0;
        boolean deleted = false;
        String targetNormalized = entry.plainTextToken().replaceAll("\\s+", "").toLowerCase();

        while ((searchIndex = fullContent.indexOf("[then]", searchIndex)) != -1) {
            int equalsIndex = fullContent.indexOf("=", searchIndex);
            int semicolonIndex = fullContent.indexOf(";", searchIndex);

            if (equalsIndex != -1 && semicolonIndex != -1 && equalsIndex < semicolonIndex) {
                String lhsSnippet = fullContent.substring(searchIndex + "[then]".length(), equalsIndex);
                String lhsNormalized = lhsSnippet.replaceAll("\\s+", "").toLowerCase();

                if (lhsNormalized.equals(targetNormalized)) {
                    // Erase from the start of [then] all the way through the closing semicolon
                    buffer.delete(searchIndex, semicolonIndex + 1);
                    deleted = true;
                    break;
                }
            }
            searchIndex += "[then]".length();
        }

        if (!deleted) {
            log.warn("StructuralDeletion: Unable to find block for line {}: {}", 
                    entry.lineNumber(), entry.plainTextToken());
        }

        return buffer.toString();
    }
}