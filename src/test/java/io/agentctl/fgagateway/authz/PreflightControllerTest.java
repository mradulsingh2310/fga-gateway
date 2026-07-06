package io.agentctl.fgagateway.authz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PreflightController.class)
@Import(PreflightControllerTest.TestBeans.class)
class PreflightControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void returnsPreflightDecision() throws Exception {
        mvc.perform(post("/v1/preflight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant_acme",
                                  "userId": "alice",
                                  "agentId": "support",
                                  "runId": "run_123",
                                  "toolName": "github_issue.update",
                                  "resourceType": "github_issue",
                                  "resourceId": "42",
                                  "resourceRelation": "can_edit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").value("ALLOWED"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        PreflightDecisionService preflightDecisionService() {
            return new PreflightDecisionService(check -> true);
        }
    }
}
