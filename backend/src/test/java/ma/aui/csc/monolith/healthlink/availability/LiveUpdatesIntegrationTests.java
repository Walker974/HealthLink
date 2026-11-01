package ma.aui.csc.monolith.healthlink.availability;

import java.util.UUID;

import ma.aui.csc.monolith.healthlink.availability.service.AvailabilityCache;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Single-instance mode (Redis disabled, in-memory cache): the same flow Redis distributes across instances. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:live;DB_CLOSE_DELAY=-1",
        "healthlink.cache.enabled=true",
        "healthlink.redis.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveUpdatesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AvailabilityCache cache;
    @Autowired RegionRepository regions;
    @Autowired HospitalRepository hospitals;
    @Autowired DepartmentRepository departments;
    @Autowired ImagingTypeRepository imagingTypes;
    @Autowired EquipmentRepository equipment;
    @Autowired StaffMemberRepository staff;

    private Equipment machine;
    private StaffMember operator;

    @BeforeEach
    void fixtures() {
        SecurityContextHolder.clearContext();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Region region = regions.saveAndFlush(new Region("L_" + suffix, "Live region"));
        Hospital hospital = hospitals.saveAndFlush(new Hospital("LH_" + suffix, "Live hospital", "Fes", "Demo", region));
        Department department = departments.saveAndFlush(new Department("RAD", "Radiology", hospital));
        ImagingType type = imagingTypes.saveAndFlush(new ImagingType("CT_" + suffix, "CT"));
        machine = equipment.saveAndFlush(new Equipment("LIVE_" + suffix, "Live scanner", department, type));
        String subject = "live-operator-" + suffix;
        operator = staff.saveAndFlush(new StaffMember(subject, "Live operator", subject + "@example.test",
                StaffRole.RADIOLOGY_OPERATOR, hospital, department));
    }

    @Test
    void openStreamsReceiveCommittedStatusChanges() throws Exception {
        MvcResult stream = mvc.perform(get("/api/availability/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();
        assertThat(stream.getResponse().getContentAsString()).contains("event:connected");

        reportFault();

        String events = stream.getResponse().getContentAsString();
        assertThat(events).contains("event:equipment-status");
        assertThat(events).contains("\"equipmentId\":" + machine.getId());
        assertThat(events).contains("\"status\":\"OUT_OF_SERVICE\"");
        assertThat(events).contains("\"previousStatus\":\"AVAILABLE\"");
        // Public channel: no staff identity or free-text notes are broadcast.
        assertThat(events).doesNotContain(operator.getKeycloakSubject()).doesNotContain("Startup self-test fails");
    }

    @Test
    void committedChangesClearCachedSearches() throws Exception {
        String key = "test:" + UUID.randomUUID();
        assertThat(cache.get(key, String.class, () -> "first")).isEqualTo("first");
        assertThat(cache.get(key, String.class, () -> "second")).isEqualTo("first");

        reportFault();

        assertThat(cache.get(key, String.class, () -> "third")).isEqualTo("third");
    }

    private void reportFault() throws Exception {
        Long version = equipment.findById(machine.getId()).orElseThrow().getVersion();
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":" + machine.getId() + ",\"equipmentVersion\":" + version
                                + ",\"description\":\"Startup self-test fails\"}"))
                .andExpect(status().isCreated());
    }

    private RequestPostProcessor as(StaffMember actor) {
        return jwt().jwt(token -> token.subject(actor.getKeycloakSubject()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + actor.getRole().name()));
    }
}
