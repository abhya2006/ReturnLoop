package com.returnloop.imports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvImportServiceTest {

    @Test
    @DisplayName("a simple row is split into its columns")
    void splitsRows() {
        List<String> values = CsvImportService.splitCsvLine("RET-1,DEFECTIVE,NWR-PH-0142,2,GOOD,Ada Lovelace,ORD-9");
        assertThat(values).containsExactly("RET-1", "DEFECTIVE", "NWR-PH-0142", "2", "GOOD", "Ada Lovelace", "ORD-9");
    }

    @Test
    @DisplayName("quoted fields keep their embedded commas")
    void handlesQuotedFields() {
        List<String> values = CsvImportService.splitCsvLine("\"RET-2\",\"Damaged, in transit\",\"NWR-CH-0663\",1,FAIR");
        assertThat(values).containsExactly("RET-2", "Damaged, in transit", "NWR-CH-0663", "1", "FAIR");
    }

    @Test
    @DisplayName("escaped double quotes are unescaped")
    void handlesEscapedQuotes() {
        List<String> values = CsvImportService.splitCsvLine("\"He said \"\"hello\"\"\",other");
        assertThat(values.get(0)).isEqualTo("He said \"hello\"");
        assertThat(values.get(1)).isEqualTo("other");
    }

    @Test
    @DisplayName("an empty line produces a single empty column")
    void handlesEmptyLine() {
        assertThat(CsvImportService.splitCsvLine("")).containsExactly("");
    }
}
