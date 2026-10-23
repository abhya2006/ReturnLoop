package com.returnloop.analytics;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Dashboard metrics and reporting")
public class AnalyticsController {

    private final AnalyticsService service;
    private final CurrentUser currentUser;

    @GetMapping("/dashboard")
    @Operation(summary = "Headline KPIs, trends, recovery distribution and map markers")
    public ApiResponse<AnalyticsService.DashboardSummary> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        return ApiResponse.of(service.dashboard(currentUser.organizationId(), effectiveFrom, effectiveTo));
    }

    @GetMapping("/reports")
    @Operation(summary = "Full reporting view for the selected period")
    public ApiResponse<AnalyticsService.ReportSummary> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(89) : from;
        return ApiResponse.of(service.report(currentUser.organizationId(), effectiveFrom, effectiveTo));
    }

    @GetMapping("/trends")
    public ApiResponse<List<AnalyticsService.TrendPoint>> trends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        return ApiResponse.of(service.trendSeries(currentUser.organizationId(), effectiveFrom, effectiveTo));
    }

    @GetMapping("/environmental")
    @Operation(summary = "Estimated environmental impact with the methodology applied")
    public ApiResponse<AnalyticsService.EnvironmentalSummary> environmental(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        return ApiResponse.of(service.environmentalSummary(currentUser.organizationId(),
                effectiveFrom.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                effectiveTo.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
    }
}
