package uk.gov.hmrc.cars.cleaner;

import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.gov.hmrc.cleaner.strategy.DslReaderPatterns;
import uk.gov.hmrc.cleaner.strategy.DslrMatcherPatterns;
import uk.gov.hmrc.cleaner.strategy.DslDeletionPatterns;

import static org.junit.jupiter.api.Assertions.*;

class PatternLibraryTest {

    @Test
    void testDslrMatcherCollapsesConsecutiveSpaces() {
        String input = "validation    error   for";
        String normalized = DslrMatcherPatterns.normalizeForVerification(input);
        assertEquals("validation error for", normalized);
    }

    @Test
    void testDslReaderPatternExtractsMultiLineAssignments() {
        String dslContent = "[then] Emit error =\n   ValidationResult vr = new VR();\n   line 2;";
        Matcher matcher = DslReaderPatterns.getExtractorPattern().matcher(dslContent);
        
        assertTrue(matcher.find());
        assertEquals("[then] Emit error", matcher.group(1).trim());
        assertTrue(matcher.group(2).contains("ValidationResult vr"));
    }

    @Test
    void testDslDeletionPatternHandlesWhitespaceBlindness() {
        Pattern deletionPattern = DslDeletionPatterns.buildForgivingDeletionPattern("Emit blank error");
        String fileContent = "[then] Emit   blank   error =\nValidationResult vr = new VR();\nline 2;\n[then] Next Rule = ...;";
        
        Matcher matcher = deletionPattern.matcher(fileContent);
        assertTrue(matcher.find());
        
        // Ensure atomic lookahead stops cleanly before bleeding into next blocks
        String cleaned = matcher.replaceAll("");
        assertTrue(cleaned.contains("[then] Next Rule"));
        assertFalse(cleaned.contains("Emit blank error"));
    }
}