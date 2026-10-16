package org.aui.csc.monolith.healthlink.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTests {

    @Autowired MockMvc mvc;

    @Test
    void openApiIsPublicAndDistinguishesCitizenAndStaffOperations() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("HealthLink API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/equipment/available'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/organization/hospitals/{hospitalId}'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/equipment'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/organization/departments/{departmentId}'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/staff/me'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/staff'].post.description", containsString("PLATFORM_ADMIN")));
    }

    @Test
    void requestSchemasIncludeExamplesConstraintsAndConditionalAssignments() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreateRegionRequest.required", containsInAnyOrder("code", "name")))
                .andExpect(jsonPath("$.components.schemas.CreateRegionRequest.properties.code.example").value("FES_MEKNES"))
                .andExpect(jsonPath("$.components.schemas.CreateRegionRequest.properties.code.maxLength").value(30))
                .andExpect(jsonPath("$.components.schemas.CreateRegionRequest.properties.code.pattern").value("[A-Z0-9_-]+"))
                .andExpect(jsonPath("$.components.schemas.CreateHospitalRequest.required",
                        containsInAnyOrder("code", "name", "city", "address", "regionId")))
                .andExpect(jsonPath("$.components.schemas.CreateStaffRequest.required",
                        containsInAnyOrder("keycloakSubject", "fullName", "email", "role")))
                .andExpect(jsonPath("$.components.schemas.CreateStaffRequest.properties.keycloakSubject.description",
                        containsString("JWT sub")))
                .andExpect(jsonPath("$.components.schemas.CreateStaffRequest.properties.departmentId.description",
                        containsString("Required only for operators")));
    }

    @Test
    void faultRequestOnlyAdvertisesAcceptedStateWhileResponseRetainsAllStates() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ChangeStatusRequest.properties.status.enum", contains("OUT_OF_SERVICE")))
                .andExpect(jsonPath("$.components.schemas.EquipmentResponse.properties.status.enum",
                        containsInAnyOrder("AVAILABLE", "OUT_OF_SERVICE", "UNDER_MAINTENANCE")))
                .andExpect(jsonPath("$.paths['/api/equipment/{id}/status'].patch.description",
                        containsString("equipment's department")));
    }

    @Test
    void creationResponsesAndPaginationMatchTheHttpContract() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].post.responses['401']['$ref']")
                        .value("#/components/responses/Unauthorized"))
                .andExpect(jsonPath("$.paths['/api/staff'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/organization/regions'].get.parameters[?(@.name == 'size')].schema.maximum",
                        contains(100)))
                .andExpect(jsonPath("$.paths['/api/organization/regions'].get.parameters[?(@.name == 'page')].schema.minimum",
                        contains(0)));
    }

    @Test
    void swaggerPageAndConfigurationAreAccessibleWithoutSigningIn() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
        mvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/v3/api-docs/swagger-config")));
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void yamlSpecificationIsAccessibleWithoutSigningIn() throws Exception {
        mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.")))
                .andExpect(content().string(containsString("HealthLink API")));
    }

    @Test
    void publicDocumentationDoesNotOpenProtectedApiOperations() throws Exception {
        mvc.perform(post("/api/organization/regions")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/future-sensitive-route")).andExpect(status().isUnauthorized());
    }
}
