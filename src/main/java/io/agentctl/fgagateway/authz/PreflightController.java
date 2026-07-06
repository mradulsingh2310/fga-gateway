package io.agentctl.fgagateway.authz;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/preflight")
public class PreflightController {
    private final PreflightDecisionService decisions;

    public PreflightController(PreflightDecisionService decisions) {
        this.decisions = decisions;
    }

    @PostMapping
    public PreflightDecision decide(@RequestBody PreflightRequest request) {
        return decisions.decide(request);
    }
}
