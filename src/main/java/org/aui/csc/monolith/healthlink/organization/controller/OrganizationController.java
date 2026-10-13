package org.aui.csc.monolith.healthlink.organization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import org.aui.csc.monolith.healthlink.organization.service.OrganizationService;

import static org.aui.csc.monolith.healthlink.organization.dto.OrganizationDtos.*;

@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    @PostMapping("/regions")
    public ResponseEntity<RegionResponse> createRegion(
            @Valid @RequestBody CreateRegionRequest request
    ) {
        RegionResponse response = service.createRegion(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/regions/{regionId}")
    public RegionResponse getRegion(@PathVariable Long regionId) {
        return service.getRegion(regionId);
    }

    @GetMapping("/regions")
    public PageResponse<RegionResponse> listRegions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listRegions(page, size);
    }

    @PostMapping("/hospitals")
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody CreateHospitalRequest request
    ) {
        HospitalResponse response = service.createHospital(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/hospitals/{hospitalId}")
    public HospitalResponse getHospital(@PathVariable Long hospitalId) {
        return service.getHospital(hospitalId);
    }

    @GetMapping("/regions/{regionId}/hospitals")
    public PageResponse<HospitalResponse> listHospitals(
            @PathVariable Long regionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listHospitals(regionId, page, size);
    }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        DepartmentResponse response = service.createDepartment(request);

        return ResponseEntity
                .created(resourceLocation(response.id()))
                .body(response);
    }

    @GetMapping("/departments/{departmentId}")
    public DepartmentResponse getDepartment(
            @PathVariable Long departmentId
    ) {
        return service.getDepartment(departmentId);
    }

    @GetMapping("/hospitals/{hospitalId}/departments")
    public PageResponse<DepartmentResponse> listDepartments(
            @PathVariable Long hospitalId,
            @RequestParam(defaultValue = "0") int page,
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