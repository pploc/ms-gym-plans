package com.gym.plans.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlansHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenSuperAdmin_whenCreateGymAndPlan_thenPublicHttpCatalogWorks() throws Exception {
        // Given / When
        MvcResult gymResult = mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "admin-1")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"chain-http","name":"HTTP Gym","address":"1 St","city":"Hanoi"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.chainId").value("chain-http"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String gymId = objectMapper.readTree(gymResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult planResult = mockMvc.perform(post("/api/v1/gyms/" + gymId + "/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "admin-1")
                        .header("x-user-role", "SUPER_ADMIN")
                        .header("x-gym-id", gymId)
                        .content(
                                """
                                {"name":"Monthly","planType":"MONTHLY","durationDays":30,"priceVnd":450000,"description":"base"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gymId").value(gymId))
                .andExpect(jsonPath("$.planType").value("MONTHLY"))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        String planId =
                objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asText();

        // Then
        mockMvc.perform(get("/api/v1/gyms/" + gymId)
                        .header("x-user-id", "cust-1")
                        .header("x-user-role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gymId));

        mockMvc.perform(get("/api/v1/gyms/" + gymId + "/plans")
                        .header("x-user-id", "cust-1")
                        .header("x-user-role", "CUSTOMER")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[0].id").value(planId));

        mockMvc.perform(get("/api/v1/plans/" + planId)
                        .header("x-user-id", "cust-1")
                        .header("x-user-role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId));
    }

    @Test
    void givenMissingAuth_whenCreateGym_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"chainId":"c","name":"n","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenMissingAuth_whenReadGymCatalog_thenUnauthorized() throws Exception {
        // Given
        MvcResult gymResult = mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"c","name":"G","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String gymId = objectMapper.readTree(gymResult.getResponse().getContentAsString()).get("id").asText();

        // When / Then
        mockMvc.perform(get("/api/v1/gyms/" + gymId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gyms")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gyms/" + gymId + "/plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void givenCustomer_whenCreateGym_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "cust-1")
                        .header("x-user-role", "CUSTOMER")
                        .content(
                                """
                                {"chainId":"c","name":"n","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminOutsideGym_whenCreatePlan_thenForbidden() throws Exception {
        // Given
        MvcResult gymResult = mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"c","name":"G","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String gymId = objectMapper.readTree(gymResult.getResponse().getContentAsString()).get("id").asText();

        // When / Then
        mockMvc.perform(post("/api/v1/gyms/" + gymId + "/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "admin-2")
                        .header("x-user-role", "ADMIN")
                        .header("x-gym-id", "other-gym")
                        .content(
                                """
                                {"name":"Monthly","planType":"MONTHLY","durationDays":30,"priceVnd":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenInternalWorkloadPaths_whenHttpGet_thenNotMapped() throws Exception {
        mockMvc.perform(get("/api/v1/gyms/active")
                        .header("x-user-id", "u")
                        .header("x-user-role", "SUPER_ADMIN"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/plans/resolve")
                        .header("x-user-id", "u")
                        .header("x-user-role", "SUPER_ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenInvalidPlanType_whenCreatePlan_thenBadRequest() throws Exception {
        MvcResult gymResult = mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"c","name":"G","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode gym = objectMapper.readTree(gymResult.getResponse().getContentAsString());
        assertTrue(gym.get("id").asText().length() > 0);

        mockMvc.perform(post("/api/v1/gyms/" + gym.get("id").asText() + "/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"name":"X","planType":"WEEKLY","durationDays":7,"priceVnd":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void givenClosedStatus_whenUpdateGym_thenPersistsClosed() throws Exception {
        MvcResult gymResult = mockMvc.perform(post("/api/v1/gyms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"c","name":"G","address":"a","city":"Hanoi"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String gymId = objectMapper.readTree(gymResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/gyms/" + gymId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", "sa")
                        .header("x-user-role", "SUPER_ADMIN")
                        .content(
                                """
                                {"chainId":"c","name":"G","address":"a","city":"Hanoi","status":"CLOSED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/gyms")
                        .header("x-user-id", "cust")
                        .header("x-user-role", "CUSTOMER")
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[0].id").value(gymId))
                .andExpect(jsonPath("$.locations[0].status").value("CLOSED"));
    }
}
