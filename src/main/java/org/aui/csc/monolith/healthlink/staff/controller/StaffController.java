package org.aui.csc.monolith.healthlink.staff.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aui.csc.monolith.healthlink.staff.service.StaffService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.aui.csc.monolith.healthlink.staff.dto.StaffDtos.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return service.create(request);
    }

    @GetMapping("/me")
    public StaffResponse me() {
        return service.me();
    }
}
