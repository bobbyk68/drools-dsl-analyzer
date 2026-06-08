package uk.gov.hmrc.cleaner.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportRegistry {

    public record RedundantEntry(int lineNumber, String lhsToken) {}
    
    public static class RuleSetMetrics {
        private int totalEntries = 0;
        private int usedEntries = 0;
        private int unusedEntries = 0;
        private String originalContent = "";
        private String cleanedContent = "";
        private final List<RedundantEntry> redundantDetails = new ArrayList<>();

        public int getTotalEntries() { return totalEntries; }
        public int getUsedEntries() { return usedEntries; }
        public int getUnusedEntries() { return unusedEntries; }
        public String getOriginalContent() { return originalContent; }
        public String getCleanedContent() { return cleanedContent; }
        public List<RedundantEntry> getRedundantDetails() { return redundantDetails; }
    }

    private final Map<String, RuleSetMetrics> registry = new TreeMap<>();

    public Map<String, RuleSetMetrics> getRegistry() {
        return registry;
    }

    public void initializeRuleSetMetrics(String ruleId, int totalCount) {
        RuleSetMetrics metrics = registry.computeIfAbsent(ruleId, k -> new RuleSetMetrics());
        metrics.totalEntries = totalCount;
        metrics.usedEntries = totalCount; 
    }

    public void logRedundant(String ruleId, int lineNumber, String lhsToken) {
        RuleSetMetrics metrics = registry.get(ruleId);
        if (metrics != null) {
            metrics.redundantDetails.add(new RedundantEntry(lineNumber, lhsToken));
            metrics.unusedEntries++;
            metrics.usedEntries--;
        }
    }

    public void registerFileDiff(String ruleId, String original, String cleaned) {
        RuleSetMetrics metrics = registry.get(ruleId);
        if (metrics != null) {
            metrics.originalContent = original;
            metrics.cleanedContent = cleaned;
        }
    }
}