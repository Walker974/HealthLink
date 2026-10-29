package ma.aui.csc.monolith.healthlink.security;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the real JWT decoder, filter chain, method security, services and database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTests {
    private static final String ISSUER = "http://localhost:8180/realms/healthlink";
    private static final RSAKey SIGNING_KEY = signingKey();
    private static final HttpServer JWK_SERVER = jwkServer();

    @Autowired MockMvc mvc;
    @Autowired RegionRepository regions;
    @Autowired HospitalRepository hospitals;
    @Autowired DepartmentRepository departments;
    @Autowired ImagingTypeRepository imagingTypes;
    @Autowired EquipmentRepository equipment;
    @Autowired StaffMemberRepository staff;

    private Hospital hospital;
    private Hospital otherHospital;
    private Department department;
    private Department otherDepartment;
    private Equipment machine;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWK_SERVER.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stopJwkServer() {
        JWK_SERVER.stop(0);
    }

    @BeforeEach
    void fixtures() {
        Region region = regions.save(new Region("FES", "Fes Meknes"));
        hospital = hospitals.save(new Hospital("H1", "Hospital one", "Fes", "Address one", region));
        otherHospital = hospitals.save(new Hospital("H2", "Hospital two", "Fes", "Address two", region));
        department = departments.save(new Department("RAD", "Radiology", hospital));
        otherDepartment = departments.save(new Department("RAD2", "Other radiology", hospital));
        ImagingType type = imagingTypes.save(new ImagingType("MRI", "MRI"));
        machine = equipment.save(new Equipment("SN-1", "Model", department, type));
    }

    @Test
    void citizenSearchIsPublicAndDoesNotCreateSession() throws Exception {
        mvc.perform(get("/api/equipment/available").param("type", "MRI").param("city", "Fes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(machine.getId()))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
        mvc.perform(get("/api/organization/regions")).andExpect(status().isOk());
    }

    @Test
    void protectedWriteRequiresBearerToken() throws Exception {
        mvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(registration()))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
        mvc.perform(post("/api/equipment").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void platformAdminCanOnboardOrganizationsWithoutEquipmentAccess() throws Exception {
        String token = token("admin", "PLATFORM_ADMIN");
        mvc.perform(post("/api/organization/regions").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"RAB\",\"name\":\"Rabat\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/equipment").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(registration()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/equipment/{id}/status", machine.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OUT_OF_SERVICE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanRegisterEquipmentOnlyInAssignedHospital() throws Exception {
        assign("manager", StaffRole.HOSPITAL_MANAGER, hospital, null);
        mvc.perform(post("/api/equipment").header("Authorization", token("manager", "HOSPITAL_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(registration()))
                .andExpect(status().isCreated());
    }

    @Test
    void managerCannotRegisterEquipmentOrCreateDepartmentsInAnotherHospital() throws Exception {
        assign("manager", StaffRole.HOSPITAL_MANAGER, otherHospital, null);
        String token = token("manager", "HOSPITAL_MANAGER");
        mvc.perform(post("/api/equipment").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(registration()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/organization/departments").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"code":"NEW","name":"New department","hospitalId":%d}
                                """.formatted(hospital.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanReportFaultInAssignedDepartment() throws Exception {
        assign("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, department);
        reportFault(token("operator", "RADIOLOGY_OPERATOR"), 200);
        assertThat(machine.getStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
    }

    @Test
    void operatorCannotTouchAnotherDepartmentInSameHospital() throws Exception {
        assign("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, otherDepartment);
        reportFault(token("operator", "RADIOLOGY_OPERATOR"), 403);
        assertThat(machine.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void operatorCannotTouchAnotherHospital() throws Exception {
        Department foreignDepartment = departments.save(new Department("RAD", "Radiology", otherHospital));
        assign("operator", StaffRole.RADIOLOGY_OPERATOR, otherHospital, foreignDepartment);
        reportFault(token("operator", "RADIOLOGY_OPERATOR"), 403);
    }

    @Test
    void genericStatusEndpointCannotRestoreAvailabilityOrPerformRepairs() throws Exception {
        assign("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, department);
        machine.changeStatus(EquipmentStatus.OUT_OF_SERVICE);
        for (String newStatus : List.of("AVAILABLE", "UNDER_MAINTENANCE")) {
            mvc.perform(patch("/api/equipment/{id}/status", machine.getId())
                            .header("Authorization", token("operator", "RADIOLOGY_OPERATOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + newStatus
                                    + "\",\"version\":" + machine.getVersion() + ",\"description\":\"Fault report\"}"))
                    .andExpect(status().isBadRequest());
        }
        reportFault(token("technician", "MAINTENANCE_TECHNICIAN"), 403);
    }

    @Test
    void missingInactiveOrMismatchedStaffProfileCannotUseTokenRoleAlone() throws Exception {
        reportFault(token("unlinked", "RADIOLOGY_OPERATOR"), 403);
        reportFault(token(null, "RADIOLOGY_OPERATOR"), 403);
        StaffMember inactive = assign("inactive", StaffRole.RADIOLOGY_OPERATOR, hospital, department);
        inactive.deactivate();
        reportFault(token("inactive", "RADIOLOGY_OPERATOR"), 403);
        assign("wrong-role", StaffRole.HOSPITAL_MANAGER, hospital, null);
        reportFault(token("wrong-role", "RADIOLOGY_OPERATOR"), 403);
    }

    @Test
    void inactiveHospitalOrDepartmentDeniesChanges() throws Exception {
        assign("operator", StaffRole.RADIOLOGY_OPERATOR, hospital, department);
        department.deactivate();
        reportFault(token("operator", "RADIOLOGY_OPERATOR"), 403);
        department.activate();
        hospital.deactivate();
        reportFault(token("operator", "RADIOLOGY_OPERATOR"), 403);
    }

    @Test
    void departmentDataCannotBeReadAcrossHospitals() throws Exception {
        assign("manager", StaffRole.HOSPITAL_MANAGER, otherHospital, null);
        String token = token("manager", "HOSPITAL_MANAGER");
        mvc.perform(get("/api/organization/departments/{id}", department.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/organization/hospitals/{id}/departments", hospital.getId())
                        .header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanLinkExistingKeycloakUserAndUserCanReadOwnProfile() throws Exception {
        mvc.perform(post("/api/staff").header("Authorization", token("admin", "PLATFORM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"keycloakSubject":"new-manager","fullName":"Demo Manager",
                                 "email":"manager@example.test","role":"HOSPITAL_MANAGER","hospitalId":%d}
                                """.formatted(hospital.getId())))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/staff/me").header("Authorization", token("new-manager", "HOSPITAL_MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hospitalId").value(hospital.getId()));
    }

    @Test
    void staffCannotProvisionThemselvesOrAnotherAccount() throws Exception {
        mvc.perform(post("/api/staff").header("Authorization", token("operator", "RADIOLOGY_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void provisioningRejectsDepartmentFromDifferentHospital() throws Exception {
        mvc.perform(post("/api/staff").header("Authorization", token("admin", "PLATFORM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"keycloakSubject":"invalid","fullName":"Demo Operator","email":"invalid@example.test",
                                 "role":"RADIOLOGY_OPERATOR","hospitalId":%d,"departmentId":%d}
                                """.formatted(otherHospital.getId(), department.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidSignatureIssuerAudienceAndExpiredTokens() throws Exception {
        String good = jwt("manager", "HOSPITAL_MANAGER", ISSUER, "healthlink-api", Instant.now().plusSeconds(300), SIGNING_KEY);
        String[] invalid = {
                jwt("manager", "HOSPITAL_MANAGER", ISSUER, "healthlink-api", Instant.now().plusSeconds(300), signingKey()),
                jwt("manager", "HOSPITAL_MANAGER", "http://wrong-issuer", "healthlink-api", Instant.now().plusSeconds(300), SIGNING_KEY),
                jwt("manager", "HOSPITAL_MANAGER", ISSUER, "another-api", Instant.now().plusSeconds(300), SIGNING_KEY),
                jwt("manager", "HOSPITAL_MANAGER", ISSUER, "healthlink-api", Instant.now().minusSeconds(300), SIGNING_KEY),
                "malformed"
        };
        for (String invalidToken : invalid) {
            mvc.perform(get("/api/staff/me").header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }
        // A valid token gets past authentication but still requires a local staff assignment.
        mvc.perform(get("/api/staff/me").header("Authorization", "Bearer " + good))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/equipment").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/equipment").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unconfiguredRoutesAreDeniedEvenWithAdminRole() throws Exception {
        mvc.perform(get("/api/future-sensitive-route").header("Authorization", token("admin", "PLATFORM_ADMIN")))
                .andExpect(status().isForbidden());
    }

    private StaffMember assign(String subject, StaffRole role, Hospital assignedHospital, Department assignedDepartment) {
        return staff.saveAndFlush(new StaffMember(subject, "Demo staff", subject + "@example.test", role,
                assignedHospital, assignedDepartment));
    }

    private String registration() {
        return """
                {"serialNumber":"SN-2","model":"Model two","departmentId":%d,"imagingTypeCode":"MRI"}
                """.formatted(department.getId());
    }

    private void reportFault(String token, int expectedStatus) throws Exception {
        mvc.perform(patch("/api/equipment/{id}/status", machine.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OUT_OF_SERVICE\",\"version\":"
                                + machine.getVersion() + ",\"description\":\"Fault reported during startup test\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private String token(String subject, String role) throws JOSEException {
        return "Bearer " + jwt(subject, role, ISSUER, "healthlink-api", Instant.now().plusSeconds(300), SIGNING_KEY);
    }

    private static String jwt(String subject, String role, String issuer, String audience, Instant expiry, RSAKey key)
            throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(subject).issuer(issuer).audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(600))).expirationTime(Date.from(expiry))
                .claim("realm_access", Map.of("roles", List.of(role))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static RSAKey signingKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer jwkServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/certs", exchange -> {
                byte[] json = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(json);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
