package ma.aui.csc.monolith.healthlink.maintenance;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;
import ma.aui.csc.monolith.healthlink.audit.data.repository.AuditRecordRepository;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.*;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.*;
import ma.aui.csc.monolith.healthlink.equipment.notification.*;
import ma.aui.csc.monolith.healthlink.maintenance.data.entity.*;
import ma.aui.csc.monolith.healthlink.maintenance.data.repository.WorkOrderRepository;
import ma.aui.csc.monolith.healthlink.maintenance.service.WorkOrderService;
import ma.aui.csc.monolith.healthlink.organization.data.entity.*;
import ma.aui.csc.monolith.healthlink.organization.data.repository.*;
import ma.aui.csc.monolith.healthlink.staff.data.entity.*;
import ma.aui.csc.monolith.healthlink.staff.data.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import static ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:maintenance;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkOrderIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired RegionRepository regions;
    @Autowired HospitalRepository hospitals;
    @Autowired DepartmentRepository departments;
    @Autowired ImagingTypeRepository imagingTypes;
    @Autowired EquipmentRepository equipment;
    @Autowired StaffMemberRepository staff;
    @Autowired WorkOrderRepository orders;
    @Autowired WorkOrderService workOrders;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean StatusChangeNotifier notifier;
    @MockitoSpyBean AuditRecordRepository auditRepository;

    private Equipment machine;
    private Hospital hospital;
    private ImagingType type;
    private StaffMember operator;
    private StaffMember otherOperator;
    private StaffMember technician;
    private StaffMember secondTechnician;
    private StaffMember manager;
    private StaffMember foreignTechnician;
    private StaffMember foreignManager;
    private String suffix;

    @BeforeEach
    void fixtures() {
        SecurityContextHolder.clearContext();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        Region region = regions.saveAndFlush(new Region("R_" + suffix, "Demo region"));
        hospital = hospitals.saveAndFlush(new Hospital("H_" + suffix, "Demo hospital", "City_" + suffix, "Demo address", region));
        Hospital foreign = hospitals.saveAndFlush(new Hospital("F_" + suffix, "Other hospital", "Other city", "Demo address", region));
        Department department = departments.saveAndFlush(new Department("RAD", "Radiology", hospital));
        Department otherDepartment = departments.saveAndFlush(new Department("RAD2", "Other department", hospital));
        type = imagingTypes.saveAndFlush(new ImagingType("MRI_" + suffix, "MRI"));
        machine = equipment.saveAndFlush(new Equipment("SN_" + suffix, "Demo scanner", department, type));
        operator = person("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, department);
        otherOperator = person("other-operator", StaffRole.RADIOLOGY_OPERATOR, hospital, otherDepartment);
        technician = person("technician", StaffRole.MAINTENANCE_TECHNICIAN, hospital, null);
        secondTechnician = person("technician-2", StaffRole.MAINTENANCE_TECHNICIAN, hospital, null);
        manager = person("manager", StaffRole.HOSPITAL_MANAGER, hospital, null);
        foreignTechnician = person("foreign-tech", StaffRole.MAINTENANCE_TECHNICIAN, foreign, null);
        foreignManager = person("foreign-manager", StaffRole.HOSPITAL_MANAGER, foreign, null);
    }

    @Test
    void completeRepairCycleRestoresAvailabilityOnlyAfterIndependentVerification() throws Exception {
        availability(1);
        WorkOrderResponse open = report();
        assertThat(open.status()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(open.equipmentStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
        assertThat(open.equipmentVersion()).isEqualTo(machine.getVersion() + 1);
        availability(0);
        WorkOrderResponse accepted = success(open, "accept", technician, new VersionRequest(open.version()));
        assertThat(accepted.repairerId()).isEqualTo(technician.getId());
        assertThat(accepted.equipmentStatus()).isEqualTo(EquipmentStatus.UNDER_MAINTENANCE);
        WorkOrderResponse completed = success(accepted, "complete", technician,
                new CompleteRepairRequest(accepted.version(), "Power supply replaced and tested"));
        assertThat(completed.status()).isEqualTo(WorkOrderStatus.AWAITING_VERIFICATION);
        availability(0);
        WorkOrderResponse verified = success(completed, "verify", manager,
                new ReviewRepairRequest(completed.version(), "Independent checks passed"));
        assertThat(verified.status()).isEqualTo(WorkOrderStatus.VERIFIED);
        assertThat(verified.verifierId()).isEqualTo(manager.getId());
        assertThat(verified.verifiedAt()).isNotNull();
        availability(1);
        mvc.perform(get("/api/equipment/{id}/history", machine.getId()).with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].action", contains("FAULT_REPORTED", "REPAIR_ACCEPTED", "REPAIR_COMPLETED", "REPAIR_VERIFIED")))
                .andExpect(jsonPath("$.items[0].actorSubject").value(operator.getKeycloakSubject()))
                .andExpect(jsonPath("$.items[3].actorStaffId").value(manager.getId()));
        assertThat(jdbc.queryForObject("select created_by_id from work_orders where id = ?", Long.class, open.id()))
                .isEqualTo(operator.getId());
        assertThat(jdbc.queryForObject("select last_modified_by_id from work_orders where id = ?", Long.class, open.id()))
                .isEqualTo(manager.getId());
        verify(notifier, times(3)).statusChanged(any());
        // Verification releases the unique open-order slot, allowing a later fault.
        WorkOrderResponse next = report();
        assertThat(next.id()).isNotEqualTo(open.id());
    }

    @Test
    void rejectionKeepsEquipmentUnavailableAndAllowsAnotherRepairSubmission() throws Exception {
        WorkOrderResponse pending = pending();
        WorkOrderResponse rejected = success(pending, "reject", manager,
                new ReviewRepairRequest(pending.version(), "Image quality check failed"));
        assertThat(rejected.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(rejected.equipmentStatus()).isEqualTo(EquipmentStatus.UNDER_MAINTENANCE);
        assertThat(rejected.completedAt()).isNull();
        availability(0);
        WorkOrderResponse completed = success(rejected, "complete", technician,
                new CompleteRepairRequest(rejected.version(), "Calibration corrected"));
        success(completed, "verify", manager, new ReviewRepairRequest(completed.version(), "Retest passed"));
        availability(1);
        assertThat(auditCount("REPAIR_REJECTED")).isEqualTo(1);
    }

    @Test
    void duplicateFaultsStaleVersionsAndInvalidTransitionsReturnConflictWithoutExtraAudits() throws Exception {
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReportFaultRequest(machine.getId(), 999L, "Fault"))))
                .andExpect(status().isConflict());
        assertThat(auditCount()).isZero();
        WorkOrderResponse open = report();
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReportFaultRequest(machine.getId(), currentEquipmentVersion(), "Duplicate"))))
                .andExpect(status().isConflict());
        action(open, "complete", technician, new CompleteRepairRequest(open.version(), "Premature completion"), 409);
        WorkOrderResponse accepted = success(open, "accept", technician, new VersionRequest(open.version()));
        action(accepted, "accept", technician, new VersionRequest(accepted.version()), 409);
        action(accepted, "complete", technician, new CompleteRepairRequest(open.version(), "Stale submission"), 409);
        action(accepted, "verify", manager, new ReviewRepairRequest(accepted.version(), "Premature verification"), 409);
        assertThat(auditCount()).isEqualTo(2);
    }

    @Test
    void onlyAssignedTechnicianCanCompleteRepair() throws Exception {
        WorkOrderResponse open = report();
        WorkOrderResponse accepted = success(open, "accept", technician, new VersionRequest(open.version()));
        action(accepted, "complete", secondTechnician,
                new CompleteRepairRequest(accepted.version(), "Someone else's repair"), 403);
        assertThat(auditCount()).isEqualTo(2);
    }

    @Test
    void repairerCannotVerifyOrRejectOwnRepairEvenAfterBecomingManager() throws Exception {
        WorkOrderResponse pending = pending();
        jdbc.update("update staff_members set role = 'HOSPITAL_MANAGER', version = version + 1 where id = ?", technician.getId());
        for (String verb : List.of("verify", "reject")) {
            mvc.perform(post("/api/work-orders/{id}/" + verb, pending.id())
                            .with(asSubject(technician.getKeycloakSubject(), "HOSPITAL_MANAGER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new ReviewRepairRequest(pending.version(), "Self review"))))
                    .andExpect(status().isForbidden());
        }
        assertThat(auditCount()).isEqualTo(3);
        availability(0);
    }

    @Test
    void readsAndWritesRespectHospitalDepartmentAndRoleBoundaries() throws Exception {
        mvc.perform(post("/api/work-orders").with(as(otherOperator)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReportFaultRequest(machine.getId(), currentEquipmentVersion(), "Foreign department"))))
                .andExpect(status().isForbidden());
        WorkOrderResponse open = report();
        action(open, "accept", foreignTechnician, new VersionRequest(open.version()), 403);
        action(open, "accept", manager, new VersionRequest(open.version()), 403);
        for (StaffMember denied : List.of(otherOperator, foreignManager, foreignTechnician)) {
            mvc.perform(get("/api/work-orders/{id}", open.id()).with(as(denied))).andExpect(status().isForbidden());
            mvc.perform(get("/api/work-orders").param("equipmentId", machine.getId().toString()).with(as(denied)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/equipment/{id}/history", machine.getId()).with(as(denied))).andExpect(status().isForbidden());
            mvc.perform(get("/api/equipment/{id}", machine.getId()).with(as(denied))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/work-orders/{id}", open.id()).with(asSubject("platform-admin", "PLATFORM_ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/work-orders/{id}", open.id())).andExpect(status().isUnauthorized());
        WorkOrderResponse accepted = success(open, "accept", technician, new VersionRequest(open.version()));
        WorkOrderResponse completed = success(accepted, "complete", technician,
                new CompleteRepairRequest(accepted.version(), "Ready"));
        action(completed, "verify", foreignManager, new ReviewRepairRequest(completed.version(), "Foreign hospital"), 403);
    }

    @Test
    void auditFailureRollsBackEquipmentAndWorkOrderAndSendsNoNotification() throws Exception {
        AuditRecordRepository target = AopTestUtils.getUltimateTargetObject(auditRepository);
        doThrow(new DataIntegrityViolationException("Simulated audit failure")).when(target).append(any());
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReportFaultRequest(machine.getId(), currentEquipmentVersion(), "Fault"))))
                .andExpect(status().isConflict());
        assertThat(equipment.findById(machine.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(orderCount()).isZero();
        assertThat(auditCount()).isZero();
        verifyNoInteractions(notifier);
    }

    @Test
    void outerTransactionRollbackDiscardsQueuedNotificationsAndAudit() {
        var token = Jwt.withTokenValue("test").header("alg", "RS256").subject(operator.getKeycloakSubject()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token,
                List.of(new SimpleGrantedAuthority("ROLE_RADIOLOGY_OPERATOR"))));
        try {
            new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                workOrders.reportFault(new ReportFaultRequest(machine.getId(), currentEquipmentVersion(), "Rolled-back fault"));
                verifyNoInteractions(notifier);
                transaction.setRollbackOnly();
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(orderCount()).isZero();
        assertThat(auditCount()).isZero();
        assertThat(equipment.findById(machine.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        verifyNoInteractions(notifier);
    }

    @Test
    void concurrentReportsCreateOnlyOneOpenOrder() throws Exception {
        Long version = currentEquipmentVersion();
        String body = json.writeValueAsString(new ReportFaultRequest(machine.getId(), version, "Concurrent fault"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> report = () -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(report);
            Future<Integer> second = executor.submit(report);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(orderCount()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);
    }

    @Test
    void notificationsSeeCommittedStateAndDeliveryFailureDoesNotUndoTheCommit() throws Exception {
        doAnswer(invocation -> {
            EquipmentStatusChanged event = invocation.getArgument(0);
            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                    "select status from equipment where id = ?")) {
                statement.setLong(1, event.equipmentId());
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo(event.status().name());
                }
            }
            throw new IllegalStateException("Simulated notification delivery failure");
        }).when(notifier).statusChanged(any());
        WorkOrderResponse open = report();
        assertThat(open.status()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);
        verify(notifier).statusChanged(any());
    }

    @Test
    void concurrentAcceptsHaveOneWinnerAndOneAuditEntry() throws Exception {
        WorkOrderResponse open = report();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var results = List.of(technician, secondTechnician).stream().map(actor -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return mvc.perform(post("/api/work-orders/{id}/accept", open.id()).with(as(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new VersionRequest(open.version()))))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(results.get(0).get(20, TimeUnit.SECONDS), results.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(auditCount("REPAIR_ACCEPTED")).isEqualTo(1);
    }

    @Test
    void databaseGuardPreventsASecondOpenOrderEvenWhenServiceIsBypassed() throws Exception {
        report();
        assertThatThrownBy(() -> orders.saveAndFlush(new WorkOrder(machine, operator, "Duplicate direct insert")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void historySurvivesDeactivationAndPaginationAndPayloadsAreValidated() throws Exception {
        WorkOrderResponse open = report();
        jdbc.update("update equipment set active = false, version = version + 1 where id = ?", machine.getId());
        mvc.perform(get("/api/equipment/{id}/history", machine.getId()).with(as(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        action(open, "accept", technician, new VersionRequest(open.version()), 409);
        for (String route : List.of("/api/work-orders?equipmentId=" + machine.getId() + "&size=101",
                "/api/equipment/" + machine.getId() + "/history?page=-1")) {
            mvc.perform(get(route).with(as(manager))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentId\":" + machine.getId() + ",\"description\":\"Fault\"}"))
                .andExpect(status().isBadRequest());
    }

    private StaffMember person(String name, StaffRole role, Hospital assignedHospital, Department department) {
        String subject = name + "-" + suffix;
        return staff.saveAndFlush(new StaffMember(subject, name, subject + "@example.test", role, assignedHospital, department));
    }

    private RequestPostProcessor as(StaffMember actor) {
        return asSubject(actor.getKeycloakSubject(), actor.getRole().name());
    }

    private RequestPostProcessor asSubject(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private WorkOrderResponse report() throws Exception {
        MvcResult result = mvc.perform(post("/api/work-orders").with(as(operator)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReportFaultRequest(machine.getId(), currentEquipmentVersion(), "Startup test failure"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readValue(result.getResponse().getContentAsString(), WorkOrderResponse.class);
    }

    private WorkOrderResponse pending() throws Exception {
        WorkOrderResponse open = report();
        WorkOrderResponse accepted = success(open, "accept", technician, new VersionRequest(open.version()));
        return success(accepted, "complete", technician, new CompleteRepairRequest(accepted.version(), "Repair completed"));
    }

    private WorkOrderResponse success(WorkOrderResponse order, String verb, StaffMember actor, Object request) throws Exception {
        return json.readValue(action(order, verb, actor, request, 200).getResponse().getContentAsString(), WorkOrderResponse.class);
    }

    private MvcResult action(WorkOrderResponse order, String verb, StaffMember actor, Object request, int expected) throws Exception {
        return mvc.perform(post("/api/work-orders/{id}/" + verb, order.id()).with(as(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().is(expected)).andReturn();
    }

    private Long currentEquipmentVersion() {
        return equipment.findById(machine.getId()).orElseThrow().getVersion();
    }

    private void availability(int count) throws Exception {
        mvc.perform(get("/api/equipment/available").param("type", type.getCode()).param("city", hospital.getCity()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(count)));
    }

    private long orderCount() {
        return jdbc.queryForObject("select count(*) from work_orders where equipment_id = ?", Long.class, machine.getId());
    }

    private long auditCount() {
        return jdbc.queryForObject("select count(*) from audit_records where equipment_id = ?", Long.class, machine.getId());
    }

    private long auditCount(String action) {
        return jdbc.queryForObject("select count(*) from audit_records where equipment_id = ? and action = ?",
                Long.class, machine.getId(), action);
    }
}
