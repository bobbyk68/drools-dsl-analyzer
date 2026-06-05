package uk.gov.hmrc.cleaner;

public interface DslDeletionStrategy {
    /**
     * Locates a redundant entry inside the raw DSL string content and safely erases it.
     * * @param fullContent The raw, un-cleaned text content of the entire DSL file.
     * @param entry The parsed rule entry that needs to be scrubbed.
     * @return The updated string content with the entry completely removed.
     */
    String deleteEntry(String fullContent, DslParsingStrategy.ParsedDslEntry entry);
}