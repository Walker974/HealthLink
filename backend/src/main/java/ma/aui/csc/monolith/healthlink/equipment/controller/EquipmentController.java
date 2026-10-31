package ma.aui.csc.monolith.healthlink.equipment.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.ChangeStatusRequest;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.EquipmentResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.RegisterEquipmentRequest;
import ma.aui.csc.monolith.healthlink.equipment.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * WEB SERVICE for equipment: the public HTTP door (REST).
 * It does NOT apply business rules: it receives the request,
 * validates the DTO, calls the business service, and returns JSON.
 *
 * @RestController: answers HTTP requests with JSON.
 * @RequestMapping: every URL in this class starts with /api/equipment.
 * @RequiredArgsConstructor: generates a constructor with the final service field.
 * @Validated: also checks rules on simple parameters (@RequestParam).
 */
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Validated
@Tag(name = "Equipment", description = "Public availability searches, equipment registration and fault reporting")
public class EquipmentController {

    private final EquipmentService service;

    @PostMapping
    @Operation(summary = "Register equipment",
            description = "Requires HOSPITAL_MANAGER with an active staff assignment to the department's hospital. "
                    + "The department must be active and the imaging type must already exist. "
                    + "New equipment starts as AVAILABLE.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipment registered",
                    headers = @Header(name = "Location", description = "URL of the registered equipment",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<EquipmentResponse> register(
            @Valid @RequestBody RegisterEquipmentRequest request) {
        EquipmentResponse created = service.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Report a fault through the compatibility status endpoint", deprecated = true,
            description = "Requires RADIOLOGY_OPERATOR with an active staff assignment to the equipment's department "
                    + "and hospital. Requires the current equipment version and a fault description. "
                    + "Creates a work order atomically; duplicates and stale versions return 409. "
                    + "Prefer POST /api/work-orders. Only OUT_OF_SERVICE is accepted.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipment is out of service and a work order was opened"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public EquipmentResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment details and its current version",
            description = "Hospital staff in their own hospital; operators are restricted to their department.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipment details including optimistic-lock version"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public EquipmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/available")
    @Operation(summary = "Find available imaging equipment",
            description = "Public citizen search by imaging type code and city. No account or token is required. "
                    + "Returns active equipment whose status is AVAILABLE; an empty list means no matches.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching available equipment"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public List<EquipmentResponse> findAvailable(
            @Parameter(description = "Existing imaging type code", example = "MRI")
            @RequestParam("type") @NotBlank String type,
            @Parameter(description = "City used to filter hospitals", example = "Fes")
            @RequestParam("city") @NotBlank String city) {
        return service.findAvailable(type, city);
    }
}
