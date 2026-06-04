package uk.gov.hmrc.cleaner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDslParsingStrategy implements DslParsingStrategy {

    private static final Pattern DSL_ENTRY_PATTERN = Pattern.compile(
            "(\\[then\\]\\s*+[^=;\n\r]+(?:\\s*+[\n\r]\\s*+[^=;\n\r]+)*+)\\s*+=\\s*+([^;]++);",
            Pattern.MULTILINE
    );

    @Override
    public List<ParsedDslEntry> parse(Path dslPath) throws IOException {
        String content = Files.readString(dslPath);
        List<ParsedDslEntry> entries = new ArrayList<>();
        Matcher matcher = DSL_ENTRY_PATTERN.matcher(content);

        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String lhs = matcher.group(1);

            int lineNumber = calculateLineNumber(content, matcher.start());
            String cleanToken = lhs.replaceFirst("\\[then\\]", "").trim().replaceAll("\\s+", " ");

            entries.add(new ParsedDslEntry(fullMatch, cleanToken, lineNumber));
        }
        return entries;
    }

    private int calculateLineNumber(String content, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }
}