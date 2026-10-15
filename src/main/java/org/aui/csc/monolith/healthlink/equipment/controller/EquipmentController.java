package org.aui.csc.monolith.healthlink.equipment.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.aui.csc.monolith.healthlink.equipment.dto.EquipmentDtos.ChangeStatusRequest;
import org.aui.csc.monolith.healthlink.equipment.dto.EquipmentDtos.EquipmentResponse;
import org.aui.csc.monolith.healthlink.equipment.dto.EquipmentDtos.RegisterEquipmentRequest;
import org.aui.csc.monolith.healthlink.equipment.service.EquipmentService;
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
 * @Validated: also checks rules on simple parameters (@RequestParam).
 */
@RestController
@RequestMapping("/api/equipment")
@Validated
public class EquipmentController {

    // The business service this web service delegates to.
    private final EquipmentService service;

    // Constructor injection: Spring passes the EquipmentService bean here.
    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    /**
     * POST /api/equipment          -> register a new machine (CREATE)
     * Body: {"serialNumber": "...", "model": "...", "departmentId": 3, "imagingTypeCode": "MRI"}
     * Reply: 201 Created + Location header pointing to the new resource.
     */
    @PostMapping
    public ResponseEntity<EquipmentResponse> register(
            @Valid @RequestBody RegisterEquipmentRequest request) {
        EquipmentResponse created = service.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PATCH /api/equipment/{id}/status   -> change ONE field (partial update)
     * PATCH, not PUT: we do not replace the whole machine, only its status.
     * Body: {"status": "OUT_OF_SERVICE"}
     */
    @PatchMapping("/{id}/status")
    public EquipmentResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    /**
     * GET /api/equipment/available?type=MRI&city=Fes   -> the citizen's search (READ)
     * @RequestParam reads values from the URL after the "?".
     */
    @GetMapping("/available")
    public List<EquipmentResponse> findAvailable(
            @RequestParam("type") @NotBlank String type,
            @RequestParam("city") @NotBlank String city) {
        return service.findAvailable(type, city);
    }
}
