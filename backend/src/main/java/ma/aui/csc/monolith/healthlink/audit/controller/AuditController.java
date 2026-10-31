package ma.aui.csc.monolith.healthlink.audit.controller;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.audit.data.AuditDtos.AuditResponse;
import ma.aui.csc.monolith.healthlink.audit.service.AuditService;
import ma.aui.csc.monolith.healthlink.common.data.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Append-only operational history, with server-resolved actor identities")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final AuditService service;

    @GetMapping("/api/equipment/{equipmentId}/history")
    @Operation(summary = "Read equipment and repair history", description = "Hospital manager or technician in the same hospital, "
            + "or operator in the equipment's department. Records are ordered oldest first. "
            + "Page is zero-based; size must be 1–100. History remains readable after equipment deactivation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of immutable audit records"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public PageResponse<AuditResponse> history(@PathVariable Long equipmentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.history(equipmentId, page, size);
    }
}
