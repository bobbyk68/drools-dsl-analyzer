package uk.gov.hmrc.cleaner.strategy;

import uk.gov.hmrc.cleaner.model.ParsedDslEntry;

public class RegexDeletionStrategy implements DslDeletionStrategy {

    @Override
    public String deleteEntry(String originalFileContent, ParsedDslEntry entry) {
        // 1. Generate the safe, lookahead deletion pattern using our factory class
        java.util.regex.Pattern targetPattern = DslDeletionPatterns.buildForgivingDeletionPattern(entry.plainTextToken());

        // 2. Execute the clean sweep across the content buffer stream instantly
        return targetPattern.matcher(originalFileContent).replaceAll("");
    }
}