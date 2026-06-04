package uk.gov.hmrc.cleaner;

public class Application {
    public static void main(String[] args) {
        boolean isDryRun = false;
        
        // Easily toggle between implementations here
        DslParsingStrategy strategy = new DroolsDslParsingStrategy(); 
        // DslParsingStrategy strategy = new RegexDslParsingStrategy();

        RuleSetLocator locator = new RuleSetLocator();
        ReportGenerator reportGenerator = new ReportGenerator();
        
        // Pass the chosen strategy into the engine context
        DslAnalyzerEngine engine = new DslAnalyzerEngine(strategy, reportGenerator, isDryRun);
        
        // ... rest of the driver execution code stays identical
    }
}