package org.aui.csc.monolith.healthlink.organization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.aui.csc.monolith.healthlink.organization.entity.Department;
import org.aui.csc.monolith.healthlink.organization.entity.Hospital;
import org.aui.csc.monolith.healthlink.organization.entity.Region;
import org.aui.csc.monolith.healthlink.organization.mapper.OrganizationMapper;
import org.aui.csc.monolith.healthlink.organization.repository.DepartmentRepository;
import org.aui.csc.monolith.healthlink.organization.repository.HospitalRepository;
import org.aui.csc.monolith.healthlink.organization.repository.RegionRepository;

import static org.aui.csc.monolith.healthlink.organization.dto.OrganizationDtos.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final RegionRepository regionRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationMapper mapper;

    @Transactional
    public RegionResponse createRegion(
            @NotNull @Valid CreateRegionRequest request
    ) {
        Region region = new Region(
                request.code(),
                request.name().strip()
        );

        Region saved = regionRepository.saveAndFlush(region);
        return mapper.toResponse(saved);
    }

    @Transactional
    public HospitalResponse createHospital(
            @NotNull @Valid CreateHospitalRequest request
    ) {
        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> notFound("Region"));

        Hospital hospital = new Hospital(
                request.code(),
                request.name().strip(),
                request.city().strip(),
                request.address().strip(),
                region
        );

        Hospital saved = hospitalRepository.saveAndFlush(hospital);
        return mapper.toResponse(saved);
    }

    @Transactional
    public DepartmentResponse createDepartment(
            @NotNull @Valid CreateDepartmentRequest request
    ) {
        Hospital hospital = hospitalRepository
                .findById(request.hospitalId())
                .orElseThrow(() -> notFound("Hospital"));

        Department department = new Department(
                request.code(),
                request.name().strip(),
                hospital
        );

        Department saved = departmentRepository.saveAndFlush(department);
        return mapper.toResponse(saved);
    }

    public RegionResponse getRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> notFound("Region"));

        return mapper.toResponse(region);
    }

    public HospitalResponse getHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> notFound("Hospital"));

        return mapper.toResponse(hospital);
    }

    public DepartmentResponse getDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> notFound("Department"));

        return mapper.toResponse(department);
    }

    public PageResponse<RegionResponse> listRegions(int page, int size) {
        Page<RegionResponse> result = regionRepository
                .findAll(pageRequest(page, size))
                .map(mapper::toResponse);

        return toPageResponse(result);
    }

    public PageResponse<HospitalResponse> listHospitals(
            Long regionId,
            int page,
            int size
    ) {
        PageRequest pageable = pageRequest(page, size);

        if (!regionRepository.existsById(regionId)) {
            throw notFound("Region");
        }

        Page<HospitalResponse> result = hospitalRepository
                .findByRegion_Id(regionId, pageable)
                .map(mapper::toResponse);

        return toPageResponse(result);
    }

    public PageResponse<DepartmentResponse> listDepartments(
            Long hospitalId,
            int page,
            int size
    ) {
        PageRequest pageable = pageRequest(page, size);

        if (!hospitalRepository.existsById(hospitalId)) {
            throw notFound("Hospital");
        }

        Page<DepartmentResponse> result = departmentRepository
                .findByHospital_Id(hospitalId, pageable)
                .map(mapper::toResponse);

        return toPageResponse(result);
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0; size must be between 1 and 100"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by("id").ascending()
        );
    }

    private <T> PageResponse<T> toPageResponse(Page<T> result) {
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                resource + " not found"
        );
    }
}