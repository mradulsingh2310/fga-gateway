package io.agentctl.fgagateway.authz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PreflightDecisionServiceTest {

    @Test
    void allowsOnlyWhenUserCanAccessResourceAndAgentCanInvokeTool() {
        var checks = new RecordingCheckClient(List.of(true, true));
        var service = new PreflightDecisionService(checks);

        PreflightDecision decision = service.decide(request());

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(DecisionReason.ALLOWED);
        assertThat(checks.requests()).containsExactly(
                new FgaCheck("user:alice", "can_edit", "github_issue:42"),
                new FgaCheck("agent:support", "can_invoke", "tool:github_issue.update"));
    }

    @Test
    void deniesWhenUserCannotAccessResource() {
        var service = new PreflightDecisionService(new RecordingCheckClient(List.of(false, true)));

        PreflightDecision decision = service.decide(request());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(DecisionReason.USER_DENIED);
    }

    @Test
    void deniesWhenAgentCannotInvokeTool() {
        var service = new PreflightDecisionService(new RecordingCheckClient(List.of(true, false)));

        PreflightDecision decision = service.decide(request());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(DecisionReason.AGENT_DENIED);
    }

    @Test
    void failsClosedWhenAuthorizationCheckErrors() {
        var service = new PreflightDecisionService(check -> {
            throw new IllegalStateException("OpenFGA unavailable");
        });

        PreflightDecision decision = service.decide(request());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(DecisionReason.CHECK_ERROR);
    }

    private static PreflightRequest request() {
        return new PreflightRequest(
                "tenant_acme",
                "alice",
                "support",
                "run_123",
                "github_issue.update",
                "github_issue",
                "42",
                "can_edit");
    }

    private static final class RecordingCheckClient implements FgaCheckClient {
        private final List<Boolean> results;
        private final List<FgaCheck> requests = new ArrayList<>();
        private int index;

        private RecordingCheckClient(List<Boolean> results) {
            this.results = results;
        }

        @Override
        public boolean check(FgaCheck check) {
            requests.add(check);
            return results.get(index++);
        }

        private List<FgaCheck> requests() {
            return requests;
        }
    }
}
