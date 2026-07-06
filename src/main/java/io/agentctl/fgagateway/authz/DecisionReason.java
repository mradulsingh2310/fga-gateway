package io.agentctl.fgagateway.authz;

public enum DecisionReason {
    ALLOWED,
    USER_DENIED,
    AGENT_DENIED,
    CHECK_ERROR
}
