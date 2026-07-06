package io.agentctl.fgagateway.authz;

public record PreflightRequest(
        String tenantId,
        String userId,
        String agentId,
        String runId,
        String toolName,
        String resourceType,
        String resourceId,
        String resourceRelation) {

    public String resourceObject() {
        return resourceType + ":" + resourceId;
    }
}
