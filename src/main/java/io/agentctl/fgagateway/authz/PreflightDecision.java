package io.agentctl.fgagateway.authz;

public record PreflightDecision(boolean allowed, DecisionReason reason) {
}
