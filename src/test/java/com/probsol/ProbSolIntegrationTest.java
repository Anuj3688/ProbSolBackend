package com.probsol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.probsol.dto.request.CreateEntryRequest;
import com.probsol.dto.request.LoginRequest;
import com.probsol.dto.request.RegisterRequest;
import com.probsol.dto.request.UpdateStatusRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:file:testdb?mode=memory&cache=shared",
    "probsol.seed.enabled=true",
    "probsol.seed.email=anuj@probsol.dev",
    "probsol.seed.password=Password123!",
    "probsol.seed.display-name=Anuj Tiwari"
})
public class ProbSolIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify seeded account can log in immediately")
    void testSeedAccountLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("anuj@probsol.dev", "Password123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.email", is("anuj@probsol.dev")))
                .andExpect(jsonPath("$.data.user.displayName", is("Anuj Tiwari")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("probsol_rt"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String token = root.path("data").path("accessToken").asText();

        // Check seeded timeline
        mockMvc.perform(get("/api/v1/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.pagination.totalItems", greaterThanOrEqualTo(5)));

        // Check seeded analytics
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEntries", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data.problems.total", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.solutions.total", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.topTags", notNullValue()));
    }

    @Test
    @DisplayName("Tenant isolation test: User B cannot view or modify User A's entries")
    void testTenantIsolation() throws Exception {
        // Register User A
        String emailA = "user_a_" + System.currentTimeMillis() + "@example.com";
        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(emailA, "Pass1234!", "User A"))))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Register User B
        String emailB = "user_b_" + System.currentTimeMillis() + "@example.com";
        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(emailB, "Pass1234!", "User B"))))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // User A creates a confidential entry
        CreateEntryRequest entryA = new CreateEntryRequest("problem", "OPEN", "User A Private Note", "Secret", List.of("private"));
        MvcResult createRes = mockMvc.perform(post("/api/v1/entries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryA)))
                .andExpect(status().isCreated())
                .andReturn();
        String entryAId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // User B queries /entries -> should not see User A's entry
        mockMvc.perform(get("/api/v1/entries")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalItems", is(0)));

        // User B directly attempts to get User A's entry by ID -> 404 Not Found
        mockMvc.perform(get("/api/v1/entries/" + entryAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        // User B directly attempts to delete User A's entry -> 404 Not Found
        mockMvc.perform(delete("/api/v1/entries/" + entryAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Full lifecycle test: Registration, Login, Token Refresh, CRUD, Search, and Analytics")
    void testFullLifecycle() throws Exception {
        String testEmail = "testuser_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest(testEmail, "SecretPass123!", "Test User");

        // 1. Register
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.email", is(testEmail)))
                .andExpect(cookie().exists("probsol_rt"))
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = regNode.path("data").path("accessToken").asText();
        Cookie refreshCookie = regResult.getResponse().getCookie("probsol_rt");
        assertNotNull(refreshCookie);

        // 2. GET /me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(testEmail)))
                .andExpect(jsonPath("$.data.displayName", is("Test User")));

        // 3. Create Entry 1 (Problem)
        CreateEntryRequest entry1 = new CreateEntryRequest(
                "problem",
                "OPEN",
                "High database connection latency in SQLite",
                "Pool exhaustion when many threads open connections simultaneously.",
                List.of("database", "sqlite", "backend")
        );

        MvcResult create1Result = mockMvc.perform(post("/api/v1/entries")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is(entry1.title())))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.tags", hasItem("sqlite")))
                .andReturn();

        String entry1Id = objectMapper.readTree(create1Result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 4. Create Entry 2 (Solution)
        CreateEntryRequest entry2 = new CreateEntryRequest(
                "solution",
                null, // Should default to SOLVED
                "Use HikariCP shared pool with minIdle keepalive",
                "Configuring minIdle=1 keeps memory database alive across connections.",
                List.of("sqlite", "backend")
        );

        mockMvc.perform(post("/api/v1/entries")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SOLVED")));

        // 5. Query /entries with search filter 'latency'
        mockMvc.perform(get("/api/v1/entries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "latency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id", is(entry1Id)));

        // 6. Query /entries with tag filter and tag_mode=all
        mockMvc.perform(get("/api/v1/entries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("tags", "database,sqlite")
                        .param("tag_mode", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // 7. Update status of Entry 1 to SOLVED
        UpdateStatusRequest statusUpdate = new UpdateStatusRequest("SOLVED");
        mockMvc.perform(patch("/api/v1/entries/" + entry1Id + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SOLVED")));

        // 8. Tags check
        mockMvc.perform(get("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)));

        // 9. Analytics check
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEntries", is(2)))
                .andExpect(jsonPath("$.data.problems.total", is(1)))
                .andExpect(jsonPath("$.data.problems.solved", is(1)))
                .andExpect(jsonPath("$.data.solutions.total", is(1)))
                .andExpect(jsonPath("$.data.solveRatio", is(1.0)));

        // 10. Soft Delete Entry 1
        mockMvc.perform(delete("/api/v1/entries/" + entry1Id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Entry removed successfully")));

        // Verify Entry 1 is no longer in listing
        mockMvc.perform(get("/api/v1/entries")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // Verify Entry 1 returns 404 on direct fetch
        mockMvc.perform(get("/api/v1/entries/" + entry1Id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Entry not found")));

        // 11. Refresh Token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("probsol_rt"))
                .andReturn();

        Cookie newRefreshCookie = refreshResult.getResponse().getCookie("probsol_rt");

        // 12. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(newRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }
}
