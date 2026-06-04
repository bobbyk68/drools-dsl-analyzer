package uk.gov.hmrc.cleaner;

import org.drools.drl.parser.lang.dsl.DSLMappingEntry;
import org.drools.drl.parser.lang.dsl.DSLMappingFile;
import org.drools.drl.parser.lang.dsl.DSLTokenizedMappingFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DroolsDslParsingStrategy implements DslParsingStrategy {

    @Override
    public List<ParsedDslEntry> parse(Path dslPath) throws IOException {
        List<ParsedDslEntry> entries = new ArrayList<>();
        DSLMappingFile dslFile = new DSLTokenizedMappingFile();
        
        try (BufferedReader reader = Files.newBufferedReader(dslPath)) {
            if (dslFile.parseAndLoad(reader)) {
                for (DSLMappingEntry entry : dslFile.getMapping().getEntries()) {
                    if (entry.getSection() == DSLMappingEntry.CONSEQUENCE) {
                        String cleanToken = entry.getMappingKey().replaceAll("\\s+", " ");
                        int line = findLineNumber(dslPath, entry.getMappingKey());
                        
                        // We rebuild the reconstructable raw block string for removal matching
                        String rawBlock = "[then]" + entry.getMappingKey() + "=" + entry.getMappingValue() + ";";
                        
                        entries.add(new ParsedDslEntry(rawBlock, cleanToken, line));
                    }
                }
            }
        }
        return entries;
    }

    private int findLineNumber(Path path, String key) throws IOException {
        List<String> lines = Files.readAllLines(path);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(key)) return i + 1;
        }
        return 1;
    }
}