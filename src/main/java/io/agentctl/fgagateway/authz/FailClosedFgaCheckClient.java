package io.agentctl.fgagateway.authz;

import org.springframework.stereotype.Component;

@Component
public class FailClosedFgaCheckClient implements FgaCheckClient {
    @Override
    public boolean check(FgaCheck check) {
        return false;
    }
}
