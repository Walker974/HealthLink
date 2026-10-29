package ma.aui.csc.monolith.healthlink.maintenance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.common.data.PageResponse;
import ma.aui.csc.monolith.healthlink.maintenance.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import static ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderDtos.*;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Versioned fault reports, assigned repairs and independent verification")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
})
public class WorkOrderController {
    private final WorkOrderService service;

    @PostMapping
    @Operation(summary = "Report a fault and open a work order", description = "RADIOLOGY_OPERATOR, own department. "
            + "Submit the current equipmentVersion from GET /api/equipment/{id}. "
            + "Only one open work order is allowed per machine; stale versions and duplicates return 409.")
    @ApiResponse(responseCode = "201", description = "Fault, unavailable equipment and audit entry committed together")
    public ResponseEntity<WorkOrderResponse> report(@Valid @RequestBody ReportFaultRequest request) {
        WorkOrderResponse response = service.reportFault(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a work order", description = "Hospital staff in their own hospital; operators are restricted to their department.")
    public WorkOrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List work orders for equipment", description = "Scoped staff access. Newest reports first. Page is zero-based; size must be 1–100.")
    public PageResponse<WorkOrderResponse> list(@RequestParam Long equipmentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(equipmentId, page, size);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a repair", description = "MAINTENANCE_TECHNICIAN, own hospital. "
            + "Assigns the authenticated technician and moves OPEN to IN_PROGRESS. Equipment enters UNDER_MAINTENANCE.")
    public WorkOrderResponse accept(@PathVariable Long id, @Valid @RequestBody VersionRequest request) {
        return service.accept(id, request);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Submit a completed repair for verification", description = "Only the assigned technician. "
            + "Moves IN_PROGRESS to AWAITING_VERIFICATION; equipment remains unavailable.")
    public WorkOrderResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRepairRequest request) {
        return service.complete(id, request);
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify a repair and restore availability", description = "HOSPITAL_MANAGER, own hospital. "
            + "The manager must be different from the repairer, even after a role change. "
            + "Moves AWAITING_VERIFICATION to VERIFIED and equipment to AVAILABLE.")
    public WorkOrderResponse verify(@PathVariable Long id, @Valid @RequestBody ReviewRepairRequest request) {
        return service.verify(id, request);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a repair", description = "HOSPITAL_MANAGER, own hospital, different from the repairer. "
            + "A reason is required. Returns the work order to IN_PROGRESS for the same technician; equipment remains unavailable.")
    public WorkOrderResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewRepairRequest request) {
        return service.reject(id, request);
    }
}
