package uk.gov.hmrc.cleaner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DslParserTest {

    @Test
    public void testParseEntriesWithMultiLineFormatting(@TempDir Path tempDir) throws IOException {
        String dslContent = """
                // Consignment Shipment - Consignor
                
                [then] Emit BR092 validation error for consignment shipment consignor physical address street and number =
                    insert(emitter.emit(drools,BR092, of($cons, CONSIGNOR_PHYSICAL_ADDRESS_STREET_NUMBER)));
                
                [then] Emit BR092 validation error for consignment shipment consignor physical address country code =
                    insert(emitter.emit(drools,BR092, of($cons, CONSIGNOR_PHYSICAL_ADDRESS_COUNTRY_CODE)));
                """;

        Path mockDslFile = tempDir.resolve("validationResult-BR092.dsl");
        Files.writeString(mockDslFile, dslContent);

        DslParser parser = new DslParser();
        List<DslParser.DslEntry> entries = parser.parseEntries(mockDslFile);

        assertEquals(2, entries.size());
        
        DslParser.DslEntry firstEntry = entries.get(0);
        assertEquals("Emit BR092 validation error for consignment shipment consignor physical address street and number", 
                firstEntry.plainTextToken());
        
        assertEquals(3, firstEntry.lineNumber());
        assertEquals(6, entries.get(1).lineNumber());
    }
}
