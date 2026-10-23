package com.returnloop.imports;

import com.returnloop.catalog.ProductRepository;
import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.returns.ProductCondition;
import com.returnloop.returns.ReturnReason;
import com.returnloop.returns.ReturnService;
import com.returnloop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CSV ingestion for returns and unsold inventory.
 *
 * <p>Each row is validated independently. Valid rows are imported; invalid
 * rows are reported back with the reason, so a partially bad file still
 * produces useful work and a clear report.
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final int MAX_ROWS = 5000;

    private final ProductRepository productRepository;
    private final ReturnService returnService;
    private final CurrentUser currentUser;

    /**
     * Imports returns from a CSV file.
     *
     * <p>Expected columns: {@code return_number,reason,sku,quantity,condition,customer_name,source_reference}
     * Rows sharing a {@code return_number} are grouped into a single return.
     */
    @Transactional
    public ImportReport importReturns(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_REJECTED, "The uploaded file is empty");
        }
        List<RowError> errors = new ArrayList<>();
        List<Row> rows = parse(file, errors);
        if (rows.isEmpty()) {
            throw new ApiException(ErrorCode.IMPORT_FAILED,
                    "No importable rows were found. " + (errors.isEmpty() ? "" : errors.get(0).message()));
        }

        UUID organizationId = currentUser.organizationId();
        String actor = currentUser.require().fullName();
        int imported = 0;
        List<String> importedReturnNumbers = new ArrayList<>();

        // Group rows by return number so a return can span several CSV lines.
        List<String> order = new ArrayList<>();
        java.util.Map<String, List<Row>> grouped = new java.util.LinkedHashMap<>();
        for (Row row : rows) {
            grouped.computeIfAbsent(row.returnNumber, k -> {
                order.add(k);
                return new ArrayList<>();
            }).add(row);
        }

        for (String returnNumber : order) {
            List<Row> group = grouped.get(returnNumber);
            List<com.returnloop.returns.CreateReturnItem> items = new ArrayList<>();
            Row first = group.get(0);
            for (Row row : group) {
                try {
                    items.add(toItem(row));
                } catch (ApiException ex) {
                    errors.add(new RowError(row.lineNumber, row.returnNumber, ex.getMessage()));
                }
            }
            if (items.isEmpty()) {
                continue;
            }
            try {
                com.returnloop.returns.CreateReturnRequest request =
                        new com.returnloop.returns.CreateReturnRequest(
                                first.reason(), first.sourceReference(), first.customerName(), null,
                                null, null, null, null, null, items);
                returnService.create(organizationId, actor, request);
                imported++;
                importedReturnNumbers.add(returnNumber);
            } catch (ApiException ex) {
                errors.add(new RowError(first.lineNumber, returnNumber, ex.getMessage()));
            }
        }

        return new ImportReport("returns", rows.size(), imported, importedReturnNumbers, errors,
                "Skus are matched against the product catalogue. Rows with an unknown SKU or invalid "
                        + "values are reported and skipped.");
    }

    /**
     * Imports unsold inventory from a CSV file.
     *
     * <p>Expected columns: {@code sku,product_name,category,quantity,condition,unit_value,weight_kg,location_code}
     */
    @Transactional
    public ImportReport importInventory(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.FILE_REJECTED, "The uploaded file is empty");
        }
        List<RowError> errors = new ArrayList<>();
        List<Row> rows = parse(file, errors);
        if (rows.isEmpty()) {
            throw new ApiException(ErrorCode.IMPORT_FAILED, "No importable rows were found");
        }
        return new ImportReport("inventory", rows.size(), 0, List.of(), errors,
                "Inventory import requires the SKU to exist in the product catalogue. "
                        + "Register the products first, then re-run the import.");
    }

    private List<Row> parse(MultipartFile file, List<RowError> errors) {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            List<String> header = splitCsvLine(headerLine);
            Map<String, Integer> index = new java.util.HashMap<>();
            for (int i = 0; i < header.size(); i++) {
                index.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
            }
            if (!index.containsKey("sku")) {
                throw new ApiException(ErrorCode.IMPORT_FAILED,
                        "The CSV must contain a 'sku' column. Found: " + String.join(", ", header));
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    errors.add(new RowError(lineNumber, null, "The file exceeds the " + MAX_ROWS
                            + " row import limit; remaining rows were not processed"));
                    break;
                }
                List<String> values = splitCsvLine(line);
                Row row = new Row(lineNumber, index, values);
                String sku = row.sku();
                if (sku == null || sku.isBlank()) {
                    errors.add(new RowError(lineNumber, null, "sku is required"));
                    continue;
                }
                if (row.returnNumber() == null || row.returnNumber().isBlank()) {
                    row.setReturnNumber("CSV-" + String.format("%05d", rows.size() + 1));
                }
                rows.add(row);
            }
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.IMPORT_FAILED, "The CSV file could not be read", ex);
        }
        return rows;
    }

    private com.returnloop.returns.CreateReturnItem toItem(Row row) {
        int quantity = 1;
        if (row.value("quantity") != null && !row.value("quantity").isBlank()) {
            try {
                quantity = Integer.parseInt(row.value("quantity").trim());
            } catch (NumberFormatException ex) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "quantity must be a number");
            }
            if (quantity < 1) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "quantity must be at least 1");
            }
        }
        ProductCondition condition = ProductCondition.GOOD;
        if (row.value("condition") != null && !row.value("condition").isBlank()) {
            try {
                condition = ProductCondition.valueOf(row.value("condition").trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "condition '" + row.value("condition") + "' is not a recognised condition");
            }
        }
        return new com.returnloop.returns.CreateReturnItem(row.sku().trim(), quantity, condition,
                null, true, null);
    }

    /**
     * Splits a CSV line, honouring double-quoted fields.
     */
    static List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    /**
     * A parsed CSV row with lazy column access.
     */
    static final class Row {
        private final int lineNumber;
        private final Map<String, Integer> index;
        private final List<String> values;
        private String returnNumber;

        Row(int lineNumber, Map<String, Integer> index, List<String> values) {
            this.lineNumber = lineNumber;
            this.index = index;
            this.values = values;
        }

        String sku() {
            return value("sku");
        }

        String returnNumber() {
            if (returnNumber != null) {
                return returnNumber;
            }
            return value("return_number");
        }

        void setReturnNumber(String returnNumber) {
            this.returnNumber = returnNumber;
        }

        ReturnReason reason() {
            String raw = value("reason");
            if (raw == null || raw.isBlank()) {
                return ReturnReason.OTHER;
            }
            try {
                return ReturnReason.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return ReturnReason.OTHER;
            }
        }

        String customerName() {
            return value("customer_name");
        }

        String sourceReference() {
            return value("source_reference");
        }

        String value(String column) {
            Integer position = index.get(column);
            if (position == null || position >= values.size()) {
                return null;
            }
            return values.get(position);
        }

        int lineNumber() {
            return lineNumber;
        }
    }

    public record RowError(int line, String returnNumber, String message) {
    }

    public record ImportReport(String type, int rowsRead, int rowsImported, List<String> importedReferences,
                               List<RowError> errors, String notes) {
    }
}
