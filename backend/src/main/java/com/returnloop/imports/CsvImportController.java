package com.returnloop.imports;

import com.returnloop.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
@Tag(name = "Bulk Import", description = "CSV ingestion")
public class CsvImportController {

    private final CsvImportService service;

    @PostMapping("/returns")
    @Operation(summary = "Import returns from a CSV file")
    public ApiResponse<CsvImportService.ImportReport> importReturns(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(service.importReturns(file));
    }

    @PostMapping("/inventory")
    @Operation(summary = "Import unsold inventory from a CSV file")
    public ApiResponse<CsvImportService.ImportReport> importInventory(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(service.importInventory(file));
    }
}
