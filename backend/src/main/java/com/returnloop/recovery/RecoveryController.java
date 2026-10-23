package com.returnloop.recovery;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery")
@RequiredArgsConstructor
@Tag(name = "Recovery Engine", description = "Recovery option calculation, comparison and decisions")
public class RecoveryController {

    private final RecoveryEngine engine;
    private final CurrentUser currentUser;

    @GetMapping("/returns/{returnId}/options")
    @Operation(summary = "Preview recovery options without persisting a recommendation")
    public ApiResponse<RecoveryEngine.RecoveryEvaluation> preview(@PathVariable UUID returnId) {
        return ApiResponse.of(engine.preview(currentUser.organizationId(), returnId));
    }

    @PostMapping("/returns/{returnId}/recommend")
    @Operation(summary = "Calculate and persist recovery recommendations for a return")
    public ApiResponse<RecoveryEngine.RecoveryEvaluation> recommend(@PathVariable UUID returnId) {
        return ApiResponse.of(engine.recommend(currentUser.organizationId(), returnId));
    }

    @GetMapping("/returns/{returnId}/recommendations")
    public ApiResponse<List<RecoveryEngine.RecoveryRecommendationResponse>> list(@PathVariable UUID returnId) {
        return ApiResponse.of(engine.listForReturn(currentUser.organizationId(), returnId));
    }

    @PostMapping("/recommendations/{id}/decision")
    @Operation(summary = "Approve or reject a recovery recommendation")
    public ApiResponse<RecoveryEngine.RecoveryRecommendationResponse> decide(
            @PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.of(engine.decide(currentUser.organizationId(), id, request.approve(), request.reason()));
    }

    public record DecisionRequest(@NotNull(message = "approve is required") Boolean approve, String reason) {
    }
}
