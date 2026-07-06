package io.agentctl.fgagateway.authz;

@FunctionalInterface
public interface FgaCheckClient {
    boolean check(FgaCheck check);
}
