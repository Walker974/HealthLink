package ma.aui.csc.monolith.healthlink.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import ma.aui.csc.monolith.healthlink.organization.service.OrganizationService;

import static ma.aui.csc.monolith.healthlink.organization.data.OrganizationDtos.*;

@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
@Tag(name = "Organization", description = "Regions, public hospitals and hospital departments")
public class OrganizationController {

    private final OrganizationService service;

    @PostMapping("/regions")
    @Operation(summary = "Create a region", description = "Requires PLATFORM_ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Region created",
                    headers = @Header(name = "Location", description = "URL of the created region",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<RegionResponse> createRegion(
            @Valid @RequestBody CreateRegionRequest request
    ) {
        RegionResponse response = service.createRegion(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/regions/{regionId}")
    @Operation(summary = "Get a region", description = "Public reference data; no authentication required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region details"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public RegionResponse getRegion(@PathVariable Long regionId) {
        return service.getRegion(regionId);
    }

    @GetMapping("/regions")
    @Operation(summary = "List regions", description = "Public reference data, ordered by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of regions"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public PageResponse<RegionResponse> listRegions(
            @Parameter(description = "Zero-based page index", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listRegions(page, size);
    }

    @PostMapping("/hospitals")
    @Operation(summary = "Create a hospital", description = "Requires PLATFORM_ADMIN and an existing region.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hospital created",
                    headers = @Header(name = "Location", description = "URL of the created hospital",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody CreateHospitalRequest request
    ) {
        HospitalResponse response = service.createHospital(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/hospitals/{hospitalId}")
    @Operation(summary = "Get a hospital", description = "Public reference data; no authentication required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hospital details"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public HospitalResponse getHospital(@PathVariable Long hospitalId) {
        return service.getHospital(hospitalId);
    }

    @GetMapping("/regions/{regionId}/hospitals")
    @Operation(summary = "List hospitals in a region", description = "Public reference data, ordered by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of hospitals"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public PageResponse<HospitalResponse> listHospitals(
            @PathVariable Long regionId,
            @Parameter(description = "Zero-based page index", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listHospitals(regionId, page, size);
    }

    @PostMapping("/departments")
    @Operation(summary = "Create a department",
            description = "Requires HOSPITAL_MANAGER with an active staff assignment to the requested hospital.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Department created",
                    headers = @Header(name = "Location", description = "URL of the created department",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        DepartmentResponse response = service.createDepartment(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/departments/{departmentId}")
    @Operation(summary = "Get a department",
            description = "Requires HOSPITAL_MANAGER, RADIOLOGY_OPERATOR or MAINTENANCE_TECHNICIAN "
                    + "with an active staff assignment to the department's hospital.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department details"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public DepartmentResponse getDepartment(
            @PathVariable Long departmentId
    ) {
        return service.getDepartment(departmentId);
    }

    @GetMapping("/hospitals/{hospitalId}/departments")
    @Operation(summary = "List departments in a hospital",
            description = "Requires HOSPITAL_MANAGER, RADIOLOGY_OPERATOR or MAINTENANCE_TECHNICIAN "
                    + "with an active staff assignment to the requested hospital. Results are ordered by ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of departments"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public PageResponse<DepartmentResponse> listDepartments(
            @PathVariable Long hospitalId,
            @Parameter(description = "Zero-based page index", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listDepartments(hospitalId, page, size);
    }

    private URI resourceLocation(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
