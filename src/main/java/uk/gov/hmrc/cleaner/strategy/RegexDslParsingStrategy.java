package uk.gov.hmrc.cleaner.strategy;

import uk.gov.hmrc.cleaner.model.ParsedDslEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDslParsingStrategy implements DslParsingStrategy {

    private static final Pattern THEN_BLOCK_PATTERN = Pattern.compile(
            "(\\[then\\][^=;]+(?:\\s*\\R\\s*[^=;]+)*)\\s*=\\s*([^;]+);",
            Pattern.MULTILINE
    );

    @Override
    public List<ParsedDslEntry> parse(Path dslFilePath) throws IOException {
        List<ParsedDslEntry> entries = new ArrayList<>();
        String content = Files.readString(dslFilePath, StandardCharsets.UTF_8);

        Matcher matcher = THEN_BLOCK_PATTERN.matcher(content);
        int lineNumber = 1; // Basic tracking or you can compute exact line index offset

        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String lhs = matcher.group(1);

            // Strip the [then] tag out to leave only the raw token phrase text
            String strictToken = lhs.replaceFirst("\\[then\\]", "").trim();

            // Normalize internal newlines/tabs down to clean spaces for engine regex lookups
            String cleanToken = strictToken.replaceAll("\\s+", " ");

            // Reconstruct a standard rawBlock format for simple exact deletions
            String rawBlock = "[then]" + strictToken + "=" + matcher.group(2).trim() + ";";

            entries.add(new ParsedDslEntry(rawBlock, cleanToken, lineNumber));

            // Increment line tracker loosely based on line breaks present inside fullMatch block
            lineNumber += fullMatch.split("\\R", -1).length - 1;
        }

        return entries;
    }
}