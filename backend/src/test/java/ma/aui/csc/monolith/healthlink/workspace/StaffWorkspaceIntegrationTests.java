package ma.aui.csc.monolith.healthlink.workspace;

import java.util.UUID;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.EquipmentRepository;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.ImagingTypeRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Hospital;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Region;
import ma.aui.csc.monolith.healthlink.organization.data.repository.DepartmentRepository;
import ma.aui.csc.monolith.healthlink.organization.data.repository.HospitalRepository;
import ma.aui.csc.monolith.healthlink.organization.data.repository.RegionRepository;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffRole;
import ma.aui.csc.monolith.healthlink.staff.data.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff screens: inventory, repair queue, staff management, Ministry figures and reference data. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:workspace;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffWorkspaceIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired RegionRepository regions;
    @Autowired HospitalRepository hospitals;
    @Autowired DepartmentRepository departments;
    @Autowired ImagingTypeRepository imagingTypes;
    @Autowired EquipmentRepository equipment;
    @Autowired StaffMemberRepository staff;

    private String suffix;
    private Region region;
    private Hospital hospital;
    private Hospital foreign;
    private Department radiology;
    private Department otherDepartment;
    private ImagingType type;
    private Equipment machine;
    private Equipment otherMachine;
    private Equipment foreignMachine;
    private StaffMember operator;
    private StaffMember otherOperator;
    private StaffMember technician;
    private StaffMember manager;
    private StaffMember foreignTechnician;
    private StaffMember foreignManager;
    private StaffMember ministryOfficer;

    @BeforeEach
    void fixtures() {
        SecurityContextHolder.clearContext();
        suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        region = regions.saveAndFlush(new Region("R_" + suffix, "Region " + suffix));
        hospital = hospitals.saveAndFlush(new Hospital("H_" + suffix, "Demo hospital", "City", "Demo address", region));
        foreign = hospitals.saveAndFlush(new Hospital("F_" + suffix, "Other hospital", "Other city", "Demo address", region));
        radiology = departments.saveAndFlush(new Department("RAD", "Radiology", hospital));
        otherDepartment = departments.saveAndFlush(new Department("RAD2", "Imaging annex", hospital));
        Department foreignDepartment = departments.saveAndFlush(new Department("RAD", "Radiology", foreign));
        type = imagingTypes.saveAndFlush(new ImagingType("MRI_" + suffix, "MRI " + suffix));
        machine = equipment.saveAndFlush(new Equipment("SN1_" + suffix, "Scanner A", radiology, type));
        otherMachine = equipment.saveAndFlush(new Equipment("SN2_" + suffix, "Scanner B", otherDepartment, type));
        foreignMachine = equipment.saveAndFlush(new Equipment("SN3_" + suffix, "Scanner C", foreignDepartment, type));
        operator = person("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, radiology);
        otherOperator = person("other-operator", StaffRole.RADIOLOGY_OPERATOR, hospital, otherDepartment);
        technician = person("technician", StaffRole.MAINTENANCE_TECHNICIAN, hospital, null);
        manager = person("manager", StaffRole.HOSPITAL_MANAGER, hospital, null);
        foreignTechnician = person("foreign-tech", StaffRole.MAINTENANCE_TECHNICIAN, foreign, null);
        foreignManager = person("foreign-manager", StaffRole.HOSPITAL_MANAGER, foreign, null);
        ministryOfficer = person("ministry", StaffRole.MINISTRY_OFFICER, null, null);
    }

    @Test
    void equipmentInventoryIsScopedToTheCallersHospitalAndDepartment() throws Exception {
        mvc.perform(get("/api/equipment").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].id", containsInAnyOrder(machine.getId().intValue(), otherMachine.getId().intValue())));
        mvc.perform(get("/api/equipment").with(as(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains(machine.getId().intValue())));
        mvc.perform(get("/api/equipment").param("departmentId", otherDepartment.getId().toString()).with(as(operator)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/equipment").param("hospitalId", foreign.getId().toString()).with(as(manager)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/equipment").param("status", "OUT_OF_SERVICE").with(as(technician)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/equipment").param("size", "101").with(as(manager)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/equipment")).andExpect(status().isUnauthorized());
    }

    @Test
    void repairQueueShowsUnverifiedOrdersOfTheCallersHospitalOnly() throws Exception {
        reportFault(machine);
        mvc.perform(get("/api/work-orders/queue").with(as(technician)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].equipmentId").value(machine.getId()))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"));
        mvc.perform(get("/api/work-orders/queue").with(as(foreignTechnician)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/work-orders/queue").with(as(otherOperator)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/work-orders/queue").param("status", "VERIFIED").with(as(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/work-orders/queue").with(as(ministryOfficer))).andExpect(status().isForbidden());
    }

    @Test
    void managersManageTheirOwnHospitalStaffAndAdministratorsManageEveryone() throws Exception {
        mvc.perform(get("/api/staff").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get("/api/staff").param("hospitalId", foreign.getId().toString()).with(as(manager)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/staff").param("hospitalId", foreign.getId().toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", containsInAnyOrder(
                        foreignTechnician.getId().intValue(), foreignManager.getId().intValue())));
        mvc.perform(get("/api/staff").with(as(operator))).andExpect(status().isForbidden());

        mvc.perform(post("/api/staff/{id}/deactivate", technician.getId()).with(as(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/staff/me").with(as(technician))).andExpect(status().isForbidden());
        mvc.perform(post("/api/staff/{id}/activate", technician.getId()).with(as(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        mvc.perform(get("/api/staff/me").with(as(technician))).andExpect(status().isOk());

        mvc.perform(post("/api/staff/{id}/deactivate", foreignTechnician.getId()).with(as(manager)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/staff/{id}/deactivate", manager.getId()).with(as(manager)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/staff/{id}/deactivate", foreignManager.getId()).with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(post("/api/staff/{id}/deactivate", technician.getId()).with(as(operator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ministryOfficersSeeRegionalAndNationalFigures() throws Exception {
        mvc.perform(get("/api/ministry/availability").param("imagingType", type.getCode()).with(as(ministryOfficer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagingType").value(type.getCode()))
                .andExpect(jsonPath("$.national.totalMachines").value(3))
                .andExpect(jsonPath("$.national.available").value(3))
                .andExpect(jsonPath("$.national.availabilityPercent").value(100.0))
                .andExpect(jsonPath("$.regions[?(@.regionId == " + region.getId() + ")].figures.hospitals", contains(2)));
        reportFault(machine);
        mvc.perform(get("/api/ministry/availability").param("imagingType", type.getCode()).with(as(ministryOfficer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.national.available").value(2))
                .andExpect(jsonPath("$.national.outOfService").value(1))
                .andExpect(jsonPath("$.national.availabilityPercent").value(66.7));
        mvc.perform(get("/api/ministry/availability").with(as(manager))).andExpect(status().isForbidden());
        mvc.perform(get("/api/ministry/availability").with(asSubject("unlinked-" + suffix, "MINISTRY_OFFICER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorsRecordHospitalLocationsAndImagingTypes() throws Exception {
        mvc.perform(patch("/api/organization/hospitals/{id}/location", hospital.getId()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":34.03,\"longitude\":-5.0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value(34.03));
        mvc.perform(get("/api/organization/hospitals/{id}", hospital.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.longitude").value(-5.0));
        mvc.perform(patch("/api/organization/hospitals/{id}/location", hospital.getId()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":95,\"longitude\":-5.0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/organization/hospitals/{id}/location", hospital.getId()).with(as(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":34.03,\"longitude\":-5.0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/organization/hospitals").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HALF_" + suffix + "\",\"name\":\"Half\",\"city\":\"Fes\",\"address\":\"Demo\","
                                + "\"regionId\":" + region.getId() + ",\"latitude\":34.0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/imaging-types"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].code", hasItem(type.getCode())));
        String newType = "{\"code\":\"PET_" + suffix + "\",\"name\":\"PET-CT\"}";
        mvc.perform(post("/api/imaging-types").with(admin()).contentType(MediaType.APPLICATION_JSON).content(newType))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("PET_" + suffix));
        mvc.perform(post("/api/imaging-types").with(admin()).contentType(MediaType.APPLICATION_JSON).content(newType))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/imaging-types").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"lower\",\"name\":\"Bad\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/imaging-types").with(as(manager)).contentType(MediaType.APPLICATION_JSON).content(newType))
                .andExpect(status().isForbidden());
    }

    @Test
    void nearbySearchIsPublicAndValidatesItsInput() throws Exception {
        mvc.perform(get("/api/availability/nearby").param("type", "MRI").param("lat", "95").param("lng", "-5"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability/nearby").param("lat", "34").param("lng", "-5"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability/nearby").param("type", "MRI").param("lat", "34").param("lng", "-5")
                        .param("radiusKm", "0"))
                .andExpect(status().isBadRequest());
    }

    private void reportFault(Equipment target) throws Exception {
        Long version = equipment.findById(target.getId()).orElseThrow().getVersion();
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":" + target.getId() + ",\"equipmentVersion\":" + version
                                + ",\"description\":\"Startup self-test fails\"}"))
                .andExpect(status().isCreated());
    }

    private StaffMember person(String name, StaffRole role, Hospital assignedHospital, Department department) {
        String subject = name + "-" + suffix;
        return staff.saveAndFlush(new StaffMember(subject, name, subject + "@example.test", role, assignedHospital, department));
    }

    private RequestPostProcessor as(StaffMember actor) {
        return asSubject(actor.getKeycloakSubject(), actor.getRole().name());
    }

    private RequestPostProcessor admin() {
        return asSubject("platform-admin-" + suffix, "PLATFORM_ADMIN");
    }

    private RequestPostProcessor asSubject(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
