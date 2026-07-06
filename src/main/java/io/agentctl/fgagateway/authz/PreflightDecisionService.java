package io.agentctl.fgagateway.authz;

import org.springframework.stereotype.Service;

@Service
public class PreflightDecisionService {
    private final FgaCheckClient checks;

    public PreflightDecisionService(FgaCheckClient checks) {
        this.checks = checks;
    }

    public PreflightDecision decide(PreflightRequest request) {
        try {
            boolean userAllowed = checks.check(new FgaCheck(
                    "user:" + request.userId(),
                    request.resourceRelation(),
                    request.resourceObject()));
            if (!userAllowed) {
                return new PreflightDecision(false, DecisionReason.USER_DENIED);
            }

            boolean agentAllowed = checks.check(new FgaCheck(
                    "agent:" + request.agentId(),
                    "can_invoke",
                    "tool:" + request.toolName()));
            if (!agentAllowed) {
                return new PreflightDecision(false, DecisionReason.AGENT_DENIED);
            }

            return new PreflightDecision(true, DecisionReason.ALLOWED);
        } catch (RuntimeException e) {
            return new PreflightDecision(false, DecisionReason.CHECK_ERROR);
        }
    }
}
