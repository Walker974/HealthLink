package ma.aui.csc.monolith.healthlink.equipment.service;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeDtos.CreateImagingTypeRequest;
import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeDtos.ImagingTypeResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeMapper;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.ImagingTypeRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * The catalog of imaging services (MRI, CT, X-ray...). Because types are data, not a Java enum,
 * a new service can be offered without changing or redeploying code.
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImagingTypeService {
    private final ImagingTypeRepository repository;
    private final ImagingTypeMapper mapper;

    /** Public: the citizen search form needs the list of searchable services. */
    public List<ImagingTypeResponse> listActive() {
        return repository.findByActiveTrueOrderByNameAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ImagingTypeResponse create(@NotNull @Valid CreateImagingTypeRequest request) {
        ImagingType type = new ImagingType(request.code(), request.name().strip());
        return mapper.toResponse(repository.saveAndFlush(type));
    }
}
