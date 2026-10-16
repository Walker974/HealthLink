package org.aui.csc.monolith.healthlink.staff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aui.csc.monolith.healthlink.staff.service.StaffService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.aui.csc.monolith.healthlink.staff.dto.StaffDtos.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Link existing Keycloak users to local staff assignments and inspect your profile")
public class StaffController {
    private final StaffService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Link a Keycloak user to a staff profile",
            description = "Requires PLATFORM_ADMIN. The user must be created and assigned the matching realm role "
                    + "in Keycloak separately. This endpoint stores the immutable Keycloak subject and organizational "
                    + "assignment; it does not create Keycloak accounts, set passwords or grant realm roles. "
                    + "Hospital roles require a hospital. Only RADIOLOGY_OPERATOR requires a department, "
                    + "which must belong to that hospital. PLATFORM_ADMIN and MINISTRY_OFFICER have neither assignment.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff profile linked"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return service.create(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get your staff profile",
            description = "Requires a valid Keycloak token and an active local staff profile with a matching role. "
                    + "An unlinked platform administrator can provision organizations and staff, "
                    + "but receives 403 here until a local profile is linked.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user's staff profile"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public StaffResponse me() {
        return service.me();
    }
}
