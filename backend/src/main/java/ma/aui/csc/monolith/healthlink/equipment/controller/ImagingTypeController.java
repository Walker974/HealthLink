package ma.aui.csc.monolith.healthlink.equipment.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeDtos.CreateImagingTypeRequest;
import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeDtos.ImagingTypeResponse;
import ma.aui.csc.monolith.healthlink.equipment.service.ImagingTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imaging-types")
@RequiredArgsConstructor
@Tag(name = "Imaging types", description = "Catalog of searchable imaging services")
public class ImagingTypeController {
    private final ImagingTypeService service;

    @GetMapping
    @Operation(summary = "List imaging types", description = "Public. Active imaging services, sorted by name.")
    @ApiResponse(responseCode = "200", description = "Active imaging types")
    public List<ImagingTypeResponse> list() {
        return service.listActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an imaging type", description = "Requires PLATFORM_ADMIN. Codes are unique.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imaging type created"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public ImagingTypeResponse create(@Valid @RequestBody CreateImagingTypeRequest request) {
        return service.create(request);
    }
}
