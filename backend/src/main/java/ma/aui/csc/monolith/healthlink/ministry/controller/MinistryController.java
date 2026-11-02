package ma.aui.csc.monolith.healthlink.ministry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.ministry.data.MinistryDtos.AvailabilitySummaryResponse;
import ma.aui.csc.monolith.healthlink.ministry.service.MinistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ministry")
@RequiredArgsConstructor
@Tag(name = "Ministry", description = "Regional and national availability summaries")
// No class-level @Validated: Spring MVC's built-in parameter validation answers 400 for invalid input.
public class MinistryController {
    private final MinistryService service;

    @GetMapping("/availability")
    @Operation(summary = "Availability by region and nationwide",
            description = "Requires MINISTRY_OFFICER with an active staff profile. Counts active machines per status "
                    + "for every active region, plus national totals. Optional imagingType filter.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability summary"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public AvailabilitySummaryResponse availability(
            @Parameter(description = "Imaging type code; omit for all types", example = "MRI")
            @RequestParam(required = false) @Size(max = 30) String imagingType) {
        return service.summary(imagingType);
    }
}
