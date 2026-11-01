package ma.aui.csc.monolith.healthlink.availability.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.NearbyHospitalResponse;
import ma.aui.csc.monolith.healthlink.availability.live.SseConnectionRegistry;
import ma.aui.csc.monolith.healthlink.availability.service.AvailabilityService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Public nearby search and live status updates for the citizen map")
// No class-level @Validated: Spring MVC's built-in parameter validation answers 400 for invalid input.
public class AvailabilityController {
    private final AvailabilityService service;
    private final SseConnectionRegistry connections;

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby hospitals with a working imaging service",
            description = "Public; no account or token. Returns hospitals within the radius that have at least one "
                    + "AVAILABLE machine of the type, nearest first. The position is rounded to about 1 km and is "
                    + "never stored. Results may be served from a short-lived cache that is cleared on every change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching hospitals; empty when none"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public List<NearbyHospitalResponse> nearby(
            @Parameter(description = "Imaging type code", example = "MRI")
            @RequestParam @NotBlank @Size(max = 30) String type,
            @Parameter(description = "Latitude of the citizen", example = "33.99")
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @Parameter(description = "Longitude of the citizen", example = "-6.85")
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @Parameter(description = "Search radius in kilometres (1–500)", example = "50")
            @RequestParam(defaultValue = "50") @DecimalMin("1") @DecimalMax("500") double radiusKm,
            @Parameter(description = "Maximum number of hospitals (1–50)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return service.nearby(type, lat, lng, radiusKm, limit);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live equipment status changes",
            description = "Public Server-Sent Events stream. Events: 'connected' once, then 'equipment-status' "
                    + "after every committed change (equipmentId, hospitalId, imagingTypeCode, previousStatus, "
                    + "status, statusUpdatedAt). Open it with the browser's EventSource. After a reconnection, "
                    + "reload the current state with the REST search, because missed events are not replayed.")
    @ApiResponse(responseCode = "200", description = "An open event stream")
    public SseEmitter stream(HttpServletResponse response) {
        // Tell Nginx not to buffer this response, and browsers/proxies not to cache it.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return connections.open();
    }
}
