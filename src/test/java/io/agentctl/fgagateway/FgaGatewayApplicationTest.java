package io.agentctl.fgagateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FgaGatewayApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void startsWithFailClosedPreflightClient() throws Exception {
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
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.reason").value("USER_DENIED"));
    }
}
